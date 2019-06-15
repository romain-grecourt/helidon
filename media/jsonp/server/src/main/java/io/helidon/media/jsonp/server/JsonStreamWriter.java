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

package io.helidon.media.jsonp.server;

import io.helidon.common.http.DataChunk;
import io.helidon.common.reactive.Flow;
import io.helidon.webserver.Response;
import io.helidon.webserver.ServerResponse;
import io.helidon.common.http.EntityWriters;
import io.helidon.common.http.EntityStreamWriter;

/**
 * Class JsonArrayStreamWriter.
 */
public class JsonStreamWriter<T> implements EntityStreamWriter<T> {

    private DataChunk beginChunk;
    private DataChunk separatorChunk;
    private DataChunk endChunk;
    private final EntityWriters mediaSupport;

    public JsonStreamWriter(ServerResponse response, Class<T> type) {
        mediaSupport = ((Response) response).mediaSupport();
    }

    public DataChunk beginChunk() {
        return beginChunk;
    }

    public void beginChunk(DataChunk beginChunk) {
        this.beginChunk = beginChunk;
    }

    public DataChunk separatorChunk() {
        return separatorChunk;
    }

    public void separatorChunk(DataChunk separatorChunk) {
        this.separatorChunk = separatorChunk;
    }

    public DataChunk endChunk() {
        return endChunk;
    }

    public void endChunk(DataChunk endChunk) {
        this.endChunk = endChunk;
    }

    @Override
    public Flow.Publisher<DataChunk> apply(Flow.Publisher<T> publisher) {
        return new JsonArrayStreamProcessor(publisher);
    }

    class JsonArrayStreamProcessor implements Flow.Processor<T, DataChunk> {

        private long itemsRequested;
        private boolean first = true;
        private Flow.Subscriber<? super DataChunk> chunkSubscriber;
        private final Flow.Publisher<T> itemPublisher;
        private Flow.Subscription itemSubscription;

        JsonArrayStreamProcessor(Flow.Publisher<T> itemPublisher) {
            this.itemPublisher = itemPublisher;
        }

        // -- Publisher<DataChunk> --------------------------------------------

        @Override
        public void subscribe(Flow.Subscriber<? super DataChunk> chunkSubscriber) {
            this.chunkSubscriber = chunkSubscriber;
            this.chunkSubscriber.onSubscribe(new Flow.Subscription() {
                @Override
                public void request(long n) {
                    itemsRequested = n;
                    itemPublisher.subscribe(JsonArrayStreamProcessor.this);
                }

                @Override
                public void cancel() {
                    if (itemSubscription != null) {
                        itemSubscription.cancel();
                    }
                    itemsRequested = 0;
                }
            });

        }

        // -- Subscriber<T> ---------------------------------------------------

        @Override
        public void onSubscribe(Flow.Subscription itemSubscription) {
            this.itemSubscription = itemSubscription;
            if (beginChunk != null) {
                chunkSubscriber.onNext(beginChunk);
            }
            itemSubscription.request(itemsRequested);
        }

        @Override
        public void onNext(T item) {
            if (!first && separatorChunk != null) {
                chunkSubscriber.onNext(separatorChunk);
            } else {
                first = false;
            }

            Flow.Publisher<DataChunk> itemChunkPublisher = mediaSupport.marshall(item);
            if (itemChunkPublisher == null) {
                throw new RuntimeException("Unable to find publisher for item " + item);
            }
            itemChunkPublisher.subscribe(new Flow.Subscriber<DataChunk>() {
                @Override
                public void onSubscribe(Flow.Subscription subscription) {
                    subscription.request(Long.MAX_VALUE);
                }

                @Override
                public void onNext(DataChunk item) {
                    chunkSubscriber.onNext(item);
                }

                @Override
                public void onError(Throwable throwable) {
                    chunkSubscriber.onError(throwable);
                }

                @Override
                public void onComplete() {
                    // no-op
                }
            });
        }

        @Override
        public void onError(Throwable throwable) {
            if (endChunk != null) {
                chunkSubscriber.onNext(endChunk);
            }
        }

        @Override
        public void onComplete() {
            if (endChunk != null) {
                chunkSubscriber.onNext(endChunk);
            }
            chunkSubscriber.onComplete();
        }
    }
}
