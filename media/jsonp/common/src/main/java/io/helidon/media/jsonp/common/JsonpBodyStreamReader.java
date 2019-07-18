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
package io.helidon.media.jsonp.common;

import java.util.Objects;

import javax.json.JsonReaderFactory;
import javax.json.JsonStructure;

import io.helidon.common.GenericType;
import io.helidon.common.http.DataChunk;
import io.helidon.common.reactive.Flow.Processor;
import io.helidon.common.reactive.Flow.Publisher;
import io.helidon.common.reactive.Flow.Subscriber;
import io.helidon.common.reactive.Flow.Subscription;
import io.helidon.media.common.MessageBodyReaderContext;
import io.helidon.media.common.MessageBodyStreamReader;

/**
 * Message body stream reader for {@link JsonStructure} sub-classes (JSON-P).
 */
public class JsonpBodyStreamReader
        implements MessageBodyStreamReader<JsonStructure> {

    private final JsonReaderFactory jsonFactory;

    JsonpBodyStreamReader(JsonReaderFactory jsonFactory) {
        Objects.requireNonNull(jsonFactory);
        this.jsonFactory = jsonFactory;
    }

    @Override
    public boolean accept(GenericType<?> type,
            MessageBodyReaderContext context) {

        return JsonStructure.class.isAssignableFrom(type.rawType());
    }

    @Override
    public <U extends JsonStructure> Publisher<U> read(
            Publisher<DataChunk> publisher, GenericType<U> type,
            MessageBodyReaderContext context) {

        return new JsonArrayStreamProcessor<>(publisher);
    }

    static class JsonArrayStreamProcessor<T extends JsonStructure>
            implements Processor<DataChunk, T> {

        private long itemsRequested;
//        private boolean first = true;
        private Subscriber<? super T> itemSubscriber;
        private final Publisher<DataChunk> chunkPublisher;
        private Subscription chunkSubscription;

        JsonArrayStreamProcessor(Publisher<DataChunk> chunkPublisher) {
            this.chunkPublisher = chunkPublisher;
        }

        @Override
        public void subscribe(Subscriber<? super T> itemSubscriber) {
            this.itemSubscriber = itemSubscriber;
            this.itemSubscriber.onSubscribe(new Subscription() {
                @Override
                public void request(long n) {
                    itemsRequested = n;
                    chunkPublisher.subscribe(JsonArrayStreamProcessor.this);
                }

                @Override
                public void cancel() {
                    if (chunkSubscription != null) {
                        chunkSubscription.cancel();
                    }
                    itemsRequested = 0;
                }
            });
        }

        @Override
        public void onSubscribe(Subscription chunkSubscription) {
            this.chunkSubscription = chunkSubscription;
            chunkSubscription.request(itemsRequested);
        }

        @Override
        public void onNext(DataChunk chunk) {
            // Should parse using regular JSON parser
        }

        @Override
        public void onError(Throwable throwable) {
        }

        @Override
        public void onComplete() {
        }
    }
}
