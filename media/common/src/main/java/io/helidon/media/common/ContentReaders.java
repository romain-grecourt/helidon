/*
 * Copyright (c) 2017, 2022 Oracle and/or its affiliates.
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

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URLDecoder;
import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.util.concurrent.Flow.Publisher;

import io.helidon.common.http.DataChunk;
import io.helidon.common.http.FormParams;
import io.helidon.common.http.Utils;
import io.helidon.common.reactive.Multi;
import io.helidon.common.reactive.Single;

/**
 * Utility class that provides standalone mechanisms for reading {@link DataChunk} publisher.
 */
public final class ContentReaders {

    /**
     * A utility class constructor.
     */
    private ContentReaders() {
    }

    /**
     * Collect the {@link DataChunk} of the given publisher into a single byte
     * array.
     *
     * @param chunks source publisher
     * @return Single
     * @since 2.0.0
     */
    public static Single<byte[]> readBytes(Publisher<DataChunk> chunks) {
        return Multi.create(chunks)
                    .collect(ByteArrayOutputStream::new, (baos, chunk) -> {
                        try {
                            for (ByteBuffer byteBuffer : chunk.data()) {
                                Utils.write(byteBuffer, baos);
                            }
                        } catch (IOException e) {
                            throw new IllegalArgumentException("Cannot convert byte buffer to a byte array!", e);
                        } finally {
                            chunk.release();
                        }
                    }).map(ByteArrayOutputStream::toByteArray);
    }

    /**
     * Convert the given publisher of {@link DataChunk} into a {@link String}.
     *
     * @param chunks  source publisher
     * @param charset charset to use for decoding the bytes
     * @return Single
     */
    public static Single<String> readString(Publisher<DataChunk> chunks, Charset charset) {
        return readBytes(chunks).map(bytes -> new String(bytes, charset));
    }

    /**
     * Convert the publisher of {@link DataChunk} into a {@link String} processed through URL decoding.
     *
     * @param chunks  source publisher
     * @param charset charset to use for decoding the input
     * @return Single
     * @since 2.0.0
     */
    public static Single<String> readURLEncodedString(Publisher<DataChunk> chunks, Charset charset) {
        return readString(chunks, charset).map(s -> URLDecoder.decode(s, charset));
    }

    /**
     * Convert the publisher of {@link DataChunk} into an {@link InputStream}.
     *
     * @param chunks source publisher
     * @return InputStream
     * @see DataChunkInputStream
     */
    public static InputStream readInputStream(Publisher<DataChunk> chunks) {
        return new DataChunkInputStream(chunks);
    }

    /**
     * Convert the publisher of {@link DataChunk} into an {@link FormParams}.
     *
     * @param chunks source publisher
     * @param charset charset
     * @return Single
     */
    public static Single<FormParams> readURLEncodedFormParams(Publisher<DataChunk> chunks, Charset charset) {
        return ContentReaders.readString(chunks, charset)
                      .map(formStr -> FormSupport.readURLEncoded(formStr, charset));
    }

    /**
     * Convert the publisher of {@link DataChunk} into an {@link FormParams}.
     *
     * @param chunks source publisher
     * @param charset charset
     * @return Single
     */
    public static Single<FormParams> readTextPlainFormParams(Publisher<DataChunk> chunks, Charset charset) {
        return ContentReaders.readString(chunks, charset).map(FormSupport::readTextPlain);
    }
}
