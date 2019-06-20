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

import java.util.Objects;

/**
 * Entity acknowledgement.
 * @param <T> content writer type
 */
public abstract class EntityAck<T> {

    private final MediaType contentType;
    private final long contentLength;
    private final T writer;

    protected EntityAck(T writer, MediaType contentType, long contentLength) {
        Objects.requireNonNull(writer, "writer cannot be null!");
        Objects.requireNonNull(contentType, "contentType cannot be null!");
        this.writer = writer;
        this.contentType = contentType;
        this.contentLength = contentLength;
    }

    protected EntityAck(T writer, MediaType contentType) {
        this(writer, contentType, -1);
    }

    public T writer() {
        return writer;
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
