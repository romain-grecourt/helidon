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
package io.helidon.common.io;

import java.nio.ByteBuffer;

/**
 * Base buffer wrapper class.
 * @param <T> buffer type
 */
abstract class WrappedBuffer<T extends WrappedBuffer> implements Buffer<T> {

    private final Buffer<?> buffer;

    /**
     * Create a new wrapped buffer.
     *
     * @param buffer buffer to wrap
     */
    protected WrappedBuffer(Buffer<?> buffer) {
        this.buffer = buffer;
    }

    /**
     * Get the wrapped buffer.
     * @return wrapped buffer
     */
    protected Buffer<?> wrappedBuffer() {
        return buffer;
    }

    @Override
    public boolean isReadOnly() {
        return buffer.isReadOnly();
    }

    @Override
    public T limit(int newLimit) {
        buffer.limit(newLimit);
        return (T) this;
    }

    @Override
    public int limit() {
        return buffer.limit();
    }

    @Override
    public T position(int newPosition) {
        buffer.position(newPosition);
        return (T) this;
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
    public T reset() {
        buffer.reset();
        return (T) this;
    }

    @Override
    public T clear() {
        buffer.clear();
        return (T) this;
    }

    @Override
    public T mark() {
        buffer.mark();
        return (T) this;
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
    public T get(byte[] dst) {
        buffer.get(dst);
        return (T) this;
    }

    @Override
    public T get(byte[] dst, int off, int length) {
        buffer.get(dst, off, length);
        return (T) this;
    }

    @Override
    public T put(ByteBuffer buffer) {
        buffer.put(buffer);
        return (T) this;
    }

    @Override
    public T put(byte[] bytes) {
        buffer.put(bytes);
        return (T) this;
    }

    @Override
    public T put(T buffer) {
        buffer.put(buffer);
        return (T) this;
    }

    @Override
    public T release(int decrement) {
        buffer.release();
        return (T) this;
    }

    @Override
    public int refCnt() {
        return buffer.refCnt();
    }

    @Override
    public T retain(int increment) {
        buffer.retain(increment);
        return (T) this;
    }
}
