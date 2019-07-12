package io.helidon.media.common;

import io.helidon.common.GenericType;
import io.helidon.common.http.Content;
import io.helidon.common.http.DataChunk;
import io.helidon.common.reactive.Flow.Publisher;
import io.helidon.common.reactive.Flow.Subscriber;
import java.util.Objects;
import java.util.concurrent.CompletionStage;
import io.helidon.common.http.Reader;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;
import java.util.function.Predicate;

/**
 * Implementation of {@link ReadableContent}.
 */
public final class MessageBodyReadableContent
        implements MessageBodyReaders, MessageBodyFilters, MessageBodyContent, Content {

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
        Objects.requireNonNull(context, "context is null!");
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
    public MessageBodyReadableContent registerFilter(MessageBodyFilter filter) {
        context.registerFilter(filter);
        return this;
    }

    @Override
    public MessageBodyReadableContent registerReader(
            MessageBodyReader<?> reader) {

        context.registerReader(reader);
        return this;
    }

    @Override
    public MessageBodyReadableContent registerReader(
            MessageBodyStreamReader<?> reader) {

        context.registerReader(reader);
        return this;
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

    @Override
    public <T> CompletionStage<T> as(final Class<T> type) {
            return context.unmarshall(publisher, GenericType.create(type))
                .toFuture();
    }

    /**
     * Consumes and converts the inbound payload into a completion stage of the
     * requested type.
     *
     * @param type the requested type class
     * @param <T> the requested type
     * @return a completion stage of the requested type
     */
    public <T> CompletionStage<T> as(final GenericType<T> type) {
        return context.unmarshall(publisher, type).toFuture();
    }

    public <T> Publisher<T> asStream(Class<T> type) {
        return asStream(GenericType.create(type));
    }

    public <T> Publisher<T> asStream(GenericType<T> type) {
        return context.unmarshallStream(publisher, type);
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
}
