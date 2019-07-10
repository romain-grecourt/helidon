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

package io.helidon.media.jsonp.common;

import io.helidon.common.GenericType;

import javax.json.JsonArray;
import javax.json.JsonException;
import javax.json.JsonObject;

import io.helidon.common.http.DataChunk;
import io.helidon.common.http.MessageBodyReaderContext;
import io.helidon.common.reactive.Mono;

import org.junit.jupiter.api.Test;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.CoreMatchers.notNullValue;
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
        JsonObject jsonObject = readJsonObject("{ \"p\" : \"val\" }");
        assertThat(jsonObject, is(notNullValue()));
        assertThat(jsonObject.getJsonString("p").getString(), is(equalTo("val")));
    }

    @Test
    public void incompatibleTypes() throws Exception {
        try {
            readJsonArray("{ \"p\" : \"val\" }");
            fail("Should have thrown an exception");
        } catch (Throwable ex) {
            assertThat(ex, is(instanceOf(ClassCastException.class)));
        }
    }

    @Test
    public void simpleJsonArray() throws Exception {
        JsonArray array = readJsonArray("[ \"val\" ]");
        assertThat(array, is(notNullValue()));
        assertThat(array.getString(0), is(equalTo("val")));
    }

    @Test
    public void invalidJson() throws Exception {
        try {
            readJsonObject("{ \"p\" : \"val\" ");
            fail("Should have thrown an exception");
        } catch (IllegalStateException e) {
            assertThat(e.getCause(), is(instanceOf(JsonException.class)));
        }
    }

    private static JsonObject readJsonObject(String json) {
        return READER.read(Mono.just(DataChunk.create(json.getBytes())),
                GenericType.create(JsonObject.class), CONTEXT)
                .block();
    }

    private static JsonArray readJsonArray(String json) {
        return READER.read(Mono.just(DataChunk.create(json.getBytes())),
                GenericType.create(JsonArray.class), CONTEXT)
                .block();
    }
}
