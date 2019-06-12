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
package io.helidon.common.http;

import java.util.function.Predicate;

import io.helidon.common.reactive.Flow;
import io.helidon.common.reactive.Flow.Publisher;

/**
 * Represents an out-bound {@link HttpContent} that can converted from an entity
 * or from a stream of entities.
 */
public interface OutBoundContent extends HttpContent {

    <T> void registerStreamWriter(Predicate<Class<T>> predicate,
            MediaType contentType, StreamWriter<T> writer);

    <T> void registerStreamWriter(Class<T> acceptType, MediaType contentType,
            StreamWriter<T> writer);

    /**
     * Registers a content writer for a given type.
     * <p>
     * Registered writer is used to marshal content of given type to the
     * {@link Flow.Publisher Publisher} of {@link DataChunk}.
     *
     * @param type a type of the content. If {@code null} then accepts any type.
     * @param writer a writer function
     * @param <T> a type of the content
     * @throws NullPointerException if {@code writer} parameter is {@code null}
     */
    <T> void registerWriter(Class<T> type, Writer<T> writer);

    /**
     * Registers a content writer for a given type and media type.
     * <p>
     * Registered writer is used to marshal content of given type to the
     * {@link Flow.Publisher Publisher} of {@link DataChunk}. It is used only if
     * {@code Content-Type} header is compatible with a given content type or if
     * it is {@code null}. If {@code Content-Type} is {@code null} and it is
     * still possible to modify headers (headers were not send yet), the
     * provided content type will be set.
     *
     * @param type a type of the content. If {@code null} then accepts any type.
     * @param contentType a {@code Content-Type} of the entity
     * @param writer a writer function
     * @param <T> a type of the content
     * @throws NullPointerException if {@code writer} parameter is {@code null}
     */
    <T> void registerWriter(Class<T> type, MediaType contentType,
            Writer<T> writer);

    /**
     * Registers a content writer for all accepted contents.
     * <p>
     * Registered writer is used to marshal content of given type to the
     * {@link Flow.Publisher Publisher} of {@link DataChunk}.
     *
     * @param accept a predicate to test if content is marshallable by the
     * writer. If {@code null} then accepts any type.
     * @param writer a writer function
     * @param <T> a type of the content
     * @throws NullPointerException if {@code writer} parameter is {@code null}
     */
    <T> void registerWriter(Predicate<?> accept, Writer<T> writer);

    /**
     * Registers a content writer for all accepted contents.
     * <p>
     * Registered writer is used to marshal content of given type to the
     * {@link Flow.Publisher Publisher} of {@link DataChunk}. It is used only if
     * {@code Content-Type} header is compatible with a given content type or if
     * it is {@code null}. If {@code Content-Type} is {@code null} and it is
     * still possible to modify headers (headers were not send yet), the
     * provided content type will be set.
     *
     * @param accept a predicate to test if content is marshallable by the
     * writer. If {@code null} then accepts any type.
     * @param contentType a {@code Content-Type} of the entity
     * @param writer a writer function
     * @param <T> a type of the content
     * @throws NullPointerException if {@code writer} parameter is {@code null}
     */
    <T> void registerWriter(Predicate<?> accept, MediaType contentType,
            Writer<T> writer);

    Publisher<DataChunk> toPublisher();
    // TODO change object to out-bound content support
    Publisher<DataChunk> toPublisher(Object delegate);
}
