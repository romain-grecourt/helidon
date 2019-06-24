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
import java.util.Objects;

/**
 * Content reader.
 * @param <T> entity type
 */
public interface ContentWriter<T> {

    Ack accept(T entity, Class<?> type, OutBoundScope scope);

    default Ack accept(T entity, GenericType<?> type, OutBoundScope scope) {
        return accept(entity, type.rawType(), scope);
    }

    /**
     * Entity acknowledgement.
     */
    static final class Ack {

        private final MediaType contentType;
        private final long contentLength;

        public Ack(MediaType contentType, long contentLength) {
            Objects.requireNonNull(contentType, "contentType cannot be null!");
            this.contentType = contentType;
            this.contentLength = contentLength;
        }

        public Ack(MediaType contentType) {
            this(contentType, -1);
        }

        public void processHeaders(HashParameters headers) {
            if (headers != null) {
                if (contentType != null) {
                    headers.putIfAbsent(Http.Header.CONTENT_TYPE,
                            contentType.toString());
                }
                if (contentLength >= 0) {
                    headers.putIfAbsent(Http.Header.CONTENT_LENGTH,
                            String.valueOf(contentLength));
                }
            }
        }
    }
}
