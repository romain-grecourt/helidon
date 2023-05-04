/*
 * Copyright (c) 2021, 2023 Oracle and/or its affiliates.
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
package io.helidon.lra.coordinator;

import java.net.URI;
import java.util.Map;

import io.helidon.common.LazyValue;
import io.helidon.config.Config;
import io.helidon.config.ConfigSources;
import io.helidon.nima.http.media.MediaContext;
import io.helidon.nima.http.media.jsonp.JsonpSupport;
import io.helidon.nima.testing.junit5.webserver.ServerTest;
import io.helidon.nima.testing.junit5.webserver.SetUpServer;
import io.helidon.nima.webclient.http1.Http1Client;
import io.helidon.nima.webserver.WebServer;
import io.helidon.nima.webserver.http.HttpRouting;

import jakarta.json.JsonArray;
import org.eclipse.microprofile.lra.annotation.LRAStatus;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Test;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

@ServerTest
@SuppressWarnings("resource")
public class CoordinatorTest {

    private static final String CONTEXT_PATH = "/test";
    private static final String COORDINATOR_ROUTING_NAME = "coordinator";
    private static final Map<String, String> CONFIG = Map.of(
            "helidon.lra.coordinator.db.connection.url", "jdbc:h2:file:./target/lra-coordinator");

    private static WebServer server;
    private static Http1Client webClient;
    private static CoordinatorService coordinatorService;

    CoordinatorTest(WebServer server) {
        this.server = server;
        this.webClient = Http1Client.builder()
                                    //.keepAlive(false)
                                    .baseUri("http://localhost:" + server.port(COORDINATOR_ROUTING_NAME) + "/lra-coordinator")
                                    .build();
    }

    @SetUpServer
    static void setUpServer(WebServer.Builder serverBuilder) {
        LazyValue<URI> coordinatorUri = LazyValue.create(() ->
                URI.create("http://localhost:" + server.port(COORDINATOR_ROUTING_NAME) + "/lra-coordinator"));

        coordinatorService = CoordinatorService.builder()
                                               .url(coordinatorUri::get)
                                               .config(Config.builder()
                                                             .addSource(ConfigSources.create(CONFIG))
                                                             .addSource(ConfigSources.classpath("application.yaml"))
                                                             .build()
                                                             .get(CoordinatorService.CONFIG_PREFIX))
                                               .build();
        serverBuilder
                .shutdownHook(true)
                .host("localhost")
                .routing(r -> r.register(CONTEXT_PATH, () -> rules -> rules.put((req, res) -> res.send())))
                .socket(COORDINATOR_ROUTING_NAME, (socket, routing) -> {
                    socket.port(0);
                    routing.addRouting(HttpRouting.builder().register("/lra-coordinator", coordinatorService));
                });
    }

    @AfterAll
    static void afterAll() {
        if (coordinatorService != null) {
            coordinatorService.shutdown();
        }
    }

    @Test
    void startAndClose() {
        String lraId = start();

        assertThat(getParsedStatusOfLra(lraId), is(LRAStatus.Active));
        assertThat(status(lraId), is(LRAStatus.Active));

        close(lraId);

        assertThat(getParsedStatusOfLra(lraId), is(LRAStatus.Closed));
        assertThat(status(lraId), is(LRAStatus.Closed));
    }

    @Test
    void startAndCancel() {
        String lraId = start();

        assertThat(getParsedStatusOfLra(lraId), is(LRAStatus.Active));
        assertThat(status(lraId), is(LRAStatus.Active));

        close(lraId);

        assertThat(getParsedStatusOfLra(lraId), is(LRAStatus.Closed));
        assertThat(status(lraId), is(LRAStatus.Closed));
    }

    private String start() {
        return webClient
                .post()
                .path("start")
                .request()
                .entity()
                .as(String.class);
    }

    private LRAStatus getParsedStatusOfLra(String lraId) {
        Http1Client client = Http1Client.builder()
                                        .mediaContext(MediaContext.builder()
                                                                  .addMediaSupport(JsonpSupport.create())
                                                                  .build())
                                        // Lra id is already whole url
                                        .baseUri(lraId)
                                        .build();

        return LRAStatus.valueOf(client.get()
                                       .request()
                                       .entity()
                                       .as(JsonArray.class)
                                       .get(0).asJsonObject()
                                       .getString("status"));
    }

    private LRAStatus status(String lraId) {
        Http1Client client = Http1Client.builder()
                                        // Lra id is already whole url
                                        .baseUri(lraId + "/status")
                                        .build();

        return LRAStatus.valueOf(client.get()
                                       .request()
                                       .entity()
                                       .as(String.class));
    }

    private void close(String lraId) {
        Http1Client.builder()
                   // Lra id is already whole url
                   .baseUri(lraId + "/close")
                   .build()
                   .put()
                   .request();
    }

    private void cancel(String lraId) {
        Http1Client.builder()
                   // Lra id is already whole url
                   .baseUri(lraId + "/cancel")
                   .build()
                   .put()
                   .request();
    }
}
