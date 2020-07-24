package io.helidon.common.http;

public interface ReleasableData {

    /**
     * Releases this chunk. The underlying data as well as the data structure instances returned by
     * methods {@link #bytes()} and {@link #iterator()} may become stale and should not be used
     * anymore. The implementations may choose to not implement this optimization and to never mutate
     * the underlying memory; in such case this method does no-op.
     * <p>
     * Note that the methods of this instance are expected to be called by a single
     * thread; if not, external synchronization must be used.
     */
    default void release() {
    }
}
