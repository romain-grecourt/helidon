package io.helidon.common.http;

import io.helidon.common.GenericType;
import io.helidon.common.http.MessageBody.Filter;
import io.helidon.common.reactive.Flow.Publisher;
import io.helidon.common.reactive.Flow.Subscriber;
import io.helidon.common.reactive.Flow.Subscription;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.atomic.AtomicReference;
import io.helidon.common.http.MessageBody.ReadableContent;
import java.util.function.Function;
import java.util.function.Predicate;

/**
 * Implementation of {@link ReadableContent}.
 */
public final class MessageBodyReadableContent implements ReadableContent {

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
    public MessageBodyReadableContent registerStreamReader(
            MessageBody.Reader<?> reader) {

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

    @Override
    public <T> CompletionStage<T> as(final Class<T> type) {
        return as(GenericType.create(type));
    }

    @Override
    public <T> CompletionStage<T> as(final GenericType<T> type) {
        FutureConverter<T> converter = new FutureConverter<>();
        Publisher<DataChunk> pub = context.applyFilters(publisher, type);
        context.unmarshall(pub, type).subscribe(converter);
        return converter.future();
    }

    @Override
    public <T> Publisher<T> asStream(Class<T> type) {
        return asStream(GenericType.create(type));
    }

    @Override
    public <T> Publisher<T> asStream(GenericType<T> type) {
        Publisher<DataChunk> pub = context.applyFilters(publisher);
        return (Publisher<T>) context.unmarshallStream(pub, type);
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
        
    }

    @Deprecated
    @Override
    public <T> void registerReader(Predicate<Class<?>> predicate, Reader<T> reader) {
    }

    /**
     * A subscriber that exposes the first item of a subscription as a future.
     * @param <T> item type parameter
     */
    private static final class FutureConverter<T> implements Subscriber<T> {

        private final CompletableFuture<T> future;
        private final AtomicReference<T> itemRef;
        private Subscription subscription;

        FutureConverter() {
            future = new CompletableFuture<>();
            itemRef = new AtomicReference<>();
        }

        @Override
        public void onSubscribe(Subscription subscription) {
            this.subscription = subscription;
            subscription.request(1);
        }

        @Override
        public void onNext(T item) {
            itemRef.set(item);
            subscription.cancel();
            future.complete(item);
        }

        @Override
        public void onError(Throwable error) {
            future.completeExceptionally(error);
        }

        @Override
        public void onComplete() {
        }

        /**
         * Get the underlying future.
         * @return CompletableFuture
         */
        CompletableFuture<T> future() {
            return future;
        }
    }
}
