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

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

import io.helidon.common.http.DataChunk;
import io.helidon.webserver.jsonb.JsonbSupport;

import org.hamcrest.core.Is;
import org.hamcrest.core.IsInstanceOf;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Flux;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;
import static io.helidon.common.reactive.ReactiveStreamsAdapter.publisherToFlow;
import javax.json.JsonException;
import javax.json.bind.JsonbException;

/**
 * The JsonbContentReaderTest.
 */
public class JsonbContentReaderTest {

    @Test
    public void simpleJsonObject() throws Exception {
        Flux<DataChunk> flux = Flux
                .just("{\"age\":4,\"bitable\":false,\"name\":\"Falco\"}")
                .map(s -> DataChunk.create(s.getBytes()));

        JsonbSupport jsonbSupport = JsonbSupport
                .builder()
                .type(Dog.class)
                .build();

        CompletionStage<? extends Dog> stage = jsonbSupport
                .reader()
                .applyAndCast(publisherToFlow(flux), Dog.class);

        Dog dog = stage.toCompletableFuture().get(10, TimeUnit.SECONDS);
        assertThat(dog.age, Is.is(4));
    }

    @Test
    public void simpleJsonArray() throws Exception {
        Flux<DataChunk> flux = Flux
                .just("[{\"age\":4,\"bitable\":false,\"name\":\"Falco\"}]")
                .map(s -> DataChunk.create(s.getBytes()));

        JsonbSupport jsonbSupport = JsonbSupport
                .builder()
                .type(List.class)
                .runtimeType(new ArrayList<Dog>(){}.getClass().getGenericSuperclass())
                .build();

        CompletionStage<? extends List<Dog>> stage = jsonbSupport
                .reader()
                .applyAndCast(publisherToFlow(flux), List.class);

        List<Dog> dogs = stage.toCompletableFuture().get(10, TimeUnit.SECONDS);
        assertNotNull(dogs);
        assertThat(dogs.get(0).age, Is.is(4));
    }

    @Test
    public void incompatibleTypes() throws Exception {
        Flux<DataChunk> flux = Flux
                .just("{\"age\":4,\"bitable\":false,\"name\":\"Falco\"}")
                .map(s -> DataChunk.create(s.getBytes()));

        JsonbSupport jsonbSupport = JsonbSupport
                .builder()
                .type(List.class)
                .runtimeType(new ArrayList<Dog>(){}.getClass().getGenericSuperclass())
                .build();

        CompletionStage<? extends List<Dog>> stage = jsonbSupport
                .reader()
                .applyAndCast(publisherToFlow(flux), List.class);

        try {
            List<Dog> dogs = stage.thenApply(o -> {
                fail("Shouldn't occur because of a class cast exception!");
                return o;
            }).toCompletableFuture().get(10, TimeUnit.SECONDS);
            fail("Should have failed because the input is an object, not an array");
        } catch (ExecutionException e) {
            assertThat(e.getCause(), IsInstanceOf.instanceOf(JsonbException.class));
        }
    }

    @Test
    public void invalidJson() throws Exception {
        Flux<DataChunk> flux = Flux
                .just("{\"age\":4,\"bitable\":false,\"name\":\"Falco")
                .map(s -> DataChunk.create(s.getBytes()));

        JsonbSupport jsonbSupport = JsonbSupport
                .builder()
                .type(Dog.class)
                .build();

        CompletionStage<? extends Dog> stage = jsonbSupport
                .reader()
                .applyAndCast(publisherToFlow(flux), Dog.class);
        try {
            stage.toCompletableFuture().get(10, TimeUnit.SECONDS);
            fail("Should have thrown an exception");
        } catch (ExecutionException e) {
            assertTrue(stage.toCompletableFuture().isCompletedExceptionally());
            assertThat(e.getCause(), IsInstanceOf.instanceOf(JsonException.class));
        }
    }
}
