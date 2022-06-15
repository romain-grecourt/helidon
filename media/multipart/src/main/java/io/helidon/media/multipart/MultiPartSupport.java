/*
 * Copyright (c) 2020, 2022 Oracle and/or its affiliates.
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

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.Flow.Publisher;

import io.helidon.common.GenericType;
import io.helidon.common.http.DataChunk;
import io.helidon.common.http.MediaType;
import io.helidon.common.reactive.Multi;
import io.helidon.common.reactive.Single;
import io.helidon.media.common.EntitySupport.PredicateResult;
import io.helidon.media.common.EntitySupport.Reader;
import io.helidon.media.common.EntitySupport.ReaderContext;
import io.helidon.media.common.EntitySupport.StreamReader;
import io.helidon.media.common.EntitySupport.StreamWriter;
import io.helidon.media.common.EntitySupport.Writer;
import io.helidon.media.common.EntitySupport.WriterContext;
import io.helidon.media.common.MediaSupport;

import static io.helidon.common.http.MediaType.MULTIPART_FORM_DATA;
import static io.helidon.media.common.EntitySupport.streamReader;
import static io.helidon.media.common.EntitySupport.streamWriter;
import static io.helidon.media.common.EntitySupport.writer;

/**
 * Multipart media support.
 */
@SuppressWarnings("unused")
public final class MultiPartSupport implements MediaSupport {

    /**
     * The default boundary used for encoding multipart messages.
     */
    public static final String DEFAULT_BOUNDARY = "[^._.^]==>boundary<==[^._.^]";

    private static final StreamReader<BodyPart> BODY_PART_STREAM_READER = streamReader(
            PredicateResult.supports(BodyPart.class), MultiPartSupport::readBodyParts);

    private static final StreamWriter<BodyPart> BODY_PART_STREAM_WRITER = bodyPartStreamWriter(DEFAULT_BOUNDARY);
    private static final Writer<MultiPart> MULTIPART_WRITER = multiPartWriter(DEFAULT_BOUNDARY);

    private final Collection<Reader<?>> readers;
    private final Collection<Writer<?>> writers;
    private final Collection<StreamReader<?>> streamReaders;
    private final Collection<StreamWriter<?>> streamWriters;

    private MultiPartSupport() {
        readers = List.of();
        writers = List.of(MULTIPART_WRITER);
        streamReaders = List.of(BODY_PART_STREAM_READER);
        streamWriters = List.of(BODY_PART_STREAM_WRITER);
    }

    @Override
    public Collection<Reader<?>> readers() {
        return readers;
    }

    @Override
    public Collection<Writer<?>> writers() {
        return writers;
    }

    @Override
    public Collection<StreamReader<?>> streamReaders() {
        return streamReaders;
    }

    @Override
    public Collection<StreamWriter<?>> streamWriters() {
        return streamWriters;
    }

    /**
     * Get the {@link BodyPart} stream reader.
     *
     * @return body part stream reader
     */
    public static StreamReader<BodyPart> bodyPartStreamReader() {
        return BODY_PART_STREAM_READER;
    }

    /**
     * Get the {@link BodyPart} stream writer.
     *
     * @return body part stream writer
     */
    public static StreamWriter<BodyPart> bodyPartStreamWriter() {
        return BODY_PART_STREAM_WRITER;
    }

    /**
     * Get the {@link BodyPart} stream writer.
     *
     * @param boundary boundary string
     * @return body part stream writer
     */
    public static StreamWriter<BodyPart> bodyPartStreamWriter(String boundary) {
        return streamWriter(PredicateResult.supports(BodyPart.class),
                (publisher, ctx) -> writeBodyParts(publisher, ctx, boundary));
    }

    /**
     * Get the default {@link MultiPart} writer.
     *
     * @return body part stream writer
     */
    public static Writer<MultiPart> multiPartWriter() {
        return MULTIPART_WRITER;
    }

    /**
     * Create a {@link MultiPart} writer with the specified boundary delimiter.
     *
     * @param boundary boundary string
     * @return body part stream writer
     */
    public static Writer<MultiPart> multiPartWriter(String boundary) {
        return writer(MultiPartSupport::acceptsMultiPart, (single, ctx) -> writeMultiPart(single, ctx, boundary));
    }

    /**
     * Create a new instance of {@link MultiPartSupport}.
     *
     * @return MultiPartSupport
     */
    public static MultiPartSupport create() {
        return new MultiPartSupport();
    }

    private static PredicateResult acceptsMultiPart(GenericType<?> type, WriterContext context) {
        return context.contentType()
                      .or(() -> Optional.of(MULTIPART_FORM_DATA))
                      .filter(mediaType -> mediaType == MULTIPART_FORM_DATA)
                      .map(it -> PredicateResult.supports(MultiPart.class, type))
                      .orElse(PredicateResult.NOT_SUPPORTED);
    }

    private static Publisher<DataChunk> writeBodyParts(Publisher<BodyPart> publisher,
                                                       WriterContext context,
                                                       String boundary) {

        context.contentType(contentType(boundary));
        MultiPartEncoder encoder = MultiPartEncoder.create(boundary, context);
        publisher.subscribe(encoder);
        return encoder;
    }

    private static Multi<BodyPart> readBodyParts(Publisher<DataChunk> publisher, ReaderContext context) {
        String boundary = null;
        MediaType contentType = context.contentType().orElse(null);
        if (contentType != null) {
            boundary = contentType.parameters().get("boundary");
        }
        if (boundary == null) {
            throw new IllegalStateException("boundary header is missing");
        }
        MultiPartDecoder decoder = MultiPartDecoder.create(boundary, context);
        publisher.subscribe(decoder);
        return decoder;
    }

    private static Publisher<DataChunk> writeMultiPart(Single<MultiPart> single,
                                                       WriterContext context,
                                                       String boundary) {

        context.contentType(contentType(boundary));
        return single.flatMap(multiPart -> {
            MultiPartEncoder encoder = MultiPartEncoder.create(boundary, context);
            Multi.just(multiPart.bodyParts()).subscribe(encoder);
            return encoder;
        });
    }

    private static MediaType contentType(String boundary) {
        return MediaType.builder()
                        .type(MULTIPART_FORM_DATA.type())
                        .subtype(MULTIPART_FORM_DATA.subtype())
                        .addParameter("boundary", "\"" + boundary + "\"")
                        .build();
    }
}
