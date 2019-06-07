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
import io.helidon.common.reactive.Flow.Processor;
import io.helidon.common.reactive.Flow.Subscriber;
import io.helidon.common.reactive.Flow.Subscription;
import io.helidon.common.reactive.OriginThreadPublisher;
import io.helidon.webserver.internal.OutBoundContent;
import io.helidon.webserver.internal.OutBoundMediaSupport;
import java.nio.ByteBuffer;
import java.util.List;
import java.util.Map;

/**
 * Reactive processor that encodes a stream of {@link BodyPart} into an HTTP
 * payload. This processor is a single use publisher that supports a single
 * subscriber, it is not resumable.
 */
public final class MultiPartEncoder
        extends OriginThreadPublisher<DataChunk, DataChunk>
        implements Processor<BodyPart, DataChunk> {

    private Subscription partsSubscription;
    private BodyPartContentSubscriber bodyPartContent;

    /**
     * The out-bound media support used to marshall the body part contents.
     */
    private final OutBoundMediaSupport mediaSupport;

    /**
     * The boundary used for the generated multi-part message.
     */
    private final String boundary;

    /**
     * Create a new multipart encoder.
     * @param boundary boundary string
     * @param mediaSupport out-bound media support
     */
    public MultiPartEncoder(String boundary, OutBoundMediaSupport mediaSupport) {
        this.mediaSupport = mediaSupport;
        this.boundary = boundary;
    }

    @Override
    protected void hookOnRequested(long n, long result) {
        requestNextPart();
    }

    @Override
    public void onSubscribe(Subscription subscription) {
        if (partsSubscription != null) {
            throw new IllegalStateException(
                    "Input subscription already set");
        }
        partsSubscription = subscription;
    }

    @Override
    public void onNext(BodyPart bodyPart) {
        Content content = bodyPart.content();
        if (!(content instanceof OutBoundContent)) {
            return;
        }
        Map<String, List<String>> headers = bodyPart.headers().toMap();
        StringBuilder sb = new StringBuilder();

        // start boundary
        sb.append(boundary).append("\r\n");

        // headers lines
        for (Map.Entry<String, List<String>> headerEntry
                : headers.entrySet()) {

            String headerName = headerEntry.getKey();
            for (String headerValue : headerEntry.getValue()) {
                sb.append(headerName)
                        .append(":")
                        .append(headerValue)
                        .append("\r\n");
            }
        }

        // end of headers empty line
        if (!headers.isEmpty()) {
            sb.append("\r\n");
        }
        submit(DataChunk.create(ByteBuffer.wrap(sb.toString().getBytes())));
        bodyPartContent = new BodyPartContentSubscriber(this);
        ((OutBoundContent) content).mediaSupport(mediaSupport);
        bodyPart.content().subscribe(bodyPartContent);
    }

    @Override
    public void onError(Throwable error) {
        error(error);
    }

    @Override
    public void onComplete() {
        submit(DataChunk.create(ByteBuffer.wrap((boundary + "--")
                .getBytes())));
        complete();
    }

    @Override
    protected DataChunk wrap(DataChunk data) {
        return data;
    }

    private void requestNextPart() {
        if (tryAcquire() > 0) {
            partsSubscription.request(1);
        }
    }

    /**
     * Subscriber of part content.
     */
    private static final class BodyPartContentSubscriber
            implements Subscriber<DataChunk> {

        Subscription subscription;
        private final MultiPartEncoder encoder;

        BodyPartContentSubscriber(MultiPartEncoder encoder) {
            this.encoder = encoder;
        }

        @Override
        public void onSubscribe(Subscription subscription) {
            if (this.subscription != null) {
                throw new IllegalStateException("Subscriber already set");
            }
            this.subscription = subscription;
        }

        @Override
        public void onNext(DataChunk item) {
            // TODO: encode with a charset ?
            encoder.submit(item);
        }

        @Override
        public void onError(Throwable error) {
            encoder.onError(error);
        }

        @Override
        public void onComplete() {
            if (!encoder.isCompleted()) {
                encoder.requestNextPart();
            }
        }
    }
}
