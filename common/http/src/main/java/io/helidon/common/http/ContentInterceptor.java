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

import io.helidon.common.reactive.Flow;
import io.helidon.common.reactive.Flow.Subscriber;
import io.helidon.common.reactive.Flow.Subscription;

/**
 * Content interceptor.
 */
public abstract class ContentInterceptor implements Subscriber<DataChunk> {

    public static interface Factory {

        default ContentInterceptor createInterceptor(
                Flow.Subscriber<? super DataChunk> subscriber) {

            return createInterceptor(subscriber, /* type */ null);
        }

        ContentInterceptor createInterceptor(
                Flow.Subscriber<? super DataChunk> subscriber, String type);
    }

    static final class WrappedFactory implements Factory {

        private final Factory delegate;
        private final String type;

        /**
         * Create a new instance.
         *
         * @param delegate delegate factory.
         * @param type type requested for conversion
         */
        WrappedFactory(Factory delegate, String type) {
            this.delegate = delegate;
            this.type = type;
        }

        @Override
        public ContentInterceptor createInterceptor(
                Subscriber<? super DataChunk> subscriber) {

            return delegate.createInterceptor(subscriber, type);
        }

        @Override
        public ContentInterceptor createInterceptor(
                Subscriber<? super DataChunk> subscriber,
                String requestedType) {

            return delegate.createInterceptor(subscriber, requestedType);
        }
    }

    private final Subscriber<? super DataChunk> delegate;
    protected final String type;

    /**
     * Create a new interceptor.
     *
     * @param subscriber delegate subscriber
     * @param requestedType type requested for conversion
     */
    protected ContentInterceptor(Subscriber<? super DataChunk> subscriber,
            String requestedType) {

        this.delegate = subscriber;
        this.type = requestedType;
    }

    /**
     * Invoked before {@link Subscriber#onSubscribe}.
     */
    public abstract void beforeOnSubscribe();

    /**
     * Invoked after {@link Subscriber#onError(java.lang.Throwable)}.
     *
     * @param throwable error
     */
    public abstract void afterOnError(Throwable throwable);

    /**
     * Invoked after {@link Subscriber#onComplete()}.
     */
    public abstract void afterOnComplete();

    @Override
    public final void onSubscribe(Subscription subscription) {
        try {
            beforeOnSubscribe();
        } finally {
            delegate.onSubscribe(subscription);
        }
    }

    @Override
    public final void onNext(DataChunk item) {
        delegate.onNext(item);
    }

    @Override
    public final void onError(Throwable throwable) {
        try {
            delegate.onError(throwable);
        } finally {
            afterOnError(throwable);
        }
    }

    @Override
    public final void onComplete() {
        try {
            delegate.onComplete();
        } finally {
            afterOnComplete();
        }
    }
}
