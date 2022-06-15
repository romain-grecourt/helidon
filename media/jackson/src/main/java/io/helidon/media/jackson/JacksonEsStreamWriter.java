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
package io.helidon.media.jackson;

import java.nio.charset.StandardCharsets;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.Flow;

import io.helidon.common.GenericType;
import io.helidon.common.http.DataChunk;
import io.helidon.common.http.MediaType;
import io.helidon.common.reactive.Multi;
import io.helidon.media.common.EntitySupport;
import io.helidon.media.common.EntitySupport.StreamWriter;
import io.helidon.media.common.EntitySupport.WriterContext;

import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * {@link StreamWriter} implementation supporting object binding with Jackson.
 * This writer is for {@link MediaType#TEXT_EVENT_STREAM} with no element-type parameter or
 * element-type="application/json".
 */
class JacksonEsStreamWriter implements StreamWriter<Object> {

    private static final MediaType TEXT_EVENT_STREAM_JSON = MediaType
            .parse("text/event-stream;element-type=\"application/json\"");
    private static final byte[] DATA = "data: ".getBytes(StandardCharsets.UTF_8);
    private static final byte[] NL = "\n\n".getBytes(StandardCharsets.UTF_8);

    private final ObjectMapper objectMapper;

    /**
     * Create a new instance.
     *
     * @param objectMapper object mapper to use
     */
    JacksonEsStreamWriter(ObjectMapper objectMapper) {
        this.objectMapper = Objects.requireNonNull(objectMapper);
    }

    @Override
    public EntitySupport.PredicateResult accept(GenericType<?> type, WriterContext context) {
        if (CharSequence.class.isAssignableFrom(type.rawType())) {
            return EntitySupport.PredicateResult.NOT_SUPPORTED;
        }
        return context.contentType()
                      .or(() -> findMediaType(context))
                      .filter(mediaType -> mediaType.equals(TEXT_EVENT_STREAM_JSON)
                              || mediaType.equals(MediaType.TEXT_EVENT_STREAM))
                      .map(it -> EntitySupport.PredicateResult.COMPATIBLE)
                      .orElse(EntitySupport.PredicateResult.NOT_SUPPORTED);
    }

    @Override
    public <U> Multi<DataChunk> write(Flow.Publisher<U> publisher, GenericType<U> type, WriterContext context) {
        MediaType contentType = context.contentType()
                                       .or(() -> findMediaType(context))
                                       .orElse(TEXT_EVENT_STREAM_JSON);
        context.contentType(contentType);
        return Multi.create(publisher)
                    .flatMap(o -> JacksonWriter.write(objectMapper, o, context.charset()))
                    .flatMap(chunk -> Multi.just(DataChunk.create(DATA), chunk, DataChunk.create(NL)));
    }

    private Optional<MediaType> findMediaType(WriterContext context) {
        try {
            return Optional.of(context.findAccepted(MediaType.JSON_EVENT_STREAM_PREDICATE, TEXT_EVENT_STREAM_JSON));
        } catch (IllegalStateException ignore) {
            //Not supported. Ignore exception.
            return Optional.empty();
        }
    }
}
