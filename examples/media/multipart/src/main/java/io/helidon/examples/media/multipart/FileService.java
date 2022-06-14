/*
 * Copyright (c) 2020, 2022 Oracle and/or its affiliates.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.helidon.examples.media.multipart;

import java.nio.file.Path;
import java.util.Map;
import java.util.concurrent.ExecutorService;

import io.helidon.common.configurable.ThreadPoolSupplier;
import io.helidon.common.http.Http;
import io.helidon.common.http.MediaType;
import io.helidon.media.multipart.BodyPart;
import io.helidon.media.multipart.ContentDisposition;
import io.helidon.media.multipart.MultiPartSupport;
import io.helidon.webclient.WebClient;
import io.helidon.webserver.BadRequestException;
import io.helidon.webserver.ResponseHeaders;
import io.helidon.webserver.Routing;
import io.helidon.webserver.ServerRequest;
import io.helidon.webserver.ServerResponse;
import io.helidon.webserver.Service;

import jakarta.json.Json;
import jakarta.json.JsonArrayBuilder;
import jakarta.json.JsonBuilderFactory;

/**
 * File service.
 */
public final class FileService implements Service {

    private static final JsonBuilderFactory JSON_FACTORY = Json.createBuilderFactory(Map.of());
    private final FileStorage storage;
    private final WebClient webClient;
    private final ExecutorService executor = ThreadPoolSupplier.create("multipart-thread-pool").get();

    /**
     * Create a new file upload service instance.
     */
    FileService() {
        storage = new FileStorage();
        webClient = WebClient.builder()
                             .baseUri("http://localhost:8080/api")
                             .addMediaSupport(MultiPartSupport.create())
                             .build();
    }

    @Override
    public void update(Routing.Rules rules) {
        rules.get("/", this::list)
             .get("/{filename}", this::download)
             .post("/re-upload", this::reUpload)
             .post("/", this::upload);
    }

    private void list(ServerRequest req, ServerResponse res) {
        JsonArrayBuilder arrayBuilder = JSON_FACTORY.createArrayBuilder();
        storage.listFiles().forEach(arrayBuilder::add);
        res.send(JSON_FACTORY.createObjectBuilder().add("files", arrayBuilder).build());
    }

    private void download(ServerRequest req, ServerResponse res) {
        Path filePath = storage.lookup(req.path().param("filename"));
        ResponseHeaders headers = res.headers();
        headers.contentType(MediaType.APPLICATION_OCTET_STREAM);
        headers.put(Http.Header.CONTENT_DISPOSITION, ContentDisposition.builder()
                                                                       .filename(filePath.getFileName().toString())
                                                                       .build()
                                                                       .toString());
        res.send(filePath);
    }

    private void reUpload(ServerRequest req, ServerResponse res) {
        webClient.post()
                 .path("/")
                 .submitStream(req.content().asStream(BodyPart.class), BodyPart.class)
                 .onError(res::send)
                 .forSingle(clientRes -> res.addHeaders(clientRes.headers())
                                            .status(clientRes.status())
                                            .send(clientRes.content()));
    }

    private void upload(ServerRequest req, ServerResponse res) {
        req.content()
           .asStream(BodyPart.class)
           .forEach(part -> {
               if (part.isNamed("file[]")) {
                   part.content()
                       .writeToFile(part.filename()
                                        .map(storage::create)
                                        .orElseThrow(() -> new BadRequestException("no filename")), executor);
               } else {
                   // unconsumed parts needs to be drained
                   part.content().drain();
               }
           })
           .onError(res::send)
           .onComplete(() -> {
               res.status(Http.Status.MOVED_PERMANENTLY_301);
               res.headers().put(Http.Header.LOCATION, "/ui");
               res.send();
           }).ignoreElement();
    }
}
