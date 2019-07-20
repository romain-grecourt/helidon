/*
 * Copyright (c) 2019 Oracle and/or its affiliates. All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.helidon.common.reactive;

import java.time.Duration;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;

import io.helidon.common.reactive.Flow.Publisher;
import io.helidon.common.reactive.Flow.Subscriber;

/**
 * Single item publisher facility.
 * @param <T> item type
 */
public abstract class Mono<T> implements Publisher<T> {

    /**
     * Retrieve the value of this {@link Mono} instance in a blocking manner.
     *
     * @return value
     * @throws IllegalStateException if the Mono wraps an error or if
     * interrupted
     */
    public final T block() {
        MonoBlockingSubscriber<T> subscriber = new MonoBlockingSubscriber<>();
        this.subscribe(subscriber);
        return subscriber.blockingGet();
    }

    /**
     * Retrieve the value of this {@link Mono} instance in a blocking manner.
     *
     * @param timeout timeout value
     * @return value
     * @throws IllegalStateException if the Mono wraps an error, or the
     * timeout is reached or if interrupted
     */
    public final T block(Duration timeout) {
        MonoBlockingSubscriber<T> subscriber = new MonoBlockingSubscriber<>();
        this.subscribe(subscriber);
        return subscriber.blockingGet(timeout.toMillis(),
                TimeUnit.MILLISECONDS);
    }

    /**
     * Map this {@link Mono} instance to a new {@link Mono} of another type
     * using the given {@link MonoMapper}.
     * @param <U> mapped item type
     * @param mapper mapper
     * @return Mono
     */
    public final <U> Mono<U> map(MonoMapper<T, U> mapper) {
        this.subscribe(mapper);
        return mapper;
    }

    /**
     * Map this {@link Mono} instance to a new {@link Mono} of another type
     * using the given java {@link Function}.
     * @param <U> mapped item type
     * @param mapperFunction mapper function
     * @return Mono
     */
    public final <U> Mono<U> map(Function<T, U> mapperFunction) {
        MonoMapperFunctional<T, U> mapper =
                new MonoMapperFunctional<>(mapperFunction);
        this.subscribe(mapper);
        return mapper;
    }

    /**
     * Map this {@link Mono} instance to a multiple items using the given
     * {@link MonoMultiMapper}.
     *
     * @param <U> mapped items type
     * @param mapper mapper
     * @return Publisher
     */
    public final <U> Publisher<U> mapMany(MonoMultiMapper<T, U> mapper) {
        this.subscribe(mapper);
        return mapper;
    }

    /**
     * Map this {@link Mono} instance to a multiple items using the given
     * java {@link Function}..
     *
     * @param <U> mapped items type
     * @param mapperFunction mapper function
     * @return Publisher
     */
    public final <U> Publisher<U> mapMany(
            Function<T, Publisher<U>> mapperFunction) {

        MonoMultiMapperFunctional<T, U> mapper =
                new MonoMultiMapperFunctional<>(mapperFunction);
        this.subscribe(mapper);
        return mapper;
    }

    /**
     * Exposes this {@link Mono} instance as a {@link CompletableFuture}.
     * @return CompletableFuture
     */
    public final CompletableFuture<T> toFuture() {
        try {
            MonoToCompletableFuture<T> subscriber =
                    new MonoToCompletableFuture<>();
            this.subscribe(subscriber);
            return subscriber;
        } catch (Throwable ex) {
            CompletableFuture<T> future = new CompletableFuture<>();
            future.completeExceptionally(ex);
            return future;
        }
    }

    /**
     * Create a {@link Mono} instance from a {@link CompletionStage}.
     * @param <T> item type
     * @param future source future
     * @return Mono
     */
    public static <T> Mono<T> fromFuture(CompletionStage<? extends T> future) {
        return new MonoFromCompletionStage<>(future);
    }

    /**
     * Create a {@link Mono} instance that publishes the first item received
     * from the given publisher.
     * @param <T> item type
     * @param source source publisher
     * @return Mono
     */
    @SuppressWarnings("unchecked")
    public static <T> Mono<T> from(Publisher<? extends T> source) {
        if (source instanceof Mono) {
            return (Mono<T>) source;
        }
        return new MonoNext<>(source);
    }

    /**
     * Create a {@link Mono} instance that publishes the given item to its
     * subscriber(s).
     *
     * @param <T> item type
     * @param item item to publish
     * @return Mono
     */
    public static <T> Mono<T> just(T item) {
        return new MonoJust<>(item);
    }

    /**
     * Create a {@link Mono} instance that reports the given given exception to
     * its subscriber(s). The exception is reported by invoking
     * {@link Subscriber#onError(java.lang.Throwable)} when
     * {@link Publisher#subscribe(Subscriber)} is called.
     *
     * @param <T> item type
     * @param error exception to hold
     * @return Mono
     */
    public static <T> Mono<T> error(Throwable error) {
        return new MonoError<>(error);
    }

    /**
     * Get a {@link Mono} instance that completes immediately.
     *
     * @param <T> item type
     * @return Mono
     */
    public static <T> Mono<T> empty() {
        return MonoEmpty.<T>instance();
    }

    /**
     * Get a {@link Mono} instance that never completes.
     * @param <T> item type
     * @return Mono
     */
    public static <T> Mono<T> never() {
        return MonoNever.<T>instance();
    }

    /**
     * Implementation of {@link Mono} that represents a non {@code null} value.
     * @param <T> item type
     */
    private static final class MonoJust<T> extends Mono<T> {

        private final T value;

        MonoJust(T value) {
            this.value = Objects.requireNonNull(value, "value");
        }

        @Override
        public void subscribe(Subscriber<? super T> subscriber) {
            subscriber.onSubscribe(new MonoSubscription<>(value, subscriber));
        }
    }

    /**
     * Implementation of {@link Mono} that represents the absence of a value by
     * invoking {@link Subscriber#onComplete() } during
     * {@link Publisher#subscribe(Subscriber)}.
     */
    private static final class MonoEmpty extends Mono<Object> {

        /**
         * Singleton instance.
         */
        private static final MonoEmpty INSTANCE = new MonoEmpty();

        private MonoEmpty() {
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

    /**
     * Implementation of {@link Mono} that represents an error, raised during
     * {@link Publisher#subscribe(Subscriber)} by invoking
     * {@link Subscriber#onError(java.lang.Throwable)}.
     *
     * @param <T> item type
     */
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

    /**
     * Implementation of {@link Mono} that exposed the first item of a
     * {@link Publisher}.
     *
     * @param <T> item type
     */
    private static final class MonoNext<T> extends Mono<T> {

        private final Publisher<? extends T> source;

        MonoNext(Publisher<? extends T> source) {
            this.source = Objects.requireNonNull(source,
                    "source cannot be null!");
        }

        @Override
        public void subscribe(Subscriber<? super T> actual) {
            source.subscribe(new MonoSubscriber<>(actual));
        }
    }

    /**
     * Implementation of {@link Mono} that never invokes
     * {@link Subscriber#onComplete()} or
     * {@link Subscriber#onError(java.lang.Throwable)}.
     */
    private static final class MonoNever extends Mono<Object> {

        /**
         * Singleton instance.
         */
        private static final MonoNever INSTANCE = new MonoNever();

        private MonoNever() {
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

    /**
     * Implementation of {@link MonoMapper} backed by a java function for
     * mapping the items.
     *
     * @param <T> input type
     * @param <U> output type
     */
    private static final class MonoMapperFunctional<T, U>
            extends MonoMapper<T, U> {

        private final Function<T, U> mapperFunction;

        MonoMapperFunctional(Function<T, U> mapperFunction) {
            this.mapperFunction = mapperFunction;
        }

        @Override
        public U mapNext(T item) {
            return mapperFunction.apply(item);
        }
    }

    /**
     * Implementation of {@link MonoMultiMapper} backed by a java function for
     * mapping the items.
     *
     * @param <T> input type
     * @param <U> output type
     */
    private static final class MonoMultiMapperFunctional<T, U>
            extends MonoMultiMapper<T, U> {

        private final Function<T, Publisher<U>> mapperFunction;

        MonoMultiMapperFunctional(Function<T, Publisher<U>> mapperFunction) {
            this.mapperFunction = mapperFunction;
        }

        @Override
        public Publisher<U> mapNext(T item) {
            return mapperFunction.apply(item);
        }
    }
}
