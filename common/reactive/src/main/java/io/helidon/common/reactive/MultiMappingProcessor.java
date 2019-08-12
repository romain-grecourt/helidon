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
 * Processor of {@link Publisher} to {@link Single} that publishes and maps each received item.
 *
 * @param <T> subscribed type
 * @param <U> published type
 */
final class MultiMappingProcessor<T, U> implements Processor<T, U>, Multi<U> {

    private Subscriber<? super U> delegate;
    private boolean done;
    private Subscription subscription;
    private final Mapper<T, U> mapper;

    MultiMappingProcessor(Mapper<T, U> mapper) {
        this.mapper = mapper;
    }

    @Override
    public void onSubscribe(Subscription s) {
        if (subscription == null) {
            subscription = s;
            if (delegate != null) {
                delegate.onSubscribe(s);
            }
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
    public void onError(Throwable ex) {
        if (!done) {
            done = true;
            delegate.onError(ex);
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
    public void subscribe(Subscriber<? super U> subscriber) {
        this.delegate = subscriber;
        if (subscription != null) {
            delegate.onSubscribe(subscription);
        }
    }
}
