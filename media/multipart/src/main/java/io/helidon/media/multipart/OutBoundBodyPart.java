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

import io.helidon.common.http.ContentInfo;
import io.helidon.common.http.ContentInterceptor;
import io.helidon.common.http.DataChunk;
import io.helidon.common.http.MediaType;
import io.helidon.common.reactive.Flow.Publisher;
import io.helidon.common.http.OutBoundContent;
import io.helidon.common.http.OutBoundContext;
import io.helidon.common.reactive.Flow.Subscriber;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.List;

/**
 * In-bound body part.
 */
public final class OutBoundBodyPart extends BodyPart<OutBoundContent> {

    /**
     * Create a new out-bound body part.
     * @param content http content
     * @param headers part headers
     */
    private OutBoundBodyPart(OutBoundContent content, BodyPartHeaders headers) {
        super(content, headers);
    }

    /*
     * Create a new out-bound part backed by the specified entity.
     * @param <T> Type of the entity
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
    public static final class Builder extends BodyPart.Builder {

        private BodyPartHeaders headers;
        private OutBoundContent content;

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
            this.content = new OutBoundContent(entity, /* writersSupport */ null,
                    new OutBoundContext() {

                @Override
                public List<MediaType> acceptedTypes() {
                    return Collections.emptyList();
                }

                @Override
                public Charset defaultCharset() {
                    return StandardCharsets.UTF_8;
                }

                @Override
                public void contentInfo(ContentInfo info) {
                    // do nothing
                }

                @Override
                public ContentInterceptor createInterceptor(
                        Subscriber<? super DataChunk> subscriber, String type) {

                    return null;
                }
            });
            return this;
        }

        /**
         * Create a new out-bound body part backed by the specified publisher.
         * @param publisher publisher for the part content
         * @return this builder instance
         */
        public Builder publisher(Publisher<DataChunk> publisher) {
//            this.content = new OutBoundContent(publisher);
            return this;
        }

        @Override
        public Builder headers(BodyPartHeaders headers) {
            this.headers = headers;
            return this;
        }

        @Override
        public OutBoundBodyPart build() {
            return new OutBoundBodyPart(content, headers);
        }
    }
}
