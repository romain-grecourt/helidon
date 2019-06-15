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
import java.util.concurrent.CompletionStage;

import io.helidon.common.reactive.Flow.Publisher;
import java.nio.charset.Charset;

/**
 * Entity reader.
 * @param <T> entity type
 */
public interface EntityReader<T> {

    boolean accept(Class<?> type, ContentInfo info);

    default boolean accept(GenericType<?> type, ContentInfo info) {
        return accept(type.rawType(), info);
    }

    CompletionStage<? extends T> readEntity(Publisher<DataChunk> publisher,
            Class<? super T> type, ContentInfo info, Charset defaultCharset);

    @SuppressWarnings("unchecked")
    default CompletionStage<? extends T> readEntity(
            Publisher<DataChunk> publisher, GenericType<? super T> type,
            ContentInfo info, Charset defaultCharset) {

        return readEntity(publisher, (Class<? super T>)type.rawType(), info,
                defaultCharset);
    }
}
