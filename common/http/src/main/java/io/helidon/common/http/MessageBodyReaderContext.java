package io.helidon.common.http;

import io.helidon.common.GenericType;
import io.helidon.common.http.MessageBody.Filters;
import io.helidon.common.http.MessageBody.ReadOperator;
import io.helidon.common.http.MessageBody.ReaderContext;
import io.helidon.common.http.MessageBody.Readers;
import io.helidon.common.http.MessageBody.StreamReader;
import io.helidon.common.reactive.Flow.Publisher;
import io.helidon.common.reactive.Mono;
import io.helidon.common.reactive.Multi;
import java.nio.charset.Charset;
import java.nio.charset.IllegalCharsetNameException;
import java.nio.charset.StandardCharsets;
import java.nio.charset.UnsupportedCharsetException;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Predicate;

/**
 * Implementation of {@link ReaderContext}.
 */
public final class MessageBodyReaderContext extends MessageBodyContextBase
        implements ReaderContext, Readers, Filters {

    private static final Charset DEFAULT_CHARSET = StandardCharsets.UTF_8;

    private final ReadOnlyParameters headers;
    private final Optional<MediaType> contentType;
    private final MessageBodyOperators<ReadOperator<?>, ReaderContext> readers;
    private final MessageBodyOperators<ReadOperator<?>, ReaderContext> sreaders;

    /**
     * Create a new parented context.
     * @param parent parent context, must not be {@code null}
     * @param eventListener subscription event listener, may be {@code null}
     * @param headers backing headers, must not be {@code null}
     * @param contentType content type, must not be {@code null}
     */
    private MessageBodyReaderContext(MessageBodyReaderContext parent,
            EventListener eventListener, ReadOnlyParameters headers,
            Optional<MediaType> contentType) {

        super(parent, eventListener);
        Objects.requireNonNull(headers, "headers cannot be null!");
        Objects.requireNonNull(parent, "parent cannot be null!");
        Objects.requireNonNull(contentType, "contentType cannot be null!");
        this.headers = headers;
        this.contentType = contentType;
        this.readers = new MessageBodyOperators<>(parent.readers);
        this.sreaders = new MessageBodyOperators<>(parent.sreaders);
    }

    /**
     * Create a new standalone (non parented) context backed by empty read-only
     * headers.
     */
    private MessageBodyReaderContext() {
        super(/* eventListener */ null);
        this.headers = ReadOnlyParameters.empty();
        this.contentType = Optional.empty();
        this.readers = new MessageBodyOperators<>();
        this.sreaders = new MessageBodyOperators<>();
    }

    @Override
    public MessageBodyReaderContext registerReader(
            MessageBody.Reader<?> reader) {

        readers.registerFirst(reader);
        return this;
    }

    @Override
    public MessageBodyReaderContext registerReader(StreamReader<?> reader) {
        sreaders.registerFirst(reader);
        return this;
    }

    @Deprecated
    @Override
    public <T> void registerReader(Class<T> type, Reader<T> reader) {
        readers.registerFirst(new ReaderAdapter<>(type, reader));
    }

    @Deprecated
    @Override
    public <T> void registerReader(Predicate<Class<?>> predicate,
            Reader<T> reader) {

        readers.registerFirst(new ReaderAdapter<>(predicate, reader));
    }

    /**
     * Convert a given HTTP payload into a publisher by selecting a reader that
     * accepts the specified type and current context.
     *
     * @param <T> entity type
     * @param publisher inbound payload
     * @param type actual representation of the entity type
     * @return publisher, never {@code null}
     */
    @SuppressWarnings("unchecked")
    public <T> Mono<T> unmarshall(Publisher<DataChunk> publisher,
            GenericType<T> type) {

        try {
            if (publisher == null) {
                return Mono.<T>empty();
            }
            MessageBody.Reader<T> reader = (MessageBody.Reader<T>)
                    readers.select(type, this);
            if (reader == null) {
                return Mono.<T>error(new IllegalStateException(
                        "No reader found for type: "
                                + type.getTypeName()));
            }
            return reader.read(applyFilters(publisher, type), type, this);
        } catch (Throwable ex) {
            return Mono.<T>error(new IllegalStateException(
                    "Transformation failed!", ex));
        }
    }

    /**
     * Convert a given HTTP payload into a publisher by selecting a reader with
     * the specified class.
     *
     * @param <T> entity type
     * @param publisher inbound payload
     * @param readerType the requested reader class
     * @param type actual representation of the entity type
     * @return publisher, never {@code null}
     */
    @SuppressWarnings("unchecked")
    public <T> Mono<T> unmarshall(Publisher<DataChunk> publisher,
            Class<? extends MessageBody.Reader<T>> readerType,
            GenericType<T> type) {

        try {
            if (publisher == null) {
                return Mono.<T>empty();
            }
            MessageBody.Reader<T> reader = (MessageBody.Reader<T>)
                    readers.get(readerType);
            if (reader == null) {
                return Mono.<T>error(new IllegalStateException(
                        "No reader found of type: "
                                + readerType.getTypeName()));
            }
            return reader.read(applyFilters(publisher, type), type, this);
        } catch (Throwable ex) {
            return Mono.<T>error(new IllegalStateException(
                    "Transformation failed!", ex));
        }
    }

    /**
     * Convert a given HTTP payload into a publisher by selecting a
     * stream reader that accepts the specified type and current context.
     *
     * @param <T> entity type
     * @param publisher inbound payload
     * @param type actual representation of the entity type
     * @return publisher, never {@code null}
     */
    @SuppressWarnings("unchecked")
    public <T> Publisher<T> unmarshallStream(
            Publisher<DataChunk> publisher, GenericType<T> type) {

        try {
            if (publisher == null) {
                return Multi.<T>empty();
            }
            MessageBody.StreamReader<T> reader = (MessageBody.StreamReader<T>)
                    sreaders.select(type, this);
            if (reader == null) {
                return Multi.<T>error(new IllegalStateException(
                        "No stream reader found for type: "
                                + type.getTypeName()));
            }
            return reader.read(applyFilters(publisher, type), type, this);
        } catch (Throwable ex) {
            return Multi.<T>error(new IllegalStateException(
                    "Transformation failed!", ex));
        }
    }

    /**
     * Convert a given HTTP payload into a publisher by selecting a stream
     * reader with the specified class.
     *
     * @param <T> entity type
     * @param publisher inbound payload
     * @param readerType the requested reader class
     * @param type actual representation of the entity type
     * @return publisher, never {@code null}
     */
    @SuppressWarnings("unchecked")
    public <T> Publisher<? extends T> unmarshallStream(
            Publisher<DataChunk> publisher,
            Class<? extends MessageBody.Reader<T>> readerType,
            GenericType<T> type) {

        try {
            if (publisher == null) {
                return Multi.<T>empty();
            }
            MessageBody.StreamReader<T> reader = (MessageBody.StreamReader<T>)
                    sreaders.get(readerType);
            if (reader == null) {
                return Multi.<T>error(new IllegalStateException(
                        "No stream reader found of type: "
                                + readerType.getTypeName()));
            }
            return reader.read(applyFilters(publisher, type), type, this);
        } catch (Throwable ex) {
            return Multi.<T>error(new IllegalStateException(
                    "Transformation failed!", ex));
        }
    }

    @Override
    public ReadOnlyParameters headers() {
        return headers;
    }

    @Override
    public Optional<MediaType> contentType() {
        return contentType;
    }

    @Override
    public final Charset charset() {
        if (contentType.isPresent()) {
            try {
                return contentType.get().charset().map(Charset::forName)
                        .orElse(DEFAULT_CHARSET);
            } catch (IllegalCharsetNameException
                    | UnsupportedCharsetException ex) {
                throw new IllegalStateException(ex);
            }
        }
        return DEFAULT_CHARSET;
    }

    /**
     * Create a new empty writer context backed by the specified headers.
     * @param parent parent context, must not be {@code null}
     * @param eventListener subscription event listener, may be {@code null}
     * @param headers backing headers, must not be {@code null}
     * @param contentType content type, must not be {@code null}
     * @return MessageBodyReaderContext
     */
    public static MessageBodyReaderContext create(
            MessageBodyReaderContext parent, EventListener eventListener,
            ReadOnlyParameters headers, Optional<MediaType> contentType) {

        return new MessageBodyReaderContext(parent, eventListener, headers,
                contentType);
    }

    /**
     * Create a new empty reader context backed by empty read-only headers.
     * Such reader context is typically the parent context that is used to hold
     * application wide readers and inbound filters.
     * @return MessageBodyWriterContext
     */
    public static MessageBodyReaderContext create() {
        return new MessageBodyReaderContext();
    }

    /**
     * Safely cast a {@link ReaderContext} into MessageBodyReaderContext.
     * @param context context to cast
     * @return MessageBodyReaderContext, never {@code null}
     * @throws IllegalArgumentException if the specified content is not
     * an instance of MessageBodyReaderContext
     */
    public static MessageBodyReaderContext of(ReaderContext context)
        throws IllegalArgumentException {

        Objects.requireNonNull(context, "context cannot be null!");
        if (context instanceof MessageBodyReaderContext) {
            return (MessageBodyReaderContext) context;
        }
        throw new IllegalArgumentException("Invalid content " + context);
    }

    /**
     * Message body reader adapter for the old deprecated reader.
     * @param <T> reader type
     */
    private static final class ReaderAdapter<T>
            implements MessageBody.Reader<T> {

        private final Reader<T> reader;
        private final Predicate<Class<?>> predicate;
        private final Class<T> clazz;

        ReaderAdapter(Predicate<Class<?>> predicate, Reader<T> reader) {
            Objects.requireNonNull(predicate, "predicate cannot be null!");
            Objects.requireNonNull(reader, "reader cannot be null!");
            this.reader = reader;
            this.predicate = predicate;
            this.clazz = null;
        }

        ReaderAdapter(Class<T> clazz, Reader<T> reader) {
            Objects.requireNonNull(clazz, "clazz cannot be null!");
            Objects.requireNonNull(reader, "reader cannot be null!");
            this.reader = reader;
            this.clazz = clazz;
            this.predicate = null;
        }

        @Override
        @SuppressWarnings("unchecked")
        public <U extends T> Mono<U> read(Publisher<DataChunk> publisher,
                GenericType<U> type, ReaderContext context) {

            return Mono.fromFuture(reader.applyAndCast(publisher,
                    (Class<U>)type.rawType()));
        }

        @Override
        public boolean accept(GenericType<?> type, ReaderContext scope) {
            if (predicate != null) {
                return predicate.test(type.rawType());
            }
            return clazz.isAssignableFrom(type.rawType());
        }
    }
}
