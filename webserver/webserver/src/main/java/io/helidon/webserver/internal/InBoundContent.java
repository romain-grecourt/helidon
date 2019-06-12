package io.helidon.webserver.internal;

import io.helidon.common.GenericType;
import io.helidon.common.http.DataChunk;
import io.helidon.common.http.Filter;
import io.helidon.common.http.Reader;
import io.helidon.common.http.StreamReader;
import io.helidon.common.reactive.Flow.Publisher;
import io.helidon.common.reactive.Flow.Subscriber;
import java.util.Objects;
import java.util.concurrent.CompletionStage;
import java.util.function.Predicate;

/**
 * Implementation of {@link io.helidon.common.http.Content}.
 */
public final class InBoundContent implements io.helidon.common.http.Content {

    private final Publisher<DataChunk> originalPublisher;
    private final InBoundMediaSupport mediaSupport;
    private final SubscriberInterceptor.Factory interceptorFactory;

    /**
     * Create a new instance.
     * @param originalPublisher the original publisher for this content
     * @param mediaSupport in-bound media support
     * @param interceptorFactory interceptor factory
     */
    public InBoundContent(Publisher<DataChunk> originalPublisher,
            InBoundMediaSupport mediaSupport,
            SubscriberInterceptor.Factory interceptorFactory) {

        Objects.requireNonNull(originalPublisher, "publisher is null!");
        Objects.requireNonNull(mediaSupport, "mediaSupport is null!");
        this.originalPublisher = originalPublisher;
        this.mediaSupport = mediaSupport;
        this.interceptorFactory = interceptorFactory;
    }

    /**
     * Copy constructor.
     * @param orig original instance to copy
     */
    public InBoundContent(InBoundContent orig) {
        Objects.requireNonNull(orig, "orig is null!");
        this.originalPublisher = orig.originalPublisher;
        this.mediaSupport = orig.mediaSupport;
        this.interceptorFactory = orig.interceptorFactory;
    }

    /**
     * Get the in-bound media support.
     * @return InBoundMediaSupport
     */
    public InBoundMediaSupport mediaSupport() {
        return mediaSupport;
    }

    @Override
    public void registerFilter(Filter filter) {
        mediaSupport.registerFilter(filter);
    }

    @Override
    public <T> void registerStreamReader(Class<T> clazz, StreamReader<T> reader) {
        mediaSupport.registerStreamReader(clazz, reader);
    }

    @Override
    public <T> void registerReader(Class<T> type, Reader<T> reader) {
        mediaSupport.registerReader(type, reader);
    }

    @Override
    public <T> void registerReader(Predicate<Class<T>> predicate, Reader<T> reader) {
        mediaSupport.registerReader(predicate, reader);
    }

    @Override
    public <T> CompletionStage<T> as(final Class<T> type) {
        return mediaSupport.unmarshall(type, originalPublisher,
                interceptorFactory);
    }

    @Override
    public void subscribe(Subscriber<? super DataChunk> subscriber) {
        try {
            mediaSupport.applyFilters(originalPublisher, interceptorFactory)
                    .subscribe(subscriber);
        } catch (Exception e) {
            subscriber.onError(new IllegalArgumentException(
                    "Unexpected exception occurred during publishers chaining",
                    e));
        }
    }

    @Override
    public <T> Publisher<T> asPublisherOf(Class<T> type) {
        return mediaSupport.unmarshallStream(type, originalPublisher,
                interceptorFactory);
    }

    @Override
    public <T> Publisher<T> asPublisherOf(GenericType<T> type) {
        throw new UnsupportedOperationException("not implemented yet");
    }
}
