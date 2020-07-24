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
 * Adapter of {@link ByteBuffer} to {@link Buffer}.
 */
public class ByteBufferAdapter implements Buffer<ByteBufferAdapter> {

    private final ByteBuffer byteBuffer;
    private int mark = -1;

    ByteBufferAdapter(ByteBuffer byteBuffer) {
        this.byteBuffer = byteBuffer;
    }

    /**
     * Create a new {@link Buffer} backed by a{@link ByteBuffer}.
     *
     * @param byteBuffer byte buffer
     * @return created buffer
     */
    public static ByteBufferAdapter create(ByteBuffer byteBuffer) {
        return new ByteBufferAdapter(byteBuffer);
    }

    @Override
    public ByteBufferAdapter duplicate() {
        return new ByteBufferAdapter(byteBuffer.duplicate());
    }

    @Override
    public ByteBufferAdapter asReadOnly() {
        return new ByteBufferAdapter(byteBuffer.asReadOnlyBuffer());
    }

    @Override
    public byte get(int pos) {
        return byteBuffer.get(pos);
    }

    @Override
    public byte get() {
        byte b = byteBuffer.get();
        return b;
    }

    @Override
    public ByteBufferAdapter get(byte[] dst) {
        byteBuffer.get(dst);
        return this;
    }

    @Override
    public ByteBufferAdapter get(byte[] dst, int off, int length) {
        byteBuffer.get(dst, off, length);
        return this;
    }

    @Override
    public ByteBufferAdapter put(ByteBuffer buffer) {
        this.byteBuffer.put(buffer);
        return this;
    }

    @Override
    public ByteBufferAdapter put(byte[] bytes) {
        this.byteBuffer.put(bytes);
        return this;
    }

    @Override
    public ByteBufferAdapter put(ByteBufferAdapter buffer) {
        return put(buffer.byteBuffer);
    }

    @Override
    public final ByteBufferAdapter limit(int newLimit) {
        ((java.nio.Buffer) byteBuffer).limit(newLimit);
        return this;
    }

    @Override
    public int limit() {
        return byteBuffer.limit();
    }

    @Override
    public ByteBufferAdapter position(int newPosition) {
        ((java.nio.Buffer) byteBuffer).position(newPosition);
        return this;
    }

    @Override
    public int position() {
        return byteBuffer.position();
    }

    @Override
    public final ByteBufferAdapter reset() {
        ((java.nio.Buffer) byteBuffer).reset();
        return this;
    }

    @Override
    public final ByteBufferAdapter mark() {
        ((java.nio.Buffer) byteBuffer).mark();
        mark = byteBuffer.position();
        return this;
    }

    @Override
    public int markValue() {
        return mark;
    }

    @Override
    public final int remaining() {
        return byteBuffer.position();
    }

    @Override
    public final int capacity() {
        return byteBuffer.capacity();
    }

    @Override
    public ByteBufferAdapter clear() {
        byteBuffer.clear();
        return this;
    }
}
