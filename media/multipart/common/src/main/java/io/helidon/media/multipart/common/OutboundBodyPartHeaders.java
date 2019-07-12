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
package io.helidon.media.multipart.common;

import io.helidon.common.http.HashParameters;
import io.helidon.common.http.Http;
import io.helidon.common.http.MediaType;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/**
 * Outbound body part headers.
 */
public final class OutboundBodyPartHeaders extends HashParameters
        implements BodyPartHeaders {

    /**
     * Create a new instance.
     *
     * @param params headers map
     */
    private OutboundBodyPartHeaders(Map<String, List<String>> params) {
        super(params);
    }

    @Override
    public MediaType contentType() {
        return first(Http.Header.CONTENT_TYPE)
                .map(MediaType::parse)
                .orElseGet(this::defaultContentType);
    }

    /**
     * Sets the MIME type of the body part.
     *
     * @param contentType Media type of the content.
     */
    public void contentType(MediaType contentType) {
        if (contentType == null) {
            remove(Http.Header.CONTENT_TYPE);
        } else {
            put(Http.Header.CONTENT_TYPE, contentType.toString());
        }
    }

    @Override
    public ContentDisposition contentDisposition() {
        return first(Http.Header.CONTENT_DISPOSITION)
                .map(ContentDisposition::parse)
                .orElse(ContentDisposition.EMPTY);
    }

    /**
     * Sets the value of
     * {@value io.helidon.common.http.Http.Header#CONTENT_DISPOSITION} header.
     *
     * @param contentDisposition content disposition
     */
    public void contentDisposition(ContentDisposition contentDisposition) {
        if (contentDisposition != null) {
            put(Http.Header.CONTENT_DISPOSITION, contentDisposition.toString());
        }
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
     * Create a new instance of {@link OutboundBodyPartHeaders} with empty
     * headers.
     * @return OutboundBodyPartHeaders
     */
    public static OutboundBodyPartHeaders create() {
        return new OutboundBodyPartHeaders(null);
    }

    /**
     * Builder class to create {@link InBoundBodyPartHeaders} instances.
     */
    public static final class Builder
            implements io.helidon.common.Builder<OutboundBodyPartHeaders> {

        /**
         * The headers map.
         */
        private final Map<String, List<String>> headers
                = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);

        /**
         * Force the use of {@link InBoundBodyPartHeaders#builder() }.
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

        /**
         * Add a {@code Content-Type} header.
         * @param contentType value for the {@code Content-Type} header
         * @return this builder
         */
        public Builder contentType(MediaType contentType) {
           return header(Http.Header.CONTENT_TYPE, contentType.toString());
        }

        /**
         * Add a {@code Content-Disposition} header.
         * @param contentDisp content disposition
         * @return this builder
         */
        public Builder contentDisposition(ContentDisposition contentDisp) {
            return header(Http.Header.CONTENT_DISPOSITION,
                    contentDisp.toString());
        }

        @Override
        public OutboundBodyPartHeaders build() {
            return new OutboundBodyPartHeaders(headers);
        }
    }
}
