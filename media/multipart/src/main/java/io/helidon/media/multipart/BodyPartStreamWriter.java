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
import io.helidon.common.reactive.OriginThreadPublisher;
import io.helidon.webserver.BaseStreamWriter;
import io.helidon.webserver.Response;
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
        Processor processor = new Processor(getResponse());
        parts.subscribe(processor);
        return processor;
    }

    /**
     * This processor is a single use publisher that supports a single
     * subscriber. It is not resumable.
     */
    static final class Processor
            extends OriginThreadPublisher<DataChunk, DataChunk>
            implements Flow.Processor<BodyPart, DataChunk> {

        private Flow.Subscriber<? super DataChunk> chunksSubscriber;
        private Flow.Subscription partsSubscription;
        private BodyPartContentSubscriber bodyPartContent;
        boolean completed = false;
        private final ServerResponse response;

        public Processor(ServerResponse response) {
            this.response = response;
            // submit start boundary
        }

        @Override
        protected void hookOnRequested(long n, long result) {
            requestNextPart();
        }

        @Override
        public void onSubscribe(Flow.Subscription subscription) {
            if (partsSubscription != null) {
                throw new IllegalStateException(
                        "Input subscription already set");
            }
            partsSubscription = subscription;
        }

        @Override
        public void onNext(BodyPart bodyPart) {
            // TODO submit headers
            bodyPartContent = new BodyPartContentSubscriber(this);
            bodyPart.registerWriters(((Response)response).getWriters());
            bodyPart.content().subscribe(bodyPartContent);
            // TODO write boundaries
        }

        @Override
        public void onError(Throwable error) {
            error(error);
        }

        @Override
        public void onComplete() {
            completed = true;
        }

        @Override
        protected DataChunk wrap(DataChunk data) {
            return data;
        }

        private void requestNextPart() {
            if (tryAcquire() > 0){
                partsSubscription.request(1);
            }
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
            // TODO: encode with a charset ?
            processor.submit(item);
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
