package io.helidon.common.http;

import io.helidon.common.GenericType;
import io.helidon.common.reactive.Flow.Publisher;
import io.helidon.common.reactive.Flow.Subscriber;
import java.util.Objects;
import java.util.concurrent.CompletionStage;
import java.util.function.Predicate;

/**
 * In-bound reactive payload that can be converted to entities.
 */
@SuppressWarnings("deprecation")
public final class InBoundContent
        implements HttpContent, EntityReadersRegistry, Content {

    private final Publisher<DataChunk> originalPublisher;
    private final ContentInterceptor.Factory interceptorFactory;
    private final InBoundScope scope;
    private final EntityReaders readers;

    /**
     * Create a new instance.
     * @param originalPublisher the original publisher for this content
     * @param scope in-bound scope
     * @param interceptorFactory content interceptor factory, may be
     * {@code null}
     */
    public InBoundContent(Publisher<DataChunk> originalPublisher,
            InBoundScope scope, ContentInterceptor.Factory interceptorFactory) {

        Objects.requireNonNull(originalPublisher, "publisher is null!");
        Objects.requireNonNull(scope, "scope is null!");
        this.originalPublisher = originalPublisher;
        this.scope = scope;
        this.readers = scope.readers();
        this.interceptorFactory = interceptorFactory;
    }

    /**
     * Create a new instance.
     *
     * @param originalPublisher the original publisher for this content
     * @param scope in-bound scope
     */
    public InBoundContent(Publisher<DataChunk> originalPublisher,
            InBoundScope scope) {

        this(originalPublisher, scope, /* interceptorFactory */ null);
    }

    /**
     * Copy constructor.
     * @param orig original instance to copy
     */
    public InBoundContent(InBoundContent orig) {
        Objects.requireNonNull(orig, "orig is null!");
        this.originalPublisher = orig.originalPublisher;
        this.scope = orig.scope;
        this.readers = orig.readers;
        this.interceptorFactory = orig.interceptorFactory;
    }

    InBoundScope scope() {
        return scope;
    }

    @Override
    public InBoundContent registerFilter(ContentFilter filter) {
        readers.registerFilter(filter);
        return this;
    }

    @Override
    public InBoundContent registerReader(EntityReader<?> reader) {
        readers.registerReader(reader);
        return this;
    }

    @Override
    public InBoundContent registerStreamReader(EntityStreamReader<?> reader) {
        readers.registerStreamReader(reader);
        return this;
    }

    @Override
    public <T> void registerReader(Class<T> type, Reader<T> reader) {
        readers.registerReader(type, reader);
    }

    @Override
    public <T> void registerReader(Predicate<Class<?>> predicate,
            Reader<T> reader) {

        readers.registerReader(predicate, reader);
    }

    @Override
    public void subscribe(Subscriber<? super DataChunk> subscriber) {
        try {
            readers.applyFilters(originalPublisher, interceptorFactory)
                    .subscribe(subscriber);
        } catch (Exception e) {
            subscriber.onError(new IllegalArgumentException(
                    "Unexpected exception occurred during publishers chaining",
                    e));
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> CompletionStage<T> as(final Class<T> type) {
        return readers.unmarshall(originalPublisher, type, scope,
                /* readersFallback */ null, interceptorFactory);
    }

    @SuppressWarnings("unchecked")
    public <T> CompletionStage<T> as(final GenericType<T> type) {
        return readers.unmarshall(originalPublisher, type, scope,
                /* readersFallback */ null, interceptorFactory);
    }

    @SuppressWarnings("unchecked")
    public <T> Publisher<T> asStream(Class<T> type) {
        return readers.unmarshallStream(originalPublisher, type, scope,
                /* readersFallback */ null, interceptorFactory);
    }

    @SuppressWarnings("unchecked")
    public <T> Publisher<T> asStream(GenericType<T> type) {
        return readers.unmarshallStream(originalPublisher, type, scope,
                /* readersFallback */ null, interceptorFactory);
    }
}
