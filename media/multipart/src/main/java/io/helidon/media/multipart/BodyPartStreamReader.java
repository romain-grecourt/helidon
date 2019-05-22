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
import io.helidon.common.reactive.OriginThreadPublisher;
import io.helidon.media.multipart.MIMEEvent.EVENT_TYPE;
import io.helidon.webserver.BaseStreamReader;
import io.helidon.webserver.Request;
import io.helidon.webserver.ServerRequest;
import io.helidon.webserver.ServerResponse;
import java.nio.ByteBuffer;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Body part stream reader.
 */
final class BodyPartStreamReader extends BaseStreamReader<BodyPart> {

    private static final Logger LOGGER =
            Logger.getLogger(BodyPartStreamReader.class.getName());

    /**
     * Create a new instance.
     * @param req server request
     * @param res server response
     */
    BodyPartStreamReader(ServerRequest req, ServerResponse res) {
        super(req, res, BodyPart.class);
    }

    @Override
    public Flow.Publisher<BodyPart> apply(Flow.Publisher<DataChunk> chunks) {
        Processor processor = new Processor(getRequest());
        chunks.subscribe(processor);
        return processor;
    }

    /**
     * This processor is a single use publisher that supports a single
     * subscriber. It is not resumable.
     */
    static final class Processor
            extends OriginThreadPublisher<BodyPart, BodyPart>
            implements Flow.Processor<DataChunk, BodyPart> {

        /**
         * Indicate that the chunks subscription is complete.
         */
        private boolean complete;

        /**
         * The upstream subscription.
         */
        private Flow.Subscription chunksSubscription;

        /**
         * The builder for the current {@link BodyPart}.
         */
        private BodyPart.Builder bodyPartBuilder;

        /**
         * The builder for the current {@link BodyPartHeaders}.
         */
        private BodyPartHeaders.Builder bodyPartHeaderBuilder;

        /**
         * The publisher for the current part.
         */
        private BodyPartContentPublisher bodyPartContent;

        /**
         * The server request, used to derive the boundary string as well
         * as the reader to register for the published parts.
         */
        private final ServerRequest request;

        /**
         * The parser reference, used raise errors by calling
         * {@link MIMEParser#close()} on completion.
         */
        private final MIMEParser parser;

        /**
         * The iterator of event from the parser.
         */
        private final Iterator<MIMEEvent> eventIterator;

        /**
         * The last event received from the parser.
         */
        private MIMEEvent.EVENT_TYPE lastEventType;

        /**
         * Create a new instance.
         * @param request 
         */
        public Processor(ServerRequest request) {
            if (request.headers().contentType().isPresent()) {
                MediaType contentType = request.headers().contentType().get();
                String boundary = contentType.parameters().get("boundary");
                LOGGER.fine(() -> "request: #" + request.requestId()
                        + ", boundary string is " + boundary);
                this.parser = new MIMEParser(boundary);
                this.eventIterator = parser.iterator();
            } else {
                throw new IllegalStateException("Not a multipart request");
            }
            this.request = request;
        }

        @Override
        protected void hookOnRequested(long n, long result) {
            if (tryAcquire() > 0
                    && (lastEventType == null
                            || lastEventType == EVENT_TYPE.DATA_REQUIRED)) {
                chunksSubscription.request(1);
            }
        }

        @Override
        public void onSubscribe(Flow.Subscription subscription) {
            if (chunksSubscription != null) {
                throw new IllegalStateException(
                        "Input subscription already set");
            }
            chunksSubscription = subscription;
        }

        @Override
        public void onNext(DataChunk chunk) {

            // feed the parser
            try {
                parser.offer(chunk.data());
            } catch (MIMEParsingException ex) {
                error(ex);
                return;
            }

            if (!eventIterator.hasNext()){
                LOGGER.fine(() -> "request: #" + request.requestId()
                        + ", no parser events");
                return;
            }

            LinkedList<BodyPart> bodyParts = new LinkedList<>();
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
                        bodyPartContent.submit(
                                ((MIMEEvent.Content) event).getData());
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
                        break;
                    default:
                        error(new MIMEParsingException("Unknown Parser event = "
                                + event.getEventType()));
                }
                lastEventType = eventType;
            }

            // submit parsed parts
            for (BodyPart bodyPart : bodyParts){
                bodyPart.registerReaders(((Request.Content)request.content())
                        .getReaders());
                submit(bodyPart);
            }

            // complete the subscriber
            if (lastEventType == EVENT_TYPE.END_MESSAGE) {
                complete();
            }

            // request more data if not stuck at content
            // or if the part content subscriber needs more
            if (!complete &&
                    lastEventType == EVENT_TYPE.DATA_REQUIRED
                    && (!contentDataRequired
                    || bodyPartContent.requiresMoreItems())) {

                LOGGER.fine(() -> "request: #" + request.requestId()
                        + ", requesting one more chunk from upstream");
                chunksSubscription.request(1);
            }
        }

        @Override
        public void onError(Throwable error) {
            error(error);
        }

        @Override
        public void onComplete() {
            LOGGER.fine(() -> "request: #" + request.requestId()
                        + ", upstream subscription completed");
            complete = true;
            try {
                parser.close();
            } catch(MIMEParsingException ex){
                error(ex);
            }
        }

        @Override
        protected BodyPart wrap(BodyPart data) {
            return data;
        }
    }

    /**
     * Body part content publisher.
     */
    static final class BodyPartContentPublisher
            extends OriginThreadPublisher<DataChunk, ByteBuffer> {

        @Override
        protected DataChunk wrap(ByteBuffer item) {
            return DataChunk.create(item);
        }
    }
}
