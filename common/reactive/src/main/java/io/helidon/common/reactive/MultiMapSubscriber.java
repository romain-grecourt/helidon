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

/**
 * Subscriber that delegate the received items to another subscriber of a
 * different type. The item are mapped using the given {@link Mapper}.
 */
final class MultiMapSubscriber<T, U> implements Subscriber<T>, Subscription {

    private final Subscriber<? super U> delegate;
    private final Mapper<? super T, ? extends U> mapper;
    private boolean done;
    private Subscription subscription;

    MultiMapSubscriber(Subscriber<? super U> delegate,
            Mapper<? super T, ? extends U> mapper) {

        this.delegate = delegate;
        this.mapper = mapper;
    }

    @Override
    public void onSubscribe(Subscription s) {
        if (this.subscription == null) {
            this.subscription = s;
            delegate.onSubscribe(this);
        }
    }

    @Override
    public void onNext(T item) {
        if (!done) {
            try {
                U val = mapper.map(item);
                if (val == null) {
                    delegate.onError(new IllegalStateException(
                            "Mapper returned a null value"));
                } else {
                    delegate.onNext(val);
                }
            } catch (Throwable ex) {
                onError(ex);
            }
        }
    }

    @Override
    public void onError(Throwable t) {
        if (!done) {
            done = true;
            delegate.onError(t);
        }
    }

    @Override
    public void onComplete() {
        if (!done) {
            done = true;
            delegate.onComplete();
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
