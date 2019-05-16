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
import io.helidon.webserver.BaseStreamReader;
import io.helidon.webserver.ServerRequest;
import io.helidon.webserver.ServerResponse;
import java.util.Iterator;
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

        private long bodyPartsRequested;
        private Flow.Subscriber<? super BodyPart> itemsSubscriber;
        Flow.Subscription chunksSubscription;
        private BodyPart.Builder bodyPartBuilder;
        private BodyPartHeaders.Builder bodyPartHeaderBuilder;
        private BodyPartContentPublisher bodyPartContent;
        private final ServerRequest request;
        private final MIMEParser parser;
        private final Iterator<MIMEEvent> eventIterator;

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
                    if (bodyPartsRequested > 0) {
                        bodyPartsRequested = n;
                        requestNextPart();
                    }
                }

                @Override
                public void cancel() {
                    if (chunksSubscription != null) {
                        chunksSubscription.cancel();
                    }
                    bodyPartsRequested = 0;
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

        private void requestNextPart(){
            if (--bodyPartsRequested > 0){
                chunksSubscription.request(1);
            }
        }

        @Override
        public void onNext(DataChunk chunk) {
            parser.offer(chunk.data());

            if (!eventIterator.hasNext()){
                return;
            }

            while (eventIterator.hasNext()) {
                MIMEEvent event = eventIterator.next();

                switch (event.getEventType()) {
                    case START_MESSAGE:
                        LOGGER.log(Level.FINE, "MIMEEvent={0}",
                                MIMEEvent.EVENT_TYPE.START_MESSAGE);
                        break;

                    case START_PART:
                        LOGGER.log(Level.FINE, "MIMEEvent={0}",
                                MIMEEvent.EVENT_TYPE.START_PART);
                        bodyPartContent = new BodyPartContentPublisher(this);
                        bodyPartHeaderBuilder = BodyPartHeaders.builder();
                        bodyPartBuilder = BodyPart.builder()
                                .publisher(bodyPartContent);
                        break;

                    case HEADER:
                        LOGGER.log(Level.FINE, "MIMEEvent={0}",
                                MIMEEvent.EVENT_TYPE.HEADER);
                        MIMEEvent.Header header = (MIMEEvent.Header) event;
                        bodyPartHeaderBuilder.header(header.getName(),
                                header.getValue());
                        break;

                    case END_HEADERS:
                        LOGGER.log(Level.FINE, "MIMEEvent={0}",
                                MIMEEvent.EVENT_TYPE.END_HEADERS);
                        BodyPart bodyPart = bodyPartBuilder
                                .headers(bodyPartHeaderBuilder.build())
                                .build();
                        bodyPart.registerRequestReaders(request);
                        itemsSubscriber.onNext(bodyPart);
                        break;

                    case CONTENT:
                        LOGGER.log(Level.FINER, "MIMEEvent={0}",
                                MIMEEvent.EVENT_TYPE.CONTENT);
                        DataChunk partChunk = DataChunk.create(
                                ((MIMEEvent.Content) event).getData());
                        bodyPartContent.subscriber.onNext(partChunk);
                        break;

                    case END_PART:
                        LOGGER.log(Level.FINE, "Event={0}",
                                MIMEEvent.EVENT_TYPE.END_PART);
                        bodyPartContent.subscriber.onComplete();
                        requestNextPart();
                        break;

                    case END_MESSAGE:
                        LOGGER.log(Level.FINE, "Event={0}",
                                MIMEEvent.EVENT_TYPE.END_MESSAGE);
                        break;

                    default:
                        throw new MIMEParsingException("Unknown Parser state = "
                                + event.getEventType());
                }
            }
        }

        @Override
        public void onError(Throwable error) {
            if (bodyPartContent != null
                    && bodyPartContent.subscriber != null) {
                bodyPartContent.subscriber.onError(error);
            } else {
                itemsSubscriber.onError(error);
            }
        }

        @Override
        public void onComplete() {
            // close parser
            // get errors and propagate them
            // if errors, call itemsSubscriber.onError
            // otherwise call itemsSubscriber.onComplete();
            itemsSubscriber.onComplete();
            bodyPartsRequested = 0; // XXX: ?
        }
    }

    private static final class BodyPartContentPublisher
            implements Flow.Publisher<DataChunk> {

        Flow.Subscriber<? super DataChunk> subscriber;
        private final Processor processor;

        BodyPartContentPublisher(Processor processor) {
            this.processor = processor;
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
                    if (n > 1) {
                        processor.chunksSubscription.request(n - 1);
                    }
                }

                @Override
                public void cancel() {
                    processor.chunksSubscription.cancel();
                }
            });
        }
    }
}
