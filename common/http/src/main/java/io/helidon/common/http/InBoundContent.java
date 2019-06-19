package io.helidon.common.http;

import io.helidon.common.GenericType;
import io.helidon.common.reactive.FailedPublisher;
import io.helidon.common.reactive.Flow.Publisher;
import io.helidon.common.reactive.Flow.Subscriber;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;

/**
 * In-bound reactive payload that can be converted to entities.
 */
@SuppressWarnings("deprecation")
public final class InBoundContent
        implements HttpContent, EntityReadersRegistry, Content {

    private final Publisher<DataChunk> originalPublisher;
    private final ContentInterceptor.Factory interceptorFactory;
    final InBoundScope scope;

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
        this.interceptorFactory = orig.interceptorFactory;
    }

    @Override
    public InBoundContent registerFilter(ContentFilter filter) {
        scope.readers.registerFilter(filter);
        return this;
    }

    @Override
    public InBoundContent registerReader(EntityReader<?> reader) {
        scope.readers.registerReader(reader);
        return this;
    }

    @Override
    public InBoundContent registerStreamReader(EntityStreamReader<?> reader) {
        scope.readers.registerStreamReader(reader);
        return this;
    }

    @Override
    public void subscribe(Subscriber<? super DataChunk> subscriber) {
        try {
            scope.readers.applyFilters(originalPublisher, interceptorFactory)
                    .subscribe(subscriber);
        } catch (Exception e) {
            subscriber.onError(new IllegalArgumentException(
                    "Unexpected exception occurred during publishers chaining",
                    e));
        }
    }

    @Override
    public <T> CompletionStage<T> as(final Class<T> type) {
        try {
            EntityReader<T> reader = scope.readers
                    .selectReader(type, scope, /* delegate */ null);
            if (reader == null) {
                throw new IllegalArgumentException(
                        "No reader found for class: " + type);
            }
            Publisher<DataChunk> pub = filteredPublisher(type.getTypeName());
            return (CompletionStage<T>) reader.readEntity(pub, type, scope);
        } catch (IllegalArgumentException e) {
            CompletableFuture failedFuture = new CompletableFuture<>();
            failedFuture.completeExceptionally(e);
            return failedFuture;
        } catch (Exception e) {
            CompletableFuture failedFuture = new CompletableFuture<>();
            failedFuture.completeExceptionally(
                    new IllegalArgumentException("Transformation failed!", e));
            return failedFuture;
        }
    }

    public <T> CompletionStage<T> as(final GenericType<T> type) {
        try {
            EntityReader<T> reader = scope.readers.selectReader(type, scope,
                    /* delegate */ null);
            if (reader == null) {
                throw new IllegalArgumentException(
                        "No reader found for class: " + type);
            }
            Publisher<DataChunk> pub = filteredPublisher(type.getTypeName());
            return (CompletionStage<T>) reader.readEntity(pub, type, scope);
        } catch (IllegalArgumentException e) {
            CompletableFuture failedFuture = new CompletableFuture<>();
            failedFuture.completeExceptionally(e);
            return failedFuture;
        } catch (Exception e) {
            CompletableFuture failedFuture = new CompletableFuture<>();
            failedFuture.completeExceptionally(
                    new IllegalArgumentException("Transformation failed!", e));
            return failedFuture;
        }
    }

    public <T> Publisher<T> asStream(Class<T> type) {
        try {
            EntityStreamReader<T> streamReader = scope.readers
                    .selectStreamReader(type, scope, /* delegate */ null);
            if (streamReader == null) {
                throw new IllegalArgumentException(
                        "No stream reader found for class: " + type);
            }
            Publisher<DataChunk> pub = filteredPublisher(type.getTypeName());
            return (Publisher<T>) streamReader
                    .readEntityStream(pub, type,scope);
        } catch (IllegalArgumentException e) {
            return new FailedPublisher<>(e);
        } catch (Exception e) {
            return new FailedPublisher<>(new IllegalArgumentException(
                    "Transformation failed!", e));
        }
    }

    public <T> Publisher<T> asStream(GenericType<T> type) {
        try {
            EntityStreamReader<T> streamReader = scope.readers
                    .selectStreamReader(type, scope, /* delegate */ null);
            if (streamReader == null) {
                throw new IllegalArgumentException(
                        "No stream reader found for class: " + type);
            }
            Publisher<DataChunk> pub = filteredPublisher(type.getTypeName());
            return (Publisher<T>) streamReader
                    .readEntityStream(pub, type, scope);
        } catch (IllegalArgumentException e) {
            return new FailedPublisher<>(e);
        } catch (Exception e) {
            return new FailedPublisher<>(new IllegalArgumentException(
                    "Transformation failed!", e));
        }
    }

    private Publisher<DataChunk> filteredPublisher(String type) {
        ContentInterceptor.Factory ifc = null;
        if (interceptorFactory != null) {
            ifc = interceptorFactory.forType(type);
        }
        return scope.readers.applyFilters(originalPublisher, ifc);
    }
}
