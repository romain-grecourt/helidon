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
import java.nio.charset.Charset;
import java.util.List;
import java.util.Objects;

/**
 * Entity writer.
 * @param <T> entity type
 */
public interface EntityWriter<T> {

    Promise accept(Object entity, List<MediaType> acceptedTypes);

    default Promise accept(Object entity, GenericType<?> type,
            List<MediaType> acceptedTypes) {

        return accept(entity, acceptedTypes);
    }

    Publisher<DataChunk> writeEntity(T entity,
            ContentInfo info, List<MediaType> acceptedTypes,
            Charset defaultCharset);

    default Publisher<DataChunk> writeEntity(T entity,
            GenericType<? super T> type, ContentInfo info,
            List<MediaType> acceptedTypes, Charset defaultCharset) {

        return writeEntity(entity, info,
                acceptedTypes, defaultCharset);
    }

    static final class Promise<T> {

        final ContentInfo info;
        final EntityWriter<T> writer;

        public Promise(ContentInfo info, EntityWriter<T> writer) {
            Objects.requireNonNull(info, "content info cannot be null!");
            Objects.requireNonNull(writer, "writer cannot be null!");
            this.info = info;
            this.writer = writer;
        }
    }
}
