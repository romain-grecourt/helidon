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

import java.nio.charset.Charset;
import java.util.Objects;
import java.util.concurrent.Flow.Processor;
import java.util.concurrent.Flow.Publisher;
import java.util.concurrent.Flow.Subscriber;
import java.util.concurrent.Flow.Subscription;

import javax.json.JsonStructure;
import javax.json.JsonWriter;
import javax.json.JsonWriterFactory;

import io.helidon.common.GenericType;
import io.helidon.common.http.DataChunk;
import io.helidon.common.http.MediaType;
import io.helidon.media.common.CharBuffer;
import io.helidon.media.common.ContentWriters;
import io.helidon.media.common.MessageBodyStreamWriter;
import io.helidon.media.common.MessageBodyWriterContext;

/**
 * Message body writer reader for {@link JsonStructure} sub-classes (JSON-P).
 */
public abstract class JsonpBodyStreamWriter
        implements MessageBodyStreamWriter<JsonStructure> {

    private final JsonWriterFactory jsonWriterFactory;
    private final String begin;
    private final String separator;
    private final String end;

    /**
     * Create a new stream writer.
     * @param jsonFactory JSON-P factory
     * @param begin begin character
     * @param separator separator character
     * @param end end character
     */
    protected JsonpBodyStreamWriter(JsonWriterFactory jsonFactory,
            String begin, String separator, String end) {

        Objects.requireNonNull(jsonFactory);
        Objects.requireNonNull(separator);
        this.jsonWriterFactory = jsonFactory;
        this.begin = begin;
        this.separator = separator;
        this.end = end;
    }

    @Override
    public boolean accept(GenericType<?> type,
            MessageBodyWriterContext context) {

        return JsonStructure.class.isAssignableFrom(type.rawType());
    }

    @Override
    public Publisher<DataChunk> write(Publisher<JsonStructure> content,
            GenericType<? extends JsonStructure> type,
            MessageBodyWriterContext context) {

         MediaType contentType = context.findAccepted(MediaType.JSON_PREDICATE,
                MediaType.APPLICATION_JSON);
         context.contentType(contentType);
         return new JsonArrayStreamProcessor(content, context.charset());
    }

    class JsonArrayStreamProcessor
            implements Processor<JsonStructure, DataChunk> {

        private long itemsRequested;
        private boolean first = true;
        private Subscriber<? super DataChunk> chunkSubscriber;
        private final Publisher<? extends JsonStructure> itemPublisher;
        private Subscription itemSubscription;
        private final DataChunk beginChunk;
        private final DataChunk separatorChunk;
        private final DataChunk endChunk;
        private final Charset charset;

        JsonArrayStreamProcessor(Publisher<? extends JsonStructure> publisher,
                Charset charset) {

            this.itemPublisher = publisher;
            if (begin != null) {
                this.beginChunk = DataChunk.create(begin.getBytes(charset));
            } else {
                this.beginChunk = null;
            }
            this.separatorChunk = DataChunk.create(separator.getBytes(charset));
            if (end != null) {
                this.endChunk = DataChunk.create(end.getBytes(charset));
            } else {
                this.endChunk = null;
            }
            Objects.requireNonNull(charset);
            this.charset = charset;
        }

        @Override
        public void subscribe(Subscriber<? super DataChunk> chunkSubscriber) {
            this.chunkSubscriber = chunkSubscriber;
            this.chunkSubscriber.onSubscribe(new Subscription() {
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

        @Override
        public void onSubscribe(Subscription itemSubscription) {
            this.itemSubscription = itemSubscription;
            if (beginChunk != null) {
                chunkSubscriber.onNext(beginChunk);
            }
            itemSubscription.request(itemsRequested);
        }

        @Override
        public void onNext(JsonStructure item) {
            if (!first && separatorChunk != null) {
                chunkSubscriber.onNext(separatorChunk);
            } else {
                first = false;
            }

            CharBuffer buffer = new CharBuffer();
            try (JsonWriter writer = jsonWriterFactory.createWriter(buffer)) {
                if (writer != null) {
                    writer.write(item);
                }
                ContentWriters.writeCharBuffer(buffer, charset)
                        .subscribe(new Subscriber<DataChunk>() {

                @Override
                public void onSubscribe(Subscription subscription) {
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
