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
import io.helidon.common.http.EntityReaders;
import io.helidon.common.http.InBoundContent;
import io.helidon.common.reactive.Flow.Processor;
import io.helidon.common.reactive.Flow.Subscription;
import io.helidon.common.reactive.OriginThreadPublisher;
import java.nio.ByteBuffer;
import java.util.LinkedList;
import java.util.Queue;
import java.util.logging.Logger;
import io.helidon.common.http.InBoundScope;
import java.nio.charset.Charset;
import java.util.Objects;

/**
 * Reactive processor that decodes HTTP payload as a stream of {@link BodyPart}.
 * This processor is a single use publisher that supports a single subscriber,
 * it is not resumable.
 */
public final class MultiPartDecoder
        extends OriginThreadPublisher<InBoundBodyPart, InBoundBodyPart>
        implements Processor<DataChunk, InBoundBodyPart> {

    /**
     * Logger.
     */
    private static final Logger LOGGER =
            Logger.getLogger(MultiPartDecoder.class.getName());

    /**
     * Indicate that the chunks subscription is complete.
     */
    private boolean complete;

    /**
     * The upstream subscription.
     */
    private Subscription chunksSubscription;

    /**
     * The builder for the current {@link BodyPart}.
     */
    private InBoundBodyPart.Builder bodyPartBuilder;

    /**
     * The builder for the current {@link InBoundBodyPartHeaders}.
     */
    private InBoundBodyPartHeaders.Builder bodyPartHeaderBuilder;

    /**
     * The publisher for the current part.
     */
    private BodyPartContentPublisher contentPublisher;

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
    private final Queue<InBoundBodyPart> bodyParts;

    /**
     * The entity readers used to read the body part contents.
     */
    private final EntityReaders readers;

    /**
     * The default charset.
     */
    private final Charset defaultCharset;

    /**
     * Create a new instance.
     *
     * @param boundary mime message boundary
     * @param defaultCharset default charset
     * @param readers entity readers, may be {@code null}
     */
    public MultiPartDecoder(String boundary, Charset defaultCharset,
            EntityReaders readers) {

        Objects.requireNonNull(boundary, "boundary cannot be null!");
        Objects.requireNonNull(defaultCharset, "defaultCharset cannot be null!");
        this.defaultCharset = defaultCharset;
        this.readers = readers;
        parserEventProcessor = new ParserEventProcessor();
        parser = new MIMEParser(boundary, parserEventProcessor);
        bodyParts = new LinkedList<>();
    }

    @Override
    protected void hookOnRequested(long n, long result) {
        // require more raw chunks to decode if not the decoding has not
        // yet started or if more data is required to make 
        if (tryAcquire() > 0
                && (!parserEventProcessor.isStarted()
                || parserEventProcessor.isDataRequired())) {
            chunksSubscription.request(1);
        }
    }

    @Override
    public void onSubscribe(Subscription subscription) {
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
            InBoundBodyPart bodyPart = bodyParts.poll();
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
                || contentPublisher.requiresMoreItems())) {

            LOGGER.fine("Requesting one more chunk from upstream");
            chunksSubscription.request(1);
        }
    }

    @Override
    public void onError(Throwable error) {
        error(error);
    }

    @Override
    public void onComplete() {
        LOGGER.fine("Upstream subscription completed");
        complete = true;
        try {
            parser.close();
        } catch (MIMEParser.ParsingException ex) {
            error(ex);
        }
    }

    @Override
    protected InBoundBodyPart wrap(InBoundBodyPart data) {
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
                    contentPublisher = new BodyPartContentPublisher();
                    bodyPartHeaderBuilder = InBoundBodyPartHeaders.builder();
                    bodyPartBuilder = InBoundBodyPart.builder();
                    break;
                case HEADER:
                    MIMEParser.HeaderEvent headerEvent
                            = event.asHeaderEvent();
                    bodyPartHeaderBuilder.header(headerEvent.name(),
                            headerEvent.value());
                    break;
                case END_HEADERS:
                    InBoundBodyPartHeaders headers = bodyPartHeaderBuilder
                            .build();
                    InBoundContent partContent = new InBoundContent(
                            contentPublisher,
                            new InBoundScope(headers,
                                    defaultCharset, headers.contentType(),
                                    readers));
                    bodyParts.add(bodyPartBuilder
                            .headers(headers)
                            .content(partContent)
                            .build());
                    break;
                case CONTENT:
                    contentPublisher.submit(event.asContentEvent()
                            .data());
                    break;
                case END_PART:
                    contentPublisher.complete();
                    contentPublisher = null;
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
         *
         * @return {@code true} if more data is required, {@code false}
         * otherwise
         */
        boolean isDataRequired() {
            return lastEvent.type() == MIMEParser.EVENT_TYPE.DATA_REQUIRED;
        }

        /**
         * Indicate if more content data is required.
         *
         * @return {@code true} if more content data is required, {@code false}
         * otherwise
         */
        boolean isContentDataRequired() {
            return isDataRequired()
                    && lastEvent.asDataRequiredEvent().isContent();
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
