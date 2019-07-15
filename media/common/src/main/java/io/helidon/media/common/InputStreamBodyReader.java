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
package io.helidon.media.common;

import io.helidon.common.GenericType;
import io.helidon.common.http.DataChunk;
import io.helidon.common.reactive.Flow.Publisher;
import java.io.InputStream;
import io.helidon.common.reactive.Mono;

/**
 * Message body reader for {@link InputStream}.
 */
public class InputStreamBodyReader implements MessageBodyReader<InputStream> {

    /**
     * Singleton instance.
     */
    private static final InputStreamBodyReader INSTANCE =
            new InputStreamBodyReader();

    /**
     * Enforce the use of {@link #get() }.
     */
    private InputStreamBodyReader() {
    }

    @Override
    public boolean accept(GenericType<?> type, MessageBodyReaderContext context) {
        return InputStream.class.isAssignableFrom(type.rawType());
    }

    @Override
    @SuppressWarnings("unchecked")
    public <U extends InputStream> Mono<U> read(
            Publisher<DataChunk> publisher, GenericType<U> type,
            MessageBodyReaderContext context) {

        return (Mono<U>) Mono.just(new PublisherInputStream(publisher));
    }

    /**
     * Get the {@link InputStreamBodyReader} singleton.
     * @return InputStreamBodyReader
     */
    public static InputStreamBodyReader get() {
        return INSTANCE;
    }
}
