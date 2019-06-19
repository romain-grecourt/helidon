/*
 * Copyright (c) 2018 Oracle and/or its affiliates. All rights reserved.
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
package io.helidon.common.http;

import io.helidon.common.reactive.Flow.Subscriber;
import io.helidon.common.reactive.Flow.Subscription;

/**
 * Content interceptor.
 */
public abstract class ContentInterceptor implements Subscriber<DataChunk> {

    private final Subscriber<? super DataChunk> subscriber;

    /**
     * Create a new interceptor.
     *
     * @param subscriber delegate subscriber
     */
    protected ContentInterceptor(Subscriber<? super DataChunk> subscriber) {

        this.subscriber = subscriber;
    }

    /**
     * Invoked before {@link Subscriber#onSubscribe}.
     * @param type type requested for conversion
     */
    public abstract void beforeOnSubscribe(String type);

    /**
     * Invoked after {@link Subscriber#onError(java.lang.Throwable)}.
     *
     * @param throwable error
     * @param type type requested for conversion
     */
    public abstract void afterOnError(Throwable throwable, String type);

    /**
     * Invoked after {@link Subscriber#onComplete()}.
     * @param type type requested for conversion
     */
    public abstract void afterOnComplete(String type);

    @Override
    public final void onSubscribe(Subscription subscription) {
        try {
            beforeOnSubscribe(null);
        } finally {
            subscriber.onSubscribe(subscription);
        }
    }

    @Override
    public final void onNext(DataChunk item) {
        subscriber.onNext(item);
    }

    @Override
    public final void onError(Throwable throwable) {
        try {
            subscriber.onError(throwable);
        } finally {
            afterOnError(throwable, null);
        }
    }

    @Override
    public final void onComplete() {
        try {
            subscriber.onComplete();
        } finally {
            afterOnComplete(null);
        }
    }

    public static interface Factory {

        ContentInterceptor create(Subscriber<? super DataChunk> subscriber);

        default Factory forType(String type) {
            return new DelegatedFactory(this, type);
        }
    }

    private static final class DelegatedInterceptor extends ContentInterceptor {

        private final ContentInterceptor delegate;
        private final String type;

        DelegatedInterceptor(ContentInterceptor delegate, String type) {
            super(delegate.subscriber);
            this.delegate = delegate;
            this.type = type;
        }

        @Override
        public void beforeOnSubscribe(String type) {
            delegate.beforeOnSubscribe(this.type);
        }

        @Override
        public void afterOnError(Throwable throwable, String type) {
            delegate.afterOnError(throwable, this.type);
        }

        @Override
        public void afterOnComplete(String type) {
            delegate.afterOnComplete(this.type);
        }
    }

    private static final class DelegatedFactory implements Factory {

        private final String type;
        private final Factory delegate;

        DelegatedFactory(Factory delegate, String type) {
            this.delegate = delegate;
            this.type = type;
        }

        @Override
        public ContentInterceptor create(
                Subscriber<? super DataChunk> subscriber) {

            return new DelegatedInterceptor(delegate.create(subscriber), type);
        }
    }
}
