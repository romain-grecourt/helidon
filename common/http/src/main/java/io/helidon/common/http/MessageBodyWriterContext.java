package io.helidon.common.http;

import io.helidon.common.CollectionsHelper;
import io.helidon.common.GenericType;
import io.helidon.common.http.MessageBody.Filters;
import io.helidon.common.http.MessageBody.StreamWriter;
import io.helidon.common.http.MessageBody.WriteOperator;
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
import io.helidon.common.reactive.Flow.Publisher;
import io.helidon.common.reactive.Mono;
import io.helidon.common.reactive.Multi;
import java.util.function.Function;

/**
 * Implementation of {@link WriterContext}.
 */
public final class MessageBodyWriterContext extends MessageBodyContextBase
        implements WriterContext, Writers, Filters {

    private static final Charset DEFAULT_CHARSET = StandardCharsets.UTF_8;

    private final Parameters headers;
    private final List<MediaType> acceptedTypes;
    private final MessageBodyOperators<WriteOperator<?>, WriterContext> writers;
    private final MessageBodyOperators<WriteOperator<?>, WriterContext> swriters;
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
        writers.registerFirst(writer);
        return this;
    }

    @Override
    public MessageBodyWriterContext registerWriter(StreamWriter<?> writer) {
        swriters.registerFirst(writer);
        return this;
    }

    /**
     * Convert a given input publisher into HTTP payload by selecting a
     * writer that accepts the specified type and current context.
     *
     * @param <T> entity type parameter
     * @param content input publisher
     * @param type actual representation of the entity type
     * @param fallback fallback context, may be {@code null}
     * @return publisher, never {@code null}
     */
    @SuppressWarnings("unchecked")
    public <T> Publisher<DataChunk> marshall(Publisher<T> content,
            GenericType<T> type, MessageBodyWriterContext fallback) {

        try {
            if (content == null) {
                return applyFilters(Multi.<DataChunk>empty());
            }
            Writer<T> writer = (Writer<T>) writers.select(type, this,
                    fallback != null ? fallback.writers : null);
            if (writer == null) {
                return Multi.<DataChunk>error(new IllegalStateException(
                        "No writer found for type: "
                        + type.getTypeName()));
            }
            return applyFilters(writer.write(Mono.from(content), type, this));
        } catch (Throwable ex) {
            throw new IllegalStateException("Transformation failed!", ex);
        }
    }

    /**
     * Convert a given input publisher into HTTP payload by selecting a
     * writer with the specified class.
     *
     * @param <T> entity type parameter
     * @param content input publisher
     * @param writerType the requested writer class
     * @param type actual representation of the entity type
     * @param fallback fallback context, may be {@code null}
     * @return publisher, never {@code null}
     */
    @SuppressWarnings("unchecked")
    public <T> Publisher<DataChunk> marshall(Publisher<T> content,
            Class<? extends Writer<T>> writerType, GenericType<T> type,
            MessageBodyWriterContext fallback) {

        try {
            if (content == null) {
                return applyFilters(Multi.<DataChunk>empty());
            }
            Writer<T> writer = (Writer<T>) writers.get(writerType,
                    fallback != null ? fallback.writers : null);
            if (writer == null) {
                return Multi.<DataChunk>error(new IllegalStateException(
                        "No writer found of type: "
                                + type.getTypeName()));
            }
            return applyFilters(writer.write(Mono.from(content), type, this));
        } catch (Throwable ex) {
            throw new IllegalStateException("Transformation failed!", ex);
        }
    }

    /**
     * Convert a given input publisher into HTTP payload by selecting a
     * stream writer that accepts the specified type and current context.
     *
     * @param <T> entity type parameter
     * @param content input publisher
     * @param type actual representation of the entity type
     * @param fallback fallback context
     * @return publisher, never {@code null}
     */
    @SuppressWarnings("unchecked")
    public <T> Publisher<DataChunk> marshallStream(Publisher<T> content,
            GenericType<T> type, MessageBodyWriterContext fallback) {

        try {
            if (content == null) {
                return applyFilters(Multi.<DataChunk>empty());
            }
            StreamWriter<T> writer = (StreamWriter<T>) swriters.select(type,
                    this, fallback != null ? fallback.swriters : null);
            if (writer == null) {
                return Multi.<DataChunk>error(new IllegalStateException(
                        "No stream writer found of type: "
                        + type.getTypeName()));
            }
            return applyFilters(writer.write(content, type, this));
        } catch (Throwable ex) {
            throw new IllegalStateException("Transformation failed!", ex);
        }
    }

    /**
     * Convert a given input publisher into HTTP payload by selecting a
     * stream writer with the specified class.
     *
     * @param <T> entity type parameter
     * @param content input publisher
     * @param writerType the requested writer class
     * @param type actual representation of the entity type
     * @param fallback fallback context
     * @return publisher, never {@code null}
     */
    @SuppressWarnings("unchecked")
    public <T> Publisher<DataChunk> marshallStream(Publisher<T> content,
            Class<? extends Writer<T>> writerType, GenericType<T> type,
            MessageBodyWriterContext fallback) {

        try {
            if (content == null) {
                return applyFilters(Multi.<DataChunk>empty());
            }
            StreamWriter<T> writer = (StreamWriter<T>) swriters.get(writerType,
                    fallback != null ? fallback.swriters : null);
            if (writer == null) {
                return Multi.<DataChunk>error(new IllegalStateException(
                        "No stream writer found of type: "
                        + type.getTypeName()));
            }
            return applyFilters(writer.write(content, type, this));
        } catch (Throwable ex) {
            throw new IllegalStateException("Transformation failed!", ex);
        }
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
    private static final class WriterAdapter<T> implements Writer<T> {

        private final Function<T, Publisher<DataChunk>> function;
        private final Predicate predicate;
        private final Class<T> type;
        private final MediaType contentType;

        @SuppressWarnings("unchecked")
        WriterAdapter(Function<T, Publisher<DataChunk>> function,
                Predicate<?> predicate, MediaType contentType) {

            Objects.requireNonNull(function, "function cannot be null!");
            Objects.requireNonNull(predicate, "predicate cannot be null!");
            this.function = function;
            this.predicate = predicate;
            this.contentType = contentType;
            this.type = null;
        }

        @SuppressWarnings("unchecked")
        WriterAdapter(Function<? extends T, Publisher<DataChunk>> function,
                Class<T> type, MediaType contentType) {

            Objects.requireNonNull(function, "function cannot be null!");
            Objects.requireNonNull(type, "type cannot be null!");
            this.function = (Function<T, Publisher<DataChunk>>) function;
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
        public Publisher<DataChunk> write(Mono<T> mono,
                GenericType<? extends T> type, WriterContext context) {

            return mono.flatMapMany(function);
        }
    }
}
