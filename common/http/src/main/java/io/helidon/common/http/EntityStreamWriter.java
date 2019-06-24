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
 * Stream writer.
 * @param <T> stream entity type
 */
public interface EntityStreamWriter<T> extends ContentWriter<Publisher<Object>> {

    Publisher<DataChunk> writeEntityStream(Publisher<T> entityStream,
            Class<T> type, OutBoundScope scope);

    @SuppressWarnings("unchecked")
    default Publisher<DataChunk> writeEntityStream(Publisher<T> entityStream,
            GenericType<T> type, OutBoundScope scope) {

        return writeEntityStream(entityStream, (Class<T>) type.rawType(),
                scope);
    }
}
