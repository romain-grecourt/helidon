/*
 * Copyright (c) 2019 Oracle and/or its affiliates. All rights reserved.
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
package io.helidon.common.http;

import java.util.Iterator;
import java.util.LinkedList;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.function.Predicate;

/**
 * Registry of content operators.
 */
class ContentOperatorRegistry<T> implements Iterable<T>, AutoCloseable {

    private final ContentOperatorRegistry<T> parent;
    private final LinkedList<T> operators;
    private final ReadWriteLock lock;
    private AtomicBoolean readLocked;

    ContentOperatorRegistry(ContentOperatorRegistry<T> parent) {
        this.parent = parent;
        this.operators = new LinkedList<>();
        this.lock = new ReentrantReadWriteLock();
        this.readLocked = new AtomicBoolean(false);
    }

    ContentOperatorRegistry() {
        this(null);
    }

    void registerLast(T operator) {
        register(operator, false);
    }

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

    T select(Predicate<T> predicate, ContentOperatorRegistry<T> fallback) {
        Objects.requireNonNull(predicate, "predicate is null!");
        try {
            lock.readLock().lock();
            for (T operator : operators) {
                if (predicate.test(operator)) {
                    return operator;
                }
            }
        } finally {
            lock.readLock().unlock();
        }
        if (parent != null) {
            return parent.select(predicate, fallback);
        }
        if (fallback != null) {
            return fallback.select(predicate, fallback);
        }
        return null;
    }

    T get(Class qualifier, ContentOperatorRegistry<T> fallback) {
        return select(new ClassPredicate<>(qualifier), fallback);
    }

    @Override
    public Iterator<T> iterator() {
        return new RegistryIterator(this);
    }

    @Override
    public void close() {
        if (readLocked.compareAndSet(true, false)) {
            lock.readLock().unlock();
        }
    }

    private static final class RegistryIterator<T> implements Iterator<T> {

        private final Iterator<T> iterator;
        private final Lock readLock;
        private final Iterator<T> parent;
        private final AtomicBoolean locked;
        private final AtomicBoolean hasNext;

        public RegistryIterator(ContentOperatorRegistry<T> registry) {
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

    static final class ClassPredicate<T> implements Predicate<T> {

        private final Class clazz;

        ClassPredicate(Class clazz) {
            this.clazz = clazz;
        }

        @Override
        public boolean test(T t) {
            return clazz.equals(t.getClass());
        }
    }
}
