package io.helidon.common.http;

import io.helidon.common.GenericType;
import io.helidon.common.http.MessageBody.Filter;
import io.helidon.common.http.MessageBody.Writer;
import io.helidon.common.http.MessageBody.WriteableContent;
import io.helidon.common.reactive.FixedItemsPublisher;
import io.helidon.common.reactive.Flow.Publisher;
import io.helidon.common.reactive.Flow.Subscriber;
import java.util.Objects;

import static io.helidon.common.CollectionsHelper.listOf;
import java.util.function.Function;
import java.util.function.Predicate;

/**
 * Implementation of {@link WriteableContent}.
 */
public final class MessageBodyWriteableContent implements WriteableContent {

    private final Object entity;
    private final Publisher<Object> stream;
    private final GenericType<Object> type;
    private final Publisher<DataChunk> publisher;
    private final MessageBodyWriterContext context;

    /**
     * Create a new writeable content backed by an entity. The created content
     * creates its own standalone (non parented) writer context with the
     * specified headers.
     *
     * @param entity entity object
     * @param headers writer context backing headers
     */
    MessageBodyWriteableContent(Object entity, Parameters headers) {
        Objects.requireNonNull(entity, "entity cannot be null!");
        this.type = GenericType.<Object>create(entity.getClass());
        this.entity = entity;
        this.context = MessageBodyWriterContext.create(headers);
        this.publisher = null;
        this.stream = null;
    }

    /**
     * Create a new writeable content backed by an entity stream.
     * The created content
     * creates its own standalone (non parented) writer context with the
     * specified headers.
     *
     * @param stream entity stream
     * @param type actual type representation
     * @param headers writer context backing headers
     */
    @SuppressWarnings("unchecked")
    MessageBodyWriteableContent(Publisher<Object> stream,
            GenericType<? extends Object> type, Parameters headers) {

        Objects.requireNonNull(stream, "stream cannot be null!");
        Objects.requireNonNull(type, "type cannot be null!");
        this.stream = stream;
        this.type = (GenericType<Object>) type;
        this.context = MessageBodyWriterContext.create(new HashParameters());
        this.entity = null;
        this.publisher = null;
    }

    /**
     * Create a new writeable content backed by a raw publisher.
     * The created content uses an standalone (non parented) writer context with
     * read-only headers.
     * @param publisher raw publisher
     * @param headers writer context backing headers
     */
    MessageBodyWriteableContent(Publisher<DataChunk> publisher,
            Parameters headers) {

        Objects.requireNonNull(publisher, "publisher cannot be null!");
        this.publisher = publisher;
        this.context = MessageBodyWriterContext.create(headers);
        this.entity = null;
        this.stream = null;
        this.type = null;
    }

    @Override
    public void subscribe(Subscriber<? super DataChunk> subscriber) {
        toPublisher(null).subscribe(subscriber);
    }

    public Publisher<DataChunk> toPublisher(MessageBodyWriterContext fallback) {
        if (publisher != null) {
            return fallback.applyFilters(publisher);
        }
        Publisher<Object> content;
        if (entity != null) {
            content = new FixedItemsPublisher<>(listOf(entity));
        } else {
            content = stream;
        }
        Publisher<DataChunk> pub = fallback.marshall(content, type);
        return fallback.applyFilters(pub, type);
    }

    @Override
    public MessageBodyWriteableContent registerFilter(Filter filter) {
        context.registerFilter(filter);
        return this;
    }

    @Override
    public MessageBodyWriteableContent registerWriter(Writer<?> writer) {
        context.registerWriter(writer);
        return this;
    }

    @Override
    public MessageBodyWriteableContent registerStreamWriter(Writer<?> writer) {
        context.registerWriter(writer);
        return this;
    }

    @Deprecated
    @Override
    public <T> MessageBodyWriteableContent registerWriter(Class<T> type,
            Function<T, Publisher<DataChunk>> function) {

        return null;
    }

    @Deprecated
    @Override
    public <T> MessageBodyWriteableContent registerWriter(Class<T> type,
            MediaType contentType,
            Function<? extends T, Publisher<DataChunk>> function) {

        return null;
    }

    @Deprecated
    @Override
    public <T> MessageBodyWriteableContent registerWriter(
            Predicate<?> accept, Function<T, Publisher<DataChunk>> function) {

        return null;
    }

    @Deprecated
    @Override
    public <T> MessageBodyWriteableContent registerWriter(Predicate<?> accept,
            MediaType contentType, Function<T, Publisher<DataChunk>> function) {

        return null;
    }

    @Deprecated
    @Override
    public void registerFilter(
            Function<Publisher<DataChunk>, Publisher<DataChunk>> function) {

        context.registerFilter(function);
    }

    /**
     * Create a new writeable content backed by an entity object.
     *
     * @param entity object, must not be {@code null}
     * @param headers writer context backing headers, must not be {@code null}
     * @return MessageBodyWriteableContent
     */
    public static MessageBodyWriteableContent create(Object entity,
            Parameters headers) {

        return new MessageBodyWriteableContent(entity, headers);
    }

    /**
     * Create a new writeable content backed by an entity stream.
     *
     * @param stream entity stream, must not be {@code null}
     * @param type actual type representation, must not be {@code null}
     * @param headers writer context backing headers, must not be {@code null}
     * @return MessageBodyWriteableContent
     */
    public static MessageBodyWriteableContent create(Publisher<Object> stream,
            GenericType<? extends Object> type, Parameters headers) {

        return new MessageBodyWriteableContent(stream, type, headers);
    }

    /**
     * Create a new writeable content backed by a raw publisher.
     *
     * @param publisher raw publisher
     * @param headers writer context backing headers, must not be {@code null}
     * @return MessageBodyWriteableContent
     */
    public static MessageBodyWriteableContent create(
            Publisher<DataChunk> publisher, Parameters headers) {

        return new MessageBodyWriteableContent(publisher, headers);
    }

    /**
     * Safely cast a {@link WriteableContent} into MessageBodyWriteableContent.
     * @param content content to cast
     * @return MessageBodyWriteableContent, never {@code null}
     * @throws IllegalArgumentException if the specified content is not
     * an instance of MessageBodyWriteableContent
     */
    public static MessageBodyWriteableContent of(WriteableContent content)
        throws IllegalArgumentException {

        Objects.requireNonNull(content, "content cannot be null!");
        if (content instanceof MessageBodyWriteableContent) {
            return (MessageBodyWriteableContent) content;
        }
        throw new IllegalArgumentException("Invalid content " + content);
    }
}
