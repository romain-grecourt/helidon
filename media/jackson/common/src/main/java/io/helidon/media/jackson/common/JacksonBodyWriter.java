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
package io.helidon.media.jackson.common;

import java.io.IOException;
import java.util.Objects;

import io.helidon.common.GenericType;
import io.helidon.common.http.DataChunk;
import io.helidon.common.http.MediaType;
import io.helidon.common.reactive.Flow.Publisher;
import io.helidon.media.common.CharBuffer;
import io.helidon.media.common.ContentWriters;
import io.helidon.media.common.MessageBodyWriter;
import io.helidon.media.common.MessageBodyWriterContext;

import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Message body writer supporting object binding with Jackson.
 */
public final class JacksonBodyWriter implements MessageBodyWriter<Object> {

    private final ObjectMapper objectMapper;

    private JacksonBodyWriter(ObjectMapper objectMapper) {
        Objects.requireNonNull(objectMapper);
        this.objectMapper = objectMapper;
    }

    @Override
    public boolean accept(GenericType<?> type,
            MessageBodyWriterContext context) {

        return !CharSequence.class.isAssignableFrom(type.rawType())
                && objectMapper.canSerialize(type.rawType());
    }

    @Override
    public Publisher<DataChunk> write(Object content,
            GenericType<? extends Object> type,
            MessageBodyWriterContext context) {

        MediaType contentType = context.findAccepted(MediaType.JSON_PREDICATE,
                MediaType.APPLICATION_JSON);
        context.contentType(contentType);
        try {
            CharBuffer buffer = new CharBuffer();
            objectMapper.writeValue(buffer, content);
            return ContentWriters.writeCharBuffer(buffer, context.charset());
        } catch (IOException wrapMe) {
            throw new JacksonRuntimeException(wrapMe.getMessage(), wrapMe);
        }
    }

    /**
     * Create a new {@link JacksonBodyWriter} instance.
     * @param objectMapper object mapper to use
     * @return JacksonBodyWriter
     */
    public static JacksonBodyWriter create(ObjectMapper objectMapper) {
        return new JacksonBodyWriter(objectMapper);
    }
}
