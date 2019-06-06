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
package io.helidon.media.multipart;

import io.helidon.common.http.Headers;
import io.helidon.common.http.Http;
import io.helidon.common.http.MediaType;
import io.helidon.common.http.ReadOnlyParameters;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/**
 * Body part headers.
 */
public final class BodyPartHeaders extends ReadOnlyParameters
        implements Headers {

    private final Object internalLock = new Object();
    private ContentDisposition contentDisposition;

    /**
     * Create a new instance.
     *
     * @param params headers map
     */
    BodyPartHeaders(Map<String, List<String>> params) {
        super(params);
    }

    /**
     * Get the {@code Content-Type} header.
     *
     * @return MediaType, never {@code null}
     */
    public MediaType contentType() {
        return first(Http.Header.CONTENT_TYPE)
                .map(MediaType::parse)
                .orElseGet(this::defaultContentType);
    }

    /**
     * Apply the default content-type described in
     * https://tools.ietf.org/html/rfc7578#section-4.4
     *
     * @return MediaType, never {@code null}
     */
    private MediaType defaultContentType() {
        return contentDisposition().filename()
                .map(fname -> MediaType.APPLICATION_OCTET_STREAM)
                .orElse(MediaType.TEXT_PLAIN);
    }

    /**
     * Get the {@code Content-Disposition} header.
     *
     * @return ContentDisposition
     */
    public ContentDisposition contentDisposition() {
        if (contentDisposition == null) {
            synchronized (internalLock) {
                contentDisposition = first(Http.Header.CONTENT_DISPOSITION)
                        .map(ContentDisposition::parse)
                        .orElse(ContentDisposition.EMPTY);
            }
        }
        return contentDisposition;
    }

    /**
     * Create a new builder instance.
     *
     * @return Builder
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Builder class to create {@link BodyPartHeaders} instances.
     */
    public static final class Builder
            implements io.helidon.common.Builder<BodyPartHeaders> {

        /**
         * The headers map.
         */
        private final Map<String, List<String>> headers
                = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);

        /**
         * Force the use of {@link BodyPartHeaders#builder() }.
         */
        private Builder() {
        }

        /**
         * Add a new header.
         *
         * @param name header name
         * @param value header value
         * @return this builder
         */
        public Builder header(String name, String value) {
            List<String> values = headers.get(name);
            if (values == null) {
                values = new ArrayList<>();
                headers.put(name, values);
            }
            values.add(value);
            return this;
        }

        @Override
        public BodyPartHeaders build() {
            return new BodyPartHeaders(headers);
        }
    }
}
