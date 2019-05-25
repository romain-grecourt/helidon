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
import io.helidon.common.http.StreamReader;
import io.helidon.common.reactive.Flow;
import io.helidon.common.reactive.OriginThreadPublisher;
import io.helidon.webserver.Request;
import io.helidon.webserver.ServerRequest;
import java.nio.ByteBuffer;
import java.util.LinkedList;
import java.util.Queue;
import java.util.logging.Logger;

/**
 * Body part stream reader.
 */
final class BodyPartStreamReader implements StreamReader<BodyPart> {

    private static final Logger LOGGER =
            Logger.getLogger(BodyPartStreamReader.class.getName());

    private final ServerRequest request;

    /**
     * Create a new instance.
     * @param req server request
     * @param res server response
     */
    BodyPartStreamReader(ServerRequest req) {
        request = req;
    }

    @Override
    public Flow.Publisher<? extends BodyPart> apply(
            Flow.Publisher<DataChunk> chunks, Class<? super BodyPart> clazz) {

        Processor processor = new Processor(request);
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
         * The MIME parser.
         */
        private final MIMEParser parser;

        /**
         * The parser event processor.
         */
        private final ParserEventProcessor parserEventProcessor;

        /**
         * The bodyParts processed during each {@code onNext}.
         */
        private final Queue<BodyPart> bodyParts;

        /**
         * Create a new instance.
         * @param req 
         */
        public Processor(ServerRequest req) {
            String boundary;
            if (req.headers().contentType().isPresent()) {
                MediaType contentType = req.headers().contentType().get();
                boundary = contentType.parameters().get("boundary");
                LOGGER.fine(() -> "request: #" + req.requestId()
                        + ", boundary string is " + boundary);
            } else {
                throw new IllegalStateException("Not a multipart request");
            }
            request = req;
            parserEventProcessor = new ParserEventProcessor();
            parser = new MIMEParser(boundary, parserEventProcessor);
            bodyParts = new LinkedList<>();
        }

        @Override
        protected void hookOnRequested(long n, long result) {
            // require more raw chunks to decode if not the decoding has not
            // yet started or if more data is required to make progress
            if (tryAcquire() > 0
                    && (!parserEventProcessor.isStarted()
                    || parserEventProcessor.isDataRequired())) {
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
            try {
                // feed the parser
                parser.offer(chunk.data());
            } catch (MIMEParser.ParsingException ex) {
                error(ex);
            }

            // submit parsed parts
            while (!bodyParts.isEmpty()) {
                BodyPart bodyPart = bodyParts.poll();
                bodyPart.registerReaders(((Request.Content) request.content())
                        .getReaders());
                submit(bodyPart);
            }

            // complete the subscriber
            if (parserEventProcessor.isCompleted()) {
                complete();
            }

            // request more data if not stuck at content
            // or if the part content subscriber needs more
            if (!complete && parserEventProcessor.isDataRequired()
                    && (!parserEventProcessor.isContentDataRequired()
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
            } catch (MIMEParser.ParsingException ex) {
                error(ex);
            }
        }

        @Override
        protected BodyPart wrap(BodyPart data) {
            return data;
        }

        /**
         * MIMEParser event processor.
         */
        private final class ParserEventProcessor
                implements MIMEParser.EventProcessor {

            MIMEParser.ParserEvent lastEvent = null;

            @Override
            public void process(MIMEParser.ParserEvent event) {
                MIMEParser.EVENT_TYPE eventType = event.type();
                LOGGER.fine(() -> "Parser event: " + eventType);
                switch (eventType) {
                    case START_PART:
                        bodyPartContent = new BodyPartContentPublisher();
                        bodyPartHeaderBuilder = BodyPartHeaders.builder();
                        bodyPartBuilder = BodyPart.builder()
                                .publisher(bodyPartContent);
                        break;
                    case HEADER:
                        MIMEParser.HeaderEvent headerEvent =
                                event.asHeaderEvent();
                        bodyPartHeaderBuilder.header(headerEvent.name(),
                                headerEvent.value());
                        break;
                    case END_HEADERS:
                        bodyParts.add(bodyPartBuilder
                                .headers(bodyPartHeaderBuilder.build())
                                .build());
                        break;
                    case CONTENT:
                        bodyPartContent.submit(event.asContentEvent()
                                .data());
                        break;
                    case END_PART:
                        bodyPartContent.complete();
                        bodyPartContent = null;
                        bodyPartHeaderBuilder = null;
                        bodyPartBuilder = null;
                        break;
                    default:
                        // nothing to do
                }
                lastEvent = event;
            }

            /**
             * Indicate if the parser has received any data.
             *
             * @return {@code true} if the parser has been offered data,
             * {@code false} otherwise
             */
            boolean isStarted() {
                return lastEvent != null;
            }

            /**
             * Indicate if the parser has reached the end of the message.
             *
             * @return {@code true} if completed, {@code false} otherwise
             */
            boolean isCompleted() {
                return lastEvent.type() == MIMEParser.EVENT_TYPE.END_MESSAGE;
            }

            /**
             * Indicate if the parser requires more data to make progress.
             * @return {@code true} if more data is required, {@code false}
             * otherwise
             */
            boolean isDataRequired() {
                return lastEvent.type() == MIMEParser.EVENT_TYPE.DATA_REQUIRED;
            }

            /**
             * Indicate if more content data is required.
             *
             * @return {@code true} if more content data is required,
             * {@code false} otherwise
             */
            boolean isContentDataRequired() {
                return isDataRequired()
                        && lastEvent.asDataRequiredEvent().isContent();
            }
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
