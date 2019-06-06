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

import io.helidon.common.http.Content;
import java.util.concurrent.ExecutionException;
import io.helidon.common.http.DataChunk;
import io.helidon.common.reactive.Flow.Publisher;
import io.helidon.webserver.internal.InBoundContent;
import io.helidon.webserver.internal.InBoundMediaSupport;
import io.helidon.webserver.internal.OutBoundContent;
import java.util.Optional;

/**
 * Body part entity.
 */
public final class BodyPart {

    private final Content content;
    private final BodyPartHeaders headers;
    private final boolean buffered;

    BodyPart(Content content, BodyPartHeaders headers, boolean buffered) {
        this.content = content;
        this.headers = headers;
        this.buffered = buffered;
    }

    /**
     * Indicate if the content of this {@link BodyPart} instance is buffered in
     * memory. When buffered, {@link #as(java.lang.Class)} can be called safely
     * to get the unmarshall the content without the use of a
     * {@code CompletionStage}.
     *
     * @return {@code true} if buffered, {@code false} otherwise
     */
    public boolean isBuffered() {
        return buffered;
    }

    /**
     * Converts the part content into an instance of the requested type.
     * <strong>This method can only be used if the part content is
     * buffered!</strong>
     * If the content is not buffered, use {@link #content()}.
     *
     * @param <T> the requested type
     * @param clazz the requested type class
     * @return T the converted content
     * @throws IllegalStateException if the part is not buffered or if an
     * error occurs while converting the content
     */
    public <T> T as(Class<T> clazz) {
        if (!buffered) {
            throw new IllegalStateException(
                    "The content of this part is not buffered");
        }
        try {
            return content.as(clazz).toCompletableFuture().get();
        } catch (InterruptedException | ExecutionException ex) {
            throw new IllegalStateException(ex.getMessage(), ex);
        }
    }

    /**
     * Returns {@link Content reactive representation} of the part content.
     * @return Content, never {@code null}
     */
    public Content content() {
        return content;
    }

    /**
     * Returns http part headers.
     * @return BodyPartHeaders, never {@code null}
     */
    public BodyPartHeaders headers(){
        return headers;
    }

    /**
     * Get the control name.
     *
     * @return the name parameter of the {@code Content-Disposition} header,
     * or {@code null} if not present.
     */
    public String name() {
        return headers().contentDisposition().name().orElse(null);
    }

    /**
     * Create a new out-bound part backed by the specified entity.
     * @param <T> Type of the entity
     * @param entity entity for the created part content
     * @return BodyPart
     */
    public static <T> BodyPart create(T entity){
        return builder().entity(entity).build();
    }

    /**
     * Create a new builder instance.
     * @return Builder
     */
    public static Builder builder(){
        return new Builder();
    }

    /**
     * Builder class for creating {@link BodyPart} instances.
     */
    public static final class Builder
            implements io.helidon.common.Builder<BodyPart> {

        private Content content;
        private Object entity;
        private BodyPartHeaders headers;
        private boolean buffered;

        /**
         * Private constructor to force the use of {@link BodyPart#builder() }.
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
        @SuppressWarnings("unchecked")
        public Builder publisher(Publisher<DataChunk> publisher) {
            this.content = new OutBoundContent(publisher);
            return this;
        }

        /**
         * Create a new in-bound body part backed by the specified publisher.
         *
         * @param publisher publisher for the part content
         * @param mediaSupport in-bound media support used to unmarshall the
         * content
         * @return this builder instance
         */
        Builder inBoundPublisher(Publisher<DataChunk> publisher,
                InBoundMediaSupport mediaSupport) {

            this.content = new InBoundContent(publisher, mediaSupport);
            return this;
        }

        /**
         * Sets the in-bound body part content as buffered.
         * @return this builder instance
         */
        Builder buffered() {
            this.buffered = true;
            return this;
        }

        /**
         * Set the headers for this part.
         * @param headers headers
         * @return this builder instance
         */
        public Builder headers(BodyPartHeaders headers) {
            this.headers = headers;
            return this;
        }

        @Override
        public BodyPart build() {
            BodyPartHeaders zHeaders = headers;
            if (zHeaders == null) {
                zHeaders = BodyPartHeaders.builder().build();
            }
            if (entity != null) {
                content = new OutBoundContent<>(entity, zHeaders.contentType());
            }
            if (content == null) {
                throw new IllegalStateException("content is null");
            }
            return new BodyPart(content, zHeaders, buffered);
        }
    }
}
