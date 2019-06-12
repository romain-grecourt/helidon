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
import java.util.function.Function;

/**
 * The Writer transforms an object into a publisher of {@link DataChunk}.
 *
 * @param <T> the type
 */
@FunctionalInterface
public interface Writer<T> extends Function<T, Flow.Publisher<DataChunk>> {
    // charset
    // context or content support
}
