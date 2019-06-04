package io.helidon.webserver.internal;

import io.helidon.common.http.DataChunk;
import io.helidon.common.http.Filter;
import io.helidon.common.reactive.Flow.Publisher;
import java.util.LinkedList;
import java.util.Objects;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.function.Predicate;

/**
 * Base class for media support implementations.
 */
public abstract class MediaSupport {

    /**
     * The registered filters.
     */
    private final Registry<Filter> filters;

    /**
     * Create a new instance.
     */
    protected MediaSupport() {
        this.filters = new Registry<>(/* addFirst */ false);
    }

    /**
     * Register a new filter.
     * @param filter filter to register
     */
    public final void registerFilter(Filter filter) {
        filters.register(filter);
    }

    /**
     * Apply the filters by creating a publisher chain of each filter.
     * @param publisher the initial publisher
     * @return the last publisher of the resulting chain
     */
    public final Publisher<DataChunk> applyFilters(Publisher<DataChunk> publisher) {
        Publisher<DataChunk> lastPublisher = publisher;
        try {
            filters.lock.readLock().lock();
            for (Filter filter : filters.registry) {
                Publisher<DataChunk> p = filter.apply(lastPublisher);
                if (p != null) {
                    lastPublisher = p;
                }
            }
        } finally {
            filters.lock.readLock().unlock();
        }
        return lastPublisher;
    }

    /**
     * Media support registry.
     * @param <T> type of the registered items
     */
    protected static class Registry<T> {

        protected final LinkedList<T> registry;
        protected final ReadWriteLock lock;
        private final boolean addFirst;

        /**
         * Create a new empty registry.
         * @param addFirst if {@code true} the item is added first in the
         * registry, otherwise it is added last
         */
        protected Registry(boolean addFirst) {
            this.addFirst = addFirst;
            this.registry = new LinkedList<>();
            this.lock = new ReentrantReadWriteLock();
        }

        /**
         * Create a new predicate registry with initial items.
         * @param init initial items for the registry
         * @param addFirst if {@code true} the item is added first in the
         * registry, otherwise it is added last
         */
        protected Registry(boolean addFirst, LinkedList<T> init) {
            this(addFirst);
            registry.addAll(init);
        }

        /**
         * Register a new item.
         *
         * @param item item to register
         */
        protected void register(T item) {
            Objects.requireNonNull(item, "Register item is null!");
            try {
                lock.writeLock().lock();
                if (addFirst) {
                    registry.addFirst(item);
                } else {
                    registry.addLast(item);
                }
            } finally {
                lock.writeLock().unlock();
            }
        }
    }

    /**
     * Media support registry with item 
     * @param <T> registered item type
     * @param <U> predicate type
     */
    protected static final class PredicateRegistry<T extends Predicate<U>, U>
            extends Registry<T> {

        /**
         * Create a new empty predicate registry.
         */
        protected PredicateRegistry() {
            super(/* addFirst */ true);
        }

        /**
         * Create a new predicate registry with initial items.
         * @param init initial items for the registry
         */
        protected PredicateRegistry(LinkedList<T> init) {
            super(/* addFirst */ true, init);
        }

        /**
         * Select a registry item with the first predicate that matches the
         * input.
         *
         * @param input predicate input
         * @return selected item or {@code null} if no predicate matched the
         * input
         */
        protected T select(U input) {
            try {
                lock.readLock().lock();
                for (T item : registry) {
                    if (item.test(input)) {
                        return item;
                    }
                }
                return null;
            } finally {
                lock.readLock().unlock();
            }
        }
    }
}
