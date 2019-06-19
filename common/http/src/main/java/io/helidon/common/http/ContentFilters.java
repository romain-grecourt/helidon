package io.helidon.common.http;

import io.helidon.common.reactive.Flow.Publisher;
import io.helidon.common.reactive.Flow.Subscriber;
import java.util.LinkedList;
import java.util.Objects;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * Content filter support.
 */
public abstract class ContentFilters implements ContentFiltersRegistry {

    private final ContentFilters delegate;
    private final LinkedList<ContentFilter> filters;
    private final ReadWriteLock filtersLock;

    /**
     * Create a new content support instance.
     */
    protected ContentFilters() {
        this.delegate = null;
        this.filters = new LinkedList<>();
        this.filtersLock = new ReentrantReadWriteLock();
    }

    /**
     * Create a new delegated content support instance.
     * @param delegate content support delegate
     */
    protected ContentFilters(ContentFilters delegate) {
        this.delegate = delegate;
        this.filters = new LinkedList<>();
        this.filtersLock = new ReentrantReadWriteLock();
    }

    @Override
    public final ContentFilters registerFilter(ContentFilter filter) {
        Objects.requireNonNull(filter, "filter is null!");
        try {
            filtersLock.writeLock().lock();
            filters.addLast(filter);
            return this;
        } finally {
            filtersLock.writeLock().unlock();
        }
    }

    /**
     * Apply the filters by creating a publisher chain of each filter.
     *
     * @param publisher the initial publisher
     * @return the last publisher of the resulting chain
     */
    public final Publisher<DataChunk> applyFilters(
            Publisher<DataChunk> publisher) {

        return applyFilters(publisher, /* interceptor */ null);
    }

    /**
     * Apply the filters by creating a publisher chain of each filter.
     * @param publisher the initial publisher
     * @param contentScope content scope
     * @return the last publisher of the resulting chain
     */
    final Publisher<DataChunk> applyFilters(Publisher<DataChunk> publisher,
            ContentInterceptor.Factory interceptorFactory) {

        Publisher<DataChunk> lastPublisher = doApplyFilters(publisher);
        if (delegate != null) {
            lastPublisher = delegate.doApplyFilters(lastPublisher);
        }
        return new FilteredPublisher(lastPublisher, interceptorFactory);
    }

    private Publisher<DataChunk> doApplyFilters(
            Publisher<DataChunk> publisher) {

        Publisher<DataChunk> lastPublisher = publisher;
        try {
            filtersLock.readLock().lock();
            for (ContentFilter filter : filters) {
                Publisher<DataChunk> p = filter.apply(lastPublisher);
                if (p != null) {
                    lastPublisher = p;
                }
            }
        } finally {
            filtersLock.readLock().unlock();
        }
        return lastPublisher;
    }

    /**
     * The publisher created as a result of {@link #applyFilters}.
     */
    private static final class FilteredPublisher
            implements Publisher<DataChunk> {

        private final Publisher<DataChunk> originalPublisher;
        private final ContentInterceptor.Factory interceptorFactory;

        FilteredPublisher(Publisher<DataChunk> originalPublisher,
                ContentInterceptor.Factory interceptorFactory) {

            this.originalPublisher = originalPublisher;
            this.interceptorFactory = interceptorFactory;
        }

        @Override
        public void subscribe(Subscriber<? super DataChunk> subscriber) {
            ContentInterceptor interceptor = null;
            if (interceptorFactory != null) {
                interceptor = interceptorFactory.create(subscriber);
            }
            if (interceptor != null) {
                originalPublisher.subscribe(interceptor);
            } else {
                originalPublisher.subscribe(subscriber);
            }
        }
    }
}
