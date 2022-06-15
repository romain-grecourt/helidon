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
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.Objects;
import java.util.concurrent.Flow.Publisher;

import io.helidon.common.GenericType;
import io.helidon.common.http.DataChunk;
import io.helidon.common.reactive.Single;
import io.helidon.media.common.ContentReaders;
import io.helidon.media.common.EntitySupport.Reader;
import io.helidon.media.common.EntitySupport.ReaderContext;

import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.type.TypeFactory;

/**
 * {@link Reader} implementation that supports object binding with Jackson.
 */
final class JacksonReader implements Reader<Object> {

    private final ObjectMapper objectMapper;

    /**
     * Create a new instance.
     *
     * @param objectMapper object mapper to use
     */
    JacksonReader(ObjectMapper objectMapper) {
        Objects.requireNonNull(objectMapper);
        this.objectMapper = objectMapper;
    }

    @Override
    public PredicateResult accept(GenericType<?> type, ReaderContext context) {
        Class<?> clazz = type.rawType();
        return !CharSequence.class.isAssignableFrom(clazz)
                && objectMapper.canDeserialize(objectMapper.constructType(clazz))
                ? PredicateResult.COMPATIBLE
                : PredicateResult.NOT_SUPPORTED;
    }

    @Override
    public <U> Single<U> read(Publisher<DataChunk> publisher, GenericType<U> type, ReaderContext context) {
        return ContentReaders.readBytes(publisher).map(bytes -> read(bytes, type));
    }

    @SuppressWarnings("unchecked")
    private <T> T read(byte[] bytes, GenericType<T> type) {
        try {
            Type t = type.type();
            if (t instanceof ParameterizedType pt) {
                TypeFactory typeFactory = objectMapper.getTypeFactory();
                JavaType javaType = typeFactory.constructType(pt);
                return objectMapper.readValue(bytes, javaType);
            } else {
                return (T) objectMapper.readValue(bytes, type.rawType());
            }
        } catch (final IOException wrapMe) {
            throw new JacksonRuntimeException(wrapMe.getMessage(), wrapMe);
        }
    }
}
