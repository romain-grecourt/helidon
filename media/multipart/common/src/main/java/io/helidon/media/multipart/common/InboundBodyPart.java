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

import io.helidon.media.common.MessageBodyReadableContent;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.ExecutionException;

/**
 * Inbound body part.
 */
public final class InboundBodyPart implements BodyPart {

    private final MessageBodyReadableContent content;
    private final InboundBodyPartHeaders headers;
    private final boolean buffered;

    /**
     * Private to enforce the use of {@link #builder()}.
     */
    private InboundBodyPart(MessageBodyReadableContent content,
            InboundBodyPartHeaders headers, boolean buffered) {

        this.content = content;
        this.headers = headers;
        this.buffered = buffered;
    }

    @Override
    public MessageBodyReadableContent content() {
        return content;
    }

    @Override
    public InboundBodyPartHeaders headers() {
        return headers;
    }

    /**
     * Indicate if the content of this {@link BodyPart} instance is buffered in
     * memory. When buffered, {@link #as(java.lang.Class)} can be called to
     * unmarshall the content synchronously. Otherwise, use {@link #content()}
     * and {@link Content#as(java.lang.Class)} to do it asynchronously with a
     * {@link CompletionStage}.
     *
     * @return {@code true} if buffered, {@code false} otherwise
     */
    public boolean isBuffered() {
        return buffered;
    }

    /**
     * Converts the part content into an instance of the requested type.
     * <strong>This method can only be used if the part content is
     * buffered!</strong>, see {@link #isBuffered()}.
     *
     * @param <T> the requested type
     * @param clazz the requested type class
     * @return T the converted content
     * @throws IllegalStateException if the part is not buffered or if an error
     * occurs while converting the content
     */
    public <T> T as(Class<T> clazz) {
        if (!buffered) {
            throw new IllegalStateException(
                    "The content of this part is not buffered");
        }
        CompletableFuture<T> future = content.as(clazz).toCompletableFuture();
        if (!future.isDone()) {
            throw new IllegalStateException(
                    "Unable to convert part content synchronously");
        }
        try {
            return future.get();
        } catch (InterruptedException | ExecutionException ex) {
            throw new IllegalStateException(ex.getMessage(), ex);
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
     * Builder class for creating {@link BodyPart} instances.
     */
    public static final class Builder
            implements io.helidon.common.Builder<InboundBodyPart> {

        private InboundBodyPartHeaders headers;
        private MessageBodyReadableContent content;
        private boolean buffered;

        /**
         * Private constructor to force the use of
         * {@link InboundBodyPart#builder() }.
         */
        private Builder() {
        }

        /**
         * Sets the inbound content for this part.
         *
         * @param content inbound content
         * @return this builder instance
         */
        public Builder content(MessageBodyReadableContent content) {
            this.content = content;
            return this;
        }

        /**
         * Mark the part content as buffered.
         *
         * @return this builder instance
         */
        public Builder buffered() {
            this.buffered = true;
            return this;
        }

        /**
         * Set the headers for this part.
         * @param headers headers
         * @return this builder instance
         */
        public Builder headers(InboundBodyPartHeaders headers) {
            this.headers = headers;
            return this;
        }

        @Override
        public InboundBodyPart build() {
            if (headers == null) {
                headers = InboundBodyPartHeaders.create();
            }
            if (content == null) {
                throw new IllegalStateException("content is required");
            }
            return new InboundBodyPart(content, headers, buffered);
        }
    }
}
