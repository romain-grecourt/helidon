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
import java.util.List;
import java.util.Objects;

/**
 * Stream writer.
 * @param <T> stream entity type
 */
public interface EntityStreamWriter<T> {

    Promise accept(Class<?> type, List<MediaType> acceptedTypes);

    default Promise accept(GenericType<?> type, List<MediaType> acceptedTypes) {
        return accept(type.rawType(), acceptedTypes);
    }

    Publisher<DataChunk> writeEntityStream(Publisher<T> entityStream,
            Class<T> type, ContentInfo info, List<MediaType> acceptedTypes,
            Charset defaultCharset);

    @SuppressWarnings("unchecked")
    default Publisher<DataChunk> writeEntityStream(Publisher<T> entityStream,
            GenericType<T> type, ContentInfo info,
            List<MediaType> acceptedTypes, Charset defaultCharset) {

        return writeEntityStream(entityStream, (Class<T>) type.rawType(), info,
                acceptedTypes, defaultCharset);
    }

    static final class Promise<T> {

        final ContentInfo info;
        final EntityStreamWriter<T> writer;

        public Promise(ContentInfo info, EntityStreamWriter<T> writer) {
            Objects.requireNonNull(info, "content info cannot be null!");
            Objects.requireNonNull(info, "writer cannot be null!");
            this.info = info;
            this.writer = writer;
        }
    }
}
