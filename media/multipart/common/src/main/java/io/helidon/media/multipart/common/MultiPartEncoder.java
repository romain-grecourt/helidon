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

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import io.helidon.common.http.DataChunk;
import io.helidon.common.reactive.Flow.Processor;
import io.helidon.common.reactive.Flow.Subscriber;
import io.helidon.common.reactive.Flow.Subscription;
import io.helidon.common.reactive.OriginThreadPublisher;
import io.helidon.media.common.MessageBodyWriterContext;

/**
 * Reactive processor that encodes a stream of {@link BodyPart} into an HTTP
 * payload. This processor is a single use publisher that supports a single
 * subscriber, it is not resumable.
 */
public final class MultiPartEncoder
        extends OriginThreadPublisher<DataChunk, DataChunk>
        implements Processor<WriteableBodyPart, DataChunk> {

    private Subscription partsSubscription;
    private BodyPartContentSubscriber contentSubscriber;

    /**
     * The writer context.
     */
    private final MessageBodyWriterContext context;

    /**
     * The boundary used for the generated multi-part message.
     */
    private final String boundary;

    /**
     * Complete flag.
     */
    private volatile boolean complete;

    /**
     * Create a new multipart encoder.
     * @param boundary boundary string
     * @param context writer context
     */
    private MultiPartEncoder(String boundary, MessageBodyWriterContext context) {
        Objects.requireNonNull(boundary, "boundary cannot be null!");
        Objects.requireNonNull(context, "context cannot be null!");
        this.context = context;
        this.boundary = boundary;
        this.complete = false;
    }

    @Override
    protected void hookOnRequested(long n, long result) {
        if (tryAcquire() > 0) {
            partsSubscription.request(1);
            if (contentSubscriber != null) {
                contentSubscriber.request(n);
            }
        }
    }

    @Override
    public void onSubscribe(Subscription subscription) {
        if (partsSubscription != null) {
            throw new IllegalStateException("Input subscription already set");
        }
        partsSubscription = subscription;
    }

    @Override
    public void onNext(WriteableBodyPart bodyPart) {
        Map<String, List<String>> headers = bodyPart.headers().toMap();
        StringBuilder sb = new StringBuilder();

        // start boundary
        sb.append("--").append(boundary).append("\r\n");

        // headers lines
        for (Map.Entry<String, List<String>> headerEntry : headers.entrySet()) {

            String headerName = headerEntry.getKey();
            for (String headerValue : headerEntry.getValue()) {
                sb.append(headerName)
                        .append(":")
                        .append(headerValue)
                        .append("\r\n");
            }
        }

        // end of headers empty line
        sb.append("\r\n");
        submit(sb.toString());
        contentSubscriber = new BodyPartContentSubscriber(this);
        bodyPart.content()
                .toPublisher(context)
                .subscribe(contentSubscriber);
    }

    /**
     * Submit the data represented by the specified string.
     * @param data data to submit
     */
    void submit(String data) {
        submit(DataChunk.create(data.getBytes(StandardCharsets.UTF_8)));
    }

    @Override
    public void onError(Throwable error) {
        error(error);
    }

    @Override
    public void onComplete() {
        complete = true;
    }

    /**
     * Complete this publisher or request the next part.
     */
    void onPartComplete() {
        if (complete) {
            submit(DataChunk.create(ByteBuffer.wrap(MIMEParser.getBytes("--" + boundary + "--"))));
            complete();
        } else {
            long n = tryAcquire();
            if (n > 0){
                partsSubscription.request(1);
                if (contentSubscriber != null) {
                    contentSubscriber.request(n);
                }
            }
        }
    }

    @Override
    protected DataChunk wrap(DataChunk data) {
        return data;
    }

    /**
     * Subscriber of part content.
     */
    private static final class BodyPartContentSubscriber implements Subscriber<DataChunk> {

        private Subscription subscription;
        private final MultiPartEncoder encoder;

        BodyPartContentSubscriber(MultiPartEncoder encoder) {
            this.encoder = encoder;
        }

        @Override
        public void onSubscribe(Subscription subscription) {
            this.subscription = subscription;
        }

        @Override
        public void onNext(DataChunk item) {
            // TODO encode with a charset ?
            encoder.submit(item);
        }

        @Override
        public void onError(Throwable error) {
            encoder.onError(error);
        }

        @Override
        public void onComplete() {
            encoder.submit("\n");
            encoder.onPartComplete();
        }

        void request(long n) {
            if (subscription != null) {
                subscription.request(n);
            }
        }
    }

    /**
     * Create a new encoder instance.
     * @param boundary multipart boundary delimiter
     * @param context writer context
     * @return MultiPartEncoder
     */
    public static MultiPartEncoder create(String boundary, MessageBodyWriterContext context) {
        return new MultiPartEncoder(boundary, context);
    }
}
