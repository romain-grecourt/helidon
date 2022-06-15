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
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.Flow;
import java.util.concurrent.Flow.Publisher;
import java.util.function.BiFunction;
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
@SuppressWarnings("unused")
public interface EntitySupport {

    /**
     * The default (fallback) charset.
     */
    Charset DEFAULT_CHARSET = StandardCharsets.UTF_8;

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

        /**
         * Create a predicate function that tests if the given type is supported.
         *
         * @param expected expected type
         * @return predicate function
         */
        public static <U extends Context> BiFunction<GenericType<?>, U, PredicateResult> supports(Class<?> expected) {
            return (type, ctx) -> supports(expected, type);
        }

        /**
         * Create a predicate function that tests if the combination of a given type and content-type is supported.
         *
         * @param expected    expected type
         * @param contentType expected content-type
         * @return predicate function
         */
        public static <U extends Context> BiFunction<GenericType<?>, U, PredicateResult> supports(Class<?> expected,
                                                                                                  MediaType contentType) {
            return (type, ctx) -> ctx.contentType()
                                     .filter(contentType::equals)
                                     .map(it -> supports(expected, type))
                                     .orElse(NOT_SUPPORTED);
        }
    }

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
         * @param context the operator context
         * @return {@link PredicateResult} result
         */
        PredicateResult accept(GenericType<?> type, T context);
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
         * @throws NullPointerException if the supplied {@code filter} is {@code null}
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
     * Entity operator that generates raw data from objects.
     *
     * @param <T> type or base type supported by the operator
     */
    interface Writer<T> extends Operator<WriterContext> {

        /**
         * Generate raw data from the specified object of the given type.
         *
         * @param single  object to be converted
         * @param type    requested type
         * @param context the writer context
         * @return {@link DataChunk} publisher
         */
        <U extends T> Publisher<DataChunk> write(Single<U> single, GenericType<U> type, WriterContext context);

        /**
         * Create a marshalling function that can be used to marshall the given value with a context.
         *
         * @param value value to marshall
         * @return Marshalling function
         */
        default Function<WriterContext, Publisher<DataChunk>> marshall(T value) {
            return ctx -> ctx.marshall(Single.just(value), this, GenericType.create(value));
        }
    }

    /**
     * Entity operator that can convert raw data into one object.
     *
     * @param <T> type or base type supported by the operator
     */
    interface Reader<T> extends Operator<ReaderContext> {

        /**
         * Convert raw data into a publisher of the given type.
         *
         * @param chunks  raw data
         * @param type    requested type
         * @param context reader context
         * @param <U>     requested type
         * @return Single
         */
        <U extends T> Single<U> read(Publisher<DataChunk> chunks, GenericType<U> type, ReaderContext context);

        /**
         * Unmarshall the given content using this reader.
         *
         * @param content readable content to unmarshall
         * @param type    requested type
         * @return Single publisher
         */
        default Single<T> unmarshall(ReadableEntity content, Class<T> type) {
            return content.readerContext().unmarshall(content, this, GenericType.create(type));
        }
    }

    /**
     * Entity operator that can convert raw data into a stream of objects.
     *
     * @param <T> type or base type supported by the operator
     */
    interface StreamReader<T> extends Operator<ReaderContext> {

        /**
         * Convert raw data into a publisher of the given type.
         *
         * @param publisher raw data
         * @param type      requested type
         * @param context   reader context
         * @param <U>       requested type
         * @return Multi
         */
        <U extends T> Multi<U> read(Publisher<DataChunk> publisher, GenericType<U> type, ReaderContext context);

        /**
         * Unmarshall the given entity using this reader.
         *
         * @param content readable content to unmarshall
         * @param type    requested type
         * @return publisher
         */
        default Publisher<T> unmarshall(ReadableEntity content, Class<T> type) {
            return content.readerContext().unmarshallStream(content, this, GenericType.create(type));
        }
    }

    /**
     * Entity operator that generate raw data from a stream of objects.
     *
     * @param <T> type or base type supported by the operator
     */
    interface StreamWriter<T> extends Operator<WriterContext> {

        /**
         * Generate raw data from the specified stream of objects of the given type.
         *
         * @param publisher stream of objects to be converted
         * @param type      requested type
         * @param context   the writer context
         * @return {@link DataChunk} publisher
         */
        <U extends T> Publisher<DataChunk> write(Publisher<U> publisher, GenericType<U> type, WriterContext context);

        /**
         * Create a marshalling function that can be used to marshall the publisher with a context.
         *
         * @param publisher objects to convert to raw data
         * @param type      requested type
         * @return Marshalling function
         */
        default Function<WriterContext, Publisher<DataChunk>> marshall(Publisher<T> publisher, GenericType<T> type) {
            return ctx -> ctx.marshallStream(publisher, this, type);
        }
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
         * Get the {@code Content-Type} header.
         *
         * @return Optional, never {@code null}
         */
        Optional<MediaType> contentType();

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
    @SuppressWarnings("unused")
    interface ReaderContext extends Context, Readers, Filters {

        /**
         * Convert raw data to an object by selecting a reader that accepts the specified type.
         *
         * @param <T>    entity type
         * @param chunks raw data
         * @param type   actual representation of the entity type
         * @return publisher, never {@code null}
         */
        default <T> Single<T> unmarshall(Publisher<DataChunk> chunks, Class<T> type) {
            return unmarshall(chunks, GenericType.create(type));
        }

        /**
         * Convert raw data to an object by using the specified reader and type.
         *
         * @param <T>    entity type
         * @param <U>    reader type
         * @param chunks raw data
         * @param reader specific reader
         * @param type   actual representation of the entity type
         * @return publisher, never {@code null}
         */
        default <U, T extends U> Single<T> unmarshall(Publisher<DataChunk> chunks, Reader<U> reader, Class<T> type) {
            return unmarshall(chunks, reader, GenericType.create(type));
        }

        /**
         * Convert raw data to a stream of objects by selecting a stream reader that accepts the specified type.
         *
         * @param <T>    entity type
         * @param chunks raw data
         * @param type   actual representation of the entity type
         * @return publisher, never {@code null}
         */
        default <T> Multi<T> unmarshallStream(Publisher<DataChunk> chunks, Class<T> type) {
            return unmarshallStream(chunks, GenericType.create(type));
        }

        /**
         * Convert raw data to a stream of objects by using the specified reader and type.
         *
         * @param <T>    entity type
         * @param <U>    reader type
         * @param chunks raw data
         * @param reader specific reader
         * @param type   actual representation of the entity type
         * @return publisher, never {@code null}
         */
        default <U, T extends U> Multi<T> unmarshallStream(Publisher<DataChunk> chunks,
                                                           StreamReader<U> reader,
                                                           Class<T> type) {

            return unmarshallStream(chunks, reader, GenericType.create(type));
        }

        /**
         * Convert raw data to an object by selecting a reader that accepts the specified type.
         *
         * @param <T>    entity type
         * @param chunks raw data
         * @param type   actual representation of the entity type
         * @return publisher, never {@code null}
         */
        <T> Single<T> unmarshall(Publisher<DataChunk> chunks, GenericType<T> type);

        /**
         * Convert raw data to an object by using the specified reader and type.
         *
         * @param <T>    entity type
         * @param <U>    reader type
         * @param chunks raw data
         * @param reader specific reader
         * @param type   actual representation of the entity type
         * @return publisher, never {@code null}
         */
        <U, T extends U> Single<T> unmarshall(Publisher<DataChunk> chunks, Reader<U> reader, GenericType<T> type);

        /**
         * Convert raw data to a stream of objects by selecting a stream reader that accepts the specified type.
         *
         * @param <T>    entity type
         * @param chunks raw data
         * @param type   actual representation of the entity type
         * @return publisher, never {@code null}
         */
        <T> Multi<T> unmarshallStream(Publisher<DataChunk> chunks, GenericType<T> type);

        /**
         * Convert raw data to a stream of objects by using the specified reader and type.
         *
         * @param <T>    entity type
         * @param <U>    reader type
         * @param chunks raw data
         * @param reader specific reader
         * @param type   actual representation of the entity type
         * @return publisher, never {@code null}
         */
        <U, T extends U> Multi<T> unmarshallStream(Publisher<DataChunk> chunks,
                                                   StreamReader<U> reader,
                                                   GenericType<T> type);

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
         * Create a new child context.
         *
         * @return new context
         */
        default ReaderContext createChild() {
            return createChild(null, null, null);
        }

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
    @SuppressWarnings("unused")
    interface WriterContext extends Context, Writers, Filters {

        /**
         * Convert an object into raw data by selecting a writer that accepts the specified type and current context.
         *
         * @param <T>    entity type
         * @param entity entity publisher
         * @param type   entity type
         * @return publisher, never {@code null}
         */
        default <T> Publisher<DataChunk> marshall(Single<T> entity, Class<T> type) {
            return marshall(entity, GenericType.create(type));
        }

        /**
         * Convert an object into raw data by selecting a writer with the specified type.
         *
         * @param <T>    entity type
         * @param <U>    writer type
         * @param entity entity publisher
         * @param writer specific writer
         * @param type   entity type
         * @return publisher, never {@code null}
         */
        default <U, T extends U> Publisher<DataChunk> marshall(Single<T> entity, Writer<U> writer, Class<T> type) {
            return marshall(entity, writer, GenericType.create(type));
        }

        /**
         * Convert an object into raw data by selecting a stream writer that accepts the specified type.
         *
         * @param <T>    entity type
         * @param entity entity publisher
         * @param type   entity type
         * @return publisher, never {@code null}
         */
        default <T> Publisher<DataChunk> marshallStream(Publisher<T> entity, Class<T> type) {
            return marshallStream(entity, GenericType.create(type));
        }

        /**
         * Convert a stream of objects into raw data by selecting a stream writer with the specified type.
         *
         * @param <T>    entity type
         * @param <U>    writer type
         * @param entity entity publisher
         * @param writer specific writer
         * @param type   entity type
         * @return publisher, never {@code null}
         */
        default <U, T extends U> Publisher<DataChunk> marshallStream(Publisher<T> entity,
                                                                     StreamWriter<U> writer,
                                                                     Class<T> type) {

            return marshallStream(entity, writer, GenericType.create(type));
        }

        /**
         * Convert a stream of objects into raw data by selecting a stream writer that accepts the specified type.
         *
         * @param <T>    entity type
         * @param entity entity publisher
         * @param type   entity type
         * @return publisher, never {@code null}
         */
        <T> Publisher<DataChunk> marshallStream(Publisher<T> entity, GenericType<T> type);

        /**
         * Convert an object into raw data by selecting a writer that accepts the specified type.
         *
         * @param <T>    entity type
         * @param entity entity publisher
         * @param type   entity type
         * @return publisher, never {@code null}
         */
        <T> Publisher<DataChunk> marshall(Single<T> entity, GenericType<T> type);

        /**
         * Convert an object into raw data by selecting a writer with the specified type.
         *
         * @param <T>    entity type
         * @param <U>    writer type
         * @param entity entity publisher
         * @param writer specific writer
         * @param type   entity type
         * @return publisher, never {@code null}
         */
        <U, T extends U> Publisher<DataChunk> marshall(Single<T> entity, Writer<U> writer, GenericType<T> type);

        /**
         * Convert a stream of objects into raw data by selecting a stream writer with the specified type.
         *
         * @param <T>    entity type
         * @param <U>    writer type
         * @param entity entity publisher
         * @param writer specific writer
         * @param type   actual representation of the entity type
         * @return publisher, never {@code null}
         */
        <U, T extends U> Publisher<DataChunk> marshallStream(Publisher<T> entity,
                                                             StreamWriter<U> writer,
                                                             GenericType<T> type);

        /**
         * Get the {@code Accept} header.
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
         * Find a media type in the {@code Accept} header with the given predicate and default value.
         * <ul>
         * <li>The default value is returned if the predicate matches a media type with a wildcard subtype.<li>
         * <li>The default value if the current {@code Content-Type} header is not set and the {@code Accept} header
         * is empty or missing.</li>
         * <li>When the {@code Content-Type} header is set, if the predicate matches the {@code Content-Type} header value
         * is returned.</li>
         * </ul>
         *
         * @param predicate   a predicate to match against the {@code Accept} header
         * @param defaultType a default media type
         * @return MediaType, never {@code null}
         * @throws IllegalStateException if no media type can be returned
         */
        MediaType findAccepted(Predicate<MediaType> predicate, MediaType defaultType) throws IllegalStateException;

        /**
         * Find the given media type in the {@code Accept} header.
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
         * Create a new child context.
         *
         * @return new context
         */
        default WriterContext createChild() {
            return createChild(null, null, null);
        }

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
     * A functional adapter of {@link Writer}.
     *
     * @param <T> supported type
     */
    final class FunctionalWriter<T> implements Writer<T> {

        private final BiFunction<Single<T>, WriterContext, Publisher<DataChunk>> function;
        private final BiFunction<GenericType<?>, WriterContext, PredicateResult> predicate;

        /**
         * Create a new instance.
         *
         * @param predicate predicate function
         * @param function  writer function
         */
        FunctionalWriter(BiFunction<GenericType<?>, WriterContext, PredicateResult> predicate,
                         BiFunction<Single<T>, WriterContext, Publisher<DataChunk>> function) {

            this.predicate = Objects.requireNonNull(predicate, "predicate is null!");
            this.function = Objects.requireNonNull(function, "function is null!");
        }

        @Override
        public PredicateResult accept(GenericType<?> type, WriterContext context) {
            return predicate.apply(type, context);
        }

        @Override
        @SuppressWarnings("unchecked")
        public <U extends T> Publisher<DataChunk> write(Single<U> single, GenericType<U> type, WriterContext context) {
            return function.apply((Single<T>) single, context);
        }
    }

    /**
     * A functional adapter of {@link Reader}.
     *
     * @param <T> supported type
     */
    final class FunctionalReader<T> implements Reader<T> {

        private final BiFunction<GenericType<?>, ReaderContext, PredicateResult> predicate;
        private final BiFunction<Publisher<DataChunk>, ReaderContext, Single<T>> function;

        /**
         * Create a new instance.
         *
         * @param predicate predicate function
         * @param function  reader function
         */
        FunctionalReader(BiFunction<GenericType<?>, ReaderContext, PredicateResult> predicate,
                         BiFunction<Publisher<DataChunk>, ReaderContext, Single<T>> function) {

            this.predicate = Objects.requireNonNull(predicate, "predicate is null!");
            this.function = Objects.requireNonNull(function, "function is null!");
        }

        @Override
        public PredicateResult accept(GenericType<?> type, ReaderContext context) {
            return predicate.apply(type, context);
        }

        @Override
        @SuppressWarnings("unchecked")
        public <U extends T> Single<U> read(Publisher<DataChunk> publisher, GenericType<U> type, ReaderContext context) {
            return (Single<U>) function.apply(publisher, context);
        }
    }

    /**
     * A functional adapter of {@link StreamReader}.
     *
     * @param <T> supported type
     */
    final class FunctionalStreamReader<T> implements StreamReader<T> {

        private final BiFunction<GenericType<?>, ReaderContext, PredicateResult> predicate;
        private final BiFunction<Publisher<DataChunk>, ReaderContext, Multi<T>> function;

        /**
         * Create a new instance.
         *
         * @param predicate predicate function
         * @param function  reader function
         */
        FunctionalStreamReader(BiFunction<GenericType<?>, ReaderContext, PredicateResult> predicate,
                               BiFunction<Publisher<DataChunk>, ReaderContext, Multi<T>> function) {

            this.predicate = Objects.requireNonNull(predicate, "predicate is null!");
            this.function = Objects.requireNonNull(function, "function is null!");
        }

        @Override
        public PredicateResult accept(GenericType<?> type, ReaderContext context) {
            return predicate.apply(type, context);
        }

        @Override
        @SuppressWarnings("unchecked")
        public <U extends T> Multi<U> read(Publisher<DataChunk> publisher, GenericType<U> type, ReaderContext context) {
            return (Multi<U>) function.apply(publisher, context);
        }
    }

    /**
     * A functional adapter of {@link StreamWriter}.
     *
     * @param <T> supported type
     */
    final class FunctionalStreamWriter<T> implements StreamWriter<T> {

        private final BiFunction<GenericType<?>, WriterContext, PredicateResult> predicate;
        private final BiFunction<Publisher<T>, WriterContext, Publisher<DataChunk>> function;

        /**
         * Create a new instance.
         *
         * @param predicate predicate function
         * @param function  writer function
         */
        FunctionalStreamWriter(BiFunction<GenericType<?>, WriterContext, PredicateResult> predicate,
                               BiFunction<Publisher<T>, WriterContext, Publisher<DataChunk>> function) {

            this.predicate = Objects.requireNonNull(predicate, "predicate is null!");
            this.function = Objects.requireNonNull(function, "function is null!");
        }

        @Override
        public PredicateResult accept(GenericType<?> type, WriterContext context) {
            return predicate.apply(type, context);
        }

        @Override
        @SuppressWarnings("unchecked")
        public <U extends T> Publisher<DataChunk> write(Publisher<U> publisher,
                                                        GenericType<U> type,
                                                        WriterContext context) {
            return function.apply((Publisher<T>) publisher, context);
        }
    }

    /**
     * Create a new writer backed by a predicate function and a writer function.
     *
     * @param predicate predicate function
     * @param function  writer function
     * @param <T>       supported type
     * @return writer
     */
    static <T> Writer<T> writer(BiFunction<GenericType<?>, WriterContext, PredicateResult> predicate,
                                BiFunction<Single<T>, WriterContext, Publisher<DataChunk>> function) {

        return new FunctionalWriter<>(predicate, function);
    }

    /**
     * Create a new writer backed by a predicate function and a writer function.
     *
     * @param predicate predicate function
     * @param function  writer function
     * @param <T>       supported type
     * @return writer
     */
    static <T> Writer<T> writer(BiFunction<GenericType<?>, WriterContext, PredicateResult> predicate,
                                Function<Single<T>, Publisher<DataChunk>> function) {

        return new FunctionalWriter<>(predicate, (single, ctx) -> function.apply(single));
    }

    /**
     * Create a new stream writer backed by a predicate function and a writer function.
     *
     * @param predicate predicate function
     * @param function  writer function
     * @param <T>       supported type
     * @return stream writer
     */
    static <T> StreamWriter<T> streamWriter(BiFunction<GenericType<?>, WriterContext, PredicateResult> predicate,
                                            BiFunction<Publisher<T>, WriterContext, Publisher<DataChunk>> function) {

        return new FunctionalStreamWriter<>(predicate, function);
    }

    /**
     * Create a new stream writer backed by a predicate function and a writer function.
     *
     * @param predicate predicate function
     * @param function  writer function
     * @param <T>       supported type
     * @return stream writer
     */
    static <T> StreamWriter<T> streamWriter(BiFunction<GenericType<?>, WriterContext, PredicateResult> predicate,
                                            Function<Publisher<T>, Publisher<DataChunk>> function) {

        return new FunctionalStreamWriter<>(predicate, (publisher, ctx) -> function.apply(publisher));
    }

    /**
     * Create a new reader backed by a predicate function and a reader function.
     *
     * @param predicate predicate function
     * @param function  reader function
     * @param <T>       supported type
     * @return reader
     */
    static <T> Reader<T> reader(BiFunction<GenericType<?>, ReaderContext, PredicateResult> predicate,
                                BiFunction<Publisher<DataChunk>, ReaderContext, Single<T>> function) {

        return new FunctionalReader<>(predicate, function);
    }

    /**
     * Create a new reader backed by a predicate function and a reader function.
     *
     * @param predicate predicate function
     * @param function  reader function
     * @param <T>       supported type
     * @return reader
     */
    static <T> Reader<T> reader(BiFunction<GenericType<?>, ReaderContext, PredicateResult> predicate,
                                Function<Publisher<DataChunk>, Single<T>> function) {

        return new FunctionalReader<>(predicate, (publisher, ctx) -> function.apply(publisher));
    }

    /**
     * Create a new stream reader backed by a predicate function and a reader function.
     *
     * @param predicate predicate function
     * @param function  reader function
     * @param <T>       supported type
     * @return stream reader
     */
    static <T> StreamReader<T> streamReader(BiFunction<GenericType<?>, ReaderContext, PredicateResult> predicate,
                                            BiFunction<Publisher<DataChunk>, ReaderContext, Multi<T>> function) {

        return new FunctionalStreamReader<>(predicate, function);
    }

    /**
     * Create a new stream reader backed by a predicate function and a reader function.
     *
     * @param predicate predicate function
     * @param function  reader function
     * @param <T>       supported type
     * @return stream reader
     */
    static <T> StreamReader<T> streamReader(BiFunction<GenericType<?>, ReaderContext, PredicateResult> predicate,
                                            Function<Publisher<DataChunk>, Multi<T>> function) {

        return new FunctionalStreamReader<>(predicate, (publisher, ctx) -> function.apply(publisher));
    }
}
