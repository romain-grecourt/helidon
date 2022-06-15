/*
 * Copyright (c) 2020, 2022 Oracle and/or its affiliates.
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
package io.helidon.media.common;

import java.util.Iterator;
import java.util.LinkedList;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import io.helidon.common.GenericType;
import io.helidon.media.common.EntitySupport.Context;
import io.helidon.media.common.EntitySupport.Operator;

/**
 * Thread-safe hierarchical registry of entity operators.
 *
 * @param <T> operator type
 */
final class OperatorRegistry<T extends Operator<?>> implements Iterable<T>, AutoCloseable {

    private final LinkedList<T> operators;
    private final ReadWriteLock lock;
    private final AtomicBoolean readLocked;
    private final OperatorRegistry<T> parent;

    /**
     * Create a new parented registry.
     *
     * @param parent parent registry
     */
    OperatorRegistry(OperatorRegistry<T> parent) {
        this.parent = parent;
        this.operators = new LinkedList<>();
        this.lock = new ReentrantReadWriteLock();
        this.readLocked = new AtomicBoolean(false);
    }

    /**
     * Create a new standalone (non parented) registry.
     */
    OperatorRegistry() {
        this(null);
    }

    /**
     * Register the specified operator at the last position.
     *
     * @param operator operation to register
     */
    void registerLast(T operator) {
        register(operator, false);
    }

    /**
     * Register the specified operator at the first position.
     *
     * @param operator operation to register
     */
    void registerFirst(T operator) {
        register(operator, true);
    }

    private void register(T operator, boolean addFirst) {
        Objects.requireNonNull(operator, "operator is null!");
        try {
            lock.writeLock().lock();
            if (addFirst) {
                operators.addFirst(operator);
            } else {
                operators.addLast(operator);
            }
        } finally {
            lock.writeLock().unlock();
        }
    }

    /**
     * Select an operator using {@link Operator#accept}.
     *
     * @param type    the type representation
     * @param context the context
     * @return operator, or {@code null} or no operator was found
     */
    @SuppressWarnings("unchecked")
    <U extends Operator<V>, V extends Context> T select(GenericType<?> type, V context) {
        Objects.requireNonNull(type, "type is null!");
        Objects.requireNonNull(context, "context is null!");
        T assignableOperator = null;
        OperatorRegistry<T> current = this;

        while (current != null) {
            try {
                current.lock.readLock().lock();
                for (T operator : current.operators) {
                    EntitySupport.PredicateResult accept = ((U) operator).accept(type, context);
                    if (accept == EntitySupport.PredicateResult.COMPATIBLE && assignableOperator == null) {
                        assignableOperator = operator;
                    } else if (accept == EntitySupport.PredicateResult.SUPPORTED) {
                        return operator;
                    }
                }
            } finally {
                current.lock.readLock().unlock();
            }
            current = current.parent;
        }
        return assignableOperator;
    }

    @Override
    public Iterator<T> iterator() {
        return new ParentedIterator<>(this);
    }

    @Override
    public void close() {
        if (readLocked.compareAndSet(true, false)) {
            lock.readLock().unlock();
        }
    }

    private static final class ParentedIterator<T extends Operator<?>> implements Iterator<T> {

        private final Iterator<T> iterator;
        private final Lock readLock;
        private final Iterator<T> parent;
        private final AtomicBoolean locked;
        private final AtomicBoolean hasNext;

        ParentedIterator(OperatorRegistry<T> registry) {
            iterator = registry.operators.iterator();
            readLock = registry.lock.readLock();
            if (registry.parent != null) {
                parent = registry.parent.iterator();
            } else {
                parent = null;
            }
            locked = registry.readLocked;
            hasNext = new AtomicBoolean(true);
        }

        @Override
        public boolean hasNext() {
            if (!hasNext.get()) {
                if (parent != null) {
                    return parent.hasNext();
                }
                return false;
            }
            if (locked.compareAndSet(false, true)) {
                readLock.lock();
            }
            if (iterator.hasNext()) {
                return true;
            }
            if (locked.compareAndSet(true, false)) {
                readLock.unlock();
            }
            hasNext.set(false);
            return false;
        }

        @Override
        public T next() {
            if (hasNext.get()) {
                return iterator.next();
            } else if (parent != null) {
                return parent.next();
            }
            throw new NoSuchElementException();
        }
    }
}
