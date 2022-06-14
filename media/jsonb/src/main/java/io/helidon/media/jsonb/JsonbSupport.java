/*
 * Copyright (c) 2020, 2021 Oracle and/or its affiliates.
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

import java.util.Collection;
import java.util.List;
import java.util.Objects;

import io.helidon.common.LazyValue;
import io.helidon.media.common.EntitySupport;
import io.helidon.media.common.MediaSupport;

import jakarta.json.bind.Jsonb;
import jakarta.json.bind.JsonbBuilder;

/**
 * Support for JSON-B integration.
 *
 * For usage examples navigate to the {@link MediaSupport}.
 *
 * @see Jsonb
 */
public final class JsonbSupport implements MediaSupport {

    private static final Jsonb JSON_B = JsonbBuilder.create();
    private static final LazyValue<JsonbSupport> DEFAULT = LazyValue.create(() -> new JsonbSupport(JSON_B));

    private final JsonbBodyReader reader;
    private final JsonbBodyWriter writer;
    private final JsonbBodyStreamWriter streamWriter;
    private final JsonbEsBodyStreamWriter esStreamWriter;
    private final JsonbNdBodyStreamWriter ndStreamWriter;

    private JsonbSupport(Jsonb jsonb) {
        this.reader = JsonbBodyReader.create(jsonb);
        this.writer = JsonbBodyWriter.create(jsonb);
        this.streamWriter = JsonbBodyStreamWriter.create(jsonb);
        this.esStreamWriter = JsonbEsBodyStreamWriter.create(jsonb);
        this.ndStreamWriter = JsonbNdBodyStreamWriter.create(jsonb);
    }

    /**
     * Creates a new {@link JsonbSupport}.
     *
     * @return a new {@link JsonbSupport}
     */
    public static JsonbSupport create() {
        return DEFAULT.get();
    }

    /**
     * Creates a new {@link JsonbSupport}.
     *
     * @param jsonb the JSON-B to use; must not be {@code null}
     *
     * @return a new {@link JsonbSupport}
     *
     * @exception NullPointerException if {@code jsonb} is {@code
     * null}
     */
    public static JsonbSupport create(Jsonb jsonb) {
        Objects.requireNonNull(jsonb);
        return new JsonbSupport(jsonb);
    }

    /**
     * Return a default JSON-B entity reader.
     *
     * @return default JSON-B body writer instance
     */
    public static EntitySupport.Reader<Object> reader() {
        return DEFAULT.get().reader;
    }

    /**
     * Create a new JSON-B entity reader based on {@link Jsonb} instance.
     *
     * @param jsonb jsonb instance
     * @return new JSON-B body reader instance
     */
    public static EntitySupport.Reader<Object> reader(Jsonb jsonb) {
        Objects.requireNonNull(jsonb);
        return JsonbBodyReader.create(jsonb);
    }

    /**
     * Return a default JSON-B entity writer.
     *
     * @return default JSON-B body writer instance
     */
    public static EntitySupport.Writer<Object> writer() {
        return DEFAULT.get().writer;
    }

    /**
     * Create a new JSON-B entity writer based on {@link Jsonb} instance.
     *
     * @param jsonb jsonb instance
     * @return new JSON-B body writer instance
     */
    public static EntitySupport.Writer<Object> writer(Jsonb jsonb) {
        Objects.requireNonNull(jsonb);
        return JsonbBodyWriter.create(jsonb);
    }

    /**
     * Return a default JSON-B entity stream writer.
     *
     * @return default JSON-B body writer stream instance
     */
    public static EntitySupport.StreamWriter<Object> streamWriter() {
        return DEFAULT.get().streamWriter;
    }

    /**
     * Create a new JSON-B entity stream writer based on {@link Jsonb} instance.
     *
     * @param jsonb jsonb instance
     * @return new JSON-B body stream writer instance
     */
    public static EntitySupport.StreamWriter<Object> streamWriter(Jsonb jsonb) {
        Objects.requireNonNull(jsonb);
        return JsonbBodyStreamWriter.create(jsonb);
    }

    /**
     * Return a default JSON-B entity event stream writer.
     * This writer is for {@link io.helidon.common.http.MediaType#TEXT_EVENT_STREAM} content type.
     *
     * @return new JSON-B body stream writer instance
     */
    public static EntitySupport.StreamWriter<Object> eventStreamWriter() {
        return DEFAULT.get().esStreamWriter;
    }

    /**
     * Create a new JSON-B entity stream writer based on {@link Jsonb} instance.
     * This writer is for {@link io.helidon.common.http.MediaType#TEXT_EVENT_STREAM} content type.
     *
     * @param jsonb jsonb instance
     * @return new JSON-B body stream writer instance
     */
    public static EntitySupport.StreamWriter<Object> eventStreamWriter(Jsonb jsonb) {
        Objects.requireNonNull(jsonb);
        return JsonbEsBodyStreamWriter.create(jsonb);
    }

    /**
     * Return a default JSON-B entity event stream writer.
     * This writer is for {@link io.helidon.common.http.MediaType#APPLICATION_X_NDJSON} content type.
     *
     * @return new JSON-B body stream writer instance
     */
    public static EntitySupport.StreamWriter<Object> ndJsonStreamWriter() {
        return DEFAULT.get().ndStreamWriter;
    }

    /**
     * Create a new JSON-B entity stream writer based on {@link Jsonb} instance.
     * This writer is for {@link io.helidon.common.http.MediaType#APPLICATION_X_NDJSON} content type.
     *
     * @param jsonb jsonb instance
     * @return new JSON-B body stream writer instance
     */
    public static EntitySupport.StreamWriter<Object> ndJsonStreamWriter(Jsonb jsonb) {
        Objects.requireNonNull(jsonb);
        return JsonbNdBodyStreamWriter.create(jsonb);
    }

    /**
     * Return JSON-B reader instance.
     *
     * @return JSON-B reader instance
     */
    public EntitySupport.Reader<Object> readerInstance() {
        return reader;
    }

    /**
     * Return JSON-B writer instance.
     *
     * @return JSON-B writer instance
     */
    public EntitySupport.Writer<Object> writerInstance() {
        return writer;
    }

    /**
     * Return JSON-B stream writer instance.
     *
     * @return JSON-B stream writer instance
     */
    public EntitySupport.StreamWriter<Object> streamWriterInstance() {
        return streamWriter;
    }

    /**
     * Return JSON-B stream writer instance for {@link io.helidon.common.http.MediaType#TEXT_EVENT_STREAM} content type.
     *
     * @return JSON-B event stream writer instance
     */
    public EntitySupport.StreamWriter<Object> eventStreamWriterInstance() {
        return esStreamWriter;
    }

    /**
     * Return JSON-B stream writer instance for {@link io.helidon.common.http.MediaType#APPLICATION_X_NDJSON} content type.
     *
     * @return JSON-B event stream writer instance
     */
    public EntitySupport.StreamWriter<Object> ndJsonStreamWriterInstance() {
        return ndStreamWriter;
    }


    @Override
    public Collection<EntitySupport.Reader<?>> readers() {
        return List.of(reader);
    }

    @Override
    public Collection<EntitySupport.Writer<?>> writers() {
        return List.of(writer);
    }

    @Override
    public Collection<EntitySupport.StreamWriter<?>> streamWriters() {
        return List.of(streamWriter, ndStreamWriter, esStreamWriter);
    }

}
