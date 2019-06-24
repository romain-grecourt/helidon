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
import java.util.concurrent.CompletionStage;

import io.helidon.common.reactive.Flow.Publisher;

/**
 * Entity reader.
 * @param <T> entity type
 */
public interface EntityReader<T> extends ContentReader {

    CompletionStage<? extends T> readEntity(Publisher<DataChunk> publisher,
            Class<? super T> type, InBoundScope scope);

    @SuppressWarnings("unchecked")
    default <R extends T> CompletionStage<? extends R> readEntityAndCast(
            Publisher<DataChunk> publisher, Class<R> type, InBoundScope scope) {

        return readEntity(publisher, (Class<T>)type, scope)
                .thenApply(type::cast);
    }

    @SuppressWarnings("unchecked")
    default CompletionStage<? extends T> readEntity(
            Publisher<DataChunk> publisher, GenericType<? super T> type,
            InBoundScope scope) {

        return readEntity(publisher, (Class<? super T>) type.rawType(), scope);
    }

    @SuppressWarnings("unchecked")
    default <R extends T> CompletionStage<? extends R> readEntityAndCast(
            Publisher<DataChunk> publisher, GenericType<R> type,
            InBoundScope scope) {

        return readEntity(publisher, (GenericType<T>)type, scope)
                .thenApply(type::cast);
    }
}
