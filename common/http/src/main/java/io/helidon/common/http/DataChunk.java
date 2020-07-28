/*
 * Copyright (c) 2018, 2020 Oracle and/or its affiliates.
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

import java.nio.ByteBuffer;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

import io.helidon.common.io.Buffer;
import io.helidon.common.io.CompositeBuffer;
import io.helidon.common.io.NioBuffer;

/**
 * The DataChunk represents a part of the HTTP body content.
 * <p>
 * The DataChunk and the content it carries stay immutable as long as it is not released. See {@link #release()}.
 * When released the data structure of a DataChunk may be reused by a different DataChunk and thus should not used.
 * <p>
 * The instances of this class are expected to be accessed by a single thread. Calling the methods of this class from
 * different threads may result in a race condition unless an external synchronization is used.
 */
public interface DataChunk extends Buffer<DataChunk> {

    /**
     * Creates a data chunk.
     *
     * @param byteBuffer the data for the chunk
     * @return a data chunk
     * @see NioBuffer
     * @deprecated since 2.1.0, use {@link #create(Buffer)} instead
     */
    @Deprecated(since = "2.1.0")
    static DataChunk create(ByteBuffer byteBuffer) {
        return create(NioBuffer.create(byteBuffer));
    }

    /**
     * Creates a data chunk.
     *
     * @param bytes the data for the chunk
     * @return a data chunk
     * @see NioBuffer
     * @deprecated since 2.1.0, use {@link #create(Buffer)} instead
     */
    @Deprecated(since = "2.1.0")
    static DataChunk create(byte[] bytes) {
        return create(NioBuffer.create(ByteBuffer.wrap(bytes)));
    }

    /**
     * Creates a data chunk.
     *
     * @param byteBuffers the data for the chunk
     * @return a data chunk
     * @see NioBuffer
     * @deprecated since 2.1.0, use {@link #create(Buffer)} instead
     */
    @Deprecated(since = "2.1.0")
    static DataChunk create(ByteBuffer... byteBuffers) {
        return create(false, false, byteBuffers);
    }

    /**
     * Creates a data chunk.
     *
     * @param flush       a signal that this chunk should be written and flushed from any cache if possible
     * @param byteBuffers the data for this chunk
     * @return a data chunk
     * @see NioBuffer
     * @deprecated since 2.1.0, use {@link #create(boolean, Runnable, Buffer)} instead
     */
    @Deprecated(since = "2.1.0")
    static DataChunk create(boolean flush, ByteBuffer... byteBuffers) {
        return create(flush, false, byteBuffers);
    }

    /**
     * Creates a reusable data chunk.
     *
     * @param flush       a signal that this chunk should be written and flushed from any cache if possible
     * @param readOnly    indicates underlying buffers are not reused
     * @param byteBuffers the data for this chunk
     * @return a data chunk
     * @see NioBuffer
     * @deprecated since 2.1.0, use {@link #create(boolean, Runnable, Buffer)} instead
     */
    @Deprecated(since = "2.1.0")
    static DataChunk create(boolean flush, boolean readOnly, ByteBuffer... byteBuffers) {
        return create(flush, readOnly, null, byteBuffers);
    }

    /**
     * Creates a reusable data chunk.
     *
     * @param flush           a signal that this chunk should be written and flushed from any cache if possible
     * @param releaseCallback a callback which is called when this chunk is released so that the buffer may be re-used
     * @param byteBuffers     the data for this chunk
     * @return a data chunk
     * @see NioBuffer
     * @deprecated since 2.1.0, use {@link #create(boolean, Runnable, Buffer)} instead
     */
    @Deprecated(since = "2.1.0")
    static DataChunk create(boolean flush, Runnable releaseCallback, ByteBuffer... byteBuffers) {
        return create(flush, false, releaseCallback, byteBuffers);
    }

    /**
     * Creates a data chunk.
     *
     * @param flush           a signal that this chunk should be written and flushed from any cache if possible
     * @param readOnly        indicates underlying buffers are not reused
     * @param byteBuffers     the data for this chunk
     * @param releaseCallback a callback which is called when this chunk is released so that the buffer may be re-used
     * @return a data chunk
     * @deprecated since 2.1.0, use {@link #create(boolean, Runnable, Buffer)} instead
     */
    @Deprecated(since = "2.1.0")
    static DataChunk create(boolean flush, boolean readOnly, Runnable releaseCallback, ByteBuffer... byteBuffers) {
        Buffer buffer;
        if (byteBuffers == null) {
            buffer = NioBuffer.create(null, readOnly);
        } else if (byteBuffers.length == 1) {
            buffer = NioBuffer.create(byteBuffers[0], readOnly);
        } else {
            buffer = CompositeBuffer.create();
            for (ByteBuffer byteBuffer : byteBuffers) {
                ((CompositeBuffer) buffer).put(NioBuffer.create(byteBuffer));
            }
        }
        return new DataChunkImpl(flush, releaseCallback, buffer);
    }

    /**
     * Creates a data chunk.
     *
     * @param buffer the data for this chunk.
     * @return a data chunk
     */
    static DataChunk create(Buffer buffer) {
        return new DataChunkImpl(false, null, buffer);
    }

    /**
     * Creates a data chunk.
     *
     * @param flush  a signal that this chunk should be written and flushed from any cache if possible
     * @param buffer the data for this chunk.
     * @return a data chunk
     */
    static DataChunk create(boolean flush, Buffer buffer) {
        return new DataChunkImpl(flush, null, buffer);
    }

    /**
     * Creates a data chunk.
     *
     * @param flush           a signal that this chunk should be written and flushed from any cache if possible
     * @param buffer          the data for this chunk.
     * @param releaseCallback a callback which is called when this chunk is released so that the buffer may be re-used
     * @return a reusable data chunk with a release callback
     */
    static DataChunk create(boolean flush, Runnable releaseCallback, Buffer buffer) {
        return new DataChunkImpl(flush, releaseCallback, buffer);
    }

    /**
     * Returns a representation of this chunk as an array of ByteBuffer.
     *
     * <p>
     * The returned ByteBuffer instances may hold references to data that will become stale upon calling method
     * {@link #release()}.
     *
     * @return an array of ByteBuffer
     * @deprecated since 2.1.0, use {@link #toNioBuffers()} instead
     */
    @Deprecated(since = "2.1.0")
    default ByteBuffer[] data() {
        return toNioBuffers();
    }

    /**
     * The tracing ID of this data chunk.
     *
     * @return the tracing ID of this data chunk
     */
    default long id() {
        return System.identityHashCode(this);
    }

    /**
     * Returns {@code true} if all caches are requested to flush when this chunk is written.
     * This method is only meaningful when handing data over to Helidon APIs (e.g. for server response and client requests).
     *
     * @return {@code true} if it is requested to flush all caches after this chunk is written, defaults to {@code false}.
     */
    default boolean flush() {
        return false;
    }

    /**
     * An empty data chunk with a flush flag can be used to force a connection flush.
     * This method determines if this chunk is used for that purpose.
     *
     * @return Outcome of test.
     */
    default boolean isFlushChunk() {
        if (!flush()) {
            return false;
        }
        for (ByteBuffer byteBuffer : data()) {
            if (byteBuffer.limit() != 0) {
                return false;
            }
        }
        return true;
    }

    /**
     * Set a write future that will complete when data chunk has been written to a connection.
     *
     * @param writeFuture Write future.
     */
    default void writeFuture(CompletableFuture<DataChunk> writeFuture) {
    }

    /**
     * Returns a write future associated with this data chunk.
     *
     * @return Write future if one has ben set.
     */
    default Optional<CompletableFuture<DataChunk>> writeFuture() {
        return Optional.empty();
    }
}
