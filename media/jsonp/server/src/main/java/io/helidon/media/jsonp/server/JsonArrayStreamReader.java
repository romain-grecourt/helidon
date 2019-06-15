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

import java.nio.charset.Charset;

import io.helidon.common.http.DataChunk;
import io.helidon.common.reactive.Flow;
import io.helidon.webserver.ServerRequest;

import static io.helidon.media.common.ContentTypeCharset.determineCharset;
import io.helidon.common.http.InBoundContent;
import io.helidon.common.http.EntityReaders;
import io.helidon.common.http.EntityStreamReader;

/**
 * Class JsonArrayStreamReader.
 */
public class JsonArrayStreamReader<T> implements EntityStreamReader<T> {

    private final DataChunk beginChunk;
    private final DataChunk separatorChunk;
    private final DataChunk endChunk;
    private final EntityReaders mediaSupport;

    public JsonArrayStreamReader(ServerRequest request, Class<T> type) {
        Charset charset = determineCharset(request.headers());
        beginChunk = DataChunk.create("[".getBytes(charset));
        separatorChunk = DataChunk.create(",".getBytes(charset));
        endChunk = DataChunk.create("]".getBytes(charset));
        mediaSupport = ((InBoundContent)request.content()).contentSupport();
    }

    @Override
    public Flow.Publisher<T> apply(Flow.Publisher<DataChunk> publisher) {
        return new JsonArrayStreamProcessor(publisher);
    }

    @Override
    public Flow.Publisher<? extends T> apply(Flow.Publisher<DataChunk> publisher,
            Class<? super T> clazz) {

        return new JsonArrayStreamProcessor(publisher);
    }

    class JsonArrayStreamProcessor implements Flow.Processor<DataChunk, T> {

        private long itemsRequested;
        private boolean first = true;
        private Flow.Subscriber<? super T> itemSubscriber;
        private final Flow.Publisher<DataChunk> chunkPublisher;
        private Flow.Subscription chunkSubscription;

        JsonArrayStreamProcessor(Flow.Publisher<DataChunk> chunkPublisher) {
            this.chunkPublisher = chunkPublisher;
        }

        @Override
        public void subscribe(Flow.Subscriber<? super T> itemSubscriber) {
            this.itemSubscriber = itemSubscriber;
            this.itemSubscriber.onSubscribe(new Flow.Subscription() {
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

        // -- Subscriber<DataChunk> ---------------------------------------------------

        @Override
        public void onSubscribe(Flow.Subscription chunkSubscription) {
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
