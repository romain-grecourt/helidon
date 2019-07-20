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
import java.lang.reflect.Type;
import java.util.Objects;

import io.helidon.common.GenericType;
import io.helidon.common.http.DataChunk;
import io.helidon.common.reactive.Flow.Publisher;
import io.helidon.common.reactive.Mono;
import io.helidon.common.reactive.MonoMapper;
import io.helidon.media.common.ContentReaders;
import io.helidon.media.common.MessageBodyReader;
import io.helidon.media.common.MessageBodyReaderContext;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Message body reader supporting object binding with Jackson.
 */
public final class JacksonBodyReader implements MessageBodyReader<Object> {

    private final ObjectMapper objectMapper;

    private JacksonBodyReader(ObjectMapper objectMapper) {
        Objects.requireNonNull(objectMapper);
        this.objectMapper = objectMapper;
    }

    @Override
    public boolean accept(GenericType<?> type,
            MessageBodyReaderContext context) {

        Class<?> clazz = type.rawType();
        return !CharSequence.class.isAssignableFrom(clazz)
                && objectMapper.canDeserialize(
                        objectMapper.constructType(clazz));
    }

    @Override
    public <U extends Object> Mono<U> read(Publisher<DataChunk> publisher,
            GenericType<U> type, MessageBodyReaderContext context) {

        return ContentReaders.readBytes(publisher).map(new BytesToObject<>(type));
    }

    /**
     * Create a new {@link JacksonBodyReader} instance.
     * @param objectMapper object mapper to use
     * @return JacksonBodyWriter
     */
    public static JacksonBodyReader create(ObjectMapper objectMapper) {
        return new JacksonBodyReader(objectMapper);
    }

    private final class BytesToObject<T>
            extends MonoMapper<byte[], T> {

        private final TypeReference<? super T> type;

        BytesToObject(GenericType<? super T> gtype) {
            this.type = new TypeReferenceAdapter(gtype);
        }

        @SuppressWarnings("unchecked")
        @Override
        public T mapNext(byte[] bytes) {
            try {
                return objectMapper.readValue(bytes, type);
            } catch (final IOException wrapMe) {
                throw new JacksonRuntimeException(wrapMe.getMessage(), wrapMe);
            }
        }
    }

    private static final class TypeReferenceAdapter<T> extends TypeReference<T> {

        private final GenericType<T> gtype;

        TypeReferenceAdapter(GenericType<T> gtype) {
            this.gtype = gtype;
        }

        @Override
        public Type getType() {
            return gtype.type();
        }
    }
}
