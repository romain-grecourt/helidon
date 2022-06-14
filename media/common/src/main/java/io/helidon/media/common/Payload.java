/*
 * Copyright (c) 2020, 2022 Oracle and/or its affiliates.
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

import java.nio.channels.WritableByteChannel;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.concurrent.Executor;

import io.helidon.common.http.DataChunk;
import io.helidon.common.reactive.IoMulti;
import io.helidon.common.reactive.Multi;
import io.helidon.common.reactive.Single;

/**
 * Reactive contract for an HTTP payload.
 */
public interface Payload extends Multi<DataChunk> {

    /**
     * Write this payload to a file.
     *
     * @param file destination file
     * @return Single completed when the content is fully written
     * @see IoMulti#writeToFile(Path)
     * @since 3.0.0
     */
    default Single<Path> writeToFile(Path file) {
        return map(DataChunk::data)
                .flatMapIterable(Arrays::asList)
                .to(IoMulti.writeToFile(file).build())
                .toOptionalSingle()
                .map(v -> file);
    }

    /**
     * Write this payload to a file.
     *
     * @param file     destination file
     * @param executor custom executor for handling the blocking of {@link WritableByteChannel}.
     * @return Single completed when the content is fully written
     * @see IoMulti#writeToFile(Path)
     * @since 3.0.0
     */
    default Single<Path> writeToFile(Path file, Executor executor) {
        return map(DataChunk::data)
                .flatMapIterable(Arrays::asList)
                .to(IoMulti.writeToFile(file)
                           .executor(executor)
                           .build())
                .toOptionalSingle()
                .map(v -> file);
    }

    /**
     * Drain the data. I.e. Release all chunks and complete publisher.
     *
     * @return Single completed when the stream terminates
     * @since 3.0.0
     */
    default Single<Void> drain() {
        return forEach(DataChunk::release);
    }
}
