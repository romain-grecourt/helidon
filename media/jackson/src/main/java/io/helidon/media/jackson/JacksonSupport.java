/*
 * Copyright (c) 2019, 2022 Oracle and/or its affiliates.
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

import java.util.Collection;
import java.util.List;
import java.util.Objects;

import io.helidon.common.LazyValue;
import io.helidon.media.common.MediaSupport;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.fasterxml.jackson.module.paramnames.ParameterNamesModule;

/**
 * Support for Jackson integration.
 * <p>
 * For usage examples navigate to the {@link MediaSupport}.
 */
@SuppressWarnings("unused")
public final class JacksonSupport implements MediaSupport {

    private static final ObjectMapper MAPPER = new ObjectMapper()
            .registerModule(new ParameterNamesModule())
            .registerModule(new Jdk8Module())
            .registerModule(new JavaTimeModule());
    private static final LazyValue<JacksonSupport> DEFAULT = LazyValue.create(() -> new JacksonSupport(MAPPER));

    private final JacksonReader reader;
    private final JacksonWriter writer;
    private final JacksonStreamWriter streamWriter;
    private final JacksonEsStreamWriter esStreamWriter;
    private final JacksonNdStreamWriter ndStreamWriter;

    private JacksonSupport(final ObjectMapper objectMapper) {
        Objects.requireNonNull(objectMapper);
        this.reader = new JacksonReader(objectMapper);
        this.writer = new JacksonWriter(objectMapper);
        this.streamWriter = new JacksonStreamWriter(objectMapper);
        this.esStreamWriter = new JacksonEsStreamWriter(objectMapper);
        this.ndStreamWriter = new JacksonNdStreamWriter(objectMapper);
    }

    /**
     * Creates a new {@link JacksonSupport}.
     *
     * @return a new {@link JacksonSupport}
     */
    public static JacksonSupport create() {
        return DEFAULT.get();
    }

    /**
     * Creates a new {@link JacksonSupport}.
     *
     * @param objectMapper must not be {@code null}
     * @return a new {@link JacksonSupport}
     */
    public static JacksonSupport create(ObjectMapper objectMapper) {
        return new JacksonSupport(objectMapper);
    }

    /**
     * Return a default Jackson entity reader.
     *
     * @return default Jackson writer instance
     */
    public static Reader<Object> reader() {
        return DEFAULT.get().reader;
    }

    /**
     * Create a new Jackson entity reader based on {@link ObjectMapper} instance.
     *
     * @param objectMapper object mapper instance
     * @return new Jackson reader instance
     */
    public static Reader<Object> reader(ObjectMapper objectMapper) {
        return new JacksonReader(objectMapper);
    }

    /**
     * Return a default Jackson entity writer.
     *
     * @return default Jackson writer instance
     */
    public static Writer<Object> writer() {
        return DEFAULT.get().writer;
    }

    /**
     * Create a new Jackson entity writer based on {@link ObjectMapper} instance.
     *
     * @param objectMapper object mapper instance
     * @return new Jackson writer instance
     */
    public static Writer<Object> writer(ObjectMapper objectMapper) {
        return new JacksonWriter(objectMapper);
    }

    /**
     * Return a default Jackson entity stream writer.
     *
     * @return default Jackson writer stream instance
     */
    public static StreamWriter<Object> streamWriter() {
        return DEFAULT.get().streamWriter;
    }

    /**
     * Create a new Jackson entity stream writer based on {@link ObjectMapper} instance.
     *
     * @param objectMapper object mapper instance
     * @return new Jackson stream writer instance
     */
    public static StreamWriter<Object> streamWriter(ObjectMapper objectMapper) {
        return new JacksonEsStreamWriter(objectMapper);
    }

    /**
     * Return a default Jackson entity event stream writer.
     * This writer is for {@link io.helidon.common.http.MediaType#TEXT_EVENT_STREAM} content type.
     *
     * @return new Jackson stream writer instance
     */
    public static StreamWriter<Object> eventStreamWriter() {
        return DEFAULT.get().esStreamWriter;
    }

    /**
     * Create a new Jackson entity stream writer based on {@link ObjectMapper} instance.
     * This writer is for {@link io.helidon.common.http.MediaType#TEXT_EVENT_STREAM} content type.
     *
     * @param objectMapper object mapper instance
     * @return new Jackson stream writer instance
     */
    public static StreamWriter<Object> eventStreamWriter(ObjectMapper objectMapper) {
        return new JacksonEsStreamWriter(objectMapper);
    }

    /**
     * Return a default Jackson entity event stream writer.
     * This writer is for {@link io.helidon.common.http.MediaType#APPLICATION_X_NDJSON} content type.
     *
     * @return new Jackson stream writer instance
     */
    public static StreamWriter<Object> ndJsonStreamWriter() {
        return DEFAULT.get().ndStreamWriter;
    }

    /**
     * Create a new Jackson entity stream writer based on {@link ObjectMapper} instance.
     * This writer is for {@link io.helidon.common.http.MediaType#APPLICATION_X_NDJSON} content type.
     *
     * @param objectMapper object mapper instance
     * @return new Jackson stream writer instance
     */
    public static StreamWriter<Object> ndJsonStreamWriter(ObjectMapper objectMapper) {
        return new JacksonNdStreamWriter(objectMapper);
    }

    /**
     * Return Jackson reader instance.
     *
     * @return Jackson reader instance
     */
    public Reader<Object> readerInstance() {
        return reader;
    }

    /**
     * Return Jackson writer instance.
     *
     * @return Jackson writer instance
     */
    public Writer<Object> writerInstance() {
        return writer;
    }

    /**
     * Return Jackson stream writer instance.
     *
     * @return Jackson stream writer instance
     */
    public StreamWriter<Object> streamWriterInstance() {
        return streamWriter;
    }

    /**
     * Return Jackson stream writer instance for {@link io.helidon.common.http.MediaType#TEXT_EVENT_STREAM} content type.
     *
     * @return Jackson event stream writer instance
     */
    public StreamWriter<Object> eventStreamWriterInstance() {
        return esStreamWriter;
    }

    /**
     * Return Jackson stream writer instance for {@link io.helidon.common.http.MediaType#APPLICATION_X_NDJSON} content type.
     *
     * @return Jackson event stream writer instance
     */
    public StreamWriter<Object> ndJsonStreamWriterInstance() {
        return ndStreamWriter;
    }

    @Override
    public Collection<Reader<?>> readers() {
        return List.of(reader);
    }

    @Override
    public Collection<Writer<?>> writers() {
        return List.of(writer);
    }

    @Override
    public Collection<StreamWriter<?>> streamWriters() {
        return List.of(streamWriter, ndStreamWriter, esStreamWriter);
    }
}
