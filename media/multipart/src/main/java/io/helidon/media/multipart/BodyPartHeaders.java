/*
 * Copyright (c) 2020, 2022 Oracle and/or its affiliates.
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

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.function.Supplier;

import io.helidon.common.http.Headers;
import io.helidon.common.http.Http;
import io.helidon.common.http.MediaType;

/**
 * Body part headers.
 */
public interface BodyPartHeaders extends Headers {

    /**
     * Get the {@code Content-Type} header. If the {@code Content-Type} header
     * is not present, the default value is retrieved using
     * {@link #defaultContentType()}.
     *
     * @return MediaType, never {@code null}
     */
    default MediaType contentType() {
        return first(Http.Header.CONTENT_TYPE)
                .map(MediaType::parse)
                .orElseGet(this::defaultContentType);
    }

    /**
     * Sets the MIME type of the body part.
     *
     * @param contentType Media type of the content.
     */
    default void contentType(MediaType contentType) {
        if (contentType == null) {
            remove(Http.Header.CONTENT_TYPE);
        } else {
            put(Http.Header.CONTENT_TYPE, contentType.toString());
        }
    }

    /**
     * Get the {@code Content-Disposition} header.
     *
     * @return ContentDisposition, never {@code null}
     */
    default ContentDisposition contentDisposition() {
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
    default void contentDisposition(ContentDisposition contentDisposition) {
        if (contentDisposition != null) {
            put(Http.Header.CONTENT_DISPOSITION, contentDisposition.toString());
        }
    }

    /**
     * Returns the default {@code Content-Type} header value:
     * {@link MediaType#APPLICATION_OCTET_STREAM} if the
     * {@code Content-Disposition} header is present with a non empty value,
     * otherwise {@link MediaType#TEXT_PLAIN}.
     *
     * @return MediaType, never {@code null}
     * @see <a href="https://tools.ietf.org/html/rfc7578#section-4.4">RFC-7578</a>
     */
    default MediaType defaultContentType() {
        return contentDisposition()
                .filename()
                .map(filename -> MediaType.APPLICATION_OCTET_STREAM)
                .orElse(MediaType.TEXT_PLAIN);
    }

    /**
     * Create a new builder.
     *
     * @return builder
     */
    static Builder builder() {
        return new Builder();
    }

    /**
     * {@link BodyPartHeaders} builder.
     */
    final class Builder implements io.helidon.common.Builder<Builder, BodyPartHeaders> {

        private final Map<String, List<String>> headers = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
        private boolean readOnly;
        private String name;
        private String filename;

        private Builder() {
        }

        /**
         * Add all headers from the given map.
         *
         * @param map headers map
         * @return this builder instance
         */
        public Builder headers(Map<String, List<String>> map) {
            map.forEach((key, value) -> {
                List<String> values = headers.get(key);
                if (values == null) {
                    headers.put(key, new ArrayList<>(value));
                } else {
                    for (String v : value) {
                        if (values.contains(v)) {
                            values.add(v);
                        }
                    }
                }
            });
            return this;
        }

        /**
         * Add a new header.
         *
         * @param name  header name
         * @param value header value
         * @return this builder
         */
        public Builder header(String name, String value) {
            headers.computeIfAbsent(name, k -> new ArrayList<>()).add(value);
            return this;
        }

        /**
         * Add a {@code Content-Type} header.
         *
         * @param contentType value for the {@code Content-Type} header
         * @return this builder
         */
        public Builder contentType(MediaType contentType) {
            return header(Http.Header.CONTENT_TYPE, contentType.toString());
        }

        /**
         * Add a {@code Content-Disposition} header.
         *
         * @param contentDisp content disposition
         * @return this builder
         */
        public Builder contentDisposition(ContentDisposition contentDisp) {
            return header(Http.Header.CONTENT_DISPOSITION, contentDisp.toString());
        }

        /**
         * Add a {@code Content-Disposition} header.
         *
         * @param contentDisp content disposition supplier
         * @return this builder
         */
        public Builder contentDisposition(Supplier<ContentDisposition> contentDisp) {
            return contentDisposition(contentDisp.get());
        }

        /**
         * Name which will be used in {@link ContentDisposition}.
         * <p>
         * This value will be ignored if an actual instance of {@link ContentDisposition} is set.
         *
         * @param name content disposition name parameter
         * @return this builder
         */
        public Builder name(String name) {
            this.name = name;
            return this;
        }

        /**
         * Filename which will be used in {@link ContentDisposition}.
         * <p>
         * This value will be ignored if an actual instance of {@link ContentDisposition} is set.
         *
         * @param filename content disposition filename parameter
         * @return this builder
         */
        public Builder filename(String filename) {
            this.filename = filename;
            return this;
        }

        /**
         * Set the headers to be built as read-only.
         *
         * @param readOnly {@code true} if read-only, {@code false} otherwise
         * @return this builder
         */
        public Builder readOnly(boolean readOnly) {
            this.readOnly = readOnly;
            return this;
        }

        @Override
        public BodyPartHeaders build() {
            if (!headers.containsKey(Http.Header.CONTENT_DISPOSITION) && name != null) {
                ContentDisposition.Builder builder = ContentDisposition.builder().name(this.name);
                if (filename != null) {
                    builder.filename(filename);
                    if (!headers.containsKey(Http.Header.CONTENT_TYPE)) {
                        contentType(MediaType.APPLICATION_OCTET_STREAM);
                    }
                }
                contentDisposition(builder.build());
            }
            return readOnly ? new ReadOnlyBodyPartHeaders(headers) : new HashBodyPartHeaders(headers);
        }
    }
}
