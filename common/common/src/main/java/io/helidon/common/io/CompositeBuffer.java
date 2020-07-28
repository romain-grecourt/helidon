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
import java.util.LinkedList;

/**
 * A virtual buffer to work against multiple {@link Buffer}.
 */
public class CompositeBuffer implements Buffer<CompositeBuffer> {

    private Entry head;
    private Entry tail;
    private Entry current;
    private int count;
    private int mark;
    private int position;
    private int capacity;
    private int limit;

    private CompositeBuffer() {
    }

    /**
     * Copy constructor.
     *
     * @param buffer The buffer to copy
     */
    private CompositeBuffer(CompositeBuffer buffer) {
        head = buffer.head;
        tail = buffer.tail;
        current = buffer.current;
        count = buffer.count;
        mark = buffer.mark;
        position = buffer.position;
        limit = buffer.limit;
        capacity = buffer.capacity;
    }

    /**
     * Create a new composite buffer.
     * @return created buffer
     */
    public static CompositeBuffer create() {
        return new CompositeBuffer();
    }

    @Override
    public CompositeBuffer duplicate() {
        CompositeBuffer compositeBuffer = new CompositeBuffer();
        compositeBuffer.count = count;
        compositeBuffer.capacity = capacity;
        compositeBuffer.limit = limit;
        compositeBuffer.position = position;
        if (head != null) {
            Entry previous = null;
            Entry entry = new Entry(head.buffer.duplicate());
            compositeBuffer.head = entry;
            if (current == head) {
                compositeBuffer.current = entry;
            }
            while (entry.next != null) {
                Entry next = new Entry(entry.next.buffer.duplicate());
                entry.next = next;
                if (current == entry.next) {
                    compositeBuffer.current = next;
                }
                if (previous != null) {
                    entry.previous = previous;
                }
                previous = entry;
                entry = next;
            }
            compositeBuffer.tail = entry;
        }
        return compositeBuffer;
    }

    @Override
    public CompositeBuffer asReadOnly() {
        if (isReadOnly()) {
            return this;
        }
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
        mark = -1;
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
    public byte get(int pos) {
        if (pos < 0 | pos >= limit) {
            throw new IndexOutOfBoundsException("Invalid position: " + pos);
        }
        int curPos = 0;
        for (Entry entry = head; entry != null; entry = entry.next) {
            int nextPos = curPos + entry.buffer.capacity();
            if (nextPos > pos) {
                return entry.buffer.get(pos - curPos + entry.buffer.markValue());
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
        while (current != null && current.buffer.remaining() == 0) {
            current = current.next;
        }
        if (current == null) {
            throw new BufferUnderflowException();
        }
        byte b = current.buffer.get();
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
        for (Entry entry = current; entry != null; entry = entry.next) {
            int bufRemaining = entry.buffer.remaining();
            int rbytes = length - count;
            int nbytes;
            if (bufRemaining > rbytes) {
                nbytes = rbytes;
            } else {
                nbytes = bufRemaining;
            }
            entry.buffer.get(dst, off + count, nbytes);
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
        return put(new Entry(NioBuffer.create(byteBuffer)), position);
    }

    @Override
    public CompositeBuffer put(byte[] bytes) {
        return put(ByteBuffer.wrap(bytes));
    }

    @Override
    public CompositeBuffer put(Buffer<?> buffer) {
        if (buffer == this) {
            throw new IllegalArgumentException("The source buffer is this buffer");
        }
        if (buffer instanceof CompositeBuffer) {
            int offset = 0;
            for (Entry entry = ((CompositeBuffer)buffer).head; entry.next != null; entry = entry.next) {
                put(new Entry(entry.buffer.asReadOnly()), position + offset);
                offset += entry.buffer.remaining();
            }
        } else {
            put(new Entry(buffer.asReadOnly()), position);
        }
        return this;
    }

    @Override
    public ByteBuffer[] toNioBuffers() {
        LinkedList<ByteBuffer> byteBuffers = new LinkedList<>();
        for (Entry entry = head; entry.next != null; entry = entry.next) {
            for (ByteBuffer byteBuffer : entry.buffer.toNioBuffers()) {
                byteBuffers.add(byteBuffer);
            }
        }
        return byteBuffers.toArray(new ByteBuffer[byteBuffers.size()]);
    }

    @Override
    public int refCnt() {
        if (head != null) {
            return head.buffer.refCnt();
        }
        return 0;
    }

    @Override
    public CompositeBuffer release(int decrement) {
        for (Entry entry = head; entry != null; entry = entry.next) {
            entry.buffer.release(decrement);
        }
        return this;
    }

    @Override
    public CompositeBuffer retain(int increment) {
        for (Entry entry = head; entry != null; entry = entry.next) {
            entry.buffer.retain(increment);
        }
        return this;
    }

    private CompositeBuffer delete(int pos, int length) {
        checkBounds(pos, pos + length);
        int rbytes = length; // nbytes to remove
        Entry entry;
        int curPos;
        if (position <= pos) {
            curPos = position;
            entry = current;
        } else {
            curPos = 0;
            entry = head;
        }
        for (; entry.next != null && rbytes > 0; entry = entry.next) {
            int mark = entry.buffer.markValue();
            if (curPos == position) {
                curPos -= entry.buffer.position() - mark;
            }
            int bufCap = entry.buffer.capacity();
            int nextPos = curPos + bufCap;
            if (nextPos > pos) {
                // in-range
                int nbytes;
                int bufAbsPos = pos - curPos + mark;
                if (bufAbsPos == mark) {
                    if (bufCap > rbytes) {
                        // shrink left
                        entry.buffer.position(mark + rbytes).mark();
                        nbytes = rbytes;
                    } else {
                        // remove
                        entry.delete();
                        nbytes = bufCap;
                    }
                } else if (bufAbsPos + rbytes < entry.buffer.limit()) {
                    // split
                    Buffer<?> splitBuffer = entry.buffer.asReadOnly().position(bufAbsPos + rbytes).limit(bufAbsPos);
                    entry.next(new Entry(splitBuffer));
                    nbytes = bufCap;
                } else {
                    // shrink right
                    nbytes = entry.buffer.limit() - bufAbsPos;
                    entry.buffer.limit(bufAbsPos);
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

    private CompositeBuffer put(Entry newEntry, int pos) {
        if (pos < 0 | pos > limit) {
            throw new IndexOutOfBoundsException("Invalid position: " + pos);
        }
        if (pos == 0) {
            head.previous(newEntry);
            head = newEntry;
        } else if (pos == limit) {
            tail.next(newEntry);
        } else {
            Entry entry;
            int curPos;
            if (position <= pos) {
                entry = current;
                curPos = position;
            } else {
                entry = head;
                curPos = 0;
            }
            for (; entry.next != null; entry = entry.next) {
                if (curPos == position) {
                    curPos -= entry.buffer.position() - entry.buffer.markValue();
                }
                int nextPos = curPos + entry.buffer.capacity();
                if (nextPos > pos) {
                    break;
                }
                curPos = nextPos;
            }
            if (entry == null) {
                throw new IllegalStateException("Unable to find position: " + pos);
            }
            if (curPos == pos) {
                // insert before current buffer
                entry.previous(newEntry);
            } else {
                // split current buffer
                int splitPos = pos - curPos + entry.buffer.markValue();
                entry.buffer.limit(splitPos);
                Entry splitEntry = new Entry(entry.buffer.asReadOnly().position(splitPos));
                entry.next(newEntry).next(splitEntry);
            }
        }
        int remaining = newEntry.buffer.remaining();
        capacity += remaining;
        limit += remaining;
        if (position > pos) {
            position += remaining;
        }
        return this;
    }

    private void checkBounds(int begin, int end) {
        if (!(begin >= 0 && begin < limit)
                || !(end > 0 && end <= limit)
                || begin > end) {
            throw new IndexOutOfBoundsException("Invalid range: begin=" + begin + ", end=" + end);
        }
    }

    private CompositeBuffer backwardPosition(int newPosition) {
        int curPos = 0;
        for (Entry entry = current; entry != null && curPos < position; entry = entry.previous) {
            int mark = entry.buffer.markValue();
            int nextPos = curPos + entry.buffer.limit() - mark;
            if (nextPos > newPosition) {
                // in-range
                if (curPos <= newPosition) {
                    entry.buffer.position(newPosition - curPos + mark);
                } else {
                    entry.buffer.reset();
                }
            }
            curPos = nextPos;
        }
        this.position = newPosition;
        return this;
    }

    private CompositeBuffer forwardPosition(int newPosition) {
        int curPos = current == head ? 0 : position;
        for (Entry entry = current; entry != null; entry = entry.next) {
            int mark = entry.buffer.markValue();
            if (curPos == position) {
                curPos -= entry.buffer.position() - mark;
            }
            int nextPos = curPos + entry.buffer.capacity();
            if (curPos <= newPosition && nextPos > newPosition) {
                entry.buffer.position(newPosition - curPos + mark);
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
    private static class Entry {

        private Buffer<?> buffer;
        private Entry next;
        private Entry previous;

        Entry(Buffer<?> buffer) {
            this.buffer = buffer;
        }

        Entry next(Entry entry) {
            if (next != null) {
                entry.next = next.next;
            }
            entry.previous = this;
            next = entry;
            return this;
        }

        Entry previous(Entry entry) {
            if (previous != null) {
                entry.previous = previous.previous;
            }
            entry.next = this;
            previous = entry;
            return this;
        }

        Entry delete() {
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
    private static class ReadonlyBuffer extends CompositeBuffer {

        ReadonlyBuffer(CompositeBuffer buffer) {
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
        public CompositeBuffer put(Buffer<?> buffer) {
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

