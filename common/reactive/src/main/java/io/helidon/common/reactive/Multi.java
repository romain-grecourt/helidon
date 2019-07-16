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

import io.helidon.common.reactive.Flow.Publisher;
import io.helidon.common.reactive.Flow.Subscriber;
import io.helidon.common.reactive.Flow.Subscription;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;

import static io.helidon.common.CollectionsHelper.listOf;

/**
 * Multiple items publisher facility.
 * @param <T> item type
 */
public abstract class Multi<T> implements Publisher<T> {

    /**
     * Subscribe to this {@link Multi} instance with the given delegate
     * functions.
     *
     * @param consumer onNext delegate function
     */
    public final void subscribe(Consumer<? super T> consumer) {
        this.subscribe(new FunctionalSubscriber<>(consumer, null, null, null));
    }

    /**
     * Subscribe to this {@link Multi} instance with the given delegate
     * functions.
     *
     * @param consumer onNext delegate function
     * @param errorConsumer onError delegate function
     */
    public final void subscribe(Consumer<? super T> consumer,
            Consumer<? super Throwable> errorConsumer) {

        this.subscribe(new FunctionalSubscriber<>(consumer, errorConsumer,
                null, null));
    }

    /**
     * Subscribe to this {@link Multi} instance with the given delegate
     * functions.
     *
     * @param consumer onNext delegate function
     * @param errorConsumer onError delegate function
     * @param completeConsumer onComplete delegate function
     */
    public final void subscribe(Consumer<? super T> consumer,
            Consumer<? super Throwable> errorConsumer,
            Runnable completeConsumer) {

        this.subscribe(new FunctionalSubscriber<>(consumer, errorConsumer,
                completeConsumer, null));
    }

    /**
     * Subscribe to this {@link Multi} instance with the given delegate
     * functions.
     *
     * @param consumer onNext delegate function
     * @param errorConsumer onError delegate function
     * @param completeConsumer onComplete delegate function
     * @param subscriptionConsumer onSusbcribe delegate function
     */
    public final void subscribe(Consumer<? super T> consumer,
            Consumer<? super Throwable> errorConsumer,
            Runnable completeConsumer,
            Consumer<? super Subscription> subscriptionConsumer) {

        this.subscribe(new FunctionalSubscriber<>(consumer, errorConsumer,
                completeConsumer, subscriptionConsumer));
    }

    /**
     * Map this {@link Multi} instance to a new {@link Multi} of another type
     * using the given {@link Mapper}.
     * @param <U> mapped item type
     * @param mapper mapper function
     * @return Multi
     */
    public final <U> Multi<U> map(Mapper<? super T, ? extends U> mapper) {
        return new MultiMap<>(this, mapper);
    }

    /**
     * Collect the items of this {@link Multi} instance into a {@link Mono} of
     * {@link List}.
     *
     * @return Mono
     */
    public final Mono<List<T>> collectList() {
        return new MonoCollector<>(this, new ListCollector<>());
    }

    /**
     * Collect the items of this {@link Multi} instance into a {@link Mono} of
     * {@link String}.
     *
     * @return Mono
     */
    public final Mono<String> collectString() {
        return new MonoCollector<>(this, new StringCollector<>());
    }

    /**
     * Collect the items of this {@link Multi} instance into a {@link Mono}.
     * @param <U> collector container type
     * @param collector collector to use
     * @return Mono
     */
    public final <U> Mono<U> collect(Collector<U, ? super T> collector) {
        return new MonoCollector<>(this, collector);
    }

    /**
     * Create a {@link Multi} instance wrapped around the given publisher.
     *
     * @param <T> item type
     * @param source source publisher
     * @return Multi
     */
    public static <T> Multi<T> from(Publisher<? extends T> source) {
        return new MultiFromPublisher<>(source);
    }

    /**
     * Create a {@link Multi} instance that publishes the given items to a
     * single subscriber.
     *
     * @param <T> item type
     * @param items items to publish
     * @return Multi
     */
    public static <T> Multi<T> just(Collection<T> items) {
        return new MultiFromPublisher<>(new FixedItemsPublisher<>(items));
    }

    /**
     * Create a {@link Multi} instance that publishes the given items to a
     * single subscriber.
     *
     * @param <T> item type
     * @param items items to publish
     * @return Multi
     */
    @SafeVarargs
    public static <T> Multi<T> just(T... items) {
        return new MultiFromPublisher<>(new FixedItemsPublisher<>(
                listOf(items)));
    }

    /**
     * Create a {@link Multi} instance that reports the given given exception to
     * its subscriber(s). The exception is reported by invoking
     * {@link Subscriber#onError(java.lang.Throwable)} when
     * {@link Publisher#subscribe(Subscriber)} is called.
     *
     * @param <T> item type
     * @param error exception to hold
     * @return Multi
     */
    public static <T> Multi<T> error(Throwable error) {
        return new MultiError<>(error);
    }

    /**
     * Get a {@link Multi} instance that completes immediately.
     *
     * @param <T> item type
     * @return Multi
     */
    public static <T> Multi<T> empty() {
        return MultiEmpty.<T>instance();
    }

    /**
     * Get a {@link Multi} instance that never completes.
     * @param <T> item type
     * @return Multi
     */
    public static <T> Multi<T> never() {
        return MultiNever.<T>instance();
    }

    /**
     * Implementation of {@link Multi} that represents the absence of a value by
     * invoking {@link Subscriber#onComplete() } during
     * {@link Publisher#subscribe(Subscriber)}.
     */
    private static final class MultiEmpty extends Multi<Object> {

        private static final MultiEmpty INSTANCE = new MultiEmpty();

        MultiEmpty() {
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

    /**
     * Implementation of {@link Multi} that represents an error, raised during
     * {@link Publisher#subscribe(Subscriber)} by invoking
     * {@link Subscriber#onError(java.lang.Throwable)}.
     *
     * @param <T> item type
     */
    private static final class MultiError<T> extends Multi<T> {

        private final Throwable error;

        MultiError(Throwable error) {
            this.error = Objects.requireNonNull(error, "error");
        }

        @Override
        public void subscribe(Subscriber<? super T> subscriber) {
            subscriber.onSubscribe(EmptySubscription.INSTANCE);
            subscriber.onError(error);
        }
    }

    /**
     * Implementation of {@link Multi} that never invokes
     * {@link Subscriber#onComplete()} or
     * {@link Subscriber#onError(java.lang.Throwable)}.
     */
    private static final class MultiNever extends Multi<Object> {

        /**
         * Singleton instance.
         */
        private static final MultiNever INSTANCE = new MultiNever();

        private MultiNever() {
        }

        @Override
        public void subscribe(Subscriber<? super Object> actual) {
            actual.onSubscribe(EmptySubscription.INSTANCE);
        }

        @SuppressWarnings("unchecked")
        static <T> Multi<T> instance() {
            return (Multi<T>) INSTANCE;
        }
    }

    /**
     * Implementation of {@link Multi} that is backed by a {@link Publisher}.
     * @param <T> items type
     */
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

    /**
     * Implementation of {@link Mono} that exposes items collected from the
     * specified source {@link Multi} using the given {@link Collector}.
     *
     * @param <T> collector container type
     * @param <U> collected items type
     */
    private static final class MonoCollector<U, T> extends Mono<T> {

        private final Multi<? extends U> source;
        private final Collector<T, U> collector;

        MonoCollector(Multi<? extends U> source, Collector<T, U> collector){

            this.collector = Objects.requireNonNull(collector,
                    "collector cannot be null!");
            this.source = source;
        }

        @Override
        public void subscribe(Subscriber<? super T> subscriber) {
            source.subscribe(new MultiCollectorSubscriber<>(subscriber,
                    collector));
        }
    }

    /**
     * Implementation of {@link Multi} that maps items using the given
     * {@link Mapper}.
     *
     * @param <T> input type
     * @param <R> output type
     */
    private static final class MultiMap<T, R> extends Multi<R> {

        private final Multi<? extends T> source;
        private final Mapper<? super T, ? extends R> mapper;

        MultiMap(Multi<? extends T> source,
                Mapper<? super T, ? extends R> mapper) {

            this.source = source;
            this.mapper = Objects.requireNonNull(mapper, "mapper");
        }

        @Override
        public void subscribe(Subscriber<? super R> actual) {
            source.subscribe(new MultiMapSubscriber<>(actual, mapper));
        }
    }
}
