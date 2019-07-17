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
package io.helidon.media.common;

import io.helidon.common.GenericType;
import io.helidon.common.http.DataChunk;
import io.helidon.common.reactive.Flow.Publisher;

/**
 * Conversion operator that can convert HTTP payload into many objects.
 *
 * @param <T> type or base type supported by the operator
 */
public interface MessageBodyStreamReader<T>
        extends MessageBodyOperator<MessageBodyReaderContext> {

    /**
     * Convert a HTTP payload into objects of the given type.
     *
     * @param <U> actual requested type parameter
     * @param publisher HTTP payload
     * @param type requested type
     * @param context the context providing the headers abstraction
     * @return Publisher of objects
     */
    <U extends T> Publisher<U> read(Publisher<DataChunk> publisher,
            GenericType<U> type, MessageBodyReaderContext context);
}