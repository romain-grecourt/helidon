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

import io.helidon.common.reactive.Flow;
import java.util.function.BiFunction;

/**
 * Stream reader.
 * @param <T> stream items type
 */
@FunctionalInterface
public interface StreamReader<T> extends BiFunction<Flow.Publisher<DataChunk>, Class<? super T>, Flow.Publisher<? extends T>> {

    @Override
    Flow.Publisher<? extends T> apply(Flow.Publisher<DataChunk> t, Class <? super T> clazz);

    default Flow.Publisher<? extends T> apply(Flow.Publisher<DataChunk> chunks) {
        return apply(chunks, Object.class);
    }
}
