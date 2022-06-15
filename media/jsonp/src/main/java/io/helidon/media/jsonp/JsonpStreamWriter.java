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
package io.helidon.media.jsonp;

import java.nio.charset.StandardCharsets;
import java.util.concurrent.Flow.Publisher;
import java.util.concurrent.atomic.AtomicBoolean;

import io.helidon.common.GenericType;
import io.helidon.common.http.DataChunk;
import io.helidon.common.http.MediaType;
import io.helidon.common.reactive.Multi;
import io.helidon.media.common.MediaContext.WriterContext;
import io.helidon.media.common.MediaSupport.PredicateResult;
import io.helidon.media.common.MediaSupport.StreamWriter;

import jakarta.json.JsonStructure;
import jakarta.json.JsonWriterFactory;

/**
 * {@link StreamWriter} implementation support {@link jakarta.json.JsonStructure} sub-classes (JSON-P).
 */
class JsonpStreamWriter implements StreamWriter<JsonStructure> {
    private static final byte[] ARRAY_JSON_END_BYTES = "]".getBytes(StandardCharsets.UTF_8);
    private static final byte[] ARRAY_JSON_BEGIN_BYTES = "[".getBytes(StandardCharsets.UTF_8);
    private static final byte[] COMMA_BYTES = ",".getBytes(StandardCharsets.UTF_8);

    private final JsonWriterFactory jsonWriterFactory;

    /**
     * Create a new instance.
     *
     * @param jsonWriterFactory json factory
     */
    JsonpStreamWriter(JsonWriterFactory jsonWriterFactory) {
        this.jsonWriterFactory = jsonWriterFactory;
    }

    @Override
    public PredicateResult accept(GenericType<?> type, WriterContext context) {
        return PredicateResult.supports(JsonStructure.class, type);
    }

    @Override
    public <U extends JsonStructure> Multi<DataChunk> write(Publisher<U> publisher,
                                                            GenericType<U> type,
                                                            WriterContext context) {

        MediaType contentType = context.findAccepted(MediaType.JSON_PREDICATE, MediaType.APPLICATION_JSON);
        context.contentType(contentType);

        // we do not have join operator
        AtomicBoolean first = new AtomicBoolean(true);

        return Multi.create(publisher)
                    .flatMap(o -> JsonpWriter.write(jsonWriterFactory, true, o, context.charset()))
                    .flatMap(it -> {
                        if (first.getAndSet(false)) {
                            // first record, do not prepend a comma
                            return Multi.just(DataChunk.create(ARRAY_JSON_BEGIN_BYTES), it);
                        } else {
                            // any subsequent record starts with a comma
                            return Multi.just(DataChunk.create(COMMA_BYTES), it);
                        }
                    })
                    .onCompleteResume(DataChunk.create(ARRAY_JSON_END_BYTES));
    }
}
