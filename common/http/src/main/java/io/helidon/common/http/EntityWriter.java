/*
 * Copyright (c) 2018 Oracle and/or its affiliates. All rights reserved.
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

import io.helidon.common.GenericType;
import io.helidon.common.reactive.Flow.Publisher;
import java.util.Objects;

/**
 * Entity writer.
 * @param <T> entity type
 */
public interface EntityWriter<T> {

    Promise accept(Object entity, OutBoundScope scope);

    default Promise<T> accept(Object entity, GenericType<?> type,
            OutBoundScope scope) {

        return accept(entity, scope);
    }

    Publisher<DataChunk> writeEntity(T entity, Promise<T> promise,
            OutBoundScope scope);

    default Publisher<DataChunk> writeEntity(T entity,
            GenericType<?> type, Promise<T> promise, OutBoundScope scope) {

        return writeEntity(entity, promise, scope);
    }

    static final class Promise<T> {

        public final MediaType contentType;
        public final long contentLength;
        public final EntityWriter<T> writer;

        public Promise(EntityWriter<T> writer, MediaType contentType,
                long contentLength) {

            Objects.requireNonNull(writer, "writer cannot be null!");
            Objects.requireNonNull(contentType, "contentType cannot be null!");
            this.writer = writer;
            this.contentType = contentType;
            this.contentLength = contentLength;
        }

        public Promise(EntityWriter<T> writer, MediaType contentType) {
            this(writer, contentType, -1);
        }
    }
}
