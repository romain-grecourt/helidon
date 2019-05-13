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

import io.helidon.common.http.DataChunk;
import io.helidon.common.reactive.Flow;
import io.helidon.webserver.BaseStreamWriter;
import io.helidon.webserver.ServerRequest;
import io.helidon.webserver.ServerResponse;

/**
 * Body part stream writer.
 */
final class BodyPartStreamWriter extends BaseStreamWriter<BodyPart> {

    BodyPartStreamWriter(ServerRequest req, ServerResponse res) {
        super(req, res, BodyPart.class);
    }

    @Override
    public Flow.Publisher<DataChunk> apply(Flow.Publisher<BodyPart> parts) {
        return new Processor(parts, getResponse());
    }

    /**
     * This processor is a single use publisher that supports a single
     * subscriber. It is not resumable.
     */
    static final class Processor
            implements Flow.Processor<BodyPart, DataChunk> {

        private long chunksRequested;
        Flow.Subscriber<? super DataChunk> chunksSubscriber;
        private Flow.Subscription partsSubscription;
        private BodyPartContentSubscriber bodyPartContent;
        boolean completed = false;
        private final ServerResponse response;

        public Processor(Flow.Publisher<BodyPart> partsPublisher,
                ServerResponse response) {

            this.response = response;
            partsPublisher.subscribe(this);
        }

        @Override
        public void subscribe(Flow.Subscriber<? super DataChunk> subscriber) {
            if (chunksSubscriber != null) {
                throw new IllegalStateException(
                        "Ouput subscriber already set");
            }
            chunksSubscriber = subscriber;
            subscriber.onSubscribe(new Flow.Subscription() {
                @Override
                public void request(long n) {
                    if (chunksRequested > 0) {
                        chunksRequested = n;
                        requestNextPart();
                    }
                }

                @Override
                public void cancel() {
                    if (partsSubscription != null) {
                        partsSubscription.cancel();
                    }
                    if (bodyPartContent != null
                            && bodyPartContent.subscription != null){
                        bodyPartContent.subscription.cancel();
                    }
                    chunksRequested = 0;
                }
            });
        }

        @Override
        public void onSubscribe(Flow.Subscription subscription) {
            if (partsSubscription != null) {
                throw new IllegalStateException(
                        "Input subscription already set");
            }
            partsSubscription = subscription;
        }

        void requestNextPart(){
            if (!completed && chunksRequested > 0){
                if (bodyPartContent == null) {
                    partsSubscription.request(1);
                } else {
                    bodyPartContent.subscription.request(chunksRequested);
                }
            }
        }

        @Override
        public void onNext(BodyPart bodyPart) {
            bodyPartContent = new BodyPartContentSubscriber(this);
            bodyPart.registerResponseWriters(response);
            bodyPart.content().subscribe(bodyPartContent);
            // TODO write boundaries
        }

        @Override
        public void onError(Throwable error) {
            chunksSubscriber.onError(error);
        }

        @Override
        public void onComplete() {
            completed = true;
        }
    }

    private static final class BodyPartContentSubscriber
            implements Flow.Subscriber<DataChunk> {

        Flow.Subscription subscription;
        private final Processor processor;

        BodyPartContentSubscriber(Processor processor) {
            this.processor = processor;
        }

        @Override
        public void onSubscribe(Flow.Subscription subscription) {
            if (this.subscription != null) {
                throw new IllegalStateException("Subscriber already set");
            }
            this.subscription = subscription;
        }

        @Override
        public void onNext(DataChunk item) {
            // TODO: encode
            processor.chunksSubscriber.onNext(item);
        }

        @Override
        public void onError(Throwable error) {
            processor.onError(error);
        }

        @Override
        public void onComplete() {
            if (processor.completed) {
                processor.chunksSubscriber.onComplete();
            } else {
                processor.requestNextPart();
            }
        }
    }
}
