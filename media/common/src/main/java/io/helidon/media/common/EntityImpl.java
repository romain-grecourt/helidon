/*
 * Copyright (c) 2022 Oracle and/or its affiliates.
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
package io.helidon.media.common;

import java.util.Objects;
import java.util.concurrent.Flow;
import java.util.concurrent.Flow.Publisher;
import java.util.function.Function;

import io.helidon.common.GenericType;
import io.helidon.common.http.DataChunk;
import io.helidon.common.reactive.Multi;
import io.helidon.common.reactive.Single;
import io.helidon.media.common.EntitySupport.ReaderContext;
import io.helidon.media.common.EntitySupport.WriterContext;

/**
 * Implementation of {@link Entity}.
 */
final class EntityImpl implements Entity {

    private final Function<WriterContext, Publisher<DataChunk>> factory;
    private ReaderContext readerContext;
    private WriterContext writerContext;

    /**
     * Create a new instance.
     *
     * @param factory       function that generates data with a writer context
     * @param readerContext initial reader context
     * @param writerContext initial writer context
     */
    EntityImpl(Function<WriterContext, Publisher<DataChunk>> factory,
               ReaderContext readerContext,
               WriterContext writerContext) {

        this.factory = Objects.requireNonNull(factory, "factory is null!");
        this.readerContext = readerContext;
        this.writerContext = writerContext;
    }

    @Override
    public void subscribe(Flow.Subscriber<? super DataChunk> subscriber) {
        factory.apply(writerContext()).subscribe(subscriber);
    }

    @Override
    public WriterContext writerContext() {
        if (writerContext == null) {
            writerContext = WriterContext.create();
        }
        return writerContext;
    }

    @Override
    public WriteableEntity writerContext(WriterContext writerContext) {
        this.writerContext = Objects.requireNonNull(writerContext, "writerContext is null!");
        return this;
    }

    @Override
    public ReaderContext readerContext() {
        if (readerContext == null) {
            readerContext = ReaderContext.create();
        }
        return readerContext;
    }

    @Override
    public WriteableEntity readerContext(ReaderContext readerContext) {
        this.readerContext = Objects.requireNonNull(readerContext, "readerContext is null!");
        return this;
    }

    @Override
    public <T> Single<T> as(GenericType<T> type) {
        return readerContext.unmarshall(this, type);
    }

    @Override
    public <T> Multi<T> asStream(GenericType<T> type) {
        return readerContext.unmarshallStream(this, type);
    }
}
