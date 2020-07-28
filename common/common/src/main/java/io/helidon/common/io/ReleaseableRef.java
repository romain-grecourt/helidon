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

/**
 * Releaseable reference.
 * A reference that may be pointing at resources that need to be released in order to be re-used.
 *
 * @param <T> reference type
 */
public interface ReleaseableRef<T extends ReleaseableRef> {

    /**
     * Whether this buffer and the associated resources are released.
     *
     * @return Whether this buffer has been released, defaults to {@code false}
     */
    default boolean isReleased() {
        return refCnt() == 0;
    }

    /**
     * <i>(optional operation)</i>
     * Decreases the reference count by {@code 1} and deallocates the underlying resources if the reference count
     * reaches {@code 0}. When released, the underlying resources may become stale and should not be used anymore.
     */
    default T release() {
        return release(1);
    }

    /**
     * <i>(optional operation)</i>
     * Decreases the reference count by the specified {@code decrement} and deallocates the underlying resources if the
     * reference count reaches {@code 0}. When released, the underlying resources  may become stale and should not be
     * used anymore.
     */
    default T release(int decrement) {
        return (T) this;
    }

    /**
     * Get the reference count of this object. If {@code 0}, it means this object has been deallocated.
     *
     * @return The reference count, defaults to {@code 1}
     */
    default int refCnt() {
        return 1;
    }

    /**
     * <i>(optional operation)</i>
     * Increases the reference count by {@code 1}.
     *
     * @return This buffer
     */
    default T retain() {
        return retain(1);
    }

    /**
     * <i>(optional operation)</i>
     * Increases the reference count by the specified {@code increment}.
     *
     * @return this buffer
     */
    default T retain(int increment) {
        return (T) this;
    }
}
