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

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.util.Objects;
import java.util.concurrent.Flow.Publisher;

import io.helidon.common.GenericType;
import io.helidon.common.http.DataChunk;
import io.helidon.common.reactive.Single;
import io.helidon.media.common.ContentReaders;
import io.helidon.media.common.EntitySupport.Reader;
import io.helidon.media.common.EntitySupport.ReaderContext;

import jakarta.json.JsonException;
import jakarta.json.JsonReader;
import jakarta.json.JsonReaderFactory;
import jakarta.json.JsonStructure;

/**
 * {@link Reader} implementation supporting {@link JsonStructure} sub-classes (JSON-P).
 */
final class JsonpReader implements Reader<JsonStructure> {

    private final JsonReaderFactory jsonFactory;

    /**
     * Create a new instance.
     *
     * @param jsonFactory json factory
     */
    JsonpReader(JsonReaderFactory jsonFactory) {
        Objects.requireNonNull(jsonFactory);
        this.jsonFactory = jsonFactory;
    }

    @Override
    public PredicateResult accept(GenericType<?> type, ReaderContext context) {
        return PredicateResult.supports(JsonStructure.class, type);
    }

    @Override
    public <U extends JsonStructure> Single<U> read(Publisher<DataChunk> publisher,
                                                    GenericType<U> type,
                                                    ReaderContext context) {

        return ContentReaders.readBytes(publisher).map(bytes -> read(bytes, type, context.charset()));
    }

    private <U extends JsonStructure> U read(byte[] bytes, GenericType<U> type, Charset charset) {
        InputStream is = new ByteArrayInputStream(bytes);
        JsonReader reader = jsonFactory.createReader(is, charset);
        JsonStructure json = reader.read();
        if (!type.rawType().isAssignableFrom(json.getClass())) {
            throw new JsonException("Unable to convert " + json.getClass() + " to " + type.rawType());
        }
        return (U) json;
    }
}
