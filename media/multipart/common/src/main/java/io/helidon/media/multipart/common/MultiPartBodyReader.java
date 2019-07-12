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
import io.helidon.common.reactive.Flow.Publisher;
import io.helidon.media.common.ByteArrayBodyReader;
import io.helidon.media.common.ByteArrayBodyWriter;
import java.util.LinkedList;
import io.helidon.media.common.MessageBodyReadableContent;
import io.helidon.common.reactive.Multi;
import io.helidon.common.reactive.Mono;
import io.helidon.media.common.MessageBodyReader;
import io.helidon.media.common.MessageBodyReaderContext;
import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.Function;
import java.util.function.Supplier;

/**
 * {@link InboundMultiPart} reader.
 */
public final class MultiPartBodyReader implements MessageBodyReader<MultiPart> {

    /**
     * Mapper singleton to map a list of body parts to a single multipart.
     */
    private static final Mapper MAPPER = new Mapper();

    /**
     * Collector singleton to collect body parts from a publisher as a list.
     */
    private static final Collector COLLECTOR = new Collector();

    /**
     * A supplier of list part for the collector.
     */
    private static final Supplier<List<InboundBodyPart>> LIST_SUPPLIER =
            LinkedList<InboundBodyPart>::new;

    /**
     * Private to enforce the use of {@link #create()}.
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
            throw new IllegalStateException("boudary header is missing");
        }
        MultiPartDecoder decoder = MultiPartDecoder.create(boundary, context);
        publisher.subscribe(decoder);
        return (Mono<U>) Multi.from(decoder)
                .collect(LIST_SUPPLIER, COLLECTOR)
                .flatMap(MAPPER);
    }

    /**
     * Create a new instance of {@link MultiPartBodyReader}.
     * @return MultiPartReader
     */
    public static MultiPartBodyReader create() {
        return new MultiPartBodyReader();
    }

    /**
     * A mapper that creates that maps the list of buffered
     * {@link InboundBodyPart} to a {@link Mono} of {@link InboundMultiPart}.
     */
    private static final class Mapper
            implements Function<List<InboundBodyPart>, Mono<InboundMultiPart>> {

        @Override
        public Mono<InboundMultiPart> apply(List<InboundBodyPart> bodyParts) {
            return Mono.just(new InboundMultiPart(bodyParts));
        }
    }

    /**
     * A collector that accumulates and buffers body parts.
     */
    private static final class Collector
            implements BiConsumer<List<InboundBodyPart>, InboundBodyPart> {

        @Override
        public void accept(List<InboundBodyPart> bodyParts,
                InboundBodyPart bodyPart) {

            MessageBodyReadableContent content = bodyPart.content();

            // buffer the data
            Publisher<DataChunk> bufferedData = ByteArrayBodyWriter
                    .write(ByteArrayBodyReader.read(content),
                            /* copy */ true);

            // create a content copy with the buffered data
            MessageBodyReadableContent contentCopy = MessageBodyReadableContent
                    .create(bufferedData, content.context());

            // create a new body part with the buffered content
            InboundBodyPart bufferedBodyPart = InboundBodyPart.builder()
                    .headers(bodyPart.headers())
                    .content(contentCopy)
                    .buffered()
                    .build();
            bodyParts.add(bufferedBodyPart);
        }
    }
}
