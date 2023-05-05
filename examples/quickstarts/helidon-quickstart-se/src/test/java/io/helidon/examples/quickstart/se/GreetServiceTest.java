/*
 * Copyright (c) 2018, 2023 Oracle and/or its affiliates.
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

package io.helidon.examples.quickstart.se;

import io.helidon.common.http.Http;
import io.helidon.config.Config;
import io.helidon.nima.testing.junit5.webserver.ServerTest;
import io.helidon.nima.testing.junit5.webserver.SetUpRoute;
import io.helidon.nima.webclient.http1.Http1Client;
import io.helidon.nima.webclient.http1.Http1ClientResponse;
import io.helidon.nima.webserver.http.HttpRouting;

import jakarta.json.Json;
import jakarta.json.JsonBuilderFactory;
import jakarta.json.JsonObject;
import org.junit.jupiter.api.Test;

import java.util.Collections;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

@ServerTest
class GreetServiceTest {

    private static final JsonBuilderFactory JSON_BUILDER = Json.createBuilderFactory(Collections.emptyMap());
    private static final JsonObject TEST_JSON_OBJECT = JSON_BUILDER.createObjectBuilder()
                                                                   .add("greeting", "Hola")
                                                                   .build();

    private final Http1Client client;

    GreetServiceTest(Http1Client client) {
        this.client = client;
    }

    @SetUpRoute
    static void routing(HttpRouting.Builder builder) {
        Main.routing(builder, Config.create());
    }

    @Test
    void testHelloWorld() {
        JsonObject jsonObject;

        jsonObject = client.get()
                           .path("/greet")
                           .request(JsonObject.class);
        assertThat(jsonObject.getString("message"), is("Hello World!"));

        jsonObject = client.get()
                           .path("/greet/Joe")
                           .request(JsonObject.class);
        assertThat(jsonObject.getString("message"), is("Hello Joe!"));

        try (Http1ClientResponse response = client.put()
                                                  .path("/greet/greeting")
                                                  .submit(TEST_JSON_OBJECT)) {
            assertThat(response.status().code(), is(204));
        }

        jsonObject = client.get()
                           .path("/greet/Joe")
                           .request(JsonObject.class);
        assertThat(jsonObject.getString("message"), is("Hola Joe!"));
    }

    @Test
    void testHealthObserver() {
        try (Http1ClientResponse response = client.get("/observe/health").request()) {
            assertThat(response.status(), is(Http.Status.NO_CONTENT_204));
        }
    }

    @Test
    void testDeadlockHealthCheck() {
        try (Http1ClientResponse response = client.get("/observe/health/live/deadlock").request()) {
            assertThat(response.status(), is(Http.Status.NO_CONTENT_204));
        }
    }
}
