/*
 * Copyright (c) 2020 Oracle and/or its affiliates.
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

/**
 * Default implementation of {@link DataChunk}.
 */
class DataChunkImpl implements DataChunk {

    private final Buffer<?> buffer;
    private final boolean flush;
    private final Runnable releaseCallback;
    private CompletableFuture<DataChunk> writeFuture;

    /**
     * Create a new data chunk.
     *
     * @param flush           a signal that this chunk should be written and flushed from any cache if possible
     * @param releaseCallback a callback which is called when this chunk is completely processed and instance is free for reuse
     * @param buffer          the data for this chunk. Should not be reused until {@code releaseCallback} is used
     */
    DataChunkImpl(boolean flush, Runnable releaseCallback, Buffer<?> buffer) {
        this.buffer = buffer;
        this.flush = flush;
        this.releaseCallback = releaseCallback;
    }

    @Override
    public boolean flush() {
        return flush;
    }

    @Override
    public void writeFuture(CompletableFuture<DataChunk> writeFuture) {
        this.writeFuture = writeFuture;
    }

    @Override
    public Optional<CompletableFuture<DataChunk>> writeFuture() {
        return Optional.ofNullable(writeFuture);
    }

    @Override
    public boolean isReadOnly() {
        return buffer.isReadOnly();
    }

    @Override
    public DataChunk asReadOnly() {
        if (buffer.isReadOnly()) {
            return this;
        }
        return new DataChunkImpl(flush, null, buffer.asReadOnly());
    }

    @Override
    public DataChunk duplicate() {
        return new DataChunkImpl(flush, null, buffer.duplicate());
    }

    @Override
    public DataChunk limit(int newLimit) {
        buffer.limit(newLimit);
        return this;
    }

    @Override
    public int limit() {
        return buffer.limit();
    }

    @Override
    public DataChunk position(int newPosition) {
        buffer.position(newPosition);
        return this;
    }

    @Override
    public int position() {
        return buffer.position();
    }

    @Override
    public int remaining() {
        return buffer.remaining();
    }

    @Override
    public int capacity() {
        return buffer.capacity();
    }

    @Override
    public DataChunk reset() {
        buffer.reset();
        return this;
    }

    @Override
    public DataChunk clear() {
        buffer.clear();
        return this;
    }

    @Override
    public DataChunk mark() {
        buffer.mark();
        return this;
    }

    @Override
    public int markValue() {
        return buffer.markValue();
    }

    @Override
    public byte get(int pos) {
        return buffer.get(pos);
    }

    @Override
    public byte get() {
        return buffer.get();
    }

    @Override
    public DataChunk get(byte[] dst) {
        buffer.get(dst);
        return this;
    }

    @Override
    public DataChunk get(byte[] dst, int off, int length) {
        buffer.get(dst, off, length);
        return this;
    }

    @Override
    public DataChunk put(byte b) {
        buffer.put(b);
        return this;
    }

    @Override
    public DataChunk put(byte b, int pos) {
        buffer.put(b, pos);
        return this;
    }

    @Override
    public DataChunk put(byte[] bytes, int offset, int length) {
        buffer.put(bytes, offset, length);
        return this;
    }

    @Override
    public DataChunk put(byte[] bytes) {
        buffer.put(bytes);
        return this;
    }

    @Override
    public DataChunk put(Buffer<?> src) {
        if (src instanceof DataChunkImpl) {
            if (src == this) {
                throw new IllegalArgumentException("The source buffer is this buffer");
            }
            buffer.put(((DataChunkImpl) src).buffer);
        } else {
            DataChunk.super.put(src);
        }
        return this;
    }

    @Override
    public ByteBuffer[] toNioBuffers() {
        return buffer.toNioBuffers();
    }

    @Override
    public DataChunk retain(int increment) {
        buffer.retain(increment);
        return this;
    }

    @Override
    public DataChunk release(int decrement) {
        if (buffer.refCnt() > 0 && buffer.release(decrement).refCnt() == 0 && releaseCallback != null) {
            releaseCallback.run();
        }
        return this;
    }

    @Override
    public int refCnt() {
        return buffer.refCnt();
    }
}
