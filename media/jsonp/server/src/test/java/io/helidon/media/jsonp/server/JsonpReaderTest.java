/*
 * Copyright (c) 2017, 2019 Oracle and/or its affiliates. All rights reserved.
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

package io.helidon.media.jsonp.server;

import io.helidon.common.GenericType;

import javax.json.JsonArray;
import javax.json.JsonException;
import javax.json.JsonObject;

import io.helidon.common.http.DataChunk;
import io.helidon.common.http.MessageBodyReaderContext;
import io.helidon.common.reactive.Flow.Publisher;
import io.helidon.media.jsonp.common.JsonpReader;
import io.helidon.media.jsonp.common.JsonProcessing;

import org.hamcrest.core.Is;
import org.hamcrest.core.IsInstanceOf;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import static io.helidon.common.reactive.ReactiveStreamsAdapter.publisherFromFlow;
import static io.helidon.common.reactive.ReactiveStreamsAdapter.publisherToFlow;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.CoreMatchers.sameInstance;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.fail;
/**
 * The JsonContentReaderTest.
 */
public class JsonpReaderTest {

    private final static MessageBodyReaderContext CONTEXT =
            MessageBodyReaderContext.create();

    private final static JsonpReader READER = JsonProcessing.create()
            .newReader();

    @Test
    public void simpleJsonObject() throws Exception {
        Publisher<DataChunk> publisher = publisherToFlow(
                Flux.just("{ \"p\" : \"val\" }")
                        .map(s -> DataChunk.create(s.getBytes())));

        JsonObject jsonObject = Mono.from(publisherFromFlow(READER
                .read(publisher, GenericType.create(JsonObject.class), CONTEXT)))
                .block();

        assertThat(jsonObject, is(notNullValue()));
        assertThat(jsonObject.getJsonString("p").getString(), Is.is("val"));
    }

    @Test
    public void incompatibleTypes() throws Exception {
        Publisher<DataChunk> publisher = publisherToFlow(
                Flux.just("{ \"p\" : \"val\" }")
                        .map(s -> DataChunk.create(s.getBytes())));

        try {
            JsonArray array = Mono.from(publisherFromFlow(READER
                    .read(publisher, GenericType.create(JsonArray.class), CONTEXT)))
                    .block();
            fail("Should have failed because an expected array is actually an object: " + array);
        } catch (RuntimeException e) {
            assertThat(e.getCause(), IsInstanceOf.instanceOf(ClassCastException.class));
        }
    }

    @Test
    public void simpleJsonArray() throws Exception {
        Publisher<DataChunk> publisher = publisherToFlow(
                Flux.just("[ \"val\" ]")
                        .map(s -> DataChunk.create(s.getBytes())));
        JsonArray array = Mono.from(publisherFromFlow(READER
                .read(publisher, GenericType.create(JsonArray.class), CONTEXT)))
                .block();

        assertThat(array, is(notNullValue()));
        assertThat(array.getString(0), Is.is("val"));
    }

    @Test
    public void invalidJson() throws Exception {
        Publisher<DataChunk> publisher = publisherToFlow(
                Flux.just("{ \"p\" : \"val\" ")
                        .map(s -> DataChunk.create(s.getBytes())));

        try {
            READER.read(publisher, GenericType.create(JsonObject.class), CONTEXT);
            fail("Should have thrown an exception");
        } catch (RuntimeException e) {
            assertThat(e.getCause(), IsInstanceOf.instanceOf(JsonException.class));
        }
    }

    @Test
    public void defaultJsonSupportAsSingleton() {
        assertThat(JsonSupport.create(), sameInstance(JsonSupport.create()));
    }
}
