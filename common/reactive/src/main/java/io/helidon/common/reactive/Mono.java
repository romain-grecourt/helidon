package io.helidon.common.reactive;

import io.helidon.common.reactive.Flow.Processor;
import io.helidon.common.reactive.Flow.Publisher;
import io.helidon.common.reactive.Flow.Subscriber;
import io.helidon.common.reactive.Flow.Subscription;
import java.time.Duration;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Function;

/**
 * Single item publisher.
 * @param <T> item type
 */
public abstract class Mono<T> implements Publisher<T> {

    public T block() {
        BlockingMonoSubscriber<T> subscriber = new BlockingMonoSubscriber<>();
        this.subscribe(subscriber);
        return subscriber.blockingGet();
    }

    public T block(Duration timeout) {
        BlockingMonoSubscriber<T> subscriber = new BlockingMonoSubscriber<>();
        this.subscribe(subscriber);
        return subscriber.blockingGet(timeout.toMillis(), TimeUnit.MILLISECONDS);
    }

    public final <R> Mono<R> flatMap(Function<T, ? extends Mono<R>> mapper) {
        return new MonoFlatMap<>(this, mapper);
    }

    public final <R> Publisher<R> flatMapMany(
            Function<T, ? extends Publisher<R>> mapper) {

        ToManyProcessor<T, R> processor = new ToManyProcessor<>(mapper);
        this.subscribe(processor);
        return processor;
    }

    public final CompletableFuture<T> toFuture() {
        MonoToCompletableFuture<T> subscriber = new MonoToCompletableFuture<>();
        this.subscribe(subscriber);
        return subscriber;
    }

    public static <T> Mono<T> fromFuture(CompletionStage<? extends T> future) {
        return new MonoCompletionStage<>(future);
    }

    @SuppressWarnings("unchecked")
    public static <T> Mono<T> from(Publisher<? extends T> source) {
        if (source instanceof Mono) {
            return (Mono<T>) source;
        }
        return new MonoNext<>(source);
    }

    public static <T> Mono<T> just(T data) {
        return new MonoJust<>(data);
    }

    public static <T> Mono<T> error(Throwable error) {
        return new MonoError<>(error);
    }

    public static <T> Mono<T> empty() {
        return MonoEmpty.<T>instance();
    }

    public static <T> Mono<T> never() {
        return MonoNever.<T>instance();
    }

    private static final class MonoJust<T>  extends Mono<T> {

        final T value;

        MonoJust(T value) {
            this.value = Objects.requireNonNull(value, "value");
        }

        @Override
        public void subscribe(Subscriber<? super T> subscriber) {
            subscriber.onSubscribe(new MonoSubscription<>(value, subscriber));
        }
    }
    private static final class MonoSubscription<T> implements Subscription {

        private final T value;
        private final Subscriber<? super T> subscriber;
        private final AtomicBoolean delivered;
        private final AtomicBoolean canceled;

        MonoSubscription(T value, Subscriber<? super T> subscriber) {
            this.value = value;
            this.subscriber = subscriber;
            this.delivered = new AtomicBoolean(false);
            this.canceled = new AtomicBoolean(false);
        }

        @Override
        public void request(long n) {
            if (n >= 0 && !canceled.get()) {
                if (delivered.compareAndSet(false, true)) {
                    subscriber.onNext(value);
                    subscriber.onComplete();
                }
            }
        }

        @Override
        public void cancel() {
            canceled.set(true);
        }
    }

    private static final class MonoEmpty extends Mono<Object> {

        private static final Publisher<Object> INSTANCE = new MonoEmpty();

        MonoEmpty() {
        }

        @Override
        public void subscribe(Subscriber<? super Object> subscriber) {
            subscriber.onSubscribe(EmptySubscription.INSTANCE);
            subscriber.onComplete();
        }

        @SuppressWarnings("unchecked")
        private static <T> Mono<T> instance() {
            return (Mono<T>) INSTANCE;
        }
    }

    private static final class MonoError<T> extends Mono<T> {

        private final Throwable error;

        MonoError(Throwable error) {
            this.error = Objects.requireNonNull(error, "error");
        }

        @Override
        public void subscribe(Subscriber<? super T> subscriber) {
            subscriber.onSubscribe(EmptySubscription.INSTANCE);
            subscriber.onError(error);
        }
    }

    private static final class MonoToCompletableFuture<T>
            extends CompletableFuture<T> 
            implements Subscriber<T> {

        private final AtomicReference<Subscription> ref = new AtomicReference<>();

        @Override
        public boolean cancel(boolean mayInterruptIfRunning) {
            boolean cancelled = super.cancel(mayInterruptIfRunning);
            if (cancelled) {
                Subscription s = ref.getAndSet(null);
                if (s != null) {
                    s.cancel();
                }
            }
            return cancelled;
        }

        @Override
        public void onSubscribe(Subscription next) {
            Subscription current = ref.getAndSet(next);
            Objects.requireNonNull(next, "Subscription cannot be null");
            if (current != null) {
                next.cancel();
                current.cancel();
            } else {
                next.request(Long.MAX_VALUE);
            }
        }

        @Override
        public void onNext(T t) {
            Subscription s = ref.getAndSet(null);
            if (s != null) {
                complete(t);
                s.cancel();
            }
        }

        @Override
        public void onError(Throwable t) {
            if (ref.getAndSet(null) != null) {
                completeExceptionally(t);
            }
        }

        @Override
        public void onComplete() {
            if (ref.getAndSet(null) != null) {
                complete(null);
            }
        }
    }

    private static final class ToManyProcessor<T, U>
            implements Processor<T, U> {

        private final Function<T, ? extends Publisher<U>> mapper;
        private Throwable error;
        private Publisher<U> delegate;
        private Subscription subscription;
        private Subscriber<? super U> subscriber;
        private volatile boolean subcribed;

        ToManyProcessor(Function<T, ? extends Publisher<U>> mapper) {
            this.mapper = mapper;
        }

        @Override
        public void onNext(T item) {
            if (delegate == null) {
                delegate = mapper.apply(item);
                doSusbcribe();
            }
            subscription.cancel();
        }

        @Override
        public void onError(Throwable ex) {
            if (delegate == null) {
                error = ex;
                delegate = Mono.<U>error(error);
                doSusbcribe();
            }
        }

        @Override
        public final void onSubscribe(Subscription s) {
            this.subscription = s;
            s.request(1);
        }

        @Override
        public final void onComplete() {
        }

        private void doSusbcribe() {
            if (!subcribed && subscriber != null) {
                delegate.subscribe(subscriber);
                subcribed = true;
            }
        }

        @Override
        public final void subscribe(Subscriber<? super U> subscriber) {
            this.subscriber = subscriber;
            if (delegate != null) {
                doSusbcribe();
            }
        }
    }

    private static final class MonoNext<T> extends Mono<T> {

        private final Publisher<? extends T> source;

        MonoNext(Publisher<? extends T> source) {
            this.source = Objects.requireNonNull(source, "source cannot be null!");
        }

        @Override
        public void subscribe(Subscriber<? super T> actual) {
            source.subscribe(new NextSubscriber<>(actual));
        }
    }

    private static final class NextSubscriber<T>
            implements Subscriber<T>, Subscription {

        private final Subscriber<? super T> actual;
        private final AtomicBoolean requested;
        private Subscription s;
        private boolean done;

        NextSubscriber(Subscriber<? super T> s) {
            requested = new AtomicBoolean(false);
            actual = s;
        }

        @Override
        public void onSubscribe(Subscription s) {
            Objects.requireNonNull(s, "Subscription cannot be null");
            if (this.s != null) {
                s.cancel();
                this.s.cancel();
            } else {
                this.s = s;
                actual.onSubscribe(this);
            }
        }

        @Override
        public void onNext(T t) {
            if (done) {
                return;
            }

            s.cancel();
            actual.onNext(t);
            onComplete();
        }

        @Override
        public void onError(Throwable t) {
            if (done) {
                return;
            }
            done = true;
            actual.onError(t);
        }

        @Override
        public void onComplete() {
            if (done) {
                return;
            }
            done = true;
            actual.onComplete();
        }

        @Override
        public void request(long n) {
            if (requested.compareAndSet(false, true)) {
                s.request(Long.MAX_VALUE);
            }
        }

        @Override
        public void cancel() {
            s.cancel();
        }
    }

    private static final class MonoCompletionStage<T> extends Mono<T> {

        private final CompletionStage<? extends T> future;
        private Subscriber<? super T> subscriber;
        private volatile boolean requested;

        MonoCompletionStage(CompletionStage<? extends T> future) {
            this.future = Objects.requireNonNull(future, "future");
        }

        private void submit(T item) {
            subscriber.onNext(item);
            subscriber.onComplete();
        }

        private <U extends T> U raiseError(Throwable error) {
            subscriber.onError(error);
            return null;
        }

        @Override
        public void subscribe(Subscriber<? super T> subscriber) {
            if (this.subscriber != null) {
                throw new IllegalStateException("Already subscribed to");
            }
            this.subscriber = subscriber;
            subscriber.onSubscribe(new Subscription() {
                @Override
                public void request(long n) {
                    if (n > 0 && !requested) {
                        future.exceptionally(MonoCompletionStage.this::raiseError);
                        future.thenAccept(MonoCompletionStage.this::submit);
                        requested = true;
                    }
                }

                @Override
                public void cancel() {
                }
            });
        }
    }

    private final class MonoFlatMap<T, U> extends Mono<U> {

        private final Mono<? extends T> source;
        private final Function<? super T, ? extends Mono<? extends U>> mapper;

        MonoFlatMap(Mono<? extends T> source,
                Function<? super T, ? extends Mono<? extends U>> mapper) {
            this.source = Objects.requireNonNull(source,"source cannot be null!");
            this.mapper = Objects.requireNonNull(mapper, "mapper cannot be null!");
        }

        @Override
        public void subscribe(Subscriber<? super U> actual) {
            FlatMapMain<T, U> manager = new FlatMapMain<>(actual, mapper);
            actual.onSubscribe(manager);
            source.subscribe(manager);
        }
    }

    private static final class FlatMapMain<T, U>
            implements Subscriber<T>, Subscription {

        private final Function<? super T, ? extends Mono<? extends U>> mapper;
        private final Subscriber<? super U> subscriber;
        private final FlatMapInner<U> second;
        private final AtomicBoolean requested;
        private boolean done;

        FlatMapMain(Subscriber<? super U> subscriber,
                Function<? super T, ? extends Mono<? extends U>> mapper) {

            this.subscriber = subscriber;
            this.mapper = mapper;
            this.second = new FlatMapInner<>(this);
            this.requested = new AtomicBoolean(false);
        }

        @Override
        public void request(long n) {
            if (n > 0) {
                if (requested.compareAndSet(false, true)) {
                    if (done) {
                        // TODO
                    }
                }
            }
        }

        @Override
        public void cancel() {
            // not canceling the upstream subscription
            // as this can mean closing the connection...
            second.cancel();
        }

        @Override
        public void onSubscribe(Subscription subscription) {
            subscription.request(Long.MAX_VALUE);
        }

        @Override
        public void onNext(T t) {
            if (done) {
                return;
            }
            done = true;

            Mono<? extends U> m;

            try {
                m = Objects.requireNonNull(mapper.apply(t),
                        "The mapper returned a null Mono");
            } catch (Throwable ex) {
                subscriber.onError(ex);
                return;
            }
            try {
                m.subscribe(second);
            } catch (Throwable ex) {
                subscriber.onError(ex);
            }
        }

        @Override
        public void onError(Throwable ex) {
            if (!done) {
                done = true;
                subscriber.onError(ex);
            }
        }

        @Override
        public void onComplete() {
            if (!done) {
                done = true;
                subscriber.onComplete();
            }
        }

        void complete(U item) {
            subscriber.onNext(item);
            onComplete();
        }

        void secondError(Throwable ex) {
            subscriber.onError(ex);
        }

        void secondComplete() {
            subscriber.onComplete();
        }
    }

    static final class FlatMapInner<T> implements Subscriber<T> {

        private final FlatMapMain<?, T> parent;
        private Subscription subscription;
        private boolean done;

        FlatMapInner(FlatMapMain<?, T> parent) {
            this.parent = parent;
        }

        @Override
        public void onSubscribe(Subscription subscription) {
            if (this.subscription == null) {
                this.subscription = subscription;
                subscription.request(Long.MAX_VALUE);
            }
        }

        @Override
        public void onNext(T item) {
            if (!done) {
                done = true;
                this.parent.complete(item);
            }
        }

        @Override
        public void onError(Throwable t) {
            if (!done) {
                done = true;
                this.parent.secondError(t);
            }
        }

        @Override
        public void onComplete() {
            if (done) {
                return;
            }
            done = true;
            this.parent.secondComplete();
        }

        void cancel() {
            this.subscription.cancel();
        }
    }

    private static final class BlockingMonoSubscriber<T>
            extends CountDownLatch
            implements Subscriber<T> {

        private T value;
        private Throwable error;
        private Subscription s;
        private volatile boolean cancelled;

        BlockingMonoSubscriber() {
            super(1);
        }

        @Override
        public final void onSubscribe(Subscription s) {
            this.s = s;
            if (!cancelled) {
                s.request(Long.MAX_VALUE);
            }
        }

        @Override
        public final void onComplete() {
            countDown();
        }

        @Override
        public void onNext(T t) {
            if (value == null) {
                value = t;
                countDown();
            }
        }

        @Override
        public void onError(Throwable t) {
            if (value == null) {
                error = t;
            }
            countDown();
        }

        final T blockingGet() {
            if (getCount() != 0) {
                try {
                    await();
                } catch (InterruptedException ex) {
                    throw new RuntimeException(ex);
                }
            }

            Throwable ex = error;
            if (ex != null) {
                throw new IllegalStateException(
                        "#block terminated with an error", ex);
            }
            return value;
        }

        final T blockingGet(long timeout, TimeUnit unit) {
            if (getCount() != 0) {
                try {
                    if (!await(timeout, unit)) {
                        throw new IllegalStateException(
                                "Timeout on blocking read for "
                                        + timeout + " " + unit);
                    }
                } catch (InterruptedException ex) {
                    throw new IllegalStateException(
                            "#block has been interrupted", ex);
                }
            }

            Throwable ex = error;
            if (ex != null) {
                throw new IllegalStateException(
                        "#block terminated with an error", ex);
            }
            return value;
        }
    }

    private static final class MonoNever extends Mono<Object> {

        private static final Mono<Object> INSTANCE = new MonoNever();

        MonoNever() {
        }

        @Override
        public void subscribe(Subscriber<? super Object> actual) {
            actual.onSubscribe(EmptySubscription.INSTANCE);
        }

        @SuppressWarnings("unchecked")
        static <T> Mono<T> instance() {
            return (Mono<T>) INSTANCE;
        }
    }
}
