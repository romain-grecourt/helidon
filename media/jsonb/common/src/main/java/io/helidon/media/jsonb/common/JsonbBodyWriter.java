/*
 * Copyright (c) 2019 Oracle and/or its affiliates. All rights reserved.
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
package io.helidon.media.jsonb.common;

import java.util.Objects;

import javax.json.bind.Jsonb;

import io.helidon.common.GenericType;
import io.helidon.common.http.DataChunk;
import io.helidon.common.http.MediaType;
import io.helidon.common.reactive.Flow.Publisher;
import io.helidon.media.common.CharBuffer;
import io.helidon.media.common.ContentWriters;
import io.helidon.media.common.MessageBodyWriter;
import io.helidon.media.common.MessageBodyWriterContext;

/**
 * Message body writer supporting object binding with JSON-B.
 */
public class JsonbBodyWriter implements MessageBodyWriter<Object> {

    private final Jsonb jsonb;

    private JsonbBodyWriter(Jsonb jsonb) {
        Objects.requireNonNull(jsonb);
        this.jsonb = jsonb;
    }

    @Override
    public boolean accept(GenericType<?> type,
            MessageBodyWriterContext context) {

        return !CharSequence.class.isAssignableFrom(type.rawType());
    }

    @Override
    public Publisher<DataChunk> write(Object content,
            GenericType<? extends Object> type,
            MessageBodyWriterContext context) {

        MediaType contentType = context.findAccepted(MediaType.JSON_PREDICATE,
                MediaType.APPLICATION_JSON);
        context.contentType(contentType);
        CharBuffer buffer = new CharBuffer();
        jsonb.toJson(content, buffer);
        return ContentWriters.writeCharBuffer(buffer, context.charset());
    }

    /**
     * Create a new {@link JsonbBodyWriter} instance.
     *
     * @param jsonb JSON-B instance
     * @return JsonbBodyWriter
     */
    public static JsonbBodyWriter create(Jsonb jsonb) {
        return new JsonbBodyWriter(jsonb);
    }
}
