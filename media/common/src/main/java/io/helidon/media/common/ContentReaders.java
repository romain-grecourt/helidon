/*
 * Copyright (c) 2017, 2019 Oracle and/or its affiliates. All rights reserved.
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

import java.io.InputStream;
import java.util.concurrent.CompletableFuture;

import io.helidon.common.http.DataChunk;
import io.helidon.common.http.Reader;
import io.helidon.common.reactive.Mono;
import java.nio.charset.Charset;

/**
 * Utility class that provides standalone mechanisms for reading message body
 * content.
 */
public final class ContentReaders {

    private static final Reader<byte[]> BYTE_ARRAY_READER =
            (publisher, clazz) -> {
                return ByteArrayBodyReader.read(publisher)
                        .flatMap((baos) -> Mono.just(baos.toByteArray()))
                        .toFuture();
            };

    private static final Reader<InputStream> INPUTSTREAM_READER =
            (publisher, clazz) -> CompletableFuture
                    .completedFuture(new PublisherInputStream(publisher));

    /**
     * For basic charsets, returns a cached {@link StringBodyReader} instance or
     * create a new instance otherwise.
     *
     * @param charset the charset to use with the returned string content reader
     * @return a string content reader
     * @deprecated use {@link StringReader#read(Flow.Publisher, Charset) }
     * instead
     */
    public static Reader<String> stringReader(Charset charset) {
        return (chunks, type) -> StringBodyReader.read(chunks, charset).toFuture();
    }

    /**
     * Get a reader that converts a {@link DataChunk} publisher to an array of
     * bytes.
     *
     * @return reader singleton that transforms a publisher of byte buffers to a
     * completion stage that might end exceptionally with
     * {@link IllegalArgumentException} if it wasn't possible to convert the
     * byte buffer to an array of bytes
     * @deprecated use {@link ByteArrayReader#read(Flow.Publisher)} instead
     */
    public static Reader<byte[]> byteArrayReader() {
        return BYTE_ARRAY_READER;
    }

    /**
     * Get a reader that converts a {@link DataChunk} publisher to a blocking
     * Java {@link InputStream}. The resulting
     * {@link java.util.concurrent.CompletionStage} is already completed;
     * however, the referenced {@link InputStream} in it may not already have
     * all the data available; in such case, the read method (e.g.,
     * {@link InputStream#read()}) block.
     *
     * @return a input stream content reader
     * @deprecated use {@link InputStreamReader#read(Publisher)} instead
     */
    public static Reader<InputStream> inputStreamReader() {
        return INPUTSTREAM_READER;
    }
}
