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
import io.helidon.common.http.MediaType;
import io.helidon.common.http.Reader;
import io.helidon.common.http.Writer;
import io.helidon.common.reactive.Flow;
import io.helidon.media.common.ContentWriters;
import io.helidon.webserver.Handler;
import io.helidon.webserver.Routing;
import io.helidon.webserver.ServerRequest;
import io.helidon.webserver.ServerResponse;
import io.helidon.webserver.Service;
import io.helidon.webserver.internal.InBoundContent;
import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;

/**
 * Multi part support service.
 */
public final class MultiPartSupport implements Service, Handler {

    private static final String DEFAULT_BOUNDARY = 
            "[^._.^]==>boundary<==[^._.^]";

    private MultiPartSupport(){
    }

    @Override
    public void update(Routing.Rules rules) {
        rules.any(this);
    }

    @Override
    public void accept(ServerRequest req, ServerResponse res) {
        req.content().registerStreamReader(BodyPart.class,
                new BodyPartStreamReader(req));
        req.content().registerReader(MultiPart.class, new MultiPartReader(req));
        res.registerStreamWriter(BodyPart.class, MediaType.MULTIPART_FORM_DATA,
                new BodyPartStreamWriter(res, DEFAULT_BOUNDARY));
        res.registerWriter(MultiPart.class, new MultiPartWriter(res));
        req.next();
    }

    public static MultiPartSupport create(){
        return new MultiPartSupport();
    }

    /**
     * {@link MultiPart} entity writer.
     */
    private static final class MultiPartWriter implements Writer<MultiPart> {

        private final ServerResponse response;

        MultiPartWriter(ServerResponse response) {
            this.response = response;
        }

        @Override
        public Flow.Publisher<DataChunk> apply(MultiPart multiPart) {
            return new BodyPartStreamWriter(response, DEFAULT_BOUNDARY)
                    .apply(new BodyPartPublisher(multiPart.bodyParts()));
        }
    }

    /**
     * A reactive publisher of {@link BodyPart} that publishes all the items
     * of a given {@code Collection<BodyPart>}.
     */
    private static final class BodyPartPublisher
            implements Flow.Publisher<BodyPart> {

        private final Collection<BodyPart> bodyParts;

        BodyPartPublisher(Collection<BodyPart> bodyParts) {
            this.bodyParts = bodyParts;
        }

        @Override
        public void subscribe(Flow.Subscriber<? super BodyPart> subscriber) {
            final Iterator<BodyPart> bodyPartsIt = bodyParts.iterator();
            subscriber.onSubscribe(new Flow.Subscription() {

                volatile boolean canceled = false;

                @Override
                public void request(long n) {
                    if (canceled) {
                        subscriber.onError(new IllegalStateException(
                                "Subscription canceled"));
                        return;
                    }
                    while (bodyPartsIt.hasNext() && --n >= 0) {
                        subscriber.onNext(bodyPartsIt.next());
                    }
                    if (!bodyPartsIt.hasNext()) {
                        subscriber.onComplete();
                    }
                }

                @Override
                public void cancel() {
                    canceled = true;
                }
            });
        }
    }

    /**
     * {@link MultiPart} entity reader.
     */
    private static final class MultiPartReader implements Reader<MultiPart> {

        private final ServerRequest request;

        MultiPartReader(ServerRequest request) {
            this.request = request;
        }

        @Override
        public CompletionStage<MultiPart> apply(
                Flow.Publisher<DataChunk> chunks,
                Class<? super MultiPart> clazz) {

            BufferingBodyPartSubscriber bodyPartSubscriber =
                    new BufferingBodyPartSubscriber();
            new BodyPartStreamReader(request)
                    .apply(chunks)
                    .subscribe(bodyPartSubscriber);
            CompletableFuture<MultiPart> future = new CompletableFuture<>();
            MultiPart.Builder multiPartBuilder = MultiPart.builder();
            bodyPartSubscriber.getFuture().thenAccept(bodyParts -> {
                multiPartBuilder.bodyParts(bodyParts);
                future.complete(multiPartBuilder.build());
            }).exceptionally((Throwable error) -> {
                future.completeExceptionally(error);
                return null;
            });
            return future;
        }
    }

    /**
     * A reactive subscriber of {@link BodyPart} that buffers the body part
     * content and accumulates copies in a {@code Collection<BodyPart>}.
     */
    static final class BufferingBodyPartSubscriber
            implements Flow.Subscriber<BodyPart> {

        /**
         * The resulting collection.
         */
        private final LinkedList<BodyPart> bodyParts = new LinkedList<>();

        /**
         * The future completed when {@link #onComplete()} is called.
         */
        private final CompletableFuture<Collection<BodyPart>> future =
                new CompletableFuture<>();

        /**
         * Create a new instance.
         * @param mediaSupport in-bound media support
         */
        BufferingBodyPartSubscriber() {
        }

        @Override
        public void onSubscribe(Flow.Subscription subscription) {
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
                Flow.Publisher<DataChunk> partChunks = ContentWriters
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

        CompletableFuture<Collection<BodyPart>> getFuture() {
            return future;
        }
    }
}
