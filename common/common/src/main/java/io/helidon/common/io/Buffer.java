package io.helidon.common.io;

import java.nio.BufferUnderflowException;
import java.nio.ByteBuffer;
import java.util.Iterator;
import java.util.NoSuchElementException;

/**
 * Common buffer interface.
 *
 * @param <T> buffer type
 */
public interface Buffer<T extends Buffer<T>> extends ReleaseableRef<T>, Iterable<ByteBuffer> {

    /**
     * Creates a new buffer that shares the content of this buffer.
     * The new buffer capacity, limit, position and mark values will be identical to those of this buffer.
     *
     * @return The new buffer
     */
    T duplicate();

    /**
     * Creates a new, read-only byte buffer that shares the content of this buffer.
     * The created buffer will not allow the shared content to be modified.
     *
     * @return The new read-only buffer
     */
    T asReadOnly();

    /**
     * Indicate if this buffer is read-only.
     *
     * @return {@code true} if read-only, {@code false} otherwise
     */
    default boolean isReadOnly() {
        return false;
    }

    /**
     * Set the buffer limit.
     *
     * @param newLimit The new buffer limit
     * @return This buffer
     */
    T limit(int newLimit);

    /**
     * Get the current limit of this buffer.
     *
     * @return The current limit of this buffer.
     */
    int limit();

    /**
     * Set the buffer position.
     *
     * @param newPosition The new buffer position
     * @return This buffer
     */
    T position(int newPosition);

    /**
     * Get the current position of this buffer.
     *
     * @return The current position of this buffer.
     */
    int position();

    /**
     * Returns the number of elements between the current position and the limit.
     *
     * @return The number of elements remaining in this buffer
     */
    int remaining();

    /**
     * Get the capacity of this buffer.
     *
     * @return The capacity of this buffer
     */
    int capacity();

    /**
     * Reset the position of this buffer to the previously-marked position.
     *
     * @return This buffer
     */
    T reset();

    /**
     * Clears this buffer. The position is set to zero, the limit is set to the capacity, and the mark is discarded.
     */
    T clear();

    /**
     * Sets this buffer's mark at its position.
     *
     * @return This buffer
     */
    T mark();

    /**
     * Get the current mark value.
     *
     * @return The mark value of this buffer, or {@code -1} if the mark is not set
     */
    int markValue();

    /**
     * Absolute get method. Reads the byte at the given position. Invocation of this method does not update the buffer
     * position.
     *
     * @param pos The position from which the byte will be read
     * @return The byte at the given position
     * @throws IndexOutOfBoundsException If position is negative or not smaller than the buffer's limit
     */
    byte get(int pos);

    /**
     * Relative get method. Reads the byte at the current position, and then increments the position.
     *
     * @return The byte at the buffer's current position
     * @throws BufferUnderflowException if the current position is not smaller than the limit
     */
    byte get();

    /**
     * Relative bulk get method. This method transfers bytes to the destination array.
     *
     * @param dst The array into which bytes are to be written
     * @return byte array
     * @throws BufferUnderflowException If the array length is smaller than the number of remaining bytes
     */
    T get(byte[] dst);

    /**
     * Relative bulk get method. This method transfers bytes to the destination array.
     *
     * @param dst    The array into which bytes are to be written
     * @param off    The offset within the array of the first byte to be written
     * @param length The number of bytes to return
     * @return This buffer
     * @throws BufferUnderflowException If there are fewer than {@code length} bytes remaining in this buffer
     */
    T get(byte[] dst, int off, int length);

    /**
     * Insert the given buffer at the current position.
     *
     * @param buffer buffer to insert
     * @return This buffer
     */
    T put(ByteBuffer buffer);

    /**
     * Insert the specified bytes at the current position (<b>optional</b>).
     *
     * @param bytes The bytes to insert
     * @return This buffer
     */
    T put(byte[] bytes);

    /**
     * Insert the specified buffer at the current position (<b>optional</b>).
     *
     * @param buffer The buffer to insert
     * @return This buffer
     */
    default T put(Buffer<?> buffer) {
        if (buffer == this) {
            throw new IllegalArgumentException("The source buffer is this buffer");
        }
        for (ByteBuffer byteBuffer : toNioBuffers()) {
            buffer.put(byteBuffer);
        }
        return (T) this;
    }

    /**
     * Copy the content of this buffer into a byte array.
     * Invocation of this method sets the position to limit.
     *
     * @return byte array
     */
    default byte[] bytes() {
        byte[] dst = new byte[remaining()];
        get(dst);
        return dst;
    }

    /**
     * Exposes this buffer readable bytes as an NIO {@link ByteBuffer}.  The returned buffer
     * shares the content with this buffer, while changing the position and limit of the returned
     * NIO buffer does not affect the indexes of this buffer.
     *
     * @return the nio buffers.
     * @throws UnsupportedOperationException if this buffer cannot be exposed as a {@link ByteBuffer}
     */
    ByteBuffer[] toNioBuffers();

    @Override
    default Iterator<ByteBuffer> iterator() {
        final ByteBuffer[] byteBuffers = toNioBuffers();
        return new Iterator<>() {
            private int index = 0;

            @Override
            public boolean hasNext() {
                return index < byteBuffers.length;
            }

            @Override
            public ByteBuffer next() {
                if (index < byteBuffers.length) {
                    return byteBuffers[index++];
                }
                throw new NoSuchElementException();
            }
        };
    }
}
