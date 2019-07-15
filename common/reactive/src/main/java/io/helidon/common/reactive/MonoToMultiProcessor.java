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

import io.helidon.common.reactive.Flow.Processor;
import io.helidon.common.reactive.Flow.Publisher;
import io.helidon.common.reactive.Flow.Subscriber;
import io.helidon.common.reactive.Flow.Subscription;

/**
 * Processor to convert a {@link Mono} to a {@link Multi}.
 * @param <T> Mono item type (subscribed)
 * @param <U> Multi item type (published)
 */
final class MonoToMultiProcessor<T, U> implements Processor<T, U> {

    private final MultiMapper<T, ? extends U> mapper;
    private Throwable error;
    private Publisher<? extends U> delegate;
    private Subscription subscription;
    private Subscriber<? super U> subscriber;
    private volatile boolean subcribed;

    MonoToMultiProcessor(MultiMapper<T, ? extends U> mapper) {
        this.mapper = mapper;
    }

    @Override
    public void onNext(T item) {
        if (delegate == null) {
            delegate = mapper.map(item);
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
