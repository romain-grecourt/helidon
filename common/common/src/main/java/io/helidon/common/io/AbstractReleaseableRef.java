package io.helidon.common.io;

import java.util.concurrent.atomic.AtomicInteger;

/**
 * Base implementation of {@link ReleaseableRef}.
 *
 * @param <T> ref type
 */
abstract class AbstractReleaseableRef<T extends AbstractReleaseableRef> implements ReleaseableRef<T> {

    private final AtomicInteger refCount = new AtomicInteger(1);

    /**
     * Release the reference.
     */
    protected abstract void releaseRef();

    @Override
    public T release(int decrement) {
        if (decrement > 0 && refCount.updateAndGet(v -> v > 0 ? v - decrement : 0) == 0) {
            releaseRef();
        }
        return (T) this;
    }

    @Override
    public int refCnt() {
        return refCount.intValue();
    }

    @Override
    public T retain(int increment) {
        if (increment > 0) {
            refCount.updateAndGet(v -> v + increment);
        }
        return (T) this;
    }
}
