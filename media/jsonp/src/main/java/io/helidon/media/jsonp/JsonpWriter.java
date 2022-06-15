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
package io.helidon.media.jsonp;

import java.nio.charset.Charset;
import java.util.concurrent.Flow.Publisher;

import io.helidon.common.GenericType;
import io.helidon.common.http.DataChunk;
import io.helidon.common.http.MediaType;
import io.helidon.common.reactive.Single;
import io.helidon.media.common.CharBuffer;
import io.helidon.media.common.EntitySupport.Writer;
import io.helidon.media.common.EntitySupport.WriterContext;

import jakarta.json.JsonStructure;
import jakarta.json.JsonWriter;
import jakarta.json.JsonWriterFactory;

/**
 * {@link Writer} implementation supporting for {@link JsonStructure} sub-classes (JSON-P).
 */
class JsonpWriter implements Writer<JsonStructure> {

    private final JsonWriterFactory jsonWriterFactory;

    /**
     * Create a new instance.
     *
     * @param jsonWriterFactory json factory
     */
    JsonpWriter(JsonWriterFactory jsonWriterFactory) {
        this.jsonWriterFactory = jsonWriterFactory;
    }

    @Override
    public PredicateResult accept(GenericType<?> type, WriterContext context) {
        return PredicateResult.supports(JsonStructure.class, type);
    }

    @Override
    public <U extends JsonStructure> Publisher<DataChunk> write(Single<U> content,
                                                                GenericType<U> type,
                                                                WriterContext context) {

        MediaType contentType = context.findAccepted(MediaType.JSON_PREDICATE, MediaType.APPLICATION_JSON);
        context.contentType(contentType);
        return content.flatMap(o -> write(jsonWriterFactory, false, o, context.charset()));
    }

    /**
     * Write the given {@link JsonStructure} with the specified {@link JsonWriterFactory}.
     *
     * @param factory writer factory
     * @param flush   {@code true} if the created chunks should be marked as flush
     * @param item    object to convert
     * @param charset charset
     * @return {@link DataChunk} Single
     */
    static Single<DataChunk> write(JsonWriterFactory factory, boolean flush, JsonStructure item, Charset charset) {
        CharBuffer buffer = new CharBuffer();
        try (JsonWriter writer = factory.createWriter(buffer)) {
            writer.write(item);
            return Single.just(DataChunk.create(flush, buffer.encode(charset)));
        }
    }
}
