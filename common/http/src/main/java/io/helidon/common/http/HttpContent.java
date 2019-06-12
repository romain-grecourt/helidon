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

import java.util.function.Function;
import io.helidon.common.reactive.Flow.Publisher;
import io.helidon.common.reactive.Flow.Subscriber;

/**
 * Represents an HTTP entity as a {@link Publisher publisher} of
 * {@link DataChunk chunks} with specific features.
 * <h3>Default publisher contract</h3>
 * Default publisher accepts only single subscriber. Other subscribers receives
 * {@link Subscriber#onError(Throwable) onError()}.
 * <p>
 * {@link DataChunk} provided by {@link Subscriber#onNext(Object) onNext()}
 * method <b>must</b> be consumed in this method call. Buffer can be reused by
 * network infrastructure as soon as {@code onNext()} method returns. This
 * behavior can be inconvenient yet it helps to provide excellent performance.
 *
 * <h3>Publisher Overwrite.</h3>
 * It is possible to modify contract of the original publisher by registration
 * of a new publisher using {@link #registerFilter(Function)} method. It can be
 * used to wrap or replace previously registered (or default) publisher.
 */
public interface HttpContent extends Publisher<DataChunk> {

    /**
     * If possible, adds the given Subscriber to this publisher. This publisher
     * is effectively either the original publisher or the last publisher
     * registered by the method {@link #registerFilter(Function)}.
     * <p>
     * Note that the original publisher allows only a single subscriber and
     * requires the passed {@link DataChunk} in the
     * {@link Subscriber#onNext(Object)} call to be consumed before the method
     * completes as specified by the {@link Content Default Publisher Contract}.
     *
     * @param subscriber the subscriber
     * @throws NullPointerException if subscriber is null
     */
    @Override
    void subscribe(Subscriber<? super DataChunk> subscriber);

    /**
     * Registers a filter that allows a control of the original publisher.
     * <p>
     * The provided function is evaluated upon calling either of
     * {@link #subscribe(Subscriber)} or {@link #as(Class)}. The first
     * evaluation of the function transforms the original publisher to a new
     * publisher. Any subsequent evaluation receives the publisher transformed
     * by the last previously registered filter. It is up to the implementation
     * of the given function to respect the contract of both the original
     * publisher and the previously registered ones.
     *
     * @param filter a function that transforms a given publisher (that is
     * either the original publisher or the publisher transformed by the last
     * previously registered filter).
     */
    void registerFilter(Filter filter);
}
