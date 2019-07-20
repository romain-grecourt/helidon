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

import java.nio.channels.ReadableByteChannel;

import io.helidon.common.GenericType;
import io.helidon.common.http.DataChunk;
import io.helidon.common.http.MediaType;
import io.helidon.common.reactive.Flow.Publisher;
import io.helidon.common.reactive.RetrySchema;

/**
 * Message body writer for {@link ReadableByteChannel}.
 */
public final class ByteChannelBodyWriter
        implements MessageBodyWriter<ReadableByteChannel> {

    static final RetrySchema DEFAULT_RETRY_SCHEMA =
            RetrySchema.linear(0, 10, 250);

    private static final ByteChannelBodyWriter DEFAULT_INSTANCE =
            new ByteChannelBodyWriter(DEFAULT_RETRY_SCHEMA);

    private final RetrySchema schema;

    /**
     * Enforce the use of the static factory method.
     *
     * @param schema retry schema
     */
    private ByteChannelBodyWriter(RetrySchema schema) {
        this.schema = schema;
    }

    @Override
    public boolean accept(GenericType<?> type,
            MessageBodyWriterContext context) {

        return ReadableByteChannel.class.isAssignableFrom(type.rawType());
    }

    @Override
    public Publisher<DataChunk> write(ReadableByteChannel content,
            GenericType<? extends ReadableByteChannel> type,
            MessageBodyWriterContext context) {

        context.contentType(MediaType.APPLICATION_OCTET_STREAM);
        return new ReadableByteChannelPublisher(content, schema);
    }

    /**
     * Create a new writer instance.
     * @param schema retry schema
     * @return ByteChannelWriter
     */
    public static ByteChannelBodyWriter create(RetrySchema schema) {
        return new ByteChannelBodyWriter(schema);
    }

    /**
     * Get the singleton instance that uses the default retry schema.
     * @return ByteChannelBodyWriter
     */
    public static ByteChannelBodyWriter get() {
        return DEFAULT_INSTANCE;
    }
}
