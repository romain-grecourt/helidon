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
package io.helidon.media.jsonp;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import io.helidon.common.LazyValue;
import io.helidon.media.common.MediaSupport;

import jakarta.json.Json;
import jakarta.json.JsonReaderFactory;
import jakarta.json.JsonStructure;
import jakarta.json.JsonWriterFactory;

/**
 * Support for JSON Processing integration.
 *
 * For usage examples navigate to {@link MediaSupport}.
 */
@SuppressWarnings("unused")
public final class JsonpSupport implements MediaSupport {

    private static final LazyValue<JsonpSupport> DEFAULT =
            LazyValue.create(() -> new JsonpSupport(Builder.readerFactory(null), Builder.writerFactory(null)));

    private final JsonpReader reader;
    private final JsonpWriter writer;
    private final JsonpStreamWriter streamWriter;
    private final JsonpEsStreamWriter esStreamWriter;
    private final JsonpNdStreamWriter ndStreamWriter;

    private JsonpSupport(JsonReaderFactory readerFactory, JsonWriterFactory writerFactory) {
        reader = new JsonpReader(readerFactory);
        writer = new JsonpWriter(writerFactory);
        streamWriter = new JsonpStreamWriter(writerFactory);
        esStreamWriter = new JsonpEsStreamWriter(writerFactory);
        ndStreamWriter = new JsonpNdStreamWriter(writerFactory);
    }

    /**
     * Provides a default instance for JSON-P readers and writers.
     *
     * @return json processing with default configuration
     */
    public static JsonpSupport create() {
        return DEFAULT.get();
    }

    /**
     * Create an instance with the provided JSON-P configuration.
     *
     * @param jsonPConfig configuration of the processing library
     * @return a configured JSON-P instance
     */
    public static JsonpSupport create(Map<String, ?> jsonPConfig) {
        return builder().jsonProcessingConfig(jsonPConfig).build();
    }

    /**
     * Fluent API builder to create instances of JSON-P.
     *
     * @return a new builder instance
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Return a default JSON-P entity reader.
     *
     * @return default JSON-P reader instance
     */
    public static Reader<JsonStructure> reader() {
        return DEFAULT.get().reader;
    }

    /**
     * Create a new JSON-P entity reader based on {@link JsonReaderFactory}.
     *
     * @param readerFactory json reader factory
     * @return new JSON-P  reader instance
     */
    public static Reader<JsonStructure> reader(JsonReaderFactory readerFactory) {
        return new JsonpReader(readerFactory);
    }

    /**
     * Return a default JSON-P entity writer.
     *
     * @return default JSON-P writer instance
     */
    public static Writer<JsonStructure> writer() {
        return DEFAULT.get().writer;
    }

    /**
     * Create a new JSON-P entity writer based on {@link JsonWriterFactory}.
     *
     * @param writerFactory json writer factory
     * @return new JSON-P writer instance
     */
    public static Writer<JsonStructure> writer(JsonWriterFactory writerFactory) {
        return new JsonpWriter(writerFactory);
    }

    /**
     * Return a default JSON-P entity stream writer.
     *
     * @return default JSON-P stream writer instance
     */
    public static StreamWriter<JsonStructure> streamWriter() {
        return DEFAULT.get().streamWriter;
    }

    /**
     * Create a new JSON-P entity stream writer based on {@link JsonWriterFactory}.
     *
     * @param writerFactory json writer factory
     * @return new JSON-P stream writer instance
     */
    public static StreamWriter<JsonStructure> streamWriter(JsonWriterFactory writerFactory) {
        return new JsonpStreamWriter(writerFactory);
    }

    /**
     * Return a default JSON-P entity event stream writer.
     * This writer is for {@link io.helidon.common.http.MediaType#TEXT_EVENT_STREAM} content type.
     *
     * @return new JSON-P stream writer instance
     */
    public static StreamWriter<JsonStructure> eventStreamWriter() {
        return DEFAULT.get().esStreamWriter;
    }

    /**
     * Create a new JSON-P entity stream writer based on {@link JsonWriterFactory} instance.
     * This writer is for {@link io.helidon.common.http.MediaType#TEXT_EVENT_STREAM} content type.
     *
     * @param writerFactory json writer factory
     * @return new JSON-P stream writer instance
     */
    public static StreamWriter<JsonStructure> eventStreamWriter(JsonWriterFactory writerFactory) {
        return new JsonpEsStreamWriter(writerFactory);
    }

    /**
     * Return a default JSON-P entity event stream writer.
     * This writer is for {@link io.helidon.common.http.MediaType#APPLICATION_X_NDJSON} content type.
     *
     * @return new JSON-P stream writer instance
     */
    public static StreamWriter<JsonStructure> ndJsonStreamWriter() {
        return DEFAULT.get().ndStreamWriter;
    }

    /**
     * Create a new JSON-P entity stream writer based on {@link JsonWriterFactory} instance.
     * This writer is for {@link io.helidon.common.http.MediaType#APPLICATION_X_NDJSON} content type.
     *
     * @param writerFactory json writer factory
     * @return new JSON-P stream writer instance
     */
    public static StreamWriter<JsonStructure> ndJsonStreamWriter(JsonWriterFactory writerFactory) {
        return new JsonpNdStreamWriter(writerFactory);
    }

    /**
     * Return JSON-P reader instance.
     *
     * @return JSON-P reader instance
     */
    public Reader<JsonStructure> readerInstance() {
        return reader;
    }

    /**
     * Return JSON-P entity writer.
     *
     * @return JSON-P writer instance
     */
    public Writer<JsonStructure> writerInstance() {
        return writer;
    }

    /**
     * Return JSON-P stream writer.
     * <p>
     * This stream writer supports {@link java.util.concurrent.Flow.Publisher publishers}
     * of {@link jakarta.json.JsonStructure} (such as {@link jakarta.json.JsonObject}),
     * writing them as an array of JSONs.
     *
     * @return JSON processing stream writer.
     */
    public StreamWriter<JsonStructure> streamWriterInstance() {
        return streamWriter;
    }

    /**
     * Return JSON-P stream writer.
     * <p>
     * This stream writer supports {@link java.util.concurrent.Flow.Publisher publishers}
     * of {@link jakarta.json.JsonStructure} (such as {@link jakarta.json.JsonObject}),
     * writing them as separate entries in the following format:
     * <pre><code>
     * data: {"json":"data"}\n
     * \n
     * data: {"json2":"data2"}\n
     * \n
     * </code></pre>
     *
     * This writer is for {@link io.helidon.common.http.MediaType#TEXT_EVENT_STREAM} content type.
     *
     * @return JSON processing stream writer.
     */
    public StreamWriter<JsonStructure> eventStreamWriterInstance() {
        return esStreamWriter;
    }

    /**
     * Return JSON-P stream writer.
     * <p>
     * This stream writer supports {@link java.util.concurrent.Flow.Publisher publishers}
     * of {@link jakarta.json.JsonStructure} (such as {@link jakarta.json.JsonObject}),
     * writing them as separate entries in the following format:
     * <pre><code>
     * {"json":"data"}\n
     * {"json2":"data2"}
     * </code></pre>
     *
     * This writer is for {@link io.helidon.common.http.MediaType#APPLICATION_X_NDJSON} content type.
     *
     * @return JSON processing stream writer.
     */
    public StreamWriter<JsonStructure> ndJsonStreamWriterInstance() {
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

    /**
     * Fluent-API builder for {@link JsonpSupport}.
     */
    public static class Builder implements io.helidon.common.Builder<Builder, JsonpSupport> {

        private JsonWriterFactory jsonWriterFactory;
        private JsonReaderFactory jsonReaderFactory;
        private Map<String, ?> jsonPConfig;

        @Override
        public JsonpSupport build() {
            if ((null == jsonReaderFactory) && (null == jsonWriterFactory) && (null == jsonPConfig)) {
                return DEFAULT.get();
            }

            if (null == jsonPConfig) {
                jsonPConfig = new HashMap<>();
            }

            if (null == jsonWriterFactory) {
                jsonWriterFactory = writerFactory(jsonPConfig);
            }

            if (null == jsonReaderFactory) {
                jsonReaderFactory = readerFactory(jsonPConfig);
            }

            return new JsonpSupport(jsonReaderFactory, jsonWriterFactory);
        }

        private static JsonReaderFactory readerFactory(Map<String, ?> jsonPConfig) {
            return Json.createReaderFactory(jsonPConfig);
        }

        private static JsonWriterFactory writerFactory(Map<String, ?> jsonPConfig) {
            return Json.createWriterFactory(jsonPConfig);
        }

        /**
         * Configuration to use when creating reader and writer factories.
         *
         * @param config configuration of JSON-P library
         * @return updated builder instance
         */
        public Builder jsonProcessingConfig(Map<String, ?> config) {
            this.jsonPConfig = config;
            this.jsonWriterFactory = null;
            this.jsonReaderFactory = null;
            return this;
        }

        /**
         * Explicit JSON-P Writer factory instance.
         *
         * @param factory writer factory
         * @return updated builder instance
         */
        public Builder jsonWriterFactory(JsonWriterFactory factory) {
            this.jsonWriterFactory = factory;
            return this;
        }

        /**
         * Explicit JSON-P Reader factory instance.
         *
         * @param factory reader factory
         * @return updated builder instance
         */
        public Builder jsonReaderFactory(JsonReaderFactory factory) {
            this.jsonReaderFactory = factory;
            return this;
        }
    }
}
