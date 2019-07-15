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

import io.helidon.common.GenericType;
import io.helidon.common.http.DataChunk;
import io.helidon.common.http.MediaType;
import io.helidon.common.reactive.Collector;
import io.helidon.common.reactive.Flow.Publisher;
import io.helidon.media.common.MessageBodyReadableContent;
import io.helidon.common.reactive.Multi;
import io.helidon.common.reactive.Mono;
import io.helidon.common.reactive.MultiMapper;
import io.helidon.media.common.ContentReaders;
import io.helidon.media.common.ContentWriters;
import io.helidon.media.common.MessageBodyReader;
import io.helidon.media.common.MessageBodyReaderContext;
import java.util.LinkedList;

/**
 * {@link ReadableMultiPart} reader.
 */
public final class MultiPartBodyReader implements MessageBodyReader<MultiPart> {

    /**
     * Singleton instance.
     */
    private static final MultiPartBodyReader INSTANCE =
            new MultiPartBodyReader();

    /**
     * Bytes to chunk mapper singleton.
     */
    private static final BytesToChunks BYTES_TO_CHUNKS = new BytesToChunks();

    /**
     * Collector singleton to collect body parts from a publisher as a list.
     */
    private static final PartsCollector COLLECTOR = new PartsCollector();

    /**
     * Private to enforce the use of {@link #get()}.
     */
    private MultiPartBodyReader() {
    }

    @Override
    public boolean accept(GenericType<?> type, MessageBodyReaderContext ctx) {
        return MultiPart.class.isAssignableFrom(type.rawType());
    }

    @Override
    @SuppressWarnings("unchecked")
    public <U extends MultiPart> Mono<U> read(Publisher<DataChunk> publisher,
            GenericType<U> type, MessageBodyReaderContext context) {

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
        return (Mono<U>) Multi.from(decoder).collect(COLLECTOR);
    }

    /**
     * Create a new instance of {@link MultiPartBodyReader}.
     * @return MultiPartReader
     */
    public static MultiPartBodyReader get() {
        return INSTANCE;
    }

    /**
     * A collector that accumulates and buffers body parts.
     */
    private static final class PartsCollector
            implements Collector<ReadableMultiPart, ReadableBodyPart> {

        private final LinkedList<ReadableBodyPart> bodyParts;

        PartsCollector() {
            this.bodyParts = new LinkedList<>();
        }

        @Override
        public void collect(ReadableBodyPart bodyPart) {

            MessageBodyReadableContent content = bodyPart.content();

            // buffer the data
            Publisher<DataChunk> bufferedData = ContentReaders
                    .readBytes(content)
                    .mapMany(BYTES_TO_CHUNKS);

            // create a content copy with the buffered data
            MessageBodyReadableContent contentCopy = MessageBodyReadableContent
                    .create(bufferedData, content.readerContext());

            // create a new body part with the buffered content
            ReadableBodyPart bufferedBodyPart = ReadableBodyPart.builder()
                    .headers(bodyPart.headers())
                    .content(contentCopy)
                    .buffered()
                    .build();
            bodyParts.add(bufferedBodyPart);
        }

        @Override
        public ReadableMultiPart value() {
            return new ReadableMultiPart(bodyParts);
        }
    }

    /**
     * Implementation of {@link MultiMapper} that converts {@code byte[]} to a
     * publisher of {@link DataChunk} by copying the bytes.
     */
    private static final class BytesToChunks
            implements MultiMapper<byte[], DataChunk> {

        @Override
        public Publisher<DataChunk> map(byte[] bytes) {
            return ContentWriters.writeBytes(bytes, /* copy */ true);
        }
    }
}
