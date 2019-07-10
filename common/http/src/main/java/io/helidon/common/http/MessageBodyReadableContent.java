package io.helidon.common.http;

import io.helidon.common.GenericType;
import io.helidon.common.http.MessageBody.Filter;
import io.helidon.common.reactive.Flow.Publisher;
import io.helidon.common.reactive.Flow.Subscriber;
import java.util.Objects;
import java.util.concurrent.CompletionStage;
import io.helidon.common.http.MessageBody.ReadableContent;
import io.helidon.common.http.MessageBody.StreamReader;
import io.helidon.common.reactive.Mono;
import java.io.ByteArrayOutputStream;
import java.util.function.Function;
import java.util.function.Predicate;

/**
 * Implementation of {@link ReadableContent}.
 */
public final class MessageBodyReadableContent implements ReadableContent {

    private static final GenericType<ByteArrayOutputStream> BAOS_TYPE =
        GenericType.create(ByteArrayOutputStream.class);

    private static final Function<ByteArrayOutputStream, Mono<byte[]>> BYTE_ARRAY_MAPPER =
            (baos) -> Mono.just(baos.toByteArray());

    private final Publisher<DataChunk> publisher;
    private final MessageBodyReaderContext context;

    /**
     * Create a new readable content backed by the specified inbound publisher.
     * @param publisher inbound publisher
     * @param context reader context
     */
    MessageBodyReadableContent(Publisher<DataChunk> publisher,
            MessageBodyReaderContext context) {

        Objects.requireNonNull(publisher, "publisher is null!");
        Objects.requireNonNull(context, "scope is null!");
        this.publisher = publisher;
        this.context = context;
    }

    /**
     * Copy constructor.
     * @param orig original context to be copied
     */
    private MessageBodyReadableContent(MessageBodyReadableContent orig) {
        Objects.requireNonNull(orig, "orig is null!");
        this.publisher = orig.publisher;
        this.context = orig.context;
    }

    /**
     * Get the underlying reader context.
     * @return MessageBodyReaderContext
     */
    public MessageBodyReaderContext context() {
        return context;
    }

    @Override
    public MessageBodyReadableContent registerFilter(Filter filter) {
        context.registerFilter(filter);
        return this;
    }

    @Override
    public MessageBodyReadableContent registerReader(
            MessageBody.Reader<?> reader) {

        context.registerReader(reader);
        return this;
    }

    @Override
    public MessageBodyReadableContent registerReader(StreamReader<?> reader) {
        context.registerReader(reader);
        return this;
    }

    @Override
    public void subscribe(Subscriber<? super DataChunk> subscriber) {
        try {
            context.applyFilters(publisher).subscribe(subscriber);
        } catch (Exception e) {
            subscriber.onError(new IllegalArgumentException(
                    "Unexpected exception occurred during publishers chaining",
                    e));
        }
    }

    @SuppressWarnings("unchecked")
    private <T> CompletionStage<T> asByteArray() {
        return (CompletionStage<T>) context.unmarshall(publisher, BAOS_TYPE)
                .flatMap(BYTE_ARRAY_MAPPER).toFuture();
    }

    @Override
    public <T> CompletionStage<T> as(final Class<T> type) {
        if (byte[].class.equals(type)) {
            return this.<T>asByteArray();
        }
        return context.unmarshall(publisher, GenericType.create(type))
                .toFuture();
    }

    @Override
    public <T> CompletionStage<T> as(final GenericType<T> type) {
        if (byte[].class.equals(type.rawType())) {
            this.<T>asByteArray();
        }
        return context.unmarshall(publisher, type).toFuture();
    }

    @Override
    public <T> Publisher<T> asStream(Class<T> type) {
        return asStream(GenericType.create(type));
    }

    @Override
    public <T> Publisher<T> asStream(GenericType<T> type) {
        return context.unmarshallStream(publisher, type);
    }

    /**
     * Safely cast a {@link ReadableContent} into MessageBodyReadableContent.
     * @param content content to cast
     * @return MessageBodyReadableContent, never {@code null}
     * @throws IllegalArgumentException if the specified content is not
     * an instance of MessageBodyReadableContent
     */
    public static MessageBodyReadableContent of(ReadableContent content)
        throws IllegalArgumentException {

        Objects.requireNonNull(content, "content cannot be null!");
        if (content instanceof MessageBodyReadableContent) {
            return (MessageBodyReadableContent) content;
        }
        throw new IllegalArgumentException("Invalid content " + content);
    }

    /**
     * Create a new readable content backed by the given publisher and context.
     * @param publisher inbound publisher
     * @param context reader context
     * @return MessageBodyReadableContent
     */
    public static MessageBodyReadableContent create(
            Publisher<DataChunk> publisher, MessageBodyReaderContext context) {

        return new MessageBodyReadableContent(publisher, context);
    }

    @Deprecated
    @Override
    public void registerFilter(
            Function<Publisher<DataChunk>, Publisher<DataChunk>> function) {

        context.registerFilter(function);
    }

    @Deprecated
    @Override
    public <T> void registerReader(Class<T> type, Reader<T> reader) {
        context.registerReader(type, reader);
    }

    @Deprecated
    @Override
    public <T> void registerReader(Predicate<Class<?>> predicate,
            Reader<T> reader) {

        context.registerReader(predicate, reader);
    }
}
