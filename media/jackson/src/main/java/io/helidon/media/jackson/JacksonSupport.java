/*
 * Copyright (c) 2019, 2020 Oracle and/or its affiliates.
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
import io.helidon.media.common.EntitySupport;
import io.helidon.media.common.MediaSupport;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.fasterxml.jackson.module.paramnames.ParameterNamesModule;

/**
 * Support for Jackson integration.
 *
 * For usage examples navigate to the {@link MediaSupport}.
 */
public final class JacksonSupport implements MediaSupport {

    private static final ObjectMapper MAPPER = new ObjectMapper()
            .registerModule(new ParameterNamesModule())
            .registerModule(new Jdk8Module())
            .registerModule(new JavaTimeModule());
    private static final LazyValue<JacksonSupport> DEFAULT = LazyValue.create(() -> new JacksonSupport(MAPPER));

    private final JacksonBodyReader reader;
    private final JacksonBodyWriter writer;
    private final JacksonBodyStreamWriter streamWriter;
    private final JacksonEsBodyStreamWriter esStreamWriter;
    private final JacksonNdBodyStreamWriter ndStreamWriter;

    private JacksonSupport(final ObjectMapper objectMapper) {
        this.reader = JacksonBodyReader.create(objectMapper);
        this.writer = JacksonBodyWriter.create(objectMapper);
        this.streamWriter = JacksonBodyStreamWriter.create(objectMapper);
        this.esStreamWriter = JacksonEsBodyStreamWriter.create(objectMapper);
        this.ndStreamWriter = JacksonNdBodyStreamWriter.create(objectMapper);
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
        Objects.requireNonNull(objectMapper);
        return new JacksonSupport(objectMapper);
    }

    /**
     * Return a default Jackson entity reader.
     *
     * @return default Jackson body writer instance
     */
    public static EntitySupport.Reader<Object> reader() {
        return DEFAULT.get().reader;
    }

    /**
     * Create a new Jackson entity reader based on {@link ObjectMapper} instance.
     *
     * @param objectMapper object mapper instance
     * @return new Jackson body reader instance
     */
    public static EntitySupport.Reader<Object> reader(ObjectMapper objectMapper) {
        Objects.requireNonNull(objectMapper);
        return JacksonBodyReader.create(objectMapper);
    }

    /**
     * Return a default Jackson entity writer.
     *
     * @return default Jackson body writer instance
     */
    public static EntitySupport.Writer<Object> writer() {
        return DEFAULT.get().writer;
    }

    /**
     * Create a new Jackson entity writer based on {@link ObjectMapper} instance.
     *
     * @param objectMapper object mapper instance
     * @return new Jackson body writer instance
     */
    public static EntitySupport.Writer<Object> writer(ObjectMapper objectMapper) {
        Objects.requireNonNull(objectMapper);
        return JacksonBodyWriter.create(objectMapper);
    }

    /**
     * Return a default Jackson entity stream writer.
     *
     * @return default Jackson body writer stream instance
     */
    public static EntitySupport.StreamWriter<Object> streamWriter() {
        return DEFAULT.get().streamWriter;
    }

    /**
     * Create a new Jackson entity stream writer based on {@link ObjectMapper} instance.
     *
     * @param objectMapper object mapper instance
     * @return new Jackson body stream writer instance
     */
    public static EntitySupport.StreamWriter<Object> streamWriter(ObjectMapper objectMapper) {
        Objects.requireNonNull(objectMapper);
        return JacksonEsBodyStreamWriter.create(objectMapper);
    }

    /**
     * Return a default Jackson entity event stream writer.
     * This writer is for {@link io.helidon.common.http.MediaType#TEXT_EVENT_STREAM} content type.
     *
     * @return new Jackson body stream writer instance
     */
    public static EntitySupport.StreamWriter<Object> eventStreamWriter() {
        return DEFAULT.get().esStreamWriter;
    }

    /**
     * Create a new Jackson entity stream writer based on {@link ObjectMapper} instance.
     * This writer is for {@link io.helidon.common.http.MediaType#TEXT_EVENT_STREAM} content type.
     *
     * @param objectMapper object mapper instance
     * @return new Jackson body stream writer instance
     */
    public static EntitySupport.StreamWriter<Object> eventStreamWriter(ObjectMapper objectMapper) {
        Objects.requireNonNull(objectMapper);
        return JacksonEsBodyStreamWriter.create(objectMapper);
    }

    /**
     * Return a default Jackson entity event stream writer.
     * This writer is for {@link io.helidon.common.http.MediaType#APPLICATION_X_NDJSON} content type.
     *
     * @return new Jackson body stream writer instance
     */
    public static EntitySupport.StreamWriter<Object> ndJsonStreamWriter() {
        return DEFAULT.get().ndStreamWriter;
    }

    /**
     * Create a new Jackson entity stream writer based on {@link ObjectMapper} instance.
     * This writer is for {@link io.helidon.common.http.MediaType#APPLICATION_X_NDJSON} content type.
     *
     * @param objectMapper object mapper instance
     * @return new Jackson body stream writer instance
     */
    public static EntitySupport.StreamWriter<Object> ndJsonStreamWriter(ObjectMapper objectMapper) {
        Objects.requireNonNull(objectMapper);
        return JacksonNdBodyStreamWriter.create(objectMapper);
    }

    /**
     * Return Jackson reader instance.
     *
     * @return Jackson reader instance
     */
    public EntitySupport.Reader<Object> readerInstance() {
        return reader;
    }

    /**
     * Return Jackson writer instance.
     *
     * @return Jackson writer instance
     */
    public EntitySupport.Writer<Object> writerInstance() {
        return writer;
    }

    /**
     * Return Jackson stream writer instance.
     *
     * @return Jackson stream writer instance
     */
    public EntitySupport.StreamWriter<Object> streamWriterInstance() {
        return streamWriter;
    }

    /**
     * Return Jackson stream writer instance for {@link io.helidon.common.http.MediaType#TEXT_EVENT_STREAM} content type.
     *
     * @return Jackson event stream writer instance
     */
    public EntitySupport.StreamWriter<Object> eventStreamWriterInstance() {
        return esStreamWriter;
    }

    /**
     * Return Jackson stream writer instance for {@link io.helidon.common.http.MediaType#APPLICATION_X_NDJSON} content type.
     *
     * @return Jackson event stream writer instance
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
