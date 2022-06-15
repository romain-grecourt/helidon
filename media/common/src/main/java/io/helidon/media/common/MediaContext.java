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
package io.helidon.media.common;

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.Flow;
import java.util.function.Predicate;

import io.helidon.common.GenericType;
import io.helidon.common.http.DataChunk;
import io.helidon.common.http.MediaType;
import io.helidon.common.http.Parameters;
import io.helidon.common.reactive.Multi;
import io.helidon.common.reactive.Single;
import io.helidon.config.Config;

/**
 * Media support.
 */
@SuppressWarnings("unused")
public interface MediaContext {

    /**
     * The default (fallback) charset.
     */
    Charset DEFAULT_CHARSET = StandardCharsets.UTF_8;

    /**
     * Create a new instance with default readers and writers registered to the contexts.
     *
     * @return instance with defaults
     */
    static MediaContext create() {
        return builder().build();
    }

    /**
     * Create a new instance based on the configuration.
     *
     * @param config a {@link Config}
     * @return instance based on config
     */
    static MediaContext create(Config config) {
        return builder().config(config).build();
    }

    /**
     * Creates new empty instance without registered defaults.
     *
     * @return empty instance
     */
    static MediaContext empty() {
        return builder().registerDefaults(false).build();
    }

    /**
     * Create a new {@link MediaContextBuilder} instance.
     *
     * @return a new {@link MediaContextBuilder}
     */
    static MediaContextBuilder builder() {
        return new MediaContextBuilder();
    }

    /**
     * Get the configured reader context.
     *
     * @return the reader context
     */
    ReaderContext readerContext();

    /**
     * Get the configured writer context.
     *
     * @return the writer context
     */
    WriterContext writerContext();

    /**
     * Builder that supports adding readers, writers and media services to the builder.
     *
     * @param <T> Type of the class which this builder support is added to.
     */
    interface Builder<T> {

        /**
         * Adds new instance of {@link MediaSupport}.
         *
         * @param mediaSupport media support
         * @return updated instance of the builder
         */
        T addMediaSupport(MediaSupport mediaSupport);

        /**
         * Registers new reader.
         *
         * @param reader reader
         * @return updated instance of the builder
         */
        T addReader(MediaSupport.Reader<?> reader);

        /**
         * Registers new stream reader.
         *
         * @param streamReader stream reader
         * @return updated instance of the builder
         */
        T addStreamReader(MediaSupport.StreamReader<?> streamReader);

        /**
         * Registers new writer.
         *
         * @param writer writer
         * @return updated instance of the builder
         */
        T addWriter(MediaSupport.Writer<?> writer);

        /**
         * Registers new stream writer.
         *
         * @param streamWriter stream writer
         * @return updated instance of the builder
         */
        T addStreamWriter(MediaSupport.StreamWriter<?> streamWriter);

    }

    /**
     * Builder of {@link MediaContext} that can be parented.
     *
     * @param <T> Type of the class which this builder support is added to.
     */
    interface ParentingBuilder<T> extends Builder<T> {

        /**
         * Sets the {@link MediaContext} parent and overrides the existing one.
         * This method discards all previously registered readers and writers via builder.
         *
         * @param mediaContext media context
         * @return updated instance of the builder
         */
        T mediaContext(MediaContext mediaContext);
    }

    /**
     * Registry of {@link Filters}.
     */
    interface Filters {

        /**
         * Registers a media filter.
         * <p>
         * The registered filters are applied to form a chain from the first registered to the last registered.
         * The first evaluation of the function transforms the original publisher to a new publisher. Any subsequent
         * evaluation receives the publisher transformed by the last previously registered filter.
         *
         * @param filter a function to map previously registered or original {@code Publisher} to the new one.
         *               If returns  {@code null} then the result will be ignored.
         * @return this instance of {@link Filters}
         * @throws NullPointerException if the supplied {@code filter} is {@code null}
         * @see OperatorContext#applyFilters(Flow.Publisher)
         */
        Filters registerFilter(MediaSupport.Filter filter);
    }

    /**
     * Registry of {@link MediaSupport.Writer}.
     */
    interface Writers {

        /**
         * Register a writer.
         *
         * @param writer writer to register
         * @return Writers
         */
        Writers registerWriter(MediaSupport.Writer<?> writer);

        /**
         * Register a stream writer.
         *
         * @param writer writer to register
         * @return Writers
         */
        Writers registerWriter(MediaSupport.StreamWriter<?> writer);
    }

    /**
     * Registry of {@link MediaSupport.Reader}.
     */
    interface Readers {

        /**
         * Register a reader.
         *
         * @param reader reader to register
         * @return Readers
         */
        Readers registerReader(MediaSupport.Reader<?> reader);

        /**
         * Register a stream reader.
         *
         * @param reader reader to register
         * @return Readers
         */
        Readers registerReader(MediaSupport.StreamReader<?> reader);
    }

    /**
     * Media operator context.
     */
    interface OperatorContext extends Filters {

        /**
         * Media content subscription event listener.
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
         * Media content subscription event types.
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
         * Media content subscription event contract.
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
                if (!(this instanceof OperatorContext.ErrorEvent)) {
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
        Flow.Publisher<DataChunk> applyFilters(Flow.Publisher<DataChunk> publisher);
    }

    /**
     * Media reader context.
     */
    interface ReaderContext extends OperatorContext, Readers, Filters {

        /**
         * Convert raw data to an object by selecting a reader that accepts the specified type.
         *
         * @param <T>    entity type
         * @param chunks raw data
         * @param type   entity type
         * @return publisher, never {@code null}
         */
        default <T> Single<T> unmarshall(Flow.Publisher<DataChunk> chunks, Class<T> type) {
            return unmarshall(chunks, GenericType.create(type));
        }

        /**
         * Convert raw data to an object by using the specified reader and type.
         *
         * @param <T>    entity type
         * @param <U>    reader type
         * @param chunks raw data
         * @param reader specific reader
         * @param type   entity type
         * @return publisher, never {@code null}
         */
        default <U, T extends U> Single<T> unmarshall(Flow.Publisher<DataChunk> chunks,
                                                      MediaSupport.Reader<U> reader,
                                                      Class<T> type) {

            return unmarshall(chunks, reader, GenericType.create(type));
        }

        /**
         * Convert raw data to a stream of objects by selecting a stream reader that accepts the specified type.
         *
         * @param <T>    entity type
         * @param chunks raw data
         * @param type   entity type
         * @return publisher, never {@code null}
         */
        default <T> Multi<T> unmarshallStream(Flow.Publisher<DataChunk> chunks, Class<T> type) {
            return unmarshallStream(chunks, GenericType.create(type));
        }

        /**
         * Convert raw data to a stream of objects by using the specified reader and type.
         *
         * @param <T>    entity type
         * @param <U>    reader type
         * @param chunks raw data
         * @param reader specific reader
         * @param type   entity type
         * @return publisher, never {@code null}
         */
        default <U, T extends U> Multi<T> unmarshallStream(Flow.Publisher<DataChunk> chunks,
                                                           MediaSupport.StreamReader<U> reader,
                                                           Class<T> type) {

            return unmarshallStream(chunks, reader, GenericType.create(type));
        }

        /**
         * Convert raw data to an object by selecting a reader that accepts the specified type.
         *
         * @param <T>    entity type
         * @param chunks raw data
         * @param type   entity type
         * @return publisher, never {@code null}
         */
        <T> Single<T> unmarshall(Flow.Publisher<DataChunk> chunks, GenericType<T> type);

        /**
         * Convert raw data to an object by using the specified reader and type.
         *
         * @param <T>    entity type
         * @param <U>    reader type
         * @param chunks raw data
         * @param reader specific reader
         * @param type   entity type
         * @return publisher, never {@code null}
         */
        <U, T extends U> Single<T> unmarshall(Flow.Publisher<DataChunk> chunks,
                                              MediaSupport.Reader<U> reader,
                                              GenericType<T> type);

        /**
         * Convert raw data to a stream of objects by selecting a stream reader that accepts the specified type.
         *
         * @param <T>    entity type
         * @param chunks raw data
         * @param type   entity type
         * @return publisher, never {@code null}
         */
        <T> Multi<T> unmarshallStream(Flow.Publisher<DataChunk> chunks, GenericType<T> type);

        /**
         * Convert raw data to a stream of objects by using the specified reader and type.
         *
         * @param <T>    entity type
         * @param <U>    reader type
         * @param chunks raw data
         * @param reader specific reader
         * @param type   entity type
         * @return publisher, never {@code null}
         */
        <U, T extends U> Multi<T> unmarshallStream(Flow.Publisher<DataChunk> chunks,
                                                   MediaSupport.StreamReader<U> reader,
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
    interface WriterContext extends OperatorContext, Writers, Filters {

        /**
         * Convert an object into raw data by selecting a writer that accepts the specified type and current context.
         *
         * @param <T>    entity type
         * @param single entity publisher
         * @param type   entity type
         * @return publisher, never {@code null}
         */
        default <T> Flow.Publisher<DataChunk> marshall(Single<T> single, Class<T> type) {
            return marshall(single, GenericType.create(type));
        }

        /**
         * Convert an object into raw data by selecting a writer with the specified type.
         *
         * @param <T>    entity type
         * @param <U>    writer type
         * @param single entity publisher
         * @param writer specific writer
         * @param type   entity type
         * @return publisher, never {@code null}
         */
        default <U, T extends U> Flow.Publisher<DataChunk> marshall(Single<T> single,
                                                                    MediaSupport.Writer<U> writer,
                                                                    Class<T> type) {

            return marshall(single, writer, GenericType.create(type));
        }

        /**
         * Convert an object into raw data by selecting a stream writer that accepts the specified type.
         *
         * @param <T>    entity type
         * @param single entity publisher
         * @param type   entity type
         * @return publisher, never {@code null}
         */
        default <T> Flow.Publisher<DataChunk> marshallStream(Flow.Publisher<T> single, Class<T> type) {
            return marshallStream(single, GenericType.create(type));
        }

        /**
         * Convert a stream of objects into raw data by selecting a stream writer with the specified type.
         *
         * @param <T>       entity type
         * @param <U>       writer type
         * @param publisher entity publisher
         * @param writer    specific writer
         * @param type      entity type
         * @return publisher, never {@code null}
         */
        default <U, T extends U> Flow.Publisher<DataChunk> marshallStream(Flow.Publisher<T> publisher,
                                                                          MediaSupport.StreamWriter<U> writer,
                                                                          Class<T> type) {

            return marshallStream(publisher, writer, GenericType.create(type));
        }

        /**
         * Convert a stream of objects into raw data by selecting a stream writer that accepts the specified type.
         *
         * @param <T>       entity type
         * @param publisher entity publisher
         * @param type      entity type
         * @return publisher, never {@code null}
         */
        <T> Flow.Publisher<DataChunk> marshallStream(Flow.Publisher<T> publisher, GenericType<T> type);

        /**
         * Convert an object into raw data by selecting a writer that accepts the specified type.
         *
         * @param <T>    entity type
         * @param single entity publisher
         * @param type   entity type
         * @return publisher, never {@code null}
         */
        <T> Flow.Publisher<DataChunk> marshall(Single<T> single, GenericType<T> type);

        /**
         * Convert an object into raw data by selecting a writer with the specified type.
         *
         * @param <T>    entity type
         * @param <U>    writer type
         * @param single entity publisher
         * @param writer specific writer
         * @param type   entity type
         * @return publisher, never {@code null}
         */
        <U, T extends U> Flow.Publisher<DataChunk> marshall(Single<T> single,
                                                            MediaSupport.Writer<U> writer,
                                                            GenericType<T> type);

        /**
         * Convert a stream of objects into raw data by selecting a stream writer with the specified type.
         *
         * @param <T>       entity type
         * @param <U>       writer type
         * @param publisher entity publisher
         * @param writer    specific writer
         * @param type      entity type
         * @return publisher, never {@code null}
         */
        <U, T extends U> Flow.Publisher<DataChunk> marshallStream(Flow.Publisher<T> publisher,
                                                                  MediaSupport.StreamWriter<U> writer,
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
         * <li>When the {@code Content-Type} header is set, if the predicate matches the {@code Content-Type} header
         * value is returned.</li>
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
}
