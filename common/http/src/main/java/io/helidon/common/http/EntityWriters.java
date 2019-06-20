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
import io.helidon.common.reactive.EmptyPublisher;
import io.helidon.common.reactive.Flow;
import io.helidon.common.reactive.Flow.Publisher;
import java.util.Objects;
import java.util.LinkedList;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.function.Function;
import java.util.function.Predicate;

/**
 * Entity writers support.
 */
public final class EntityWriters extends ContentFilters
        implements EntityWritersRegistry {

    private final EntityWriters parent;
    private final LinkedList<WriterEntry<EntityWriter<?>>> writers;
    private final ReadWriteLock writersLock;
    private final LinkedList<WriterEntry<EntityStreamWriter<?>>> streamWriters;
    private final ReadWriteLock streamWritersLock;

    public EntityWriters() {
        super();
        this.parent = null;
        this.writers = new LinkedList<>();
        this.writersLock = new ReentrantReadWriteLock();
        this.streamWriters = new LinkedList<>();
        this.streamWritersLock = new ReentrantReadWriteLock();
    }

    public EntityWriters(EntityWriters parent) {
        super();
        this.parent = parent;
        this.writers = new LinkedList<>();
        this.writersLock = new ReentrantReadWriteLock();
        this.streamWriters = new LinkedList<>();
        this.streamWritersLock = new ReentrantReadWriteLock();
    }

    @Override
    public EntityWriters registerWriter(EntityWriter<?> writer) {
        Objects.requireNonNull(writer, "writer is null!");
        try {
            writersLock.writeLock().lock();
            writers.addFirst(new WriterEntry<>(writer.getClass(),
                    writer));
            return this;
        } finally {
            writersLock.writeLock().unlock();
        }
    }

    @Override
    public EntityWriters registerStreamWriter(EntityStreamWriter<?> writer) {
        Objects.requireNonNull(writer, "writer is null!");
        try {
            streamWritersLock.writeLock().lock();
            streamWriters.addFirst(new WriterEntry<>(
                    writer.getClass(), writer));
            return this;
        } finally {
            streamWritersLock.writeLock().unlock();
        }
    }

    @Override
    public <T> EntityWritersRegistry registerWriter(Class<T> type,
            Function<T, Publisher<DataChunk>> function) {

        return registerWriter(new TypePredicate(type), null, function);
    }

    @Override
    public <T> EntityWritersRegistry registerWriter(Class<T> type,
            MediaType contentType,
            Function<? extends T, Publisher<DataChunk>> function) {

        return registerWriter(new TypePredicate(type), contentType, function);
    }

    @Override
    public <T> EntityWritersRegistry registerWriter(Predicate<?> predicate,
            Function<T, Publisher<DataChunk>> function) {

        return registerWriter(predicate, null, function);
    }

    @Override
    public <T> EntityWritersRegistry registerWriter(Predicate<?> predicate,
            MediaType contentType, Function<T, Publisher<DataChunk>> function) {

        return registerWriter(new CompositeWriter<>(predicate, function,
                contentType));
    }

    @SuppressWarnings("unchecked")
    private <T> EntityWriter.Ack<T> selectWriter(
            Class<? extends EntityWriter<T>> cls, T entity, OutBoundScope scope,
            EntityWriters fallback) {

        try {
            writersLock.readLock().lock();
            for (WriterEntry<EntityWriter<?>> writerEntry : writers) {
                if (writerEntry.writerClass.equals(cls)) {
                    EntityWriter.Ack<T> ack = (EntityWriter.Ack<T>)
                            writerEntry.writer.accept(entity, scope);
                    if (ack != null) {
                        return ack;
                    }
                }
            }
        } finally {
            writersLock.readLock().unlock();
        }
        if (parent != null) {
            return parent.selectWriter(cls, entity, scope, fallback);
        }
        if (fallback != null) {
            return fallback.selectWriter(cls, entity, scope, null);
        }
        throw new IllegalArgumentException("No registered writer of type "
                + cls.getTypeName());
    }

    @SuppressWarnings("unchecked")
    private <T> EntityWriter.Ack<T> selectWriter(T entity,
            OutBoundScope scope, EntityWriters fallback) {

        try {
            writersLock.readLock().lock();
            for (WriterEntry<EntityWriter<?>> writerEntry : writers) {
                EntityWriter.Ack<T> ack = (EntityWriter.Ack<T>)
                        writerEntry.writer.accept(entity, scope);
                if (ack != null) {
                    return ack;
                }
            }
        } finally {
            writersLock.readLock().unlock();
        }
        if (parent != null) {
            return parent.selectWriter(entity, scope, fallback);
        }
        if (fallback != null) {
            return fallback.selectWriter(entity, scope, null);
        }
        throw new IllegalArgumentException("No registered writer for "
                + entity.getClass().getTypeName());
    }

    @SuppressWarnings("unchecked")
    private <T> EntityWriter.Ack<T> selectWriter(
            Class<? extends EntityWriter<T>> cls, T entity, GenericType<T> type,
            OutBoundScope scope, EntityWriters fallback) {

        try {
            writersLock.readLock().lock();
            for (WriterEntry<EntityWriter<?>> writerEntry : writers) {
                if (writerEntry.writerClass.equals(cls)) {
                    EntityWriter.Ack<T> ack = (EntityWriter.Ack<T>)
                            writerEntry.writer.accept(entity, type, scope);
                    if (ack != null) {
                        return ack;
                    }
                }
            }
        } finally {
            writersLock.readLock().unlock();
        }
        if (parent != null) {
            return parent.selectWriter(cls, entity, type, scope, fallback);
        }
        if (fallback != null) {
            return fallback.selectWriter(cls, entity, type, scope, null);
        }
        throw new IllegalArgumentException("No registered writer of type "
                + cls.getTypeName());
    }

    @SuppressWarnings("unchecked")
    private <T> EntityWriter.Ack<T> selectWriter(T entity,
            GenericType<T> type, OutBoundScope scope, EntityWriters fallback) {

        try {
            writersLock.readLock().lock();
            for (WriterEntry<EntityWriter<?>> writerEntry : writers) {
                EntityWriter.Ack<T> ack = (EntityWriter.Ack<T>)
                        writerEntry.writer.accept(entity,type, scope);
                if (ack != null) {
                    return ack;
                }
            }
        } finally {
            writersLock.readLock().unlock();
        }
        if (parent != null) {
            return parent.selectWriter(entity, type, scope, fallback);
        }
        if (fallback != null) {
            return fallback.selectWriter(entity, type, scope, null);
        }
        throw new IllegalArgumentException("No registered writer for "
                + type.getTypeName());
    }

    @SuppressWarnings("unchecked")
    private <T> EntityStreamWriter.Ack<T> selectStreamWriter(
            Class<? extends EntityStreamWriter<T>> cls, Class<T> type,
            OutBoundScope scope, EntityWriters fallback) {

        try {
            streamWritersLock.readLock().lock();
            for (WriterEntry<EntityStreamWriter<?>> writerEntry
                    : streamWriters) {
                if (writerEntry.writerClass.equals(cls)) {
                    EntityStreamWriter.Ack<T> ack = (EntityStreamWriter.Ack<T>)
                            writerEntry.writer.accept(type, scope);
                    if (ack != null) {
                        return ack;
                    }
                }
            }
        } finally {
            streamWritersLock.readLock().unlock();
        }
        if (parent != null) {
            return parent.selectStreamWriter(cls, type, scope, fallback);
        }
        if (fallback != null) {
            return fallback.selectStreamWriter(cls, type, scope, null);
        }
        throw new IllegalArgumentException("No registered writer of type"
                    + cls.getTypeName());
    }

    @SuppressWarnings("unchecked")
    private <T> EntityStreamWriter.Ack<T> selectStreamWriter(Class<T> type,
            OutBoundScope scope, EntityWriters fallback) {

        try {
            streamWritersLock.readLock().lock();
            for (WriterEntry<EntityStreamWriter<?>> writerEntry
                    : streamWriters) {
                EntityStreamWriter.Ack<T> ack = (EntityStreamWriter.Ack<T>)
                        writerEntry.writer.accept(type, null);
                if (ack != null) {
                    return ack;
                }
            }
        } finally {
            streamWritersLock.readLock().unlock();
        }
        if (parent != null) {
            return parent.selectStreamWriter(type, scope, fallback);
        }
        if (fallback != null) {
            return fallback.selectStreamWriter(type, scope, null);
        }
        throw new IllegalArgumentException( "No registered writer for "
                + type.getTypeName());
    }

    @SuppressWarnings("unchecked")
    private <T> EntityStreamWriter.Ack<T> selectStreamWriter(
            GenericType<T> type, OutBoundScope scope, EntityWriters fallback) {

        try {
            streamWritersLock.readLock().lock();
            for (WriterEntry<EntityStreamWriter<?>> writerEntry
                    : streamWriters) {
                EntityStreamWriter.Ack<T> ack = (EntityStreamWriter.Ack<T>)
                        writerEntry.writer.accept(type, null);
                if (ack != null) {
                    return ack;
                }
            }
        } finally {
            streamWritersLock.readLock().unlock();
        }
        if (parent != null) {
            return parent.selectStreamWriter(type, scope, fallback);
        }
        if (fallback != null) {
            return fallback.selectStreamWriter(type, scope, null);
        }
        throw new IllegalArgumentException("No registered writer for "
                + type.getTypeName());
    }

    @SuppressWarnings("unchecked")
    private <T> EntityStreamWriter.Ack<T> selectStreamWriter(
            Class<? extends EntityStreamWriter<T>> cls, GenericType<T> type,
            OutBoundScope scope, EntityWriters fallback) {

        try {
            streamWritersLock.readLock().lock();
            for (WriterEntry<EntityStreamWriter<?>> writerEntry
                    : streamWriters) {
                if (writerEntry.writerClass.equals(cls)) {
                    EntityStreamWriter.Ack<T> ack = (EntityStreamWriter.Ack<T>)
                            writerEntry.writer.accept(type, scope);
                    if (ack != null) {
                        return ack;
                    }
                }
            }
        } finally {
            streamWritersLock.readLock().unlock();
        }
        if (parent != null) {
            return parent.selectStreamWriter(cls, type, scope, fallback);
        }
        if (fallback != null) {
            return fallback.selectStreamWriter(cls, type, scope, null);
        }
        throw new IllegalArgumentException("No registered writer of type"
                    + cls.getTypeName());
    }

    private <T> Publisher<DataChunk> doMarshall(EntityWriter.Ack<T> ack,
            T entity, OutBoundScope scope, HashParameters headers,
            ContentInterceptor.Factory ifc) {

        ack.processHeaders(headers);
        Publisher<DataChunk> publisher = ack.writer()
                .writeEntity(entity, ack, scope);
        if (publisher == null) {
            publisher = new EmptyPublisher<>();
        }
        return applyFilters(publisher, ifc);
    }

    private <T> Publisher<DataChunk> doMarshall(EntityWriter.Ack<T> ack,
            T entity, GenericType<T> type, OutBoundScope scope,
            HashParameters headers, ContentInterceptor.Factory ifc) {

        ack.processHeaders(headers);
        Publisher<DataChunk> publisher = ack.writer()
                .writeEntity(entity, type, ack, scope);
        if (publisher == null) {
            publisher = new EmptyPublisher<>();
        }
        return applyFilters(publisher, ifc);
    }

    private <T> Publisher<DataChunk> doMarshallStream(
            EntityStreamWriter.Ack<T> ack, Publisher<T> stream, Class<T> type,
            OutBoundScope scope, HashParameters headers,
            ContentInterceptor.Factory ifc) {

        ack.processHeaders(headers);
        Publisher<DataChunk> publisher = ack.writer()
                .writeEntityStream(stream, type, ack, scope);
        if (publisher == null) {
            publisher = new EmptyPublisher<>();
        }
        return applyFilters(publisher, ifc);
    }

    private <T> Publisher<DataChunk> doMarshallStream(
            EntityStreamWriter.Ack<T> ack, Publisher<T> stream,
            GenericType<T> type, OutBoundScope scope, HashParameters headers,
            ContentInterceptor.Factory ifc) {

        ack.processHeaders(headers);
        Publisher<DataChunk> publisher = ack.writer()
                .writeEntityStream(stream, type, ack, scope);
        if (publisher == null) {
            publisher = new EmptyPublisher<>();
        }
        return applyFilters(publisher, ifc);
    }

    public <T> Publisher<DataChunk> marshall(T entity, OutBoundScope scope,
            EntityWriters fallback, HashParameters headers,
            ContentInterceptor.Factory ifc) {

        return doMarshall(this.<T>selectWriter(entity, scope, fallback),
                entity, scope, headers, ifc);
    }

    public <T> Publisher<DataChunk> marshall(
            Class<? extends EntityWriter<T>> writerCls, T entity,
            OutBoundScope scope, EntityWriters fallback, HashParameters headers,
            ContentInterceptor.Factory ifc) {

        return doMarshall(
                this.<T>selectWriter(writerCls, entity, scope, fallback),
                entity, scope, headers, ifc);
    }

    public <T> Publisher<DataChunk> marshall(T entity, GenericType<T> type,
            OutBoundScope scope, EntityWriters fallback, HashParameters headers,
            ContentInterceptor.Factory ifc) {

        return doMarshall(this.<T>selectWriter(entity, type, scope, fallback),
                entity, type, scope, headers, ifc);
    }

    public <T> Publisher<DataChunk> marshall(
            Class<? extends EntityWriter<T>> writerCls, T entity,
            GenericType<T> type, OutBoundScope scope, EntityWriters fallback,
            HashParameters headers, ContentInterceptor.Factory ifc) {

        return doMarshall(
                this.<T>selectWriter(writerCls, entity, type, scope, fallback),
                entity, scope, headers, ifc);
    }

    public <T> Publisher<DataChunk> marshallStream(Publisher<T> stream,
            Class<T> type, OutBoundScope scope, EntityWriters fallback,
            HashParameters headers, ContentInterceptor.Factory ifc) {

        return doMarshallStream(
                this.<T>selectStreamWriter(type, scope, fallback),
                stream, type, scope, headers, ifc);
    }

    public <T> Publisher<DataChunk> marshallStream(
            Class<? extends EntityStreamWriter<T>> writerCls,
            Publisher<T> stream, Class<T> type, OutBoundScope scope,
            EntityWriters fallback, HashParameters headers,
            ContentInterceptor.Factory ifc) {

        return doMarshallStream(
                this.<T>selectStreamWriter(writerCls, type, scope, fallback),
                stream, type, scope, headers, ifc);
    }

    public <T> Publisher<DataChunk> marshallStream(Publisher<T> stream,
            GenericType<T> type, OutBoundScope scope, EntityWriters fallback,
            HashParameters headers, ContentInterceptor.Factory ifc) {

        return doMarshallStream(
                this.<T>selectStreamWriter(type, scope, fallback),
                stream, type, scope, headers, ifc);
    }

    public <T> Publisher<DataChunk> marshallStream(
            Class<? extends EntityStreamWriter<T>> writerCls,
            Publisher<T> stream, GenericType<T> type, OutBoundScope scope,
            EntityWriters fallback, HashParameters headers,
            ContentInterceptor.Factory ifc) {

        return doMarshallStream(
                this.<T>selectStreamWriter(writerCls, type, scope, fallback),
                stream, type, scope, headers, ifc);
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

    private static final class CompositeWriter<T> implements EntityWriter<T> {

        private final Predicate predicate;
        private final Function<T, Flow.Publisher<DataChunk>> function;
        private final MediaType contentType;

        @SuppressWarnings("unchecked")
        CompositeWriter(Predicate<?> predicate,
                Function<T, Publisher<DataChunk>> function,
                MediaType contentType) {

            this.predicate = predicate;
            this.function = function;
            this.contentType = contentType;
        }

        @Override
        @SuppressWarnings("unchecked")
        public Ack<T> accept(Object entity, OutBoundScope scope) {
            if (predicate.test(entity)) {
                return new Ack<>(this, contentType);
            }
            return null;
        }

        @Override
        public Publisher<DataChunk> writeEntity(T entity, Ack<T> ack,
                OutBoundScope scope) {

            return function.apply(entity);
        }
    }

    /**
     * Pair of writer and type.
     * @param <T> writer type
     */
    private static final class WriterEntry<T> {

        final Class<?> writerClass;
        final T writer;

        WriterEntry(Class<?> writerClass, T writer) {
            this.writerClass = writerClass;
            this.writer = writer;
        }
    }
}
