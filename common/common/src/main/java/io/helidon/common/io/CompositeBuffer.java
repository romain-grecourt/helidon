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

import java.nio.BufferUnderflowException;
import java.nio.ByteBuffer;
import java.nio.InvalidMarkException;
import java.nio.ReadOnlyBufferException;

/**
 * A virtual buffer to work against multiple {@link Buffer}.
 */
public class CompositeBuffer extends AbstractReleaseableRef<CompositeBuffer> implements Buffer<CompositeBuffer> {

    private BufferEntry head;
    private BufferEntry tail;
    private BufferEntry current;
    private int count;
    private int mark;
    private int position;
    private int capacity;
    private int limit;

    /**
     * Create a new composite buffer.
     */
    public CompositeBuffer() {
    }

    /**
     * Copy constructor.
     *
     * @param buffer The buffer to copy
     */
    protected CompositeBuffer(CompositeBuffer buffer) {
        head = buffer.head;
        tail = buffer.tail;
        current = buffer.current;
        count = buffer.count;
        mark = buffer.mark;
        position = buffer.position;
        limit = buffer.limit;
    }

    @Override
    public CompositeBuffer duplicate() {
        CompositeBuffer compositeBuffer = new CompositeBuffer();
        compositeBuffer.count = count;
        compositeBuffer.capacity = capacity;
        compositeBuffer.limit = limit;
        compositeBuffer.position = position;
        if (head != null) {
            BufferEntry previous = null;
            BufferEntry buf = head.duplicate();
            compositeBuffer.head = buf;
            if (current == head) {
                compositeBuffer.current = buf;
            }
            while (buf.next != null) {
                BufferEntry next = buf.next.duplicate();
                buf.next = next;
                if (current == buf.next) {
                    compositeBuffer.current = next;
                }
                if (previous != null) {
                    buf.previous = previous;
                }
                previous = buf;
                buf = next;
            }
            compositeBuffer.tail = buf;
        }
        return compositeBuffer;
    }

    @Override
    public CompositeBuffer asReadOnly() {
        return new ReadonlyBuffer(this);
    }

    @Override
    public int capacity() {
        return capacity;
    }

    @Override
    public CompositeBuffer limit(int newLimit) {
        if (newLimit > capacity | newLimit < 0) {
            throw new IllegalArgumentException("Invalid new limit: " + newLimit);
        }
        limit = newLimit;
        return this;
    }

    @Override
    public int limit() {
        return limit;
    }

    /**
     * Count the number of nested buffers.
     *
     * @return buffer count
     */
    public int nestedCount() {
        return count;
    }

    @Override
    public int position() {
        return position;
    }

    @Override
    public int remaining() {
        return limit - position;
    }

    @Override
    public CompositeBuffer clear() {
        count = 0;
        capacity = 0;
        limit = 0;
        position = 0;
        head = null;
        tail = null;
        current = null;
        return this;
    }

    @Override
    public CompositeBuffer position(int newPosition) {
        if (newPosition == position) {
            return this;
        }
        if (newPosition < 0 | newPosition > limit) {
            throw new IllegalArgumentException("Invalid position: " + newPosition);
        }
        if (newPosition < position) {
            return backwardPosition(newPosition);
        }
        return forwardPosition(newPosition);
    }

    @Override
    public CompositeBuffer reset() {
        int m = mark;
        if (m < 0) {
            throw new InvalidMarkException();
        }
        position = m;
        return this;
    }

    @Override
    public CompositeBuffer mark() {
        mark = position;
        return this;
    }

    @Override
    public int markValue() {
        return mark;
    }

    @Override
    protected void releaseRef() {
        for (BufferEntry buf = head; buf != null; buf = buf.next) {
            buf.release();
        }
    }

    @Override
    public byte get(int pos) {
        if (pos < 0 | pos >= limit) {
            throw new IndexOutOfBoundsException("Invalid position: " + pos);
        }
        int curPos = 0;
        for (BufferEntry buf = head; buf != null; buf = buf.next) {
            int nextPos = curPos + buf.capacity();
            if (nextPos > pos) {
                return buf.get(pos - curPos + buf.markValue());
            }
            curPos = nextPos;
        }
        // should not be reachable
        throw new IllegalStateException("Position not found: " + pos);
    }

    @Override
    public byte get() {
        if (position >= limit) {
            throw new BufferUnderflowException();
        }
        while (current != null && current.remaining() == 0) {
            current = current.next;
        }
        if (current == null) {
            throw new BufferUnderflowException();
        }
        byte b = current.get();
        position++;
        return b;
    }

    @Override
    public CompositeBuffer get(byte[] dst) {
        return get(dst, 0, dst.length);
    }

    @Override
    public CompositeBuffer get(byte[] dst, int off, int length) {
        if (length > remaining()) {
            throw new BufferUnderflowException();
        }
        int count = 0;
        for (BufferEntry buf = current; buf != null; buf = buf.next) {
            int bufRemaining = buf.remaining();
            int rbytes = length - count;
            int nbytes;
            if (bufRemaining > rbytes) {
                nbytes = rbytes;
            } else {
                nbytes = bufRemaining;
            }
            buf.get(dst, off + count, nbytes);
            count += nbytes;
            position += nbytes;
        }
        if (count < length) {
            throw new BufferUnderflowException();
        }
        return this;
    }

    /**
     * Delete n bytes starting at the current position.
     *
     * @param length The number of bytes to remove
     * @return This buffer
     */
    public CompositeBuffer delete(int length) {
        return delete(position, length);
    }

    @Override
    public CompositeBuffer put(ByteBuffer byteBuffer) {
        return put(new BufferEntry(byteBuffer), position);
    }

    @Override
    public CompositeBuffer put(byte[] bytes) {
        return put(ByteBuffer.wrap(bytes));
    }

    @Override
    public CompositeBuffer put(CompositeBuffer buffer) {
        int offset = 0;
        for (BufferEntry buf = buffer.head; buf.next != null; buf = buf.next) {
            put(buf.asReadOnly(), position + offset);
            offset += buf.remaining();
        }
        return this;
    }

    /**
     * Delete n bytes starting at the specified position.
     *
     * @param pos    The position from which to start the removal
     * @param length The number of bytes to remove
     * @return This buffer
     */
    protected CompositeBuffer delete(int pos, int length) {
        checkBounds(pos, pos + length);
        int rbytes = length; // nbytes to remove
        BufferEntry buf;
        int curPos;
        if (position <= pos) {
            curPos = position;
            buf = current;
        } else {
            curPos = 0;
            buf = head;
        }
        for (; buf.next != null && rbytes > 0; buf = buf.next) {
            int mark = buf.markValue();
            if (curPos == position) {
                curPos -= buf.position() - mark;
            }
            int bufCap = buf.capacity();
            int nextPos = curPos + bufCap;
            if (nextPos > pos) {
                // in-range
                int nbytes;
                int bufAbsPos = pos - curPos + mark;
                if (bufAbsPos == mark) {
                    if (bufCap > rbytes) {
                        // shrink left
                        buf.position(mark + rbytes).mark();
                        nbytes = rbytes;
                    } else {
                        // remove
                        buf.delete();
                        nbytes = bufCap;
                    }
                } else if (bufAbsPos + rbytes < buf.limit()) {
                    // split
                    buf.next(buf.asReadOnly().position(bufAbsPos + rbytes)).limit(bufAbsPos);
                    nbytes = bufCap;
                } else {
                    // shrink right
                    nbytes = buf.limit() - bufAbsPos;
                    buf.limit(bufAbsPos);
                }
                nextPos -= nbytes;
                rbytes -= nbytes;
            }
            curPos = nextPos;
        }
        capacity -= length;
        limit -= length;
        if (position > pos) {
            if (position - pos >= length) {
                position -= length;
            } else {
                position = pos;
            }
        }
        return this;
    }

    /**
     * Insert the specified entry at the given position.
     *
     * @param newBuffer The buffer to insert
     * @param pos       The position at which to insert the buffer
     * @return This buffer
     */
    protected CompositeBuffer put(BufferEntry newBuffer, int pos) {
        if (pos < 0 | pos > limit) {
            throw new IndexOutOfBoundsException("Invalid position: " + pos);
        }
        if (pos == 0) {
            head.previous(newBuffer);
            head = newBuffer;
        } else if (pos == limit) {
            tail.next(newBuffer);
        } else {
            BufferEntry buf;
            int curPos;
            if (position <= pos) {
                buf = current;
                curPos = position;
            } else {
                buf = head;
                curPos = 0;
            }
            for (; buf.next != null; buf = buf.next) {
                if (curPos == position) {
                    curPos -= buf.position() - buf.markValue();
                }
                int nextPos = curPos + buf.capacity();
                if (nextPos > pos) {
                    break;
                }
                curPos = nextPos;
            }
            if (buf == null) {
                throw new IllegalStateException("Unable to find position: " + pos);
            }
            if (curPos == pos) {
                // insert before current buffer
                buf.previous(newBuffer);
            } else {
                // split current buffer
                int splitPos = pos - curPos + buf.markValue();
                buf.limit(splitPos).next(newBuffer).next(buf.asReadOnly().position(splitPos));
            }
        }
        int remaining = newBuffer.remaining();
        capacity += remaining;
        limit += remaining;
        if (position > pos) {
            position += remaining;
        }
        return this;
    }

    /**
     * Check the given inclusive range and throw an exception if the range is invalid.
     *
     * @param begin range start
     * @param end   range end
     * @throws IndexOutOfBoundsException if the preconditions on the offset and length parameters do not hold
     */
    protected void checkBounds(int begin, int end) {
        if (!(begin >= 0 && begin < limit)
                || !(end > 0 && end <= limit)
                || begin > end) {
            throw new IndexOutOfBoundsException("Invalid range: begin=" + begin + ", end=" + end);
        }
    }

    private CompositeBuffer backwardPosition(int newPosition) {
        int curPos = 0;
        for (BufferEntry buf = current; buf != null && curPos < position; buf = buf.previous) {
            int mark = buf.markValue();
            int nextPos = curPos + buf.limit() - mark;
            if (nextPos > newPosition) {
                // in-range
                if (curPos <= newPosition) {
                    buf.position(newPosition - curPos + mark);
                } else {
                    buf.reset();
                }
            }
            curPos = nextPos;
        }
        this.position = newPosition;
        return this;
    }

    private CompositeBuffer forwardPosition(int newPosition) {
        int curPos = current == head ? 0 : position;
        for (BufferEntry buf = current; buf != null; buf = buf.next) {
            int mark = buf.markValue();
            if (curPos == position) {
                curPos -= buf.position() - mark;
            }
            int nextPos = curPos + buf.capacity();
            if (curPos <= newPosition && nextPos > newPosition) {
                buf.position(newPosition - curPos + mark);
                break;
            }
            curPos = nextPos;
        }
        this.position = newPosition;
        return this;
    }

    /**
     * Buffer linked list entry.
     */
    protected static class BufferEntry extends WrappedBuffer<BufferEntry> {

        protected BufferEntry next;
        protected BufferEntry previous;

        /**
         * Create a new buffer entry.
         *
         * @param buffer wrapped buffer
         */
        protected BufferEntry(Buffer<?> buffer) {
            super(buffer);
        }

        /**
         * Create a new buffer entry from a byte buffer.
         *
         * @param byteBuffer byte buffer to wrap
         */
        protected BufferEntry(ByteBuffer byteBuffer) {
            this(new ByteBufferAdapter(byteBuffer));
        }

        @Override
        public BufferEntry duplicate() {
            return new BufferEntry(wrappedBuffer().duplicate());
        }

        @Override
        public BufferEntry asReadOnly() {
            return new BufferEntry(wrappedBuffer().asReadOnly());
        }

        /**
         * Set the next link.
         *
         * @param entry the new next link
         * @return this entry
         */
        protected BufferEntry next(BufferEntry entry) {
            if (next != null) {
                entry.next = next.next;
            }
            entry.previous = this;
            next = entry;
            return this;
        }

        /**
         * Set the previous link.
         *
         * @param entry the new previous link
         * @return this entry
         */
        protected BufferEntry previous(BufferEntry entry) {
            if (previous != null) {
                entry.previous = previous.previous;
            }
            entry.next = this;
            previous = entry;
            return this;
        }

        /**
         * Delete this entry. I.e Update the links to remove this entry from the linked list.
         *
         * @return this entry
         */
        protected BufferEntry delete() {
            if (next != null) {
                previous.next = next;
                next.previous = previous;
            }
            return this;
        }
    }

    /**
     * Read-only composite buffer.
     */
    protected static class ReadonlyBuffer extends CompositeBuffer {

        /**
         * Create a new composite read-only buffer.
         *
         * @param buffer The wrapped buffer
         */
        protected ReadonlyBuffer(CompositeBuffer buffer) {
            super(buffer);
        }

        @Override
        public boolean isReadOnly() {
            return true;
        }

        @Override
        public CompositeBuffer asReadOnly() {
            return this;
        }

        @Override
        public CompositeBuffer put(CompositeBuffer buffer) {
            throw new ReadOnlyBufferException();
        }

        @Override
        public CompositeBuffer put(byte[] bytes) {
            throw new ReadOnlyBufferException();
        }

        @Override
        public CompositeBuffer put(ByteBuffer buffer) {
            throw new ReadOnlyBufferException();
        }

        @Override
        public CompositeBuffer delete(int length) {
            throw new ReadOnlyBufferException();
        }
    }
}

