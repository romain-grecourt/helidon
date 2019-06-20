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

import io.helidon.common.GenericType;
import io.helidon.common.reactive.FailedPublisher;
import io.helidon.common.reactive.Flow.Publisher;
import java.util.LinkedList;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.function.Predicate;

/**
 * Entity readers support.
 */
public final class EntityReaders extends ContentFilters
        implements EntityReadersRegistry {

    private final EntityReaders parent;
    private final LinkedList<ReaderEntry<EntityReader<?>>> readers;
    private final ReadWriteLock readersLock;
    private final LinkedList<ReaderEntry<EntityStreamReader<?>>> streamReaders;
    private final ReadWriteLock streamReadersLock;

    /**
     * Create a new instance.
     */
    public EntityReaders() {
        super();
        this.parent = null;
        this.readers = new LinkedList<>();
        this.readersLock = new ReentrantReadWriteLock();
        this.streamReaders = new LinkedList<>();
        this.streamReadersLock = new ReentrantReadWriteLock();
    }

    /**
     * Create a new parented instance.
     * @param parent entity readers parent
     */
    public EntityReaders(EntityReaders parent) {
        super(parent);
        this.parent = parent;
        this.readers = new LinkedList<>();
        this.readersLock = new ReentrantReadWriteLock();
        this.streamReaders = new LinkedList<>();
        this.streamReadersLock = new ReentrantReadWriteLock();
    }

    @Override
    public EntityReaders registerStreamReader(EntityStreamReader<?> reader) {
        Objects.requireNonNull(reader, "streamReader is null!");
        try {
            streamReadersLock.writeLock().lock();
            streamReaders.addFirst(new ReaderEntry<>(
                    reader.getClass(), reader));
            return this;
        } finally {
            streamReadersLock.writeLock().unlock();
        }
    }

    @Override
    public EntityReaders registerReader(EntityReader<?> reader) {
        Objects.requireNonNull(reader, "reader is null!");
        try {
            readersLock.writeLock().lock();
            readers.addFirst(new ReaderEntry<>(reader.getClass(),
                    reader));
            return this;
        } finally {
            readersLock.writeLock().unlock();
        }
    }

    @SuppressWarnings("unchecked")
    public <T> EntityReader<T> getReader(Class<? extends EntityReader<T>> cls,
            EntityReaders fallback) {

        try {
            readersLock.readLock().lock();
            for (ReaderEntry<EntityReader<?>> readerEntry : readers) {
                if (readerEntry.readerClass.equals(cls)){
                    return (EntityReader<T>) readerEntry.reader;
                }
            }
        } finally {
            readersLock.readLock().unlock();
        }
        if (parent != null) {
            return parent.getReader(cls, fallback);
        }
        if (fallback != null) {
            return fallback.getReader(cls, null);
        }
        throw new IllegalArgumentException("No reader found of type "
                + cls.getTypeName());
    }

    @SuppressWarnings("unchecked")
    public <T> EntityStreamReader<T> getStreamReader(
            Class<? extends EntityStreamReader<T>> cls,
            EntityReaders fallback) {

        try {
            streamReadersLock.readLock().lock();
            for (ReaderEntry<EntityStreamReader<?>> readerEntry
                    : streamReaders) {
                if (readerEntry.readerClass.equals(cls)){
                    return (EntityStreamReader<T>) readerEntry.reader;
                }
            }
        } finally {
            streamReadersLock.readLock().unlock();
        }
        if (this.parent != null) {
            return this.parent.getStreamReader(cls, fallback);
        }
        if (fallback != null) {
            return fallback.getStreamReader(cls, null);
        }
        throw new IllegalArgumentException("No stream reader found of type "
                + cls.getTypeName());
    }

    @SuppressWarnings("unchecked")
    public <T> EntityReader<T> selectReader(Class<T> type, InBoundScope scope,
            EntityReaders fallback) {

        try {
            readersLock.readLock().lock();
            for (ReaderEntry<EntityReader<?>> readerEntry : readers) {
                if (readerEntry.reader.accept(type, scope)){
                    return (EntityReader<T>) readerEntry.reader;
                }
            }
        } finally {
            readersLock.readLock().unlock();
        }
        if (parent != null) {
            return parent.selectReader(type, scope, fallback);
        }
        if (fallback != null) {
            return fallback.selectReader(type, scope, null);
        }
        throw new IllegalArgumentException("No reader found for entity type "
                + type.getTypeName());
    }

    @SuppressWarnings("unchecked")
    public <T> EntityReader<T> selectReader(GenericType<T> type,
            InBoundScope scope, EntityReaders fallback) {

        try {
            readersLock.readLock().lock();
            for (ReaderEntry<EntityReader<?>> readerEntry : readers) {
                if (readerEntry.reader.accept(type, scope)){
                    return (EntityReader<T>) readerEntry.reader;
                }
            }
        } finally {
            readersLock.readLock().unlock();
        }
        if (parent != null) {
            return parent.selectReader(type, scope, fallback);
        }
        if (fallback != null) {
            return fallback.selectReader(type, scope, null);
        }
        throw new IllegalArgumentException("No reader found for entity type "
                + type.getTypeName());
    }

    @SuppressWarnings("unchecked")
    public <T> EntityStreamReader<T> selectStreamReader(Class<T> type,
            InBoundScope scope, EntityReaders fallback) {

        try {
            streamReadersLock.readLock().lock();
            for (ReaderEntry<EntityStreamReader<?>> readerEntry
                    : streamReaders) {
                if (readerEntry.reader.accept(type, scope)){
                    return (EntityStreamReader<T>) readerEntry.reader;
                }
            }
        } finally {
            streamReadersLock.readLock().unlock();
        }
        if (parent != null) {
            return parent.selectStreamReader(type, scope, fallback);
        }
        if (fallback != null) {
            return fallback.selectStreamReader(type, scope, null);
        }
        throw new IllegalArgumentException(
                "No stream reader found for entity type " + type.getTypeName());
    }

    @SuppressWarnings("unchecked")
    public <T> EntityStreamReader<T> selectStreamReader(GenericType<T> type,
            InBoundScope scope, EntityReaders fallback) {

        try {
            streamReadersLock.readLock().lock();
            for (ReaderEntry<EntityStreamReader<?>> readerEntry
                    : streamReaders) {
                if (readerEntry.reader.accept(type, scope)){
                    return (EntityStreamReader<T>) readerEntry.reader;
                }
            }
        } finally {
            streamReadersLock.readLock().unlock();
        }
        if (parent != null) {
            return parent.selectStreamReader(type, scope, fallback);
        }
        if (fallback != null) {
            return fallback.selectStreamReader(type, scope, null);
        }
        return null;
    }

    public <T> CompletionStage<T> unmarshall(Publisher<DataChunk> publisher,
            Class<T> type, InBoundScope scope, EntityReaders fallback,
            ContentInterceptor.Factory ifc) {

        try {
            EntityReader<T> reader = selectReader(type, scope, fallback);
            Publisher<DataChunk> pub = filteredPublisher(publisher,
                    type.getTypeName(), ifc);
            return (CompletionStage<T>) reader.readEntity(pub, type, scope);
        } catch (IllegalArgumentException e) {
            CompletableFuture failedFuture = new CompletableFuture<>();
            failedFuture.completeExceptionally(e);
            return failedFuture;
        } catch (Exception e) {
            CompletableFuture failedFuture = new CompletableFuture<>();
            failedFuture.completeExceptionally(
                    new IllegalArgumentException("Transformation failed!", e));
            return failedFuture;
        }
    }

    public <T> CompletionStage<T> unmarshall(Publisher<DataChunk> publisher,
            final GenericType<T> type, InBoundScope scope,
            EntityReaders fallback, ContentInterceptor.Factory ifc) {

        try {
            EntityReader<T> reader = selectReader(type, scope, fallback);
            Publisher<DataChunk> pub = filteredPublisher(publisher,
                    type.getTypeName(), ifc);
            return (CompletionStage<T>) reader.readEntity(pub, type, scope);
        } catch (IllegalArgumentException e) {
            CompletableFuture failedFuture = new CompletableFuture<>();
            failedFuture.completeExceptionally(e);
            return failedFuture;
        } catch (Exception e) {
            CompletableFuture failedFuture = new CompletableFuture<>();
            failedFuture.completeExceptionally(
                    new IllegalArgumentException("Transformation failed!", e));
            return failedFuture;
        }
    }

    @SuppressWarnings("unchecked")
    public <T> Publisher<T> unmarshallStream(Publisher<DataChunk> publisher,
            Class<T> type, InBoundScope scope, EntityReaders fallback,
            ContentInterceptor.Factory ifc) {

        try {
            EntityStreamReader<T> streamReader = selectStreamReader(type,
                    scope, fallback);
            Publisher<DataChunk> pub = filteredPublisher(publisher,
                    type.getTypeName(), ifc);
            return (Publisher<T>) streamReader
                    .readEntityStream(pub, type, scope);
        } catch (IllegalArgumentException e) {
            return new FailedPublisher<>(e);
        } catch (Exception e) {
            return new FailedPublisher<>(new IllegalArgumentException(
                    "Transformation failed!", e));
        }
    }

    @SuppressWarnings("unchecked")
    public <T> Publisher<T> unmarshallStream(Publisher<DataChunk> publisher,
            GenericType<T> type, InBoundScope scope, EntityReaders fallback,
            ContentInterceptor.Factory ifc) {

        try {
            EntityStreamReader<T> streamReader = selectStreamReader(type,
                    scope, fallback);
            Publisher<DataChunk> pub = filteredPublisher(publisher,
                    type.getTypeName(), ifc);
            return (Publisher<T>) streamReader
                    .readEntityStream(pub, type, scope);
        } catch (IllegalArgumentException e) {
            return new FailedPublisher<>(e);
        } catch (Exception e) {
            return new FailedPublisher<>(new IllegalArgumentException(
                    "Transformation failed!", e));
        }
    }

    @Override
    public <T> void registerReader(Class<T> type, Reader<T> reader) {
        registerReader(new CompositeReader<>(new TypePredicate(type), reader));
    }

    @Override
    public <T> void registerReader(Predicate<Class<?>> predicate,
            Reader<T> reader) {

        registerReader(new CompositeReader<>(predicate, reader));
    }

    private Publisher<DataChunk> filteredPublisher(
            Publisher<DataChunk> publisher, String type,
            ContentInterceptor.Factory ifc) {

        if (ifc != null) {
            return applyFilters(publisher, ifc.forType(type));
        }
        return applyFilters(publisher, null);
    }

    /**
     * A "static" implementation of {@link Predicate} to test tests.
     */
    private static final class TypePredicate implements Predicate<Class<?>> {

        private final Class<?> type;

        TypePredicate(Class<?> type) {
            this.type = type;
        }

        @Override
        public boolean test(Class<?> cls) {
            return cls.isAssignableFrom(type);
        }
    }

    /**
     * Composite {@link EntityReader} implementation to support the deprecated
     * functional style reader.
     * @param <T> entity type
     */
    private static final class CompositeReader<T> implements EntityReader<T> {

        private final Predicate<Class<?>> predicate;
        private final Reader<T> reader;

        CompositeReader(Predicate<Class<?>> predicate, Reader<T> reader) {
            this.predicate = predicate;
            this.reader = reader;
        }

        @Override
        public boolean accept(Class<?> type, InBoundScope scope) {
            return predicate.test(type);
        }

        @Override
        public CompletionStage<? extends T> readEntity(
                Publisher<DataChunk> publisher, Class<? super T> type,
                InBoundScope scope) {

            return reader.apply(publisher, type);
        }
    }

    /**
     * Pair of reader and type.
     *
     * @param <T> reader type either {@link EntityReader} or
     * {@link EntityStreamReader}.
     */
    private static final class ReaderEntry<T> {

        final Class<?> readerClass;
        final T reader;

        ReaderEntry(Class<?> readerClass, T reader) {
            this.readerClass = readerClass;
            this.reader = reader;
        }
    }
}