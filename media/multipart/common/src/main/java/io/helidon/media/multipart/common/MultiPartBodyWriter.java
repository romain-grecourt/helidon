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
import io.helidon.common.reactive.Multi;
import io.helidon.common.reactive.Mono;
import io.helidon.media.common.MessageBodyWriter;
import io.helidon.media.common.MessageBodyWriterContext;
import java.util.function.Function;

/**
 * {@link OutboundMultiPart} writer.
 */
public final class MultiPartBodyWriter implements
        MessageBodyWriter<OutboundMultiPart> {

    /**
     * The default boundary used for encoding multipart messages.
     */
    public static final String DEFAULT_BOUNDARY = "[^._.^]==>boundary<==[^._.^]";

    /**
     * The actual boundary string.
     */
    private final String boundary;

    /**
     * Private to enforce the use of {@link #create(java.lang.String)}.
     * @param boundary 
     */
    private MultiPartBodyWriter(String boundary) {
        this.boundary = boundary;
    }

    @Override
    public boolean accept(GenericType<?> type,
            MessageBodyWriterContext context) {

        return OutboundMultiPart.class.isAssignableFrom(type.rawType());
    }

    @Override
    public Publisher<DataChunk> write(Mono<OutboundMultiPart> content,
            GenericType<? extends OutboundMultiPart> type,
            MessageBodyWriterContext context) {

        context.contentType(MediaType.MULTIPART_FORM_DATA);
        return content.flatMapMany(new Mapper(boundary, context));
    }

    /**
     * Create a new instance of {@link MultiPartBodyWriter} with the specified
     * boundary delimiter.
     *
     * @param boundary boundary string
     * @return MultiPartWriter
     */
    public static MultiPartBodyWriter create(String boundary) {
        return new MultiPartBodyWriter(boundary);
    }

    /**
     * Create a new instance of {@link MultiPartBodyWriter} with the default
     * boundary delimiter.
     *
     * @return MultiPartWriter
     */
    public static MultiPartBodyWriter create() {
        return new MultiPartBodyWriter(DEFAULT_BOUNDARY);
    }

    private static final class Mapper
            implements Function<OutboundMultiPart, Publisher<DataChunk>> {

        private final MultiPartEncoder encoder;

        Mapper(String boundary, MessageBodyWriterContext context) {
            this.encoder = MultiPartEncoder.create(boundary, context);
        }

        @Override
        public Publisher<DataChunk> apply(OutboundMultiPart multiPart) {
            Multi.just(multiPart.bodyParts()).subscribe(encoder);
            return encoder;
        }
    }
}
