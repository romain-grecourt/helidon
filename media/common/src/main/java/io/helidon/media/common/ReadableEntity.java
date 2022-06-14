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
package io.helidon.media.common;

import java.util.concurrent.Executor;
import java.util.concurrent.Flow.Publisher;
import java.util.function.Consumer;

import io.helidon.common.GenericType;
import io.helidon.common.http.DataChunk;
import io.helidon.common.reactive.Multi;
import io.helidon.common.reactive.Single;
import io.helidon.media.common.EntitySupport.ReaderContext;

/**
 * Readable {@link Payload}.
 */
public interface ReadableEntity extends Payload, EntitySupport.Readers, EntitySupport.Filters {

    /**
     * Get the reader context used to unmarshall data.
     *
     * @return reader context
     */
    ReaderContext readerContext();

    @Override
    default ReadableEntity registerFilter(EntitySupport.Filter filter) {
        readerContext().registerFilter(filter);
        return this;
    }

    @Override
    default ReadableEntity registerReader(EntitySupport.Reader<?> reader) {
        readerContext().registerReader(reader);
        return this;
    }

    @Override
    default ReadableEntity registerReader(EntitySupport.StreamReader<?> reader) {
        readerContext().registerReader(reader);
        return this;
    }

    /**
     * Convert the raw payload into a completion stage of the requested type.
     * <p>
     * The conversion requires an appropriate reader to be already registered
     * (see {@link #readerContext()#registerReader(EntitySupport.StreamReader)}. If no such reader is found, the
     * resulting completion stage ends exceptionally.
     * <p>
     * Any callback related to the returned value, should not be blocking. Blocking operation could cause deadlock.
     * If you need to use blocking API such as {@link java.io.InputStream} it is highly recommended to do so out of
     * the scope of reactive chain, or to use methods like
     * {@link java.util.concurrent.CompletionStage#thenAcceptAsync(Consumer, Executor)}.
     *
     * @param <T>  the requested type
     * @param type the requested type class
     * @return a completion stage of the requested type
     */
    default <T> Single<T> as(final Class<T> type) {
        return as(GenericType.create(type));
    }

    /**
     * Convert the raw payload into a stream of objects of the requested type.
     *
     * @param type the requested type class
     * @param <T>  the requested type
     * @return a stream of entities
     */
    default <T> Multi<T> asStream(Class<T> type) {
        return asStream(GenericType.create(type));
    }

    /**
     * Convert the raw payload into a completion stage of the requested type.
     *
     * @param type the requested type class
     * @param <T>  the requested type
     * @return a completion stage of the requested type
     */
    <T> Single<T> as(final GenericType<T> type);

    /**
     * Convert the raw payload into a stream of objects of the requested type.
     *
     * @param type the requested type class
     * @param <T>  the requested type
     * @return a stream of entities
     */
    <T> Multi<T> asStream(GenericType<T> type);
}
