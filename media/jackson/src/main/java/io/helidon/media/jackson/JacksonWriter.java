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
package io.helidon.media.jackson;

import java.io.IOException;
import java.nio.charset.Charset;
import java.util.Objects;
import java.util.concurrent.Flow.Publisher;

import io.helidon.common.GenericType;
import io.helidon.common.http.DataChunk;
import io.helidon.common.http.MediaType;
import io.helidon.common.reactive.Single;
import io.helidon.media.common.CharBuffer;
import io.helidon.media.common.ContentWriters;
import io.helidon.media.common.EntitySupport.Writer;
import io.helidon.media.common.EntitySupport.WriterContext;

import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * {@link Writer} implementation that supports object binding with Jackson.
 */
final class JacksonWriter implements Writer<Object> {

    private final ObjectMapper objectMapper;

    /**
     * Create a new instance.
     *
     * @param objectMapper object mapper to use
     */
    JacksonWriter(ObjectMapper objectMapper) {
        Objects.requireNonNull(objectMapper);
        this.objectMapper = objectMapper;
    }

    @Override
    public PredicateResult accept(GenericType<?> type, WriterContext context) {
        return !CharSequence.class.isAssignableFrom(type.rawType())
                && objectMapper.canSerialize(type.rawType())
                ? PredicateResult.COMPATIBLE
                : PredicateResult.NOT_SUPPORTED;
    }

    @Override
    public <U> Publisher<DataChunk> write(Single<U> content, GenericType<U> type, WriterContext context) {
        MediaType contentType = context.findAccepted(MediaType.JSON_PREDICATE, MediaType.APPLICATION_JSON);
        context.contentType(contentType);
        return content.flatMap(o -> write(objectMapper, o, context.charset()));
    }

    /**
     * Write the given object with the specified object mapper.
     *
     * @param objectMapper object mapper
     * @param content      object to convert
     * @param charset      charset
     * @return {@link DataChunk} Single
     */
    static Single<DataChunk> write(ObjectMapper objectMapper, Object content, Charset charset) {
        try {
            CharBuffer buffer = new CharBuffer();
            objectMapper.writeValue(buffer, content);
            return ContentWriters.writeCharBuffer(buffer, charset);
        } catch (IOException wrapMe) {
            throw new JacksonRuntimeException(wrapMe.getMessage(), wrapMe);
        }
    }
}
