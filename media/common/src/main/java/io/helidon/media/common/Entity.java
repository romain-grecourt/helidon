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

import java.util.Objects;
import java.util.concurrent.Flow.Publisher;
import java.util.function.Function;

import io.helidon.common.GenericType;
import io.helidon.common.http.DataChunk;
import io.helidon.media.common.MediaContext.ReaderContext;
import io.helidon.media.common.MediaContext.WriterContext;

/**
 * Readable and writeable {@link Payload}.
 */
@SuppressWarnings("unused")
public interface Entity extends ReadableEntity, WriteableEntity {

    /**
     * Create a new entity.
     *
     * @param publisher     data publisher
     * @param readerContext initial reader context, may be {@code null}
     * @return Entity
     * @since 3.0.0
     */
    static Entity create(Publisher<DataChunk> publisher, ReaderContext readerContext) {
        return new EntityImpl(ctx -> publisher, readerContext, null);
    }

    /**
     * Create a new entity.
     *
     * @param factory       function that generates data using a writer context
     * @param readerContext initial reader context, may be {@code null}
     * @param writerContext initial writer context, may be {@code null}
     * @return Entity
     * @since 3.0.0
     */
    static Entity create(Function<WriterContext, Publisher<DataChunk>> factory,
                         ReaderContext readerContext,
                         WriterContext writerContext) {

        return new EntityImpl(factory, readerContext, writerContext);
    }

    /**
     * Entity builder.
     *
     * @param <B> builder type
     */
    interface Builder<B extends Builder<B>> {

        /**
         * Use the specified data.
         *
         * @param <T>    entity type
         * @param entity entity
         * @return this builder instance
         */
        default <T> B entity(T entity) {
            return entity(entity, GenericType.<T>create(entity.getClass()));
        }

        /**
         * Use the specified entity.
         *
         * @param <T>    entity type
         * @param entity entity object
         * @param type   entity type
         * @return this builder instance
         */
        default <T> B entity(T entity, Class<T> type) {
            Objects.requireNonNull(type, "type cannot be null!");
            return entity(entity, GenericType.create(type));
        }

        /**
         * Use the specified entity.
         *
         * @param <T>    entity type
         * @param entity entity object
         * @param type   entity type
         * @return this builder instance
         */
        <T> B entity(T entity, GenericType<T> type);

        /**
         * Use the specified entity stream.
         *
         * @param <T>    stream item type
         * @param stream stream of entities
         * @param type   actual representation of the entity type
         * @return this builder instance
         */
        default <T> B entityStream(Publisher<T> stream, Class<T> type) {
            Objects.requireNonNull(type, "type cannot be null!");
            return entityStream(stream, GenericType.create(type));
        }

        /**
         * Use the specified entity stream.
         *
         * @param <T>    stream item type
         * @param stream stream of entities
         * @param type   actual representation of the entity type
         * @return this builder instance
         */
        <T> B entityStream(Publisher<T> stream, GenericType<T> type);

        /**
         * Use the specified publisher as entity.
         *
         * @param publisher publisher
         * @return this builder instance
         */
        B publisher(Publisher<DataChunk> publisher);
    }
}
