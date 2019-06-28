package io.helidon.common.http;

import io.helidon.common.CollectionsHelper;
import io.helidon.common.GenericType;
import io.helidon.common.http.MessageBody.Filters;
import io.helidon.common.http.MessageBody.Writer;
import java.nio.charset.Charset;
import java.nio.charset.IllegalCharsetNameException;
import java.nio.charset.StandardCharsets;
import java.nio.charset.UnsupportedCharsetException;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Predicate;
import io.helidon.common.http.MessageBody.WriterContext;
import io.helidon.common.http.MessageBody.Writers;
import io.helidon.common.reactive.EmptyPublisher;
import io.helidon.common.reactive.FailedPublisher;
import io.helidon.common.reactive.Flow.Publisher;
import io.helidon.common.reactive.Flow.Subscriber;
import io.helidon.common.reactive.SingleInputDelegatingProcessor;
import java.util.function.Function;

/**
 * Implementation of {@link WriterContext}.
 */
public final class MessageBodyWriterContext extends MessageBodyContextBase
        implements WriterContext, Writers, Filters {

    private static final Charset DEFAULT_CHARSET = StandardCharsets.UTF_8;

    private final Parameters headers;
    private final List<MediaType> acceptedTypes;
    private final MessageBodyOperators<Writer<?>, WriterContext> writers;
    private final MessageBodyOperators<Writer<?>, WriterContext> swriters;
    private boolean contentTypeCached;
    private Optional<MediaType> contentTypeCache;
    private boolean charsetCached;
    private Charset charsetCache;

    /**
     * Create a new parented context.
     * @param parent parent context, must not be {@code null}
     * @param eventListener subscription event listener, may be {@code null}
     * @param headers backing headers, must not be {@code null}
     * @param acceptedTypes accepted types, may be {@code null}
     */
    private MessageBodyWriterContext(MessageBodyWriterContext parent,
            EventListener eventListener, Parameters headers,
            List<MediaType> acceptedTypes) {

        super(parent, eventListener);
        Objects.requireNonNull(headers, "headers cannot be null!");
        this.headers = headers;
        if (acceptedTypes != null) {
            this.acceptedTypes = acceptedTypes;
        } else {
            this.acceptedTypes = CollectionsHelper.listOf();
        }
        if (parent != null) {
            this.writers = new MessageBodyOperators<>(parent.writers);
            this.swriters = new MessageBodyOperators<>(parent.swriters);
        } else {
            this.writers = new MessageBodyOperators<>();
            this.swriters = new MessageBodyOperators<>();
        }
    }

    /**
     * Create a new standalone (non parented) context.
     * @param headers backing headers, may not be {@code null}
     */
    private MessageBodyWriterContext(Parameters headers) {
        super(null);
        Objects.requireNonNull(headers, "headers cannot be null!");
        this.headers = headers;
        this.writers = new MessageBodyOperators<>();
        this.swriters = new MessageBodyOperators<>();
        this.acceptedTypes = CollectionsHelper.listOf();
    }

    /**
     * Create a new standalone (non parented) context.
     */
    private MessageBodyWriterContext() {
        super(null);
        this.headers = ReadOnlyParameters.empty();
        this.writers = new MessageBodyOperators<>();
        this.swriters = new MessageBodyOperators<>();
        this.acceptedTypes = CollectionsHelper.listOf();
        this.contentTypeCache = Optional.empty();
        this.contentTypeCached = true;
        this.charsetCache = DEFAULT_CHARSET;
        this.charsetCached = true;
    }

    @Override
    public MessageBodyWriterContext registerWriter(Writer<?> writer) {
        swriters.registerFirst(writer);
        return this;
    }

    @Override
    public MessageBodyWriterContext registerStreamWriter(Writer<?> writer) {
        swriters.registerFirst(writer);
        return this;
    }

    /**
     * Convert a given input publisher into HTTP payload using the specified
     * message body writer.
     *
     * @param <T> entity type parameter
     * @param writer message body writer
     * @param content input publisher
     * @param type actual representation of the entity type
     * @param ctx writer context
     * @return publisher, never {@code null}
     */
    private static <T> Publisher<DataChunk> doMarshall(Writer<T> writer,
            Publisher<T> content, GenericType<T> type,
            MessageBodyWriterContext ctx) {

        try {
            Publisher<DataChunk> publisher = writer.write(content, type, ctx);
            if (publisher == null) {
                publisher = new EmptyPublisher<>();
            }
            return ctx.applyFilters(publisher);
        } catch (Throwable ex) {
            if (ex instanceof IllegalArgumentException) {
                return new FailedPublisher<>(ex);
            } else {
                return new FailedPublisher<>(
                        new IllegalStateException("Transformation failed!", ex));
            }
        }
    }

    /**
     * Select a writer with the specified class from the given operators.
     *
     * @param <T> entity type parameter
     * @param writers operators to lookup the writer from
     * @param writerCls the requested writer class
     * @return Writer, never {@code null}
     * @throws IllegalArgumentException if no writer is found
     */
    @SuppressWarnings("unchecked")
    private static <T> Writer<T> selectWriter(
            MessageBodyOperators<Writer<?>, WriterContext> writers,
            Class<? extends Writer<T>> writerCls) {

        Writer<?> writer = writers.get(writerCls);
        if (writers == null) {
            throw new IllegalArgumentException("No writer found of type: "
                    + writerCls.getTypeName());
        }
        return (Writer<T>) writer;
    }

    /**
     * Select a writer that accepts the specified entity type and writer context
     * from the given operators.
     *
     * @param <T> entity type parameter
     * @param writers operators to lookup the writer from
     * @param type actual representation of the entity type
     * @param ctx writer context
     * @return Writer, never {@code null}
     * @throws IllegalArgumentException if no writer is found
     */
    @SuppressWarnings("unchecked")
    private static <T> Writer<T> selectWriter(
            MessageBodyOperators<Writer<?>, WriterContext> writers,
            GenericType<? super T> type, MessageBodyWriterContext ctx) {

        Writer<?> writer = writers.select(type, ctx);
        if (writers == null) {
            throw new IllegalArgumentException("No writer found of type: "
                    + type.getTypeName());
        }
        return (Writer<T>) writer;
    }

    /**
     * Convert a given input publisher into HTTP payload by selecting a
     * writer that accepts the specified type and current context.
     *
     * @param <T> entity type parameter
     * @param content input publisher
     * @param type actual representation of the entity type
     * @return publisher, never {@code null}
     */
    public <T> Publisher<DataChunk> marshall(Publisher<T> content,
            GenericType<T> type) {

        return doMarshall(selectWriter(writers, type, this), content, type,
                this);
    }

    /**
     * Convert a given input publisher into HTTP payload by selecting a
     * writer with the specified class.
     *
     * @param <T> entity type parameter
     * @param content input publisher
     * @param writerCls the requested writer class
     * @param type actual representation of the entity type
     * @return publisher, never {@code null}
     */
    public <T> Publisher<DataChunk> marshall(Publisher<T> content,
            Class<? extends Writer<T>> writerCls, GenericType<T> type) {

        return doMarshall(selectWriter(writers, writerCls), content, type,
                this);
    }

    /**
     * Convert a given input publisher into HTTP payload by selecting a
     * stream writer that accepts the specified type and current context.
     *
     * @param <T> entity type parameter
     * @param content input publisher
     * @param type actual representation of the entity type
     * @return publisher, never {@code null}
     */
    public <T> Publisher<DataChunk> marshallStream(Publisher<T> content,
            GenericType<T> type) {

        return doMarshall(selectWriter(swriters, type, this), content, type,
                this);
    }

    /**
     * Convert a given input publisher into HTTP payload by selecting a
     * stream writer with the specified class.
     *
     * @param <T> entity type parameter
     * @param content input publisher
     * @param writerCls the requested writer class
     * @param type actual representation of the entity type
     * @return publisher, never {@code null}
     */
    public <T> Publisher<DataChunk> marshallStream(Publisher<T> content,
            Class<? extends Writer<T>> writerCls, GenericType<T> type) {

        return doMarshall(selectWriter(swriters, writerCls), content, type,
                this);
    }

    @Override
    public Parameters headers() {
        return headers;
    }

    @Override
    public Optional<MediaType> contentType() {
        if (contentTypeCached) {
            return contentTypeCache;
        }
        contentTypeCache = Optional.ofNullable(headers
                .first(Http.Header.CONTENT_TYPE)
                .map(MediaType::parse)
                .orElse(null));
        contentTypeCached = true;
        return contentTypeCache;
    }

    @Override
    public List<MediaType> acceptedTypes() {
        return acceptedTypes;
    }

    @Override
    public void contentType(MediaType contentType) {
        if (contentType != null) {
            headers.putIfAbsent(Http.Header.CONTENT_TYPE,
                    contentType.toString());
        }
    }

    @Override
    public void contentLength(long contentLength) {
        if (contentLength >= 0) {
            headers.putIfAbsent(Http.Header.CONTENT_LENGTH,
                    String.valueOf(contentLength));
        }
    }

    @Override
    public MediaType findAccepted(Predicate<MediaType> predicate,
            MediaType defaultType) throws IllegalStateException {

        Objects.requireNonNull(predicate, "predicate cannot be null");
        Objects.requireNonNull(defaultType, "defaultType cannot be null");
        MediaType contentType = contentType().orElse(null);
        if (contentType == null) {
            if (acceptedTypes.isEmpty()) {
                return defaultType;
            } else {
                for (final MediaType acceptedType : acceptedTypes) {
                    if (predicate.test(acceptedType)) {
                        if (acceptedType.isWildcardType()
                                || acceptedType.isWildcardSubtype()) {
                            return defaultType;
                        }
                        return MediaType.create(acceptedType.type(),
                                acceptedType.subtype());
                    }
                }
            }
        } else {
            if (predicate.test(contentType)) {
                return contentType;
            }
        }
        throw new IllegalStateException("No accepted Content-Type");
    }

    @Override
    public MediaType findAccepted(MediaType mediaType)
            throws IllegalStateException {

        Objects.requireNonNull(mediaType, "mediaType cannot be null");
        for (MediaType acceptedType : acceptedTypes) {
            if (mediaType.equals(acceptedType)) {
                return acceptedType;
            }
        }
        throw new IllegalStateException("No accepted Content-Type");
    }

    @Override
    public final Charset charset() throws IllegalStateException {
        if (charsetCached) {
            return charsetCache;
        }
        MediaType contentType = contentType().orElse(null);
        if (contentType != null) {
            try {
                charsetCache = contentType.charset().map(Charset::forName)
                        .orElse(DEFAULT_CHARSET);
                charsetCached = true;
                return charsetCache;
            } catch (IllegalCharsetNameException
                    | UnsupportedCharsetException ex) {
                throw new IllegalStateException(ex);
            }
        }
        charsetCache = DEFAULT_CHARSET;
        charsetCached = true;
        return charsetCache;
    }

    /**
     * Create a new writer context
     * @param parent
     * @param eventListener
     * @param headers
     * @param acceptedTypes
     * @return 
     */
    public static MessageBodyWriterContext create(
            MessageBodyWriterContext parent, EventListener eventListener,
            Parameters headers, List<MediaType> acceptedTypes) {

        return new MessageBodyWriterContext(parent, eventListener, headers,
                acceptedTypes);
    }

    /**
     * Create a new empty writer context backed by the specified headers.
     * @param headers headers
     * @return MessageBodyWriterContext
     */
    public static MessageBodyWriterContext create(Parameters headers) {
        return new MessageBodyWriterContext(headers);
    }

    /**
     * Create a new empty writer context backed by empty read-only headers.
     * Such writer context is typically the parent context that is used to hold
     * application wide writers and outbound filters.
     * @return MessageBodyWriterContext
     */
    public static MessageBodyWriterContext create() {
        return new MessageBodyWriterContext(ReadOnlyParameters.empty());
    }

    /**
     * Safely cast a {@link WriterContext} into MessageBodyWriterContext.
     * @param context context to cast
     * @return MessageBodyWriterContext, never {@code null}
     * @throws IllegalArgumentException if the specified content is not
     * an instance of MessageBodyWriterContext
     */
    public static MessageBodyWriterContext of(WriterContext context)
        throws IllegalArgumentException {

        Objects.requireNonNull(context, "context cannot be null!");
        if (context instanceof MessageBodyWriterContext) {
            return (MessageBodyWriterContext) context;
        }
        throw new IllegalArgumentException("Invalid content " + context);
    }

    @Deprecated
    @Override
    public <T> MessageBodyWriterContext registerWriter(Class<T> type,
            Function<T, Publisher<DataChunk>> function) {

        writers.registerLast(new WriterAdapter<>(function, type, null));
        return this;
    }

    @Deprecated
    @Override
    public <T> MessageBodyWriterContext registerWriter(Class<T> type,
            MediaType contentType,
            Function<? extends T, Publisher<DataChunk>> function) {

        writers.registerLast(new WriterAdapter<>(function, type, contentType));
        return this;
    }

    @Deprecated
    @Override
    public <T> MessageBodyWriterContext registerWriter(Predicate<?> accept,
            Function<T, Publisher<DataChunk>> function) {

        writers.registerLast(new WriterAdapter<>(function, accept, null));
        return this;
    }

    @Deprecated
    @Override
    public <T> MessageBodyWriterContext registerWriter(Predicate<?> accept,
            MediaType contentType, Function<T, Publisher<DataChunk>> function) {

        writers.registerLast(new WriterAdapter<>(function, accept, contentType));
        return this;
    }

    /**
     * Message body writer adapter for the old deprecated writer.
     * @param <T> writer type
     */
    private static final class WriterAdapter<T>
            implements MessageBody.Writer<T> {

        private final WriterAdapterProcessor<? extends T> processor;
        private final Predicate predicate;
        private final Class<T> type;
        private final MediaType contentType;

        @SuppressWarnings("unchecked")
        WriterAdapter(Function<? extends T, Publisher<DataChunk>> function,
                Predicate<?> predicate, MediaType contentType) {

            Objects.requireNonNull(function, "function cannot be null!");
            Objects.requireNonNull(predicate, "predicate cannot be null!");
            this.processor = new WriterAdapterProcessor<>(function);
            this.predicate = predicate;
            this.contentType = contentType;
            this.type = null;
        }

        WriterAdapter(Function<? extends T, Publisher<DataChunk>> function,
                Class<T> type, MediaType contentType) {

            Objects.requireNonNull(function, "function cannot be null!");
            Objects.requireNonNull(type, "type cannot be null!");
            this.processor = new WriterAdapterProcessor<>(function);
            this.type = type;
            this.contentType = contentType;
            this.predicate = null;
        }

        @Override
        @SuppressWarnings("unchecked")
        public boolean accept(GenericType<?> type, WriterContext scope) {
            if (this.type != null) {
                if (!this.type.isAssignableFrom(type.rawType())) {
                    return false;
                }
            } else {
                if (!predicate.test((Object)type.rawType())) {
                    return false;
                }
            }
            MediaType ct = scope.contentType().orElse(null);
            return !(contentType != null
                    && ct != null
                    && !ct.test(contentType));
        }

        @Override
        @SuppressWarnings("unchecked")
        public <U extends T> Publisher<DataChunk> write(Publisher<U> content,
                GenericType<U> type, WriterContext context) {

            content.subscribe((Subscriber<U>) processor);
            return processor;
        }
    }

    /**
     * A processor that implements the new write function for the deprecated
     * writer.
     * @param <T> writer type
     */
    private static final class WriterAdapterProcessor<T>
            extends SingleInputDelegatingProcessor<T, DataChunk> {

        private final Function<T, Publisher<DataChunk>> function;

        WriterAdapterProcessor(
                Function<T, Publisher<DataChunk>> function) {
            this.function = function;
        }

        @Override
        protected Publisher<DataChunk> delegate(T item) {
            return function.apply(item);
        }
    }

}
