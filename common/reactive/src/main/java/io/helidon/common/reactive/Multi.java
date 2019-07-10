package io.helidon.common.reactive;

import io.helidon.common.CollectionsHelper;
import io.helidon.common.reactive.Flow.Publisher;
import io.helidon.common.reactive.Flow.Subscriber;
import io.helidon.common.reactive.Flow.Subscription;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import java.util.Objects;
import java.util.Queue;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

/**
 * Multiple items publisher.
 * @param <T> item type
 */
public abstract class Multi<T> implements Publisher<T> {

    public final void subscribe(Consumer<? super T> consumer) {
        this.subscribe(new FunctionalSubscriber<>(consumer, null, null, null));
    }

    public final void subscribe(Consumer<? super T> consumer,
            Consumer<? super Throwable> errorConsumer) {

        this.subscribe(new FunctionalSubscriber<>(consumer, errorConsumer,
                null, null));
    }

    public final void subscribe(Consumer<? super T> consumer,
            Consumer<? super Throwable> errorConsumer,
            Runnable completeConsumer) {

        this.subscribe(new FunctionalSubscriber<>(consumer, errorConsumer,
                completeConsumer, null));
    }

    public final void subscribe(Consumer<? super T> consumer,
            Consumer<? super Throwable> errorConsumer,
            Runnable completeConsumer,
            Consumer<? super Subscription> subscriptionConsumer) {

        this.subscribe(new FunctionalSubscriber<>(consumer, errorConsumer,
                completeConsumer, subscriptionConsumer));
    }

    public final <V> Multi<V> map(Function<? super T, ? extends V> mapper) {
        return new MultiMapper<>(this, mapper);
    }

    public final Mono<List<T>> collectList() {
        return new MonoListCollector<>(this, listSupplier());
    }

    public final <E> Mono<E> collect(Supplier<E> containerSupplier,
            BiConsumer<E, ? super T> collector) {

        return new MonoCollector<>(this, containerSupplier, collector);
    }

    public static <T> Multi<T> from(Publisher<? extends T> source) {
        return new MultiFromPublisher<>(source);
    }

    public static <T> Multi<T> just(Collection<T> items) {
        return new MultiFromItems<>(items);
    }

    @SafeVarargs
    public static <T> Multi<T> just(T... items) {
        return new MultiFromItems<>(CollectionsHelper.listOf(items));
    }

    public static <T> Multi<T> error(Throwable error) {
        return new ManyError<>(error);
    }

    public static <T> Multi<T> empty() {
        return ManyEmpty.<T>instance();
    }

    private static final class ManyEmpty extends Multi<Object> {

        private static final Publisher<Object> INSTANCE = new ManyEmpty();

        ManyEmpty() {
        }

        @Override
        public void subscribe(Subscriber<? super Object> subscriber) {
            subscriber.onSubscribe(EmptySubscription.INSTANCE);
            subscriber.onComplete();
        }

        @SuppressWarnings("unchecked")
        private static <T> Multi<T> instance() {
            return (Multi<T>) INSTANCE;
        }
    }

    private static final class ManyError<T> extends Multi<T> {

        private final Throwable error;

        ManyError(Throwable error) {
            this.error = Objects.requireNonNull(error, "error");
        }

        @Override
        public void subscribe(Subscriber<? super T> subscriber) {
            subscriber.onSubscribe(EmptySubscription.INSTANCE);
            subscriber.onError(error);
        }
    }

    private static final class MultiFromPublisher<T> extends Multi<T> {

        private final Publisher<? extends T> source;

        private MultiFromPublisher(Publisher<? extends T> source) {
            Objects.requireNonNull(source, "source cannot be null!");
            this.source = source;
        }

        @Override
        public void subscribe(Subscriber<? super T> subscriber) {
            source.subscribe(subscriber);
        }
    }

    private static final class MultiFromItems<T> extends Multi<T> {

        private final Queue<T> queue;
        private final SingleSubscriberHolder<T> subscriber;
        private final RequestedCounter requested;
        private final AtomicBoolean publishing;

        MultiFromItems(Collection<T> items) {
            queue = new LinkedList<>(items);
            subscriber = new SingleSubscriberHolder<>();
            requested = new RequestedCounter();
            publishing = new AtomicBoolean(false);
        }

        @Override
        public void subscribe(Subscriber<? super T> s) {
            if (subscriber.register(s)) {

                // prevent onNext from inside of onSubscribe
                publishing.set(true);

                try {
                    s.onSubscribe(new Flow.Subscription() {
                        @Override
                        public void request(long n) {
                            requested.increment(n, t -> tryComplete(t));
                            tryPublish();
                        }

                        @Override
                        public void cancel() {
                            subscriber.cancel();
                        }
                    });
                } finally {
                    publishing.set(false);
                }

                // give onNext a chance in case request has been invoked in
                // onSubscribe
                tryPublish();
            }
        }

        private void tryPublish() {
            boolean immediateRetry = true;
            while (immediateRetry) {
                immediateRetry = false;

                // Publish, if can
                if (!subscriber.isClosed()
                        && requested.get() > 0
                        && publishing.compareAndSet(false, true)) {
                    try {
                        Subscriber<? super T> sub = this.subscriber.get();
                        while (!subscriber.isClosed()
                                && requested.tryDecrement()
                                && !queue.isEmpty()) {
                            T item = queue.poll();
                            if (item != null) {
                                sub.onNext(item);
                            }
                        }
                        if (queue.isEmpty()) {
                            tryComplete();
                        }
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        tryComplete(e);
                    } catch (Exception e) {
                        tryComplete(e);
                    } finally {
                        // give a chance to some other thread to publish
                        publishing.set(false);
                    }
                }
            }
        }

        private void tryComplete() {
            subscriber.close(Subscriber::onComplete);
        }

        private void tryComplete(Throwable t) {
            subscriber.close(sub -> sub.onError(t));
        }
    }

    private static final
            class MonoListCollector<T, C extends Collection<? super T>>
            extends Mono<C> {

        private final Multi<? extends T> source;
        private final Supplier<C> collectionSupplier;

        MonoListCollector(Multi<? extends T> source,
                Supplier<C> collectionSupplier) {

            this.source = source;
            this.collectionSupplier = collectionSupplier;
        }

        @Override
        public void subscribe(Subscriber<? super C> actual) {
            C collection;

            try {
                collection = Objects.requireNonNull(collectionSupplier.get(),
                        "The collectionSupplier returned a null collection");
            } catch (Throwable ex) {
                actual.onError(ex);
                return;
            }

            source.subscribe(new CollectorListSubscriber<>(actual, collection));
        }
    }

    private static final
            class CollectorListSubscriber<T, U extends Collection<? super T>>
            implements Subscriber<T>, Subscription {

        private final Subscriber<? super U> subscriber;
        private final AtomicBoolean requested;
        private final U collection;
        private boolean done;
        private Subscription subscription;

        CollectorListSubscriber(Subscriber<? super U> subscriber, U collection) {
            this.subscriber = subscriber;
            this.collection = collection;
            this.requested = new AtomicBoolean(false);
        }

        @Override
        public void onSubscribe(Subscription s) {
            if (this.subscription == null) {
                this.subscription = s;
                subscriber.onSubscribe(this);
                s.request(Long.MAX_VALUE);
            }
        }

        @Override
        public void onNext(T t) {
            if (!done) {
                collection.add(t);
            }
        }

        @Override
        public void onError(Throwable t) {
            if (!done) {
                subscriber.onError(t);
            }
        }

        @Override
        public void onComplete() {
            if (!done) {
                done = true;
                if (requested.get()) {
                    subscriber.onNext(collection);
                    subscriber.onComplete();
                }
            }
        }

        @Override
        public void cancel() {
            subscription.cancel();
        }

        @Override
        public void request(long n) {
            if (n > 0) {
                if (requested.compareAndSet(false, true) && done) {
                    subscriber.onNext(collection);
                    subscriber.onComplete();
                }
            }
        }
    }

    private static final class MonoCollector<T, U> extends Mono<U> {

        private final Multi<? extends T> source;
        private final Supplier<U> supplier;
        private final BiConsumer<? super U, ? super T> action;

        MonoCollector(Multi<? extends T> source, Supplier<U> supplier,
                BiConsumer<? super U, ? super T> action) {

            this.supplier = Objects.requireNonNull(supplier, "supplier cannot be null!");
            this.action = Objects.requireNonNull(action, "action cannot be null!");
            this.source = source;
        }

        @Override
        public void subscribe(Subscriber<? super U> subscriber) {
            U container;
            try {
                container = Objects.requireNonNull(supplier.get(),
                        "supplier returned a null container!");
            } catch (Throwable ex) {
                subscriber.onError(ex);
                return;
            }
            source.subscribe(new CollectorSubscriber<>(subscriber, action,
                    container));
        }
    }

    private static final class CollectorSubscriber<T, R>
            implements Subscriber<T>, Subscription {

        private final BiConsumer<? super R, ? super T> action;
        private final Subscriber<? super R> subscriber;
        private final R value;
        private final AtomicBoolean requested;
        private Subscription subscription;
        private volatile boolean done;

        CollectorSubscriber(Subscriber<? super R> actual,
                BiConsumer<? super R, ? super T> action, R container) {

            this.subscriber = actual;
            this.action = action;
            this.value = container;
            this.requested = new AtomicBoolean(false);
        }

        @Override
        public void request(long n) {
            if (n > 0) {
                if (requested.compareAndSet(false, true)) {
                    if (done) {
                        subscriber.onNext(value);
                        subscriber.onComplete();
                    }
                }
            }
        }

        @Override
        public void cancel() {
            subscription.cancel();
        }

        @Override
        public void onSubscribe(Subscription subscription) {
            Objects.requireNonNull(subscription, "subscription cannot be null!");
            if (this.subscription != null) {
                this.subscription.cancel();
            } else {
                this.subscription = subscription;
                subscriber.onSubscribe(this);
                subscription.request(Long.MAX_VALUE);
            }
        }

        @Override
        public void onNext(T t) {
            if (!done) {
                try {
                    action.accept(value, t);
                } catch (Throwable e) {
                    onError(e);
                }
            }
        }

        @Override
        public void onError(Throwable t) {
            if (!done) {
                done = true;
                subscriber.onError(t);
            }
        }

        @Override
        public void onComplete() {
            if (!done) {
                done = true;
                if (requested.get()) {
                    subscriber.onNext(value);
                    subscriber.onComplete();
                }
            }
        }
    }

    @SuppressWarnings("rawtypes")
    private static final Supplier LIST_SUPPLIER = ArrayList::new;

    @SuppressWarnings("unchecked")
    private static <U> Supplier<List<U>> listSupplier() {
        return LIST_SUPPLIER;
    }

    private static final class MultiMapper<T, R> extends Multi<R> {

        private final Multi<? extends T> source;
        private final Function<? super T, ? extends R> mapper;

        MultiMapper(Multi<? extends T> source,
                Function<? super T, ? extends R> mapper) {

            this.source = source;
            this.mapper = Objects.requireNonNull(mapper, "mapper");
        }

        @Override
        public void subscribe(Subscriber<? super R> actual) {
            source.subscribe(new MapperSubscriber<>(actual, mapper));
        }
    }

    private static final class MapperSubscriber<T, R>
            implements Subscriber<T>, Subscription {

        private final Subscriber<? super R> actual;
        private final Function<? super T, ? extends R> mapper;
        private boolean done;
        private Subscription subscription;

        MapperSubscriber(Subscriber<? super R> actual,
                Function<? super T, ? extends R> mapper) {
            this.actual = actual;
            this.mapper = mapper;
        }

        @Override
        public void onSubscribe(Subscription s) {
            if (this.subscription == null) {
                this.subscription = s;
                actual.onSubscribe(this);
            }
        }

        @Override
        public void onNext(T t) {
            if (!done) {
                R v;
                try {
                    v = Objects.requireNonNull(mapper.apply(t),
                            "The mapper returned a null value.");
                } catch (Throwable ex) {
                    onError(ex);
                    return;
                }
                actual.onNext(v);
            }
        }

        @Override
        public void onError(Throwable t) {
            if (!done) {
                done = true;
                actual.onError(t);
            }
        }

        @Override
        public void onComplete() {
            if (!done) {
                done = true;
                actual.onComplete();
            }
        }

        @Override
        public void request(long n) {
            subscription.request(n);
        }

        @Override
        public void cancel() {
            subscription.cancel();
        }
    }

    private static final class FunctionalSubscriber<T>
            implements Subscriber<T> {

        private final Consumer<? super T> consumer;
        private final Consumer<? super Throwable> errorConsumer;
        private final Runnable completeConsumer;
        private final Consumer<? super Subscription> subscriptionConsumer;
        private Subscription subscription;

        FunctionalSubscriber(Consumer<? super T> consumer,
                Consumer<? super Throwable> errorConsumer,
                Runnable completeConsumer,
                Consumer<? super Subscription> subscriptionConsumer) {

            this.consumer = consumer;
            this.errorConsumer = errorConsumer;
            this.completeConsumer = completeConsumer;
            this.subscriptionConsumer = subscriptionConsumer;
        }

        @Override
        public final void onSubscribe(Subscription subscription) {
            if (this.subscription == null) {
                this.subscription = subscription;
                if (subscriptionConsumer != null) {
                    try {
                        subscriptionConsumer.accept(subscription);
                    } catch (Throwable ex) {
                        subscription.cancel();
                        onError(ex);
                    }
                } else {
                    subscription.request(Long.MAX_VALUE);
                }
            }
        }

        @Override
        public final void onComplete() {
            if (completeConsumer != null) {
                try {
                    completeConsumer.run();
                } catch (Throwable t) {
                    onError(t);
                }
            }
        }

        @Override
        public final void onError(Throwable ex) {
            if (errorConsumer != null) {
                errorConsumer.accept(ex);
            }
        }

        @Override
        public final void onNext(T x) {
            try {
                if (consumer != null) {
                    consumer.accept(x);
                }
            } catch (Throwable t) {
                this.subscription.cancel();
                onError(t);
            }
        }
    }
}
