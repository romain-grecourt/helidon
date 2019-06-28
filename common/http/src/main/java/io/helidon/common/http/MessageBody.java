/*
 * Copyright (c) 2018, 2019 Oracle and/or its affiliates. All rights reserved.
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

package io.helidon.common.http;

import io.helidon.common.GenericType;
import io.helidon.common.reactive.Flow.Processor;
import io.helidon.common.reactive.Flow.Publisher;
import java.nio.charset.Charset;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletionStage;
import java.util.function.Function;
import java.util.function.Predicate;

/**
 * Provides APIs to convert HTTP payload ({@link ReadableContent}) to
 * objects or generate HTTP payload ({@link WriteableContent}) from objects.
 *
 * <ul>
 * <li> {@link Content} defines a reactive model for raw HTTP payload.</li>
 * <li> {@link ReadableContent} models inbound payload, it extends
 * {@link Content} with methods to be consumed as objects, (see
 * {@link ReadableContent#as(io.helidon.common.GenericType)}).</li>
 * <li> {@link WriteableContent} represents outbound payload that can be
 * generated from objects.</li>
 * <li> {@link Reader} defines a reactive contract for converting payload to
 * objects.</li>
 * <li> {@link Writer} defines a reactive contract for converting objects to
 * outbound payload.</li>
 * <li> {@link Filter} defines a reactive contract for processing inbound
 * payload (before conversion) or outbound payload (after conversion).</li>
 * <li> {@link Writers} defines a registry of {@link Writer}.</li>
 * <li> {@link Readers} defines a registry of {@link Writer}.</li>
 * <li> {@link Filters} defines a registry of {@link Filter}.</li>
 * </ul>
 *
 * <p>
 * {@link Reader} and {@link Writer} need the headers as part of their
 * implementation. Headers correspond to an HTTP request (inbound or outbound)
 * or HTTP response (inbound or outbound) depending on the perspective: client
 * or a server.
 * </p>
 * <ul>
 * <li>A {@link Reader} instance can be used to convert the inbound payload of a
 * server request or client response to an object</li>
 * <li>A {@link Writer} instance can be used to generate the outbound payload of
 * a server response or client request from an object</li>
 * </ul>
 *
 * <p>
 * {@link Context} abstracts the headers so that the {@link Reader} and
 * {@link Writer} contracts are not tied to client or server specific APIs. This
 * enables implementation of {@link Reader} and {@link Writer} to be usable
 * either in a client or server perspective.
 * </p>
 * <ul>
 * <li> {@link ReaderContext} models inbound headers. It only support read-only
 * headers.</li>
 * <li> {@link WriterContext} models outbound headers and the inbound
 * {@code Accept} header value. It extends {@link Context} and provides methods
 * to work the {@code Accept} header, see
 * {@link WriterContext#findAccepted(io.helidon.common.http.MediaType)}.</li>
 * </ul>
 */
public interface MessageBody {

    /**
     * Base context contract for {@link Reader} and {@link Writer}, provides
     * an abstraction over server or client headers.
     */
    interface Context {

        /**
         * Get the underlying headers.
         * @return Parameters, never {@code null}
         */
        Parameters headers();

        /**
         * Derive the charset to use from the {@code Content-Type} header value
         * or using a fixed default charset as fallback.
         *
         * @return Charset, never {@code null}
         * @throws IllegalStateException if an error occurs loading the charset
         * specified by the {@code Content-Type} header value
         */
        Charset charset() throws IllegalStateException;

        /**
         * Get the {@code Content-Type} header.
         * @return Optional, never {@code null}
         */
        Optional<MediaType> contentType();
    }

    /**
     * Reader context specialization with read-only headers.
     */
    interface ReaderContext extends Context {

        @Override
        public ReadOnlyParameters headers();
    }

    /**
     * Writer context specialization outbound headers and inbound {@code Accept}
     * header value.
     */
    interface WriterContext extends Context {

        /**
         * Get the inbound {@code Accept} header.
         * @return List never {@code null}
         */
        public List<MediaType> acceptedTypes();

        /**
         * Set the {@code Content-Type} header value in the underlying headers
         * if not present.
         *
         * @param contentType {@code Content-Type} value to set, must not be
         * {@code null}
         */
        public void contentType(MediaType contentType);

        /**
         * Set the {@code Content-Length} header value in the underlying headers
         * if not present.
         *
         * @param contentLength {@code Content-Length} value to set, must be a
         * positive value
         */
        public void contentLength(long contentLength);

        /**
         * Find an media type in the inbound {@code Accept} header with the
         * given predicate and default value.
         * <ul>
         * <li>The default value is returned if the predicate matches a media
         * type with a wildcard subtype.<li>
         * <li>The default value if the current {@code Content-Type} header is
         * not set and the inbound {@code Accept} header is empty or
         * missing.</li>
         * <li>When the {@code Content-Type} header is set, if the predicate
         * matches the {@code Content-Type} header value is returned.</li>
         * </ul>
         *
         * @param predicate a predicate to match against the inbound
         * {@code Accept} header
         * @param defaultType a default media type
         * @return MediaType, never {@code null}
         * @throws IllegalStateException if no media type can be returned
         */
        public MediaType findAccepted(Predicate<MediaType> predicate,
                MediaType defaultType) throws IllegalStateException;

        /**
         * Find the given media type in the inbound {@code Accept} header.
         * @param mediaType media type to search for
         * @return MediaType, never {@code null}
         * @throws IllegalStateException if the media type is not found
         */
        public MediaType findAccepted(MediaType mediaType)
                throws IllegalStateException;
    }

    /**
     * Reactive contract for processing inbound payload (before conversion) or
     * outbound payload (after conversion).
     */
    interface Filter extends Processor<DataChunk, DataChunk> { }

    /**
     * Registry of {@link Filter} allowing to register filter instances in the
     * system. Inbound filters and outbound filters should be separated in
     * different registries.
     */
    interface Filters {

        @Deprecated
        void registerFilter(
                Function<Publisher<DataChunk>, Publisher<DataChunk>> function);

        /**
         * Register a filter.
         * @param filter filter to register
         * @return Filters
         */
        Filters registerFilter(Filter filter);
    }

    /**
     * Conversion operator that can convert from or to object of a given type.
     * @param <T> Type supported by the operator
     */
    interface Operator<T extends Context> {

        /**
         * Test if the operator can convert the given type.
         *
         * @param type the requested type
         * @param context the context providing the headers abstraction
         * @return {@code true} if the operator can convert the specified type
         * in the given context, {@code false} otherwise
         */
        boolean accept(GenericType<?> type, T context);
    }

    /**
     * Conversion operator that can convert raw HTTP payload into objects.
     * @param <T> type or base type supported by the operator
     */
    interface Reader<T> extends Operator<ReaderContext> {

        /**
         * Convert a raw HTTP payload into objects of the given type.
         * @param <U> actual requested type parameter
         * @param publisher raw HTTP payload
         * @param type requested type
         * @param context the context providing the headers abstraction
         * @return Publisher of objects
         */
        <U extends T> Publisher<U> read(Publisher<DataChunk> publisher,
                GenericType<U> type, ReaderContext context);
    }

    /**
     * Registry of {@link Reader} allowing to register reader instances in the
     * system.
     */
    interface Readers {

        /**
         * Register a reader.
         * @param <T> reader type
         * @param type class supported by the reader
         * @param reader reader to register
         * @deprecated {@link io.helidon.common.http.Reader} is deprecated, use
         * {@link Reader} instead
         */
        @Deprecated
        <T> void registerReader(Class<T> type,
                io.helidon.common.http.Reader<T> reader);

        /**
         * Register a reader.
         * @param <T> reader type
         * @param predicate class predicate
         * @param reader reader to register
         * @deprecated {@link io.helidon.common.http.Reader} is deprecated, use
         * {@link Reader} instead
         */
        @Deprecated
        <T> void registerReader(Predicate<Class<?>> predicate,
                io.helidon.common.http.Reader<T> reader);

        /**
         * Register a reader.
         * @param reader reader to register
         * @return Readers
         */
        Readers registerReader(Reader<?> reader);

        /**
         * Register a stream reader.
         * @param reader reader to register
         * @return Readers
         */
        Readers registerStreamReader(Reader<?> reader);
    }

    /**
     * Conversion operator that generate raw HTTP payload from objects.
     * @param <T> type or base type supported by the operator
     */
    interface Writer<T> extends Operator<WriterContext> {

        /**
         * Generate HTTP payload from the objects of the given type.
         * @param <U> actual requested type parameter
         * @param content objects to convert to payload
         * @param type requested type
         * @param context the context providing the headers abstraction
         * @return Publisher of objects
         */
         <U extends T> Publisher<DataChunk> write(Publisher<U> content,
                 GenericType<U> type, WriterContext context);
    }

    /**
     * Registry of {@link Writer} allowing to register writer instances in the
     * system.
     */
    interface Writers {

        @Deprecated
        <T> Writers registerWriter(Class<T> type,
                Function<T, Publisher<DataChunk>> function);

        @Deprecated
        <T> Writers registerWriter(Class<T> type, MediaType contentType,
                Function<? extends T, Publisher<DataChunk>> function);

        @Deprecated
        <T> Writers registerWriter(Predicate<?> accept,
                Function<T, Publisher<DataChunk>> function);

        @Deprecated
        <T> Writers registerWriter(Predicate<?> accept, MediaType contentType,
                Function<T, Publisher<DataChunk>> function);

        /**
         * Register a writer.
         * @param writer writer to register
         * @return Writers
         */
        Writers registerWriter(Writer<?> writer);

        /**
         * Register a stream writer.
         * @param writer writer to register
         * @return Writers
         */
        Writers registerStreamWriter(Writer<?> writer);
    }

    /**
     * Reactive contract for HTTP payload.
     */
    interface Content extends Publisher<DataChunk> { }

    /**
     * Inbound HTTP content that can be converted into objects.
     */
    interface ReadableContent extends Content, Readers, Filters {

        @Override
        ReadableContent registerFilter(Filter filter);

        @Override
        ReadableContent registerReader(Reader<?> reader);

        @Override
        ReadableContent registerStreamReader(Reader<?> reader);

        /**
         * Convert the content as a future of the given type.
         * @param <T> the requested type
         * @param type class representing the type to convert to
         * @return CompletionStage, never {@code null}
         */
        <T> CompletionStage<T> as(final Class<T> type);

        /**
         * Convert the content as a future of the given generic type.
         *
         * @param <T> the requested type
         * @param type generic type representing the type to convert to
         * @return CompletionStage, never {@code null}
         */
        <T> CompletionStage<T> as(final GenericType<T> type);

        /**
         * Convert the content as a publisher of objects of the given type.
         *
         * @param <T> the requested type
         * @param type class representing the type to convert to
         * @return Publisher, never {@code null}
         */
        <T> Publisher<T> asStream(Class<T> type);

        /**
         * Convert the content as a publisher of objects of the given generic
         * type.
         *
         * @param <T> the requested type
         * @param type generic type representing the type to convert to
         * @return Publisher, never {@code null}
         */
        <T> Publisher<T> asStream(GenericType<T> type);
    }

    /**
     * Outbound HTTP content that represents objects that can be converted
     * to HTTP payload.
     */
    interface WriteableContent extends Content, Writers, Filters {

        @Override
        WriteableContent registerFilter(Filter filter);

        @Override
        WriteableContent registerWriter(Writer<?> writer);

        @Override
        WriteableContent registerStreamWriter(Writer<?> writer);

        @Deprecated
        @Override
        <T> WriteableContent registerWriter(Class<T> type,
                Function<T, Publisher<DataChunk>> function);

        @Deprecated
        @Override
        <T> WriteableContent registerWriter(Predicate<?> accept,
                Function<T, Publisher<DataChunk>> function);

        @Deprecated
        @Override
        <T> WriteableContent registerWriter(Class<T> type,
                MediaType contentType,
                Function<? extends T, Publisher<DataChunk>> function);

        @Deprecated
        @Override
        <T> WriteableContent registerWriter(Predicate<?> accept,
                MediaType contentType,
                Function<T, Publisher<DataChunk>> function);
    }
}