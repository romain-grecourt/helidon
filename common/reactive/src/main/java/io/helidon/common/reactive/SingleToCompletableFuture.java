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
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicReference;

import io.helidon.common.reactive.Flow.Subscriber;
import io.helidon.common.reactive.Flow.Subscription;

/**
 * {@link Single} exposed as a {@link CompletableFuture}.
 */
final class SingleToCompletableFuture<T> extends CompletableFuture<T>
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
    public void onNext(T item) {
        Subscription s = ref.getAndSet(null);
        if (s != null) {
            complete(item);
            s.cancel();
        }
    }

    @Override
    public void onError(Throwable ex) {
        if (ref.getAndSet(null) != null) {
            completeExceptionally(ex);
        }
    }

    @Override
    public void onComplete() {
        if (ref.getAndSet(null) != null) {
            completeExceptionally(new IllegalStateException("Completed without value"));
        }
    }
}
