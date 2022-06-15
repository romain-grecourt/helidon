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
package io.helidon.media.jsonb;

import java.nio.charset.Charset;
import java.util.Objects;
import java.util.concurrent.Flow.Publisher;

import io.helidon.common.GenericType;
import io.helidon.common.http.DataChunk;
import io.helidon.common.http.MediaType;
import io.helidon.common.reactive.Single;
import io.helidon.media.common.CharBuffer;
import io.helidon.media.common.ContentWriters;
import io.helidon.media.common.MediaContext.WriterContext;
import io.helidon.media.common.MediaSupport.PredicateResult;
import io.helidon.media.common.MediaSupport.Writer;

import jakarta.json.bind.Jsonb;

/**
 * {@link Writer} implementation supporting object binding with JSON-B.
 */
class JsonbWriter implements Writer<Object> {

    private final Jsonb jsonb;

    /**
     * Create a new instance.
     *
     * @param jsonb JSON-B instance
     */
    JsonbWriter(Jsonb jsonb) {
        Objects.requireNonNull(jsonb);
        this.jsonb = jsonb;
    }

    @Override
    public PredicateResult accept(GenericType<?> type, WriterContext context) {
        return !CharSequence.class.isAssignableFrom(type.rawType())
                ? PredicateResult.COMPATIBLE
                : PredicateResult.NOT_SUPPORTED;
    }

    @Override
    public <U> Publisher<DataChunk> write(Single<U> content, GenericType<U> type, WriterContext context) {
        MediaType contentType = context.findAccepted(MediaType.JSON_PREDICATE, MediaType.APPLICATION_JSON);
        context.contentType(contentType);
        return content.flatMap(o -> write(jsonb, o, context.charset()));
    }

    /**
     * Write the given object with the specified {@link Jsonb}.
     *
     * @param jsonb   jsonb
     * @param content object to convert
     * @param charset charset
     * @return {@link DataChunk} Single
     */
    static Single<DataChunk> write(Jsonb jsonb, Object content, Charset charset) {
        CharBuffer buffer = new CharBuffer();
        jsonb.toJson(content, buffer);
        return ContentWriters.writeCharBuffer(buffer, charset);
    }
}
