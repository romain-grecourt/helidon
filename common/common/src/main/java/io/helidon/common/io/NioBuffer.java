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
public class NioBuffer implements Buffer<NioBuffer> {

    private static final ByteBuffer EMPTY_BYTE_BUFFER = ByteBuffer.wrap(new byte[0]);
    private static final ByteBuffer EMPTY_RO_BYTE_BUFFER = EMPTY_BYTE_BUFFER.asReadOnlyBuffer();

    private final ByteBuffer byteBuffer;
    private int mark = -1;

    private NioBuffer(ByteBuffer byteBuffer) {
        if (byteBuffer == null) {
            this.byteBuffer = EMPTY_BYTE_BUFFER;
        } else {
            this.byteBuffer = byteBuffer;
        }
    }

    private NioBuffer(ByteBuffer byteBuffer, boolean readOnly) {
        if (byteBuffer == null) {
            this.byteBuffer = readOnly ? EMPTY_RO_BYTE_BUFFER : EMPTY_BYTE_BUFFER;
        } else {
            this.byteBuffer = readOnly ? byteBuffer.asReadOnlyBuffer() : byteBuffer;
        }
    }

    /**
     * Create a new {@link Buffer} backed by a {@link ByteBuffer}.
     *
     * @param byteBuffer byte buffer
     * @return created buffer
     */
    public static NioBuffer create(ByteBuffer byteBuffer) {
        return new NioBuffer(byteBuffer);
    }

    /**
     * Create a new {@link Buffer} backed by a {@link ByteBuffer}.
     *
     * @param byteBuffer byte buffer
     * @param readOnly {@code true} if the created buffer should be read-only, {@code false} otherwise
     * @return created buffer
     */
    public static NioBuffer create(ByteBuffer byteBuffer, boolean readOnly) {
        return new NioBuffer(byteBuffer, readOnly);
    }

    @Override
    public NioBuffer duplicate() {
        return new NioBuffer(byteBuffer.duplicate());
    }

    @Override
    public NioBuffer asReadOnly() {
        return new NioBuffer(byteBuffer.asReadOnlyBuffer());
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
    public NioBuffer get(byte[] dst) {
        byteBuffer.get(dst);
        return this;
    }

    @Override
    public NioBuffer get(byte[] dst, int off, int length) {
        byteBuffer.get(dst, off, length);
        return this;
    }

    @Override
    public NioBuffer put(ByteBuffer buffer) {
        this.byteBuffer.put(buffer);
        return this;
    }

    @Override
    public NioBuffer put(byte[] bytes) {
        this.byteBuffer.put(bytes);
        return this;
    }

    @Override
    public NioBuffer put(Buffer<?> buffer) {
        if (buffer instanceof NioBuffer) {
            if (buffer == this) {
                throw new IllegalArgumentException("The source buffer is this buffer");
            }
            put(((NioBuffer) buffer).byteBuffer);
        } else {
            Buffer.super.put(buffer);
        }
        return this;
    }

    @Override
    public final NioBuffer limit(int newLimit) {
        ((java.nio.Buffer) byteBuffer).limit(newLimit);
        return this;
    }

    @Override
    public int limit() {
        return byteBuffer.limit();
    }

    @Override
    public NioBuffer position(int newPosition) {
        ((java.nio.Buffer) byteBuffer).position(newPosition);
        return this;
    }

    @Override
    public int position() {
        return byteBuffer.position();
    }

    @Override
    public final NioBuffer reset() {
        ((java.nio.Buffer) byteBuffer).reset();
        return this;
    }

    @Override
    public final NioBuffer mark() {
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
    public NioBuffer clear() {
        byteBuffer.clear();
        mark = -1;
        return this;
    }

    @Override
    public ByteBuffer[] toNioBuffers() {
        return new ByteBuffer[] { byteBuffer };
    }
}
