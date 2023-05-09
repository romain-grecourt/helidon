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
package io.helidon.tests.integration.webclient;

import java.io.IOException;
import java.io.InputStream;
import java.util.Collections;

import io.helidon.nima.http.media.MediaContext;
import io.helidon.nima.http.media.jsonp.JsonpSupport;
import io.helidon.nima.webclient.http1.Http1Client;
import io.helidon.nima.webclient.http1.Http1ClientRequest;
import io.helidon.nima.webclient.http1.Http1ClientResponse;
import io.helidon.nima.webserver.WebServer;

import jakarta.json.Json;
import jakarta.json.JsonBuilderFactory;
import jakarta.json.JsonObject;
import org.junit.jupiter.api.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.fail;

/**
 * Tests for MediaContext functionality in WebClient.
 */
public class MediaContextTest extends TestParent {

    private static final JsonBuilderFactory JSON_BUILDER = Json.createBuilderFactory(Collections.emptyMap());
    private static final String DEFAULT_GREETING;
    private static final JsonObject JSON_GREETING;
    private static final JsonObject JSON_NEW_GREETING;
    private static final JsonObject JSON_OLD_GREETING;

    static {
        DEFAULT_GREETING = CONFIG.get("app.greeting").asString().orElse("Hello");

        JSON_GREETING = JSON_BUILDER.createObjectBuilder()
                                    .add("message", DEFAULT_GREETING + " World!")
                                    .build();

        JSON_NEW_GREETING = JSON_BUILDER.createObjectBuilder()
                                        .add("greeting", "Hola")
                                        .build();
        JSON_OLD_GREETING = JSON_BUILDER.createObjectBuilder()
                                        .add("greeting", CONFIG.get("app.greeting").asString().orElse("Hello"))
                                        .build();
    }

    MediaContextTest(WebServer server, Http1Client client) {
        super(server, client);
    }

    @Test
    public void testMediaSupportDefaults() {
        Http1Client client = Http1Client.builder()
                                        .baseUri("http://localhost:" + server.port() + "/greet")
                                        .build();

        String greeting = client.get().request(String.class);
        assertThat(greeting, is(JSON_GREETING.toString()));
    }

    @Test
    public void testMediaSupportWithoutDefaults() {
        Http1Client client = Http1Client.builder()
                                        .baseUri("http://localhost:" + server.port() + "/greet")
                                        .mediaContext(MediaContext.builder()
                                                                  .discoverServices(false)
                                                                  .build())
                                        .build();

        IllegalStateException ex = assertThrows(IllegalStateException.class, () -> {
            client.get().request(String.class);
            fail("No reader for String should be registered!");
        });
        assertThat(ex.getMessage(), is("No reader found for type: class java.lang.String"));
    }

    @Test
    public void testReaderRegisteredOnClient() {
        Http1Client client = Http1Client.builder()
                                        .baseUri("http://localhost:" + server.port() + "/greet")
                                        .mediaContext(MediaContext.builder()
                                                                  .addMediaSupport(JsonpSupport.create())
                                                                  .discoverServices(false)
                                                                  .build())
                                        .build();

        JsonObject jsonObject = client.get().request(JsonObject.class);
        assertThat(jsonObject, is(JSON_GREETING));

        IllegalStateException ex = assertThrows(IllegalStateException.class, () -> {
            try (Http1ClientResponse ignored = client.put()
                                                     .path("/greeting")
                                                     .submit(JSON_NEW_GREETING)) {
                fail("No writer for String should be registered!");
            }
        });
        assertThat(ex.getCause().getMessage(), is("Transformation failed!"));
    }

    @Test
    public void testWriterRegisteredOnClient() {
        Http1Client client = Http1Client.builder()
                                        .baseUri("http://localhost:" + server.port() + "/greet")
                                        .mediaContext(MediaContext.builder()
                                                                  .addMediaSupport(JsonpSupport.create())
                                                                  .discoverServices(false)
                                                                  .build())
                                        .build();

        // TODO can't register just a writer, so the test will always fail with the current API.

        IllegalStateException ex = assertThrows(IllegalStateException.class, () -> {
            try (Http1ClientResponse ignored = client.put()
                                                     .path("/greeting")
                                                     .submit(JSON_NEW_GREETING)) {
                fail("JsonReader should not be registered!");
            }
        });
        assertThat(ex.getMessage(), is("No reader found for type: interface jakarta.json.JsonObject"));

        try (Http1ClientResponse res = client.put().path("/greeting").submit(JSON_OLD_GREETING)) {
            assertThat(res.status().code(), is(200));
        }
    }

    @Test
    public void testRequestSpecificReader() {
        Http1Client client = Http1Client.builder()
                                        .baseUri("http://localhost:" + server.port() + "/greet")
                                        .build();

        IllegalStateException ex = assertThrows(IllegalStateException.class, () -> {
            client.get().request(JsonObject.class);
            fail("JsonObject should not have been handled.");
        });
        assertThat(ex.getMessage(), is("No reader found for type: interface jakarta.json.JsonObject"));

        Http1ClientRequest req = client.get();
//        req.readerContext().registerReader(JsonpSupport.reader());
        JsonObject jsonObject = req.request(JsonObject.class);
        assertThat(jsonObject.getString("message"), is(DEFAULT_GREETING + " World!"));
    }

    @Test
    public void testInputStreamDifferentThreadContentAs() throws IOException {
        try (Http1ClientResponse res = client.get().request()) {
            InputStream is = res.inputStream();
            assertThat(new String(is.readAllBytes()), is("{\"message\":\"Hello World!\"}"));
        }
    }

}
