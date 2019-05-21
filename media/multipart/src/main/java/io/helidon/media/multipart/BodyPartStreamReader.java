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
import io.helidon.common.http.MediaType;
import io.helidon.common.reactive.Flow;
import io.helidon.media.multipart.MIMEEvent.EVENT_TYPE;
import io.helidon.webserver.BaseStreamReader;
import io.helidon.webserver.Request;
import io.helidon.webserver.ServerRequest;
import io.helidon.webserver.ServerResponse;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Queue;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Body part stream reader.
 */
final class BodyPartStreamReader extends BaseStreamReader<BodyPart> {

    private static final Logger LOGGER =
            Logger.getLogger(BodyPartStreamReader.class.getName());

    BodyPartStreamReader(ServerRequest req, ServerResponse res) {
        super(req, res, BodyPart.class);
    }

    @Override
    public Flow.Publisher<BodyPart> apply(Flow.Publisher<DataChunk> chunks) {
        return new Processor(chunks, getRequest());
    }

    /**
     * This processor is a single use publisher that supports a single
     * subscriber. It is not resumable.
     */
    static final class Processor
            implements Flow.Processor<DataChunk, BodyPart> {

        private boolean complete;
        private boolean canceled;
        private boolean partsCompleted;
        private long bodyPartsRequested;
        private Flow.Subscriber<? super BodyPart> itemsSubscriber;
        Flow.Subscription chunksSubscription;
        private BodyPart.Builder bodyPartBuilder;
        private BodyPartHeaders.Builder bodyPartHeaderBuilder;
        private BodyPartContentPublisher bodyPartContent;
        private final Queue<BodyPart> bodyParts = new LinkedList<>();
        private final ServerRequest request;
        private final MIMEParser parser;
        private final Iterator<MIMEEvent> eventIterator;
        private MIMEEvent.EVENT_TYPE lastEventType;

        public Processor(Flow.Publisher<DataChunk> chunksPublisher,
                ServerRequest request) {

            if (request.headers().contentType().isPresent()) {
                MediaType contentType = request.headers().contentType().get();
                this.parser = new MIMEParser(contentType.parameters()
                        .get("boundary"));
                this.eventIterator = parser.iterator();
            } else {
                throw new IllegalStateException("Not a multipart request");
            }
            this.request = request;
            chunksPublisher.subscribe(this);
        }

        @Override
        public void subscribe(Flow.Subscriber<? super BodyPart> subscriber) {
            if (itemsSubscriber != null) {
                throw new IllegalStateException(
                        "Ouput subscriber already set");
            }
            itemsSubscriber = subscriber;
            subscriber.onSubscribe(new Flow.Subscription() {
                @Override
                public void request(long n) {
                    if (n <= 0 || canceled
                            || (partsCompleted && bodyParts.isEmpty())){
                        return;
                    }
                    bodyPartsRequested += n;
                    deliverParts();
                    if (!complete
                            && bodyPartsRequested > 0
                            && (lastEventType == null
                            || lastEventType == EVENT_TYPE.DATA_REQUIRED)) {
                        chunksSubscription.request(1);
                    }
                }

                @Override
                public void cancel() {
                    if (!canceled){
                        LOGGER.fine("BodyPart subscription canceled");
                        canceled = true;
                        if (chunksSubscription != null) {
                            LOGGER.fine("Canceling data chunk subscription");
                            chunksSubscription.cancel();
                        }
                    }
                }
            });
        }

        @Override
        public void onSubscribe(Flow.Subscription subscription) {
            if (chunksSubscription != null) {
                throw new IllegalStateException(
                        "Input subscription already set");
            }
            chunksSubscription = subscription;
        }

        private void deliverParts() {
            while (!bodyParts.isEmpty() && bodyPartsRequested > 0) {
                BodyPart bodyPart = bodyParts.poll();
                if (bodyPart != null) {
                    bodyPart.registerReaders(
                            ((Request.Content)request.content()).getReaders());
                    bodyPartsRequested--;
                    itemsSubscriber.onNext(bodyPart);
                }
            }
            if (partsCompleted) {
                itemsSubscriber.onComplete();
            }
        }

        @Override
        public void onNext(DataChunk chunk) {
            parser.offer(chunk.data());

            if (!eventIterator.hasNext()){
                return;
            }

            boolean contentDataRequired = false;
            while (eventIterator.hasNext()) {
                MIMEEvent event = eventIterator.next();
                MIMEEvent.EVENT_TYPE eventType = event.getEventType();
                LOGGER.log(Level.FINE, "MIMEEvent={0}", eventType);
                switch (eventType) {
                    case START_MESSAGE:
                        break;
                    case START_PART:
                        bodyPartContent = new BodyPartContentPublisher();
                        bodyPartHeaderBuilder = BodyPartHeaders.builder();
                        bodyPartBuilder = BodyPart.builder()
                                .publisher(bodyPartContent);
                        break;
                    case HEADER:
                        MIMEEvent.Header header = (MIMEEvent.Header) event;
                        bodyPartHeaderBuilder
                                .header(header.getName(), header.getValue());
                        break;
                    case END_HEADERS:
                        bodyParts.add(bodyPartBuilder
                                .headers(bodyPartHeaderBuilder.build())
                                .build());
                        break;
                    case CONTENT:
                        DataChunk partChunk = DataChunk.create(
                                ((MIMEEvent.Content) event).getData());
                        bodyPartContent.submit(partChunk);
                        break;
                    case END_PART:
                        bodyPartContent.complete();
                        bodyPartContent = null;
                        bodyPartHeaderBuilder = null;
                        bodyPartBuilder = null;
                        break;
                    case DATA_REQUIRED:
                        if (complete) {
                            return;
                        }
                        if (lastEventType == EVENT_TYPE.CONTENT){
                            contentDataRequired = true;
                        }
                        break;
                    case END_MESSAGE:
                        partsCompleted = true;
                        break;
                    default:
                        throw new MIMEParsingException("Unknown Parser state = "
                                + event.getEventType());
                }
                lastEventType = eventType;
            }
            deliverParts();
            if (lastEventType == EVENT_TYPE.DATA_REQUIRED
                    && (!contentDataRequired || bodyPartContent.needsMore())) {
                // request more data if not stuck at content
                // or if the part content subscriber needs more
                chunksSubscription.request(1);
            }
        }

        @Override
        public void onError(Throwable error) {
            itemsSubscriber.onError(error);
        }

        @Override
        public void onComplete() {
            complete = true;
            try {
                parser.close();
            } catch(MIMEParsingException ex){
                itemsSubscriber.onError(ex);
            }
        }
    }

    private static final class BodyPartContentPublisher
            implements Flow.Publisher<DataChunk> {

        Flow.Subscriber<? super DataChunk> subscriber;
        private long requested;
        private boolean canceled;
        private boolean partCompleted;
        private boolean delivering;
        private final Queue<DataChunk> queue = new LinkedList<>();

        BodyPartContentPublisher() {
            this.requested = 0;
            this.canceled = false;
            this.partCompleted = false;
        }

        void submit(DataChunk partChunk) {
            if (canceled) {
                return;
            }
            if (requested > 0) {
                subscriber.onNext(partChunk);
                requested--;
            } else {
                queue.add(partChunk);
            }
        }

        boolean needsMore(){
            return requested - queue.size() > 0;
        }

        void complete(){
            if (canceled) {
                return;
            }
            partCompleted = true;
            if (subscriber != null && queue.isEmpty()) {
                subscriber.onComplete();
            }
        }

        @Override
        public void subscribe(Flow.Subscriber<? super DataChunk> subscriber) {
            if (this.subscriber != null) {
                throw new IllegalStateException(
                        "Content already subscribed to");
            }
            this.subscriber = subscriber;
            subscriber.onSubscribe(new Flow.Subscription() {
                @Override
                public void request(long n) {
                    // invalid request
                    if (n <= 0 || canceled){
                        return;
                    }

                    // save requested amount
                    requested += n;

                    if (delivering){
                        return;
                    }

                    delivering = true;
                    DataChunk partChunk;
                    do {
                        partChunk = queue.poll();
                        if (partChunk != null) {
                            requested--;
                            subscriber.onNext(partChunk);
                        }
                    } while (partChunk != null && requested > 0);
                    delivering = false;

                    if (partCompleted) {
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
}
