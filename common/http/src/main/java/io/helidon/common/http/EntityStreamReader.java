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
import java.nio.charset.Charset;

/**
 * Entity stream reader.
 * @param <T> stream entity type
 */
public interface EntityStreamReader<T> {

    boolean accept(Class<?> type, ContentInfo info);

    default boolean accept(GenericType<?> type, ContentInfo info) {
        return accept(type.rawType(), info);
    }

    Publisher<? extends T> readEntityStream(Publisher<DataChunk> publisher,
            Class<? super T> type, ContentInfo info, Charset defaultCharset);

    @SuppressWarnings("unchecked")
    default Publisher<? extends T> readEntityStream(Publisher<DataChunk> publisher,
            GenericType<? super T> type, ContentInfo info,
            Charset defaultCharset) {

        return readEntityStream(publisher, (Class<? super T>)type.rawType(),
                info, defaultCharset);
    }
}
