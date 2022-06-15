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
package io.helidon.media.jsonb;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Objects;
import java.util.concurrent.Flow.Publisher;

import io.helidon.common.GenericType;
import io.helidon.common.http.DataChunk;
import io.helidon.common.reactive.Single;
import io.helidon.media.common.ContentReaders;
import io.helidon.media.common.EntitySupport.Reader;
import io.helidon.media.common.EntitySupport.ReaderContext;

import jakarta.json.bind.Jsonb;
import jakarta.json.bind.JsonbException;

/**
 * {@link Reader} implementation supporting object binding with JSON-B.
 */
class JsonbReader implements Reader<Object> {

    private final Jsonb jsonb;

    /**
     * Create a new instance.
     *
     * @param jsonb JSON-B instance
     */
    JsonbReader(Jsonb jsonb) {
        Objects.requireNonNull(jsonb);
        this.jsonb = jsonb;
    }

    @Override
    public PredicateResult accept(GenericType<?> type, ReaderContext context) {
        return !CharSequence.class.isAssignableFrom(type.rawType())
                ? PredicateResult.COMPATIBLE
                : PredicateResult.NOT_SUPPORTED;
    }

    @Override
    public <U> Single<U> read(Publisher<DataChunk> publisher, GenericType<U> type, ReaderContext context) {
        return ContentReaders.readBytes(publisher).map(bytes -> read(bytes, type));
    }

    private <U> U read(byte[] bytes, GenericType<U> type) {
        try (InputStream inputStream = new ByteArrayInputStream(bytes)) {
            return jsonb.fromJson(inputStream, type.type());
        } catch (IOException ex) {
            throw new JsonbException(ex.getMessage(), ex);
        }
    }
}
