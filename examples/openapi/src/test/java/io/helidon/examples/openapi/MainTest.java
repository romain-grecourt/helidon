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

package io.helidon.examples.openapi;

import java.util.Collections;

import io.helidon.common.http.HttpMediaType;
import io.helidon.config.Config;
import io.helidon.examples.openapi.internal.SimpleAPIModelReader;
import io.helidon.nima.testing.junit5.webserver.ServerTest;
import io.helidon.nima.testing.junit5.webserver.SetUpServer;
import io.helidon.nima.webclient.http1.Http1Client;
import io.helidon.nima.webclient.http1.Http1ClientResponse;
import io.helidon.nima.webserver.WebServer;

import jakarta.json.Json;
import jakarta.json.JsonBuilderFactory;
import jakarta.json.JsonObject;
import jakarta.json.JsonPointer;
import jakarta.json.JsonString;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

@Disabled("3.0.0-JAKARTA") // OpenAPI: org.yaml.snakeyaml.constructor.ConstructorException:
// Cannot create property=paths for JavaBean=io.smallrye.openapi.api.models.OpenAPIImpl@5dcd8c7a
@ServerTest
public class MainTest {

    private final Http1Client client;

    private static final JsonBuilderFactory JSON_BF = Json.createBuilderFactory(Collections.emptyMap());
    private static final JsonObject TEST_JSON_OBJECT;

    static {
        TEST_JSON_OBJECT = JSON_BF.createObjectBuilder()
                                  .add("greeting", "Hola")
                                  .build();
    }

    MainTest(Http1Client client) {
        this.client = client;
    }

    @SetUpServer
    public static void setup(WebServer.Builder builder) {
        builder.routing(routing -> Main.routing(routing, Config.create()));
    }

    @Test
    public void testHelloWorld() {
        assertThat(client.get()
                         .path("/greet")
                         .request(JsonObject.class)
                         .getString("greeting"), is("Hello World!"));

        assertThat(client.get()
                         .path("/greet/Joe")
                         .request(JsonObject.class)
                         .getString("greeting"), is("Hello World!"));

        try (Http1ClientResponse response = client.put()
                                                  .path("/greet/greeting")
                                                  .submit(TEST_JSON_OBJECT)) {

            assertThat(response.status().code(), is(204));

        }

        assertThat(client.get()
                         .path("/greet/Joe")
                         .request(JsonObject.class)
                         .getString("greeting"), is("Hola Joe!"));

        try (Http1ClientResponse response = client.get()
                                                  .path("/observe/health")
                                                  .request()) {

            assertThat(response.status().code(), is(200));
        }


        try (Http1ClientResponse response = client.get()
                                                  .path("/metrics")
                                                  .request()) {

            assertThat(response.status().code(), is(200));
        }
    }

    @Test
    public void testOpenAPI() {
        /*
         * If you change the OpenAPI endpoint path in application.yaml, then
         * change the following path also.
         */
        JsonObject jsonObject = client.get()
                                      .accept(HttpMediaType.APPLICATION_JSON)
                                      .path("/openapi")
                                      .request(JsonObject.class);
        JsonObject paths = jsonObject.getJsonObject("paths");

        JsonPointer jp = Json.createPointer("/" + escape("/greet/greeting") + "/put/summary");
        JsonString js = (JsonString) jp.getValue(paths);
        assertThat("/greet/greeting.put.summary not as expected", js.getString(), is("Set the greeting prefix"));

        jp = Json.createPointer("/" + escape(SimpleAPIModelReader.MODEL_READER_PATH)
                + "/get/summary");
        js = (JsonString) jp.getValue(paths);
        assertThat("summary added by model reader does not match", js.getString(),
                is(SimpleAPIModelReader.SUMMARY));

        jp = Json.createPointer("/" + escape(SimpleAPIModelReader.DOOMED_PATH));
        assertThat("/test/doomed should not appear but does", jp.containsValue(paths), is(false));
    }

    private static String escape(String path) {
        return path.replace("/", "~1");
    }

}
