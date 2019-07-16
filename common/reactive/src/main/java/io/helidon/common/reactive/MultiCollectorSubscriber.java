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

import java.util.Objects;
import java.util.concurrent.atomic.AtomicBoolean;

import io.helidon.common.reactive.Flow.Subscriber;
import io.helidon.common.reactive.Flow.Subscription;

/**
 * A subscriber that wraps a given {@link Collector}.
 * @param T collected items type
 * @param U collector container type
 */
final class MultiCollectorSubscriber<T, U>
        implements Subscriber<T>, Subscription {

    private final Collector<U, T> collector;
    private final Subscriber<? super U> subscriber;
    private final AtomicBoolean requested;
    private Subscription subscription;
    private volatile boolean done;

    MultiCollectorSubscriber(Subscriber<? super U> actual,
            Collector<U, T> collector) {

        this.subscriber = actual;
        this.collector = collector;
        this.requested = new AtomicBoolean(false);
    }

    @Override
    public void request(long n) {
        if (n > 0) {
            if (requested.compareAndSet(false, true)) {
                if (done) {
                    subscriber.onNext(collector.value());
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
    public void onNext(T item) {
        if (!done) {
            try {
                collector.collect(item);
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
                subscriber.onNext(collector.value());
                subscriber.onComplete();
            }
        }
    }
}
