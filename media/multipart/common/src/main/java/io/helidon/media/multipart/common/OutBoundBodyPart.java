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

import io.helidon.common.http.DataChunk;
import io.helidon.common.reactive.Flow.Publisher;
import io.helidon.common.http.OutBoundContent;
import java.nio.charset.StandardCharsets;
import io.helidon.common.http.OutBoundScope;

/**
 * Out-bound body part.
 */
public final class OutBoundBodyPart implements BodyPart {

    private final OutBoundContent content;
    private final OutBoundBodyPartHeaders headers;

    /**
     * Create a new out-bound body part.
     * @param content http content
     * @param headers part headers
     */
    private OutBoundBodyPart(OutBoundContent content,
            OutBoundBodyPartHeaders headers) {

        this.content = content;
        this.headers = headers;
    }

    @Override
    public OutBoundContent content() {
        return content;
    }

    @Override
    public OutBoundBodyPartHeaders headers() {
        return headers;
    }

    /**
     * Create a new out-bound part backed by the specified entity.
     * @param entity entity for the created part content
     * @return BodyPart
     */
    public static OutBoundBodyPart create(Object entity){
        return builder().entity(entity).build();
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
     * Builder class for creating {@link BodyPart} instances.
     */
    public static final class Builder
            implements io.helidon.common.Builder<OutBoundBodyPart> {

        private OutBoundBodyPartHeaders headers;
        private Object entity;
        private Publisher<DataChunk> publisher;

        /**
         * Private constructor to force the use of
         * {@link InBoundBodyPart#builder() }.
         */
        private Builder() {
        }

        /**
         * Create a new out-bound body part backed by the specified entity.
         * @param entity entity for the body part content
         * @return this builder instance
         */
        public Builder entity(Object entity) {
            this.entity = entity;
            return this;
        }

        /**
         * Create a new out-bound body part backed by the specified publisher.
         * @param publisher publisher for the part content
         * @return this builder instance
         */
        public Builder publisher(Publisher<DataChunk> publisher) {
            this.publisher = publisher;
            return this;
        }

        /**
         * Set the headers for this part.
         * @param headers headers
         * @return this builder instance
         */
        public Builder headers(OutBoundBodyPartHeaders headers) {
            this.headers = headers;
            return this;
        }

        @Override
        public OutBoundBodyPart build() {
            if (headers == null) {
                headers = new OutBoundBodyPartHeaders(null);
            }
            OutBoundScope scope = new OutBoundScope(headers,
                    StandardCharsets.UTF_8);
            OutBoundContent content;
            if (entity != null) {
                content = new OutBoundContent(entity, scope);
            } else if (publisher != null) {
                content = new OutBoundContent(publisher, scope);
            } else {
                throw new IllegalStateException(
                        "Cannot create out-bound content");
            }
            return new OutBoundBodyPart(content, headers);
        }
    }
}
