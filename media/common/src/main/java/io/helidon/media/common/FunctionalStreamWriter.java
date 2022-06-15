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

import io.helidon.common.GenericType;
import io.helidon.common.http.DataChunk;
import io.helidon.media.common.MediaContext.WriterContext;
import io.helidon.media.common.MediaSupport.OperatorPredicate;
import io.helidon.media.common.MediaSupport.StreamWriter;
import io.helidon.media.common.MediaSupport.StreamWriterFunction;

/**
 * A functional adapter of {@link StreamWriter}.
 *
 * @param <T> supported type
 */
public final class FunctionalStreamWriter<T> implements StreamWriter<T> {

    private final OperatorPredicate<WriterContext> predicate;
    private final StreamWriterFunction<T> function;

    /**
     * Create a new instance.
     *
     * @param predicate predicate function
     * @param function  writer function
     */
    FunctionalStreamWriter(OperatorPredicate<WriterContext> predicate, StreamWriterFunction<T> function) {
        this.predicate = Objects.requireNonNull(predicate, "predicate is null!");
        this.function = Objects.requireNonNull(function, "function is null!");
    }

    @Override
    public MediaSupport.PredicateResult accept(GenericType<?> type, WriterContext context) {
        return predicate.accept(type, context);
    }

    @Override
    @SuppressWarnings("unchecked")
    public <U extends T> Flow.Publisher<DataChunk> write(Flow.Publisher<U> publisher,
                                                         GenericType<U> type,
                                                         WriterContext context) {

        return function.apply((Flow.Publisher<T>) publisher, context);
    }
}
