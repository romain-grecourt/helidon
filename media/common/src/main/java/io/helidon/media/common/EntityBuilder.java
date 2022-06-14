/*
 * Copyright (c) 2022 Oracle and/or its affiliates.
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

import io.helidon.common.Builder;
import io.helidon.common.GenericType;
import io.helidon.common.http.DataChunk;
import io.helidon.common.http.Parameters;
import io.helidon.common.reactive.Single;
import io.helidon.media.common.EntitySupport.WriterContext;

/**
 * Builder for {@link Entity}.
 *
 * @param <B> builder type
 * @param <U> builder target type
 */
public abstract class EntityBuilder<B extends EntityBuilder<B, U>, U> implements Entity.Builder<B>, Builder<B, U> {

    private Publisher<DataChunk> publisher;
    private EntityInfo<?> entityInfo;
    private EntityStreamInfo<?> entityStreamInfo;

    /**
     * Create a new instance.
     */
    protected EntityBuilder() {
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> B entity(T entity, GenericType<T> type) {
        this.entityInfo = new EntityInfo<>(entity, type);
        return (B) this;
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> B entityStream(Publisher<T> stream, GenericType<T> type) {
        this.entityStreamInfo = new EntityStreamInfo<>(stream, type);
        return (B) this;
    }

    @Override
    @SuppressWarnings("unchecked")
    public B publisher(Publisher<DataChunk> publisher) {
        this.publisher = publisher;
        return (B) this;
    }

    /**
     * Build the entity.
     *
     * @param headers additional headers to be used when marshalling
     * @return entity
     */
    public Entity buildEntity(Parameters headers) {
        if (publisher != null) {
            return new EntityImpl(ctx -> publisher, null, null);
        }
        if (entityInfo != null) {
            return new EntityImpl(ctx -> entityInfo.marshall(ctx, headers), null, null);
        } else {
            if (entityStreamInfo != null) {
                return new EntityImpl(ctx -> entityStreamInfo.marshall(ctx, headers), null, null);
            } else {
                return new EntityImpl(ctx -> Single.empty(), null, null);
            }
        }
    }

    private record EntityInfo<T>(T entity, GenericType<T> type) {

        private EntityInfo(T entity, GenericType<T> type) {
            this.entity = Objects.requireNonNull(entity, "entity cannot be null!");
            this.type = Objects.requireNonNull(type, "type cannot be null!");
        }

        Publisher<DataChunk> marshall(WriterContext ctx, Parameters headers) {
            return ctx.createChild(null, headers, null).marshall(Single.just(entity), type);
        }
    }

    private record EntityStreamInfo<T>(Publisher<T> stream, GenericType<T> type) {

        private EntityStreamInfo(Publisher<T> stream, GenericType<T> type) {
            this.stream = Objects.requireNonNull(stream, "entity cannot be null!");
            this.type = Objects.requireNonNull(type, "type cannot be null!");
        }

        Publisher<DataChunk> marshall(WriterContext ctx, Parameters headers) {
            return ctx.createChild(null, headers, null).marshallStream(stream, type);
        }
    }
}
