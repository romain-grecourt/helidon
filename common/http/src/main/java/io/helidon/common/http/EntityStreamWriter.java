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

import io.helidon.common.GenericType;
import io.helidon.common.reactive.Flow.Publisher;
import java.util.Objects;

/**
 * Stream writer.
 * @param <T> stream entity type
 */
public interface EntityStreamWriter<T> {

    Promise accept(Class<?> type, OutBoundScope scope);

    default Promise accept(GenericType<?> type, OutBoundScope scope) {
        return accept(type.rawType(), scope);
    }

    Publisher<DataChunk> writeEntityStream(Publisher<T> entityStream,
            Class<T> type, Promise<T> promise, OutBoundScope scope);

    @SuppressWarnings("unchecked")
    default Publisher<DataChunk> writeEntityStream(Publisher<T> entityStream,
            GenericType<T> type, Promise<T> promise, OutBoundScope scope) {

        return writeEntityStream(entityStream, (Class<T>) type.rawType(),
                promise, scope);
    }

    static final class Promise<T> {

        public final MediaType contentType;
        public final long contentLength;
        public final EntityStreamWriter<T> writer;

        public Promise(EntityStreamWriter<T> writer, MediaType contentType,
                long contentLength) {

            Objects.requireNonNull(writer, "writer cannot be null!");
            Objects.requireNonNull(contentType, "contentType cannot be null!");
            this.writer = writer;
            this.contentType = contentType;
            this.contentLength = contentLength;
        }

        public Promise(EntityStreamWriter<T> writer, MediaType contentType) {
            this(writer, contentType, -1);
        }
    }
}
