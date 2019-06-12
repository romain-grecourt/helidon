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
package io.helidon.common.http;

import java.util.concurrent.CompletionStage;
import java.util.function.Predicate;

import io.helidon.common.GenericType;
import io.helidon.common.reactive.Flow.Publisher;

/**
 * Represents an in-bound {@link HttpContent} that can be converted to an entity
 * or a stream of entities. It is possible to register function to convert
 * publisher to {@link CompletionStage} of a single entity using
 * {@link #registerReader(Class, Reader)} or
 * {@link #registerReader(Predicate, Reader)} methods. It is then possible to
 * use {@link #as(Class)} method to obtain such entity.
 */
public interface InBoundContent extends HttpContent {

    /**
     * Registers a reader for a later use with an appropriate {@link #as(Class)}
     * method call.
     * <p>
     * The reader must transform the published byte buffers into a completion
     * stage of the requested type.
     * <p>
     * Upon calling {@link #as(Class)} a matching reader is searched in the same
     * order as the readers were registered. If no matching reader is found, or
     * when the function throws an exception, the resulting completion stage
     * ends exceptionally.
     *
     * @param type the requested type the completion stage is be associated
     * with.
     * @param reader the reader as a function that transforms a publisher into
     * completion stage. If an exception is thrown, the resulting completion
     * stage of {@link #as(Class)} method call ends exceptionally.
     * @param <T> the requested type
     */
    <T> void registerReader(Class<T> type, Reader<T> reader);

    /**
     * Registers a reader for a later use with an appropriate {@link #as(Class)}
     * method call.
     * <p>
     * The reader must transform the published byte buffers into a completion
     * stage of the requested type.
     * <p>
     * Upon calling {@link #as(Class)} a matching reader is searched in the same
     * order as the readers were registered. If no matching reader is found or
     * when the predicate throws an exception, or when the function throws an
     * exception, the resulting completion stage ends exceptionally.
     *
     * @param predicate the predicate that determines whether the registered
     * reader can handle the requested type. If an exception is thrown, the
     * resulting completion stage of {@link #as(Class)} method call ends
     * exceptionally.
     * @param reader the reader as a function that transforms a publisher into
     * completion stage. If an exception is thrown, the resulting completion
     * stage of {@link #as(Class)} method call ends exceptionally.
     * @param <T> the requested type
     */
    <T> void registerReader(Predicate<Class<T>> predicate, Reader<T> reader);

    /**
     * Consumes and converts the request content into a completion stage of the
     * requested type.
     * <p>
     * The conversion requires an appropriate reader to be already registered
     * (see {@link #registerReader(Predicate, Reader)}). If no such reader is
     * found, the resulting completion stage ends exceptionally.
     *
     * @param type the requested type class
     * @param <T> the requested type
     * @return a completion stage of the requested type
     */
    <T> CompletionStage<T> as(Class<T> type);

    <T> Publisher<T> asPublisherOf(Class<T> type);

    <T> Publisher<T> asPublisherOf(GenericType<T> type);

    <T> void registerStreamReader(Class<T> type, StreamReader<T> reader);
}
