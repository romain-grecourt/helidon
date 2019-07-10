/*
 * Copyright (c) 2018 Oracle and/or its affiliates. All rights reserved.
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

package io.helidon.webserver.examples.streaming;

import javax.json.Json;
import javax.json.JsonBuilderFactory;
import javax.json.JsonObject;
import javax.json.JsonWriterFactory;
import java.net.URISyntaxException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.logging.Logger;
import java.util.logging.Level;

import io.helidon.common.reactive.Flow;
import io.helidon.media.jsonp.common.JsonpArrayStreamWriter;
import io.helidon.media.jsonp.common.JsonpLineDelimitedStreamWriter;
import io.helidon.webserver.Routing;
import io.helidon.webserver.ServerRequest;
import io.helidon.webserver.ServerResponse;
import io.helidon.webserver.Service;

import static io.helidon.webserver.examples.streaming.Main.LARGE_FILE_RESOURCE;

/**
 * StreamingService class. Uses a {@code Subscriber<DataChunk>} and a
 * {@code Publisher<DataChunk>} for uploading and downloading files.
 */
public class StreamingService implements Service {

    private static final Logger LOGGER =
            Logger.getLogger(StreamingService.class.getName());

    private static final JsonBuilderFactory JSON =
            Json.createBuilderFactory(Collections.emptyMap());

    private static final JsonWriterFactory JSON_WRITER_FACTORY =
            Json.createWriterFactory(Collections.emptyMap());

    private static final JsonpArrayStreamWriter ARRAY_WRITER =
            new JsonpArrayStreamWriter(JSON_WRITER_FACTORY);

    private static final JsonpLineDelimitedStreamWriter LINE_WRITER =
            new JsonpLineDelimitedStreamWriter(JSON_WRITER_FACTORY);

    private final Path filePath;

    StreamingService() {
        try {
            filePath = Paths.get(getClass().getResource(LARGE_FILE_RESOURCE).toURI());
        } catch (URISyntaxException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void update(Routing.Rules routingRules) {
        routingRules.get("/download", this::download)
                .post("/upload", this::upload)
                .get("/downloadJson", this::downloadJson);
    }

    private void upload(ServerRequest request, ServerResponse response) {
        LOGGER.log(Level.INFO, "Entering upload ... {0}", Thread.currentThread());
        request.content().subscribe(new ServerFileWriter(response));
        LOGGER.info("Exiting upload ...");
    }

    private void download(ServerRequest request, ServerResponse response) {
        LOGGER.log(Level.INFO, "Entering download ...{0}", Thread.currentThread());
        response.send(new ServerFileReader(filePath));
        LOGGER.info("Exiting download ...");
    }

    private void downloadJson(ServerRequest request, ServerResponse response) {
        // Register stream writers -- should be moved to JsonSupport
        response.registerWriter(ARRAY_WRITER);
        response.registerWriter(LINE_WRITER);

        request.content().asStream(JsonObject.class).subscribe(
                new Flow.Subscriber<JsonObject>() {
                    @Override
                    public void onSubscribe(Flow.Subscription subscription) {
                    }

                    @Override
                    public void onNext(JsonObject item) {
                    }

                    @Override
                    public void onError(Throwable throwable) {
                    }

                    @Override
                    public void onComplete() {
                    }
                });

        // Create JSON object
        JsonObject msg = JSON.createObjectBuilder()
                .add("message", "This is a message")
                .build();

        // Produce response as stream of objects
        response.send(subscriber -> subscriber.onSubscribe(
                new Flow.Subscription() {
                    @Override
                    public void request(long n) {
                        for (int i = 0; i < 10; i++) {
                            subscriber.onNext(msg);
                        }
                        subscriber.onComplete();
                    }

                    @Override
                    public void cancel() {
                    }
                }), JsonObject.class);
    }
}
