package io.helidon.common.http;

import io.helidon.common.GenericType;
import java.util.Objects;
import java.util.LinkedList;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * Entity writers support.
 */
public final class EntityWriters extends ContentFilters
        implements EntityWritersRegistry {

    private final EntityWriters delegate;
    private final LinkedList<WriterEntry<EntityWriter<?>>> writers;
    private final ReadWriteLock writersLock;
    private final LinkedList<WriterEntry<EntityStreamWriter<?>>> streamWriters;
    private final ReadWriteLock streamWritersLock;

    public EntityWriters() {
        super();
        this.delegate = null;
        this.writers = new LinkedList<>();
        this.writersLock = new ReentrantReadWriteLock();
        this.streamWriters = new LinkedList<>();
        this.streamWritersLock = new ReentrantReadWriteLock();
    }

    public EntityWriters(EntityWriters delegate) {
        super();
        this.delegate = delegate;
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
    public EntityWriters registerStreamWriter(EntityStreamWriter<?> streamWriter) {
        Objects.requireNonNull(streamWriter, "streamWriter is null!");
        try {
            streamWritersLock.writeLock().lock();
            streamWriters.addFirst(new WriterEntry<>(
                    streamWriter.getClass(), streamWriter));
            return this;
        } finally {
            streamWritersLock.writeLock().unlock();
        }
    }

    @SuppressWarnings("unchecked")
    public <T> EntityWriter<T> getWriter(
            Class<? extends EntityWriter<T>> writerClass,
            EntityWriters delegate) {

        try {
            writersLock.readLock().lock();
            for (WriterEntry<EntityWriter<?>> writerEntry : writers) {
                if (writerEntry.writerClass.equals(writerClass)) {
                    return (EntityWriter<T>) writerEntry.writer;
                }
            }
        } finally {
            writersLock.readLock().unlock();
        }
        if (this.delegate != null) {
            return this.delegate.getWriter(writerClass, delegate);
        }
        return null;
    }

    @SuppressWarnings("unchecked")
    public <T> EntityStreamWriter<T> getStreamWriter(
            Class<? extends EntityStreamWriter<T>> streamWriterClass,
            EntityWriters delegate) {

        try {
            streamWritersLock.readLock().lock();
            for (WriterEntry<EntityStreamWriter<?>> writerEntry
                    : streamWriters) {
                if (writerEntry.writerClass.equals(streamWriterClass)) {
                    return (EntityStreamWriter<T>) writerEntry.writer;
                }
            }
        } finally {
            streamWritersLock.readLock().unlock();
        }
        if (this.delegate != null) {
            return this.delegate.getStreamWriter(streamWriterClass, delegate);
        }
        return null;
    }

    @SuppressWarnings("unchecked")
    public <T> EntityWriter.Promise<T> selectWriter(Object entity,
            OutBoundScope scope, EntityWriters delegate) {

        try {
            writersLock.readLock().lock();
            for (WriterEntry<EntityWriter<?>> writerEntry : writers) {
                EntityWriter.Promise<T> promise = (EntityWriter.Promise<T>)
                        writerEntry.writer.accept(entity, scope);
                if (promise != null) {
                    return promise;
                }
            }
        } finally {
            writersLock.readLock().unlock();
        }
        if (this.delegate != null) {
            return this.delegate.selectWriter(entity, scope, delegate);
        }
        return null;
    }

    @SuppressWarnings("unchecked")
    public <T> EntityWriter.Promise<T> selectWriter(Object entity,
            GenericType<T> entityType, OutBoundScope scope,
            EntityWriters delegate) {

        try {
            writersLock.readLock().lock();
            for (WriterEntry<EntityWriter<?>> writerEntry : writers) {
                EntityWriter.Promise<T> promise = (EntityWriter.Promise<T>)
                        writerEntry.writer.accept(entity, scope);
                if (promise != null) {
                    return promise;
                }
            }
        } finally {
            writersLock.readLock().unlock();
        }
        if (this.delegate != null) {
            return this.delegate.selectWriter(entity, entityType, scope,
                    delegate);
        }
        return null;
    }

    @SuppressWarnings("unchecked")
    public <T> EntityStreamWriter.Promise<T> selectStreamWriter(
            Class<T> entityType, OutBoundScope scope,
            EntityWriters delegate) {

        try {
            streamWritersLock.readLock().lock();
            for (WriterEntry<EntityStreamWriter<?>> writerEntry
                    : streamWriters) {
                EntityStreamWriter.Promise<T> promise
                        = (EntityStreamWriter.Promise<T>) writerEntry
                                .writer.accept(entityType, null);
                if (promise != null) {
                    return promise;
                }
            }
        } finally {
            streamWritersLock.readLock().unlock();
        }
        if (this.delegate != null) {
            return this.delegate.selectStreamWriter(entityType, scope,
                    delegate);
        }
        return null;
    }

    @SuppressWarnings("unchecked")
    public <T> EntityStreamWriter.Promise<T> selectStreamWriter(
            GenericType<T> entityType, OutBoundScope scope,
            EntityWriters delegate) {

        try {
            streamWritersLock.readLock().lock();
            for (WriterEntry<EntityStreamWriter<?>> writerEntry
                    : streamWriters) {
                EntityStreamWriter.Promise<T> promise
                        = (EntityStreamWriter.Promise<T>) writerEntry
                                .writer.accept(entityType, null);
                if (promise != null) {
                    return promise;
                }
            }
        } finally {
            streamWritersLock.readLock().unlock();
        }
        if (this.delegate != null) {
            return this.delegate.selectStreamWriter(entityType,
                    scope, delegate);
        }
        return null;
    }

    private static final class WriterEntry<T> {

        final Class<?> writerClass;
        final T writer;

        WriterEntry(Class<?> writerClass, T writer) {
            this.writerClass = writerClass;
            this.writer = writer;
        }
    }
}
