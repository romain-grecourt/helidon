/*
 * Copyright (c) 2022 Oracle and/or its affiliates.
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
package io.helidon.media.common;

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.Flow;
import java.util.concurrent.Flow.Publisher;
import java.util.function.Function;
import java.util.function.Predicate;

import io.helidon.common.GenericType;
import io.helidon.common.http.DataChunk;
import io.helidon.common.http.MediaType;
import io.helidon.common.http.Parameters;
import io.helidon.common.reactive.Multi;
import io.helidon.common.reactive.Single;

/**
 * Entity support.
 */
public interface EntitySupport {

    /**
     * The default (fallback) charset.
     */
    Charset DEFAULT_CHARSET = StandardCharsets.UTF_8;

    /**
     * Conversion operator that can be selected based on a requested type and a context.
     *
     * @param <T> Type supported by the operator
     */
    interface Operator<T> {

        /**
         * Test if the operator can convert the given type.
         *
         * @param type    the requested type
         * @param context the context providing the headers abstraction
         * @return {@link PredicateResult} result
         */
        PredicateResult accept(GenericType<?> type, T context);

        /**
         * Status whether requested class type is supported by the operator.
         */
        enum PredicateResult {

            /**
             * Requested type not supported.
             */
            NOT_SUPPORTED,

            /**
             * Requested type is compatible with this operator, but it is not exact match.
             */
            COMPATIBLE,

            /**
             * Requested type is supported by that specific operator.
             */
            SUPPORTED;

            /**
             * Whether handled class is supported.
             * Method {@link Class#isAssignableFrom(Class)} is invoked to verify if class under expected parameter is
             * supported by by the class under actual parameter.
             *
             * @param expected expected type
             * @param actual   actual type
             * @return if supported or not
             */
            public static PredicateResult supports(Class<?> expected, GenericType<?> actual) {
                return expected.isAssignableFrom(actual.rawType()) ? SUPPORTED : NOT_SUPPORTED;
            }
        }

        /**
         * Operator that generates raw payload from objects.
         *
         * @param <T> type or base type supported by the operator
         * @param <U> publisher type
         */
        interface Writer<T, U extends Publisher<? extends T>> extends Operator<WriterContext> {

            /**
             * Generate raw payload from the objects of the given type.
             *
             * @param publisher objects publisher
             * @param type      requested type
             * @param context   the writer context
             * @return Publisher of objects
             */
            Publisher<DataChunk> write(U publisher, GenericType<? extends T> type, WriterContext context);
        }

        /**
         * Operator that generates objects from raw payload.
         *
         * @param <T> type or base type supported by the operator
         */
        interface Reader<T, U extends Publisher<? extends T>> extends Operator<ReaderContext> {

            /**
             * Convert raw payload into a publisher of the given type.
             *
             * @param publisher raw payload
             * @param type      requested type
             * @param context   the context providing the headers abstraction
             * @return Single publisher
             */
            U read(Publisher<DataChunk> publisher, GenericType<? extends T> type, ReaderContext context);
        }
    }

    /**
     * Function to filter or replace entity publisher.
     * It can be used for various purposes, for example data coding, logging, filtering, caching, etc.
     */
    interface Filter extends Function<Publisher<DataChunk>, Publisher<DataChunk>> {
    }

    /**
     * Registry of {@link Filters}.
     */
    interface Filters {

        /**
         * Registers an entity filter.
         * <p>
         * The registered filters are applied to form a chain from the first registered to the last registered.
         * The first evaluation of the function transforms the original publisher to a new publisher. Any subsequent
         * evaluation receives the publisher transformed by the last previously registered filter.
         *
         * @param filter a function to map previously registered or original {@code Publisher} to the new one.
         *               If returns  {@code null} then the result will be ignored.
         * @return this instance of {@link Filters}
         * @throws NullPointerException if parameter {@code function} is {@code null}
         * @see Context#applyFilters(Publisher)
         */
        Filters registerFilter(Filter filter);
    }

    /**
     * Registry of {@link Writer}.
     */
    interface Writers {

        /**
         * Register a writer.
         *
         * @param writer writer to register
         * @return Writers
         */
        Writers registerWriter(Writer<?> writer);

        /**
         * Register a stream writer.
         *
         * @param writer writer to register
         * @return Writers
         */
        Writers registerWriter(StreamWriter<?> writer);
    }

    /**
     * Registry of {@link Reader}.
     */
    interface Readers {

        /**
         * Register a reader.
         *
         * @param reader reader to register
         * @return Readers
         */
        Readers registerReader(Reader<?> reader);

        /**
         * Register a stream reader.
         *
         * @param reader reader to register
         * @return Readers
         */
        Readers registerReader(StreamReader<?> reader);
    }

    /**
     * Entity operator that generates raw payload from objects.
     *
     * @param <T> type or base type supported by the operator
     */
    interface Writer<T> extends Operator.Writer<T, Single<? extends T>> {
    }

    /**
     * Entity operator that can convert raw payload into one object.
     *
     * @param <T> type or base type supported by the operator
     */
    interface Reader<T> extends Operator.Reader<T, Single<T>> {
    }

    /**
     * Entity operator that can convert raw payload into a stream of objects.
     *
     * @param <T> type or base type supported by the operator
     */
    interface StreamReader<T> extends Operator.Reader<T, Multi<T>> {
    }

    /**
     * Entity operator that generate raw payload from a stream of objects.
     *
     * @param <T> type or base type supported by the operator
     */
    interface StreamWriter<T> extends Operator.Writer<T, Publisher<? extends T>> {
    }

    /**
     * Entity context.
     */
    interface Context extends Filters {

        /**
         * Entity content subscription event listener.
         */
        interface EventListener {

            /**
             * Handle a subscription event.
             *
             * @param event subscription event
             */
            void onEvent(Event event);
        }

        /**
         * Entity content subscription event types.
         */
        enum EventType {

            /**
             * Emitted before {@link Flow.Subscriber#onSubscribe(Flow.Subscription)}.
             */
            BEFORE_ONSUBSCRIBE,

            /**
             * Emitted after {@link Flow.Subscriber#onSubscribe(Flow.Subscription)}.
             */
            AFTER_ONSUBSCRIBE,

            /**
             * Emitted before {@link Flow.Subscriber#onNext(Object)}.
             */
            BEFORE_ONNEXT,

            /**
             * Emitted after {@link Flow.Subscriber#onNext(Object)}.
             */
            AFTER_ONNEXT,

            /**
             * Emitted before {@link Flow.Subscriber#onError(Throwable)}.
             */
            BEFORE_ONERROR,

            /**
             * Emitted after {@link Flow.Subscriber#onError(Throwable)}.
             */
            AFTER_ONERROR,

            /**
             * Emitted after {@link Flow.Subscriber#onComplete()}.
             */
            BEFORE_ONCOMPLETE,

            /**
             * Emitted after {@link Flow.Subscriber#onComplete()}.
             */
            AFTER_ONCOMPLETE
        }

        /**
         * Entity content subscription event contract.
         */
        interface Event {

            /**
             * Get the event type of this event.
             *
             * @return EVENT_TYPE
             */
            EventType eventType();

            /**
             * Get the type requested for conversion.
             *
             * @return never {@code null}
             */
            Optional<GenericType<?>> entityType();

            /**
             * Fluent helper method to cast this event as a {@link ErrorEvent}. This
             * is safe to do when {@link #eventType()} returns
             * {@link EventType#BEFORE_ONERROR} or {@link EventType#AFTER_ONERROR}
             *
             * @return ErrorEvent
             * @throws IllegalStateException if this event is not an instance of
             *                               {@link ErrorEvent}
             */
            default ErrorEvent asErrorEvent() {
                if (!(this instanceof Context.ErrorEvent)) {
                    throw new IllegalStateException("Not an error event");
                }
                return (ErrorEvent) this;
            }
        }

        /**
         * A subscription event emitted for {@link EventType#BEFORE_ONERROR} or
         * {@link EventType#AFTER_ONERROR} that carries the received error.
         */
        interface ErrorEvent extends Event {

            /**
             * Get the subscription error of this event.
             *
             * @return {@code Throwable}, never {@code null}
             */
            Throwable error();
        }

        /**
         * Get the underlying headers.
         *
         * @return Parameters, never {@code null}
         */
        Parameters headers();

        /**
         * Derive the charset to use from the {@code Content-Type} header value or
         * using a default charset as fallback.
         *
         * @return Charset, never {@code null}
         * @throws IllegalStateException if an error occurs loading the charset
         *                               specified by the {@code Content-Type} header value
         */
        Charset charset() throws IllegalStateException;

        /**
         * Apply the filters on the given input publisher to form a publisher chain.
         *
         * @param publisher input publisher
         * @return tail of the publisher chain
         */
        Publisher<DataChunk> applyFilters(Publisher<DataChunk> publisher);
    }

    /**
     * Entity reader context.
     */
    interface ReaderContext extends Context, Readers, Filters {

        /**
         * Convert a given raw payload into a publisher by selecting a reader that accepts the specified type.
         *
         * @param <T>     entity type
         * @param payload inbound payload
         * @param type    actual representation of the entity type
         * @return publisher, never {@code null}
         */
        default <T> Single<T> unmarshall(Publisher<DataChunk> payload, Class<T> type) {
            return unmarshall(payload, GenericType.create(type));
        }

        /**
         * Convert a given raw payload into a publisher by using the specified reader and type.
         *
         * @param <T>     entity type
         * @param payload inbound payload
         * @param reader  specific reader
         * @param type    actual representation of the entity type
         * @return publisher, never {@code null}
         */
        default <T> Single<T> unmarshall(Publisher<DataChunk> payload, Reader<T> reader, Class<T> type) {
            return unmarshall(payload, reader, GenericType.create(type));
        }

        /**
         * Convert a given raw payload into a publisher by selecting a stream reader that accepts the specified type.
         *
         * @param <T>     entity type
         * @param payload inbound payload
         * @param type    actual representation of the entity type
         * @return publisher, never {@code null}
         */
        default <T> Multi<T> unmarshallStream(Publisher<DataChunk> payload, Class<T> type) {
            return unmarshallStream(payload, GenericType.create(type));
        }

        /**
         * Convert a given raw payload into a publisher by using the specified reader and type.
         *
         * @param <T>     entity type
         * @param payload inbound payload
         * @param reader  specific reader
         * @param type    actual representation of the entity type
         * @return publisher, never {@code null}
         */
        default <T> Multi<T> unmarshallStream(Publisher<DataChunk> payload, StreamReader<T> reader, Class<T> type) {
            return unmarshallStream(payload, reader, GenericType.create(type));
        }

        /**
         * Convert a given raw payload into a publisher by selecting a reader that accepts the specified type.
         *
         * @param <T>     entity type
         * @param payload inbound payload
         * @param type    actual representation of the entity type
         * @return publisher, never {@code null}
         */
        <T> Single<T> unmarshall(Publisher<DataChunk> payload, GenericType<T> type);

        /**
         * Convert a given raw payload into a publisher by using the specified reader and type.
         *
         * @param <T>     entity type
         * @param payload inbound payload
         * @param reader  specific reader
         * @param type    actual representation of the entity type
         * @return publisher, never {@code null}
         */
        <T> Single<T> unmarshall(Publisher<DataChunk> payload, Reader<T> reader, GenericType<T> type);

        /**
         * Convert a given raw payload into a publisher by selecting a stream reader that accepts the specified type.
         *
         * @param <T>     entity type
         * @param payload inbound payload
         * @param type    actual representation of the entity type
         * @return publisher, never {@code null}
         */
        <T> Multi<T> unmarshallStream(Publisher<DataChunk> payload, GenericType<T> type);

        /**
         * Convert a given raw payload into a publisher by using the specified reader and type.
         *
         * @param <T>     entity type
         * @param payload inbound payload
         * @param reader  specific reader
         * @param type    actual representation of the entity type
         * @return publisher, never {@code null}
         */
        <T> Multi<T> unmarshallStream(Publisher<DataChunk> payload, StreamReader<T> reader, GenericType<T> type);

        /**
         * Get the {@code Content-Type} header.
         *
         * @return Optional, never {@code null}
         */
        Optional<MediaType> contentType();

        /**
         * Create a new child context.
         *
         * @param eventListener event listener, may be {@code null}
         * @param headers       headers, must not be {@code null}
         * @param contentType   content-type, may be {@code null}
         * @return new context
         */
        ReaderContext createChild(EventListener eventListener, Parameters headers, MediaType contentType);

        /**
         * Create a new reader context.
         *
         * @return reader context
         */
        static ReaderContext create() {
            return new ReaderContextImpl(null, null, null, null);
        }
    }

    /**
     * Writer context.
     */
    interface WriterContext extends Context, Writers, Filters {

        /**
         * Convert an entity publisher into raw payload by selecting a
         * writer that accepts the specified type and current context.
         *
         * @param <T>    entity type parameter
         * @param entity entity publisher
         * @param type   entity type
         * @return publisher, never {@code null}
         */
        default <T> Publisher<DataChunk> marshall(Single<T> entity, Class<T> type) {
            return marshall(entity, GenericType.create(type));
        }

        /**
         * Convert an entity publisher into raw payload by selecting a writer with the specified type.
         *
         * @param <T>    entity type parameter
         * @param entity entity publisher
         * @param writer specific writer
         * @param type   entity type
         * @return publisher, never {@code null}
         */
        default <T> Publisher<DataChunk> marshall(Single<T> entity, Writer<T> writer, Class<T> type) {
            return marshall(entity, writer, GenericType.create(type));
        }

        /**
         * Convert an entity publisher into raw payload by selecting a stream writer that accepts the specified type.
         *
         * @param <T>    entity type parameter
         * @param entity entity publisher
         * @param type   entity type
         * @return publisher, never {@code null}
         */
        default <T> Publisher<DataChunk> marshallStream(Publisher<T> entity, Class<T> type) {
            return marshallStream(entity, GenericType.create(type));
        }

        /**
         * Convert an entity publisher into raw payload by selecting a stream writer with the specified type.
         *
         * @param <T>    entity type parameter
         * @param entity entity publisher
         * @param writer specific writer
         * @param type   entity type
         * @return publisher, never {@code null}
         */
        default <T> Publisher<DataChunk> marshallStream(Publisher<T> entity, StreamWriter<T> writer, Class<T> type) {
            return marshallStream(entity, writer, GenericType.create(type));
        }

        /**
         * Convert an entity publisher into raw payload by selecting a stream writer that accepts the specified type.
         *
         * @param <T>    entity type parameter
         * @param entity entity publisher
         * @param type   entity type
         * @return publisher, never {@code null}
         */
        <T> Publisher<DataChunk> marshallStream(Publisher<T> entity, GenericType<T> type);

        /**
         * Convert an entity publisher into raw payload by selecting a writer that accepts the specified type.
         *
         * @param <T>    entity type parameter
         * @param entity entity publisher
         * @param type   entity type
         * @return publisher, never {@code null}
         */
        <T> Publisher<DataChunk> marshall(Single<T> entity, GenericType<T> type);

        /**
         * Convert an entity publisher into raw payload by selecting a writer with the specified type.
         *
         * @param <T>    entity type parameter
         * @param entity entity publisher
         * @param writer specific writer
         * @param type   entity type
         * @return publisher, never {@code null}
         */
        <T> Publisher<DataChunk> marshall(Single<T> entity, Writer<T> writer, GenericType<T> type);

        /**
         * Convert an entity publisher into raw payload by selecting a stream writer with the specified type.
         *
         * @param <T>    entity type parameter
         * @param entity entity publisher
         * @param writer specific writer
         * @param type   actual representation of the entity type
         * @return publisher, never {@code null}
         */
        <T> Publisher<DataChunk> marshallStream(Publisher<T> entity, StreamWriter<T> writer, GenericType<T> type);

        /**
         * Get the {@code Content-Type} header.
         *
         * @return Optional, never {@code null}
         */
        Optional<MediaType> contentType();

        /**
         * Get the inbound {@code Accept} header.
         *
         * @return List never {@code null}
         */
        List<MediaType> acceptedTypes();

        /**
         * Set the {@code Content-Type} header value in the underlying headers if not present.
         *
         * @param contentType {@code Content-Type} value to set, must not be {@code null}
         */
        void contentType(MediaType contentType);

        /**
         * Set the {@code Content-Length} header value in the underlying headers if not present.
         *
         * @param contentLength {@code Content-Length} value to set, must be a positive value
         */
        void contentLength(long contentLength);

        /**
         * Find an media type in the inbound {@code Accept} header with the given predicate and default value.
         * <ul>
         * <li>The default value is returned if the predicate matches a media type with a wildcard subtype.<li>
         * <li>The default value if the current {@code Content-Type} header is not set and the inbound {@code Accept}
         * header is empty or missing.</li>
         * <li>When the {@code Content-Type} header is set, if the predicate matches the {@code Content-Type} header value
         * is returned.</li>
         * </ul>
         *
         * @param predicate   a predicate to match against the inbound {@code Accept} header
         * @param defaultType a default media type
         * @return MediaType, never {@code null}
         * @throws IllegalStateException if no media type can be returned
         */
        MediaType findAccepted(Predicate<MediaType> predicate, MediaType defaultType) throws IllegalStateException;

        /**
         * Find the given media type in the inbound {@code Accept} header.
         *
         * @param mediaType media type to search for
         * @return MediaType, never {@code null}
         * @throws IllegalStateException if the media type is not found
         */
        MediaType findAccepted(MediaType mediaType) throws IllegalStateException;

        /**
         * Create a new child context.
         *
         * @param eventListener event listener, may be {@code null}
         * @param headers       headers, must not be {@code null}
         * @param acceptedTypes accepted types, may be {@code null}
         * @return new context
         */
        WriterContext createChild(EventListener eventListener, Parameters headers, List<MediaType> acceptedTypes);

        /**
         * Create a new writer context.
         *
         * @return writer context
         */
        static WriterContext create() {
            return new WriterContextImpl(null, null, null, null);
        }
    }

    /**
     * Abstract implementation of {@link Operator}.
     *
     * @param <T> supported type
     * @param <U> context type
     */
    abstract class SimpleOperator<T, U> implements Operator<U> {

        private final Class<T> type;

        /**
         * Create a new instance.
         *
         * @param type supported type
         */
        protected SimpleOperator(Class<T> type) {
            this.type = type;
        }

        @Override
        public final PredicateResult accept(GenericType<?> type, U context) {
            return PredicateResult.supports(this.type, type);
        }
    }

    /**
     * Abstract implementation of {@link Writer}.
     *
     * @param <T> supported type
     */
    abstract class SimpleWriter<T> extends SimpleOperator<T, WriterContext> implements Writer<T> {

        /**
         * Create a new instance.
         *
         * @param type supported type
         */
        protected SimpleWriter(Class<T> type) {
            super(type);
        }
    }

    /**
     * Abstract implementation of {@link Reader}.
     *
     * @param <T> supported type
     */
    abstract class SimpleReader<T> extends SimpleOperator<T, ReaderContext> implements Reader<T> {

        /**
         * Create a new instance.
         *
         * @param type supported type
         */
        protected SimpleReader(Class<T> type) {
            super(type);
        }
    }

    /**
     * Abstract implementation of {@link StreamReader}.
     *
     * @param <T> supported type
     */
    abstract class SimpleStreamReader<T> extends SimpleOperator<T, ReaderContext> implements StreamReader<T> {

        /**
         * Create a new instance.
         *
         * @param type supported type
         */
        protected SimpleStreamReader(Class<T> type) {
            super(type);
        }
    }

    /**
     * Abstract implementation of {@link StreamWriter}.
     *
     * @param <T> supported type
     */
    abstract class SimpleStreamWriter<T> extends SimpleOperator<T, WriterContext> implements StreamWriter<T> {

        /**
         * Create a new instance.
         *
         * @param type supported type
         */
        protected SimpleStreamWriter(Class<T> type) {
            super(type);
        }
    }
}
