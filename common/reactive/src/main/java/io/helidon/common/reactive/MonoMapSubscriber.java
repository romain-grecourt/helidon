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

import io.helidon.common.reactive.Flow.Subscriber;
import io.helidon.common.reactive.Flow.Subscription;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Subscriber that delegate the received item to another subscriber of a
 * different type. The item is mapped using the given {@link Mapper}.
 */
final class MonoMapSubscriber<T, U> implements Subscriber<T>, Subscription {

    private final Mapper<? super T, ? extends U> mapper;
    private final Subscriber<? super U> subscriber;
    private final AtomicBoolean requested;
    private U value;
    private boolean done;

    MonoMapSubscriber(Subscriber<? super U> subscriber,
            Mapper<? super T, ? extends U> mapper) {

        this.subscriber = subscriber;
        this.mapper = mapper;
        this.requested = new AtomicBoolean(false);
    }

    @Override
    public void request(long n) {
        if (n > 0) {
            if (requested.compareAndSet(false, true)
                    && done
                    && value != null) {
                subscriber.onNext(value);
            }
            requested.set(true);
        }
    }

    @Override
    public void cancel() {
        // do nothing.
    }

    @Override
    public void onSubscribe(Subscription subscription) {
        subscription.request(Long.MAX_VALUE);
    }

    @Override
    public void onNext(T item) {
        if (done) {
            return;
        }
        done = true;
        try {
            U val = mapper.map(item);
            if (val == null) {
                subscriber.onError(new IllegalStateException(
                        "Mapper returned a null value"));
            } else {
                value = val;
                subscriber.onNext(value);
            }
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
