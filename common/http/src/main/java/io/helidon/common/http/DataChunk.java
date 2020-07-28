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
 * The DataChunk and the content it carries stay immutable as long as method
 * {@link #release()} is not called. After that, the given instance and the associated
 * data structure instances (e.g., the {@link ByteBuffer} array obtained by {@link #toNioBuffers()})
 * should not be used. The idea behind this class is to be able to
 * minimize data copying; ideally, in order to achieve the best performance,
 * to not copy them at all. However, the implementations may choose otherwise.
 * <p>
 * The instances of this class are expected to be accessed by a single thread. Calling
 * the methods of this class from different threads may result in a race condition unless an external
 * synchronization is used.
 */
public interface DataChunk extends Buffer<DataChunk> {

    /**
     * Creates a simple {@link ByteBuffer} backed data chunk. The resulting
     * instance doesn't have any kind of a lifecycle and as such, it doesn't need
     * to be released.
     *
     * @param byteBuffer a byte buffer to create the request chunk from
     * @return a data chunk
     */
    static DataChunk create(ByteBuffer byteBuffer) {
        return create(false, byteBuffer);
    }

    /**
     * Creates a simple byte array backed data chunk. The resulting
     * instance doesn't have any kind of a lifecycle and as such, it doesn't need
     * to be released.
     *
     * @param bytes a byte array to create the request chunk from
     * @return a data chunk
     */
    static DataChunk create(byte[] bytes) {
        return create(false, false, ByteBuffer.wrap(bytes));
    }

    /**
     * Creates a data chunk backed by one or more ByteBuffer. The resulting
     * instance doesn't have any kind of a lifecycle and as such, it doesn't need
     * to be released.
     *
     * @param byteBuffers the data for the chunk
     * @return a data chunk
     */
    static DataChunk create(ByteBuffer... byteBuffers) {
        return create(false, false, byteBuffers);
    }

    /**
     * Creates a reusable data chunk.
     *
     * @param flush       a signal that this chunk should be written and flushed from any cache if possible
     * @param byteBuffers the data for this chunk. Should not be reused until {@code releaseCallback} is used
     * @return a reusable data chunk with no release callback
     */
    static DataChunk create(boolean flush, ByteBuffer... byteBuffers) {
        return create(flush, false, byteBuffers);
    }

    /**
     * Creates a reusable data chunk.
     *
     * @param flush       a signal that this chunk should be written and flushed from any cache if possible
     * @param readOnly    indicates underlying buffers are not reused
     * @param byteBuffers the data for this chunk. Should not be reused until {@code releaseCallback} is used
     * @return a reusable data chunk with no release callback
     */
    static DataChunk create(boolean flush, boolean readOnly, ByteBuffer... byteBuffers) {
        return create(flush, readOnly, null, byteBuffers);
    }

    /**
     * Creates a reusable byteBuffers chunk.
     *
     * @param flush           a signal that this chunk should be written and flushed from any cache if possible
     * @param releaseCallback a callback which is called when this chunk is completely processed and instance is free for reuse
     * @param byteBuffers     the data for this chunk. Should not be reused until {@code releaseCallback} is used
     * @return a reusable data chunk with a release callback
     */
    static DataChunk create(boolean flush, Runnable releaseCallback, ByteBuffer... byteBuffers) {
        return create(flush, false, releaseCallback, byteBuffers);
    }

    /**
     * Creates a reusable byteBuffers chunk.
     *
     * @param flush           a signal that this chunk should be written and flushed from any cache if possible
     * @param readOnly        indicates underlying buffers are not reused
     * @param byteBuffers     the data for this chunk. Should not be reused until {@code releaseCallback} is used
     * @param releaseCallback a callback which is called when this chunk is completely processed and instance is free for reuse
     * @return a reusable data chunk with a release callback
     */
    static DataChunk create(boolean flush, boolean readOnly, Runnable releaseCallback, ByteBuffer... byteBuffers) {
        Buffer buffer;
        if (byteBuffers == null) {
            buffer = NioBuffer.create(null, readOnly);
        } else if (byteBuffers.length == 1) {
            buffer = NioBuffer.create(byteBuffers[0], readOnly);
        } else {
            buffer = CompositeBuffer.create();
            for (ByteBuffer byteBuffer : byteBuffers) {
                ((CompositeBuffer)buffer).put(NioBuffer.create(byteBuffer));
            }
        }
        return new DataChunkImpl(flush, releaseCallback, buffer);
    }

    /**
     * Returns a representation of this chunk as an array of ByteBuffer.
     * <p>
     * It is expected the returned byte buffers hold references to data that
     * will become stale upon calling method {@link #release()}. (For instance,
     * the memory segment is pooled by the underlying TCP server and is reused
     * for a subsequent request chunk.) The idea behind this class is to be able to
     * minimize data copying; ideally, in order to achieve the best performance,
     * to not copy them at all. However, the implementations may choose otherwise.
     * <p>
     * Note that the methods of this instance are expected to be called by a single
     * thread; if not, external synchronization must be used.
     *
     * @return an array of ByteBuffer representing the data of this chunk that are guarantied to stay
     * immutable as long as method {@link #release()} is not called
     * @deprecated since 2.0.2, use {@link #toNioBuffers()} instead
     */
    @Deprecated(since = "2.0.2")
    default ByteBuffer[] data() {
        return toNioBuffers();
    }

    /**
     * The tracing ID of this chunk.
     *
     * @return the tracing ID of this chunk
     */
    default long id() {
        return System.identityHashCode(this);
    }

    /**
     * Returns {@code true} if all caches are requested to flush when this chunk is written.
     * This method is only meaningful when handing data over to
     * Helidon APIs (e.g. for server response and client requests).
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
     * Returns a write future associated with this chunk.
     *
     * @return Write future if one has ben set.
     */
    default Optional<CompletableFuture<DataChunk>> writeFuture() {
        return Optional.empty();
    }
}
