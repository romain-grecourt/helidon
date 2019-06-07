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
import io.helidon.common.http.DataChunk;
import io.helidon.common.reactive.Flow.Publisher;
import io.helidon.common.reactive.Flow.Subscriber;
import io.helidon.common.reactive.Flow.Subscription;
import io.helidon.media.common.ContentWriters;
import io.helidon.webserver.internal.InBoundContent;
import java.util.Collection;
import java.util.LinkedList;
import java.util.concurrent.CompletableFuture;

/**
 * A reactive subscriber of {@link BodyPart} that buffers the body part content
 * and accumulates copies in a {@code Collection<BodyPart>}. The accumulated
 * copies are made available into a future, see {@link #getFuture() }.
 */
public final class BufferingBodyPartSubscriber implements Subscriber<BodyPart> {

    /**
     * The resulting collection.
     */
    private final LinkedList<BodyPart> bodyParts = new LinkedList<>();

    /**
     * The future completed when {@link #onComplete()} is called.
     */
    private final CompletableFuture<Collection<BodyPart>> future
            = new CompletableFuture<>();

    /**
     * Create a new instance.
     *
     * @param mediaSupport in-bound media support
     */
    BufferingBodyPartSubscriber() {
    }

    @Override
    public void onSubscribe(Subscription subscription) {
        subscription.request(Long.MAX_VALUE);
    }

    @Override
    public void onNext(BodyPart bodyPart) {
        Content content = bodyPart.content();
        if (!(content instanceof InBoundContent)) {
            return;
        }
        // buffer the body part as byte[]
        bodyPart.content().as(byte[].class).thenAccept((byte[] bytes) -> {

            // create a publisher from the consumed byte
            Publisher<DataChunk> partChunks = ContentWriters
                    .byteArrayWriter(/* copy */true).apply(bytes);

            // create a new body part with the buffered content
            BodyPart bufferedBodyPart = BodyPart.builder()
                    .headers(bodyPart.headers())
                    .inBoundPublisher(partChunks,
                            ((InBoundContent) content).mediaSupport())
                    .buffered()
                    .build();
            bodyParts.add(bufferedBodyPart);
        });
    }

    @Override
    public void onError(Throwable error) {
        future.completeExceptionally(error);
    }

    @Override
    public void onComplete() {
        future.complete(bodyParts);
    }

    /**
     * Get the future of body parts.
     *
     * @return future of collection of {@link BodyPart}.
     */
    public CompletableFuture<Collection<BodyPart>> getFuture() {
        return future;
    }
}
