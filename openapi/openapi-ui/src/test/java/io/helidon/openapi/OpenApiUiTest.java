/*
 * Copyright (c) 2022, 2023 Oracle and/or its affiliates.
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
package io.helidon.openapi;

import java.util.Map;

import io.helidon.common.media.type.MediaType;
import io.helidon.common.media.type.MediaTypes;
import io.helidon.http.Http;
import io.helidon.http.HttpMediaType;
import io.helidon.webclient.api.HttpClientResponse;
import io.helidon.webclient.api.WebClient;
import io.helidon.webserver.WebServerConfig;
import io.helidon.webserver.testing.junit5.ServerTest;
import io.helidon.webserver.testing.junit5.SetUpServer;
import io.helidon.webserver.testing.junit5.Socket;

import org.junit.jupiter.api.Test;

import static io.helidon.common.testing.junit5.OptionalMatcher.optionalValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

/**
 * Tests {@link OpenApiUi}.
 */
@ServerTest
class OpenApiUiTest {

    private static final String GREETING_OPENAPI_PATH = "/openapi-greeting";

    private static final MediaType[] SIMULATED_BROWSER_ACCEPT = new MediaType[] {
            MediaTypes.TEXT_HTML,
            MediaTypes.APPLICATION_XHTML_XML,
            HttpMediaType.builder()
                    .mediaType(MediaTypes.APPLICATION_XML)
                    .parameters(Map.of("q", "0.9"))
                    .build(),
            MediaTypes.create("image", "webp"),
            MediaTypes.create("image", "apng"),
            HttpMediaType.builder()
                    .mediaType(MediaTypes.WILDCARD)
                    .parameters(Map.of("q", "0.8"))
                    .build()
    };

    private final WebClient client;
    private final WebClient altClient;

    OpenApiUiTest(WebClient client, @Socket("alt") WebClient altClient) {
        this.client = client;
        this.altClient = altClient;
    }

    @SetUpServer
    public static void setup(WebServerConfig.Builder server) {
        server.routing(routing -> routing
                        .addFeature(OpenApiFeature.builder()
                                            .servicesDiscoverServices(false)
                                            .staticFile("src/test/resources/greeting.yml")
                                            .webContext("/openapi-greeting")
                                            .cors(cors -> cors.enabled(false))
                                            .addService(OpenApiUi.create())))
                .putSocket("alt", socket -> socket
                        .routing(routing -> routing
                                .addFeature(OpenApiFeature.builder()
                                                    .servicesDiscoverServices(false)
                                                    .staticFile("src/test/resources/time.yml")
                                                    .cors(cors -> cors.enabled(false))
                                                    .addService(OpenApiUi.create()))));
    }

    @Test
    void checkNoOpUi() {
        try (HttpClientResponse response = client.get("/openapi-greeting/ui")
                .accept(MediaTypes.TEXT_HTML)
                .request()) {

            assertThat(response.status(), is(Http.Status.NOT_FOUND_404));
        }
    }

    @Test
    void checkSimulatedBrowserAccessToMainEndpoint() {
        try (HttpClientResponse response = client.get(GREETING_OPENAPI_PATH)
                .accept(SIMULATED_BROWSER_ACCEPT)
                .request()) {

            assertThat(response.status(), is(Http.Status.OK_200));
            assertThat(response.headers()
                               .contentType()
                               .map(HttpMediaType::mediaType), optionalValue(is(MediaTypes.APPLICATION_OPENAPI_YAML)));
        }
    }

    @Test
    void checkAlternateUiWebContext() {
        try (HttpClientResponse response = altClient.get("/my-ui")
                .accept(MediaTypes.TEXT_PLAIN)
                .request()) {
            assertThat(response.status(), is(Http.Status.NOT_FOUND_404));
        }
    }
}