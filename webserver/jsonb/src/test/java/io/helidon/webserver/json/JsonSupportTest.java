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
package io.helidon.webserver.json;

import java.util.concurrent.TimeUnit;
import javax.json.bind.Jsonb;
import javax.json.bind.JsonbBuilder;

import io.helidon.common.http.Http;
import io.helidon.common.http.MediaType;
import io.helidon.webserver.Handler;
import io.helidon.webserver.Routing;
import io.helidon.webserver.jsonb.JsonbSupport;
import io.helidon.webserver.testsupport.MediaPublisher;
import io.helidon.webserver.testsupport.TestClient;
import io.helidon.webserver.testsupport.TestResponse;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Tests {@link JsonSupport}.
 */
public class JsonSupportTest {

    private static final Jsonb JSONB = JsonbBuilder.create();

    @Test
    public void pingPong() throws Exception {
        Routing routing = Routing.builder()
                .register(JsonbSupport.builder().type(Dog.class).build())
                .post("/dog", Handler.of(Dog.class, (req, res, dog) -> res.send(dog)))
                .build();

        Dog dog1 = new Dog();
        dog1.name = "Falco";
        dog1.age = 4;
        dog1.bitable = false;
        String json = JSONB.toJson(dog1, Dog.class);

        TestResponse response = TestClient.create(routing)
                .path("/dog")
                .post(MediaPublisher.of(MediaType.APPLICATION_JSON.withCharset("UTF-8"), json));

        assertEquals(MediaType.APPLICATION_JSON.toString(), response.headers().first(Http.Header.CONTENT_TYPE).orElse(null));

        byte[] bytes = response.asBytes().toCompletableFuture().get(10, TimeUnit.SECONDS);
        Dog dog2 = JSONB.fromJson(new String(bytes, "UTF-8"), Dog.class);
        assertEquals(dog1.name, dog2.name);
        assertEquals(dog1.age, dog2.age);
        assertEquals(dog1.bitable, dog2.bitable);
    }

    @Test
    public void invalidJson() throws Exception {
        Routing routing = Routing.builder()
                .register(JsonbSupport.builder().type(Dog.class).build())
                .post("/dog", Handler.of(Dog.class, (req, res, dog) -> res.send(dog)))
                .build();

        TestResponse response = TestClient.create(routing)
                .path("/dog")
                .post(MediaPublisher.of(MediaType.APPLICATION_JSON.withCharset("UTF-8"), "{ ... invalid ... }"));

        assertEquals(Http.Status.INTERNAL_SERVER_ERROR_500, response.status());
    }

    @Test
    public void explicitJsonSupportRegistrationMissingJsonProperty() throws Exception {
        Routing routing = Routing.builder()
                .post("/dog", Handler.of(Dog.class, (req, res, dog) -> res.send(dog)))
                .build();

        Dog dog1 = new Dog();
        dog1.name = "Falco";
        dog1.age = 4;
        dog1.bitable = false;
        String json = JSONB.toJson(dog1, Dog.class);

        TestResponse response = TestClient.create(routing)
                .path("/dog")
                .post(MediaPublisher.of(MediaType.APPLICATION_JSON.withCharset("UTF-8"), json));

        assertEquals(Http.Status.INTERNAL_SERVER_ERROR_500, response.status());
    }

    @Test
    public void acceptHeaders() throws Exception {
        Routing routing = Routing.builder()
                .register(JsonbSupport.builder().type(Dog.class).build())
                .post("/dog", Handler.of(Dog.class, (req, res, json) -> res.send(json)))
                .build();

        Dog dog1 = new Dog();
        dog1.name = "Falco";
        dog1.age = 4;
        dog1.bitable = false;
        String json = JSONB.toJson(dog1, Dog.class);

        // Has accept
        TestResponse response = TestClient.create(routing)
                .path("/dog")
                .header("Accept", "text/plain; q=.8, application/json; q=.1")
                .post(MediaPublisher.of(MediaType.APPLICATION_JSON.withCharset("UTF-8"), json));
        assertEquals(Http.Status.OK_200, response.status());
        assertEquals(MediaType.APPLICATION_JSON.toString(), response.headers().first(Http.Header.CONTENT_TYPE).orElse(null));

        // Has accept with +json
        response = TestClient.create(routing)
                .path("/dog")
                .header("Accept", "text/plain; q=.8, application/specific+json; q=.1")
                .post(MediaPublisher.of(MediaType.APPLICATION_JSON.withCharset("UTF-8"), json));
        assertEquals(Http.Status.OK_200, response.status());
        assertEquals(MediaType.parse("application/specific+json").toString(),
                     response.headers().first(Http.Header.CONTENT_TYPE).orElse(null));

        // With start
        response = TestClient.create(routing)
                .path("/dog")
                .header("Accept", "text/plain; q=.8, application/*; q=.1")
                .post(MediaPublisher.of(MediaType.APPLICATION_JSON.withCharset("UTF-8"), json));
        assertEquals(Http.Status.OK_200, response.status());
        assertEquals(MediaType.APPLICATION_JSON.toString(), response.headers().first(Http.Header.CONTENT_TYPE).orElse(null));

        // With JOSNP standard application/javascript
        response = TestClient.create(routing)
                .path("/dog")
                .header("Accept", "application/javascript")
                .post(MediaPublisher.of(MediaType.APPLICATION_JSON.withCharset("UTF-8"), json));
        assertEquals(Http.Status.OK_200, response.status());
        assertEquals("application/javascript", response.headers().first(Http.Header.CONTENT_TYPE).orElse(null));

        // Without start
        response = TestClient.create(routing)
                .path("/dog")
                .header("Accept", "text/plain; q=.8, application/specific; q=.1")
                .post(MediaPublisher.of(MediaType.APPLICATION_JSON.withCharset("UTF-8"), json));
        assertEquals(Http.Status.INTERNAL_SERVER_ERROR_500, response.status());
    }
}
