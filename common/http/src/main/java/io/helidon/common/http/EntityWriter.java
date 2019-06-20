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

/**
 * Entity writer.
 * @param <T> entity type
 */
public interface EntityWriter<T> {

    Ack<T> accept(Object entity, OutBoundScope scope);

    default Ack<T> accept(Object entity, GenericType<?> type,
            OutBoundScope scope) {

        return accept(entity, scope);
    }

    Publisher<DataChunk> writeEntity(T entity, Ack<T> ack, OutBoundScope scope);

    default Publisher<DataChunk> writeEntity(T entity,
            GenericType<?> type, Ack<T> ack, OutBoundScope scope) {

        return writeEntity(entity, ack, scope);
    }

    public static final class Ack<T> extends EntityAck<EntityWriter<T>> {

        public Ack(EntityWriter<T> writer, MediaType contentType) {
            super(writer, contentType);
        }

        public Ack(EntityWriter<T> writer, MediaType contentType,
                long contentLength) {

            super(writer, contentType, contentLength);
        }
    }
}
