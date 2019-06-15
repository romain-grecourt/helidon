package io.helidon.common.http;

import io.helidon.common.GenericType;
import java.util.LinkedList;
import java.util.Objects;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * Entity readers support.
 */
public final class EntityReaders extends ContentFilterSupport
        implements EntityReadersRegistry {

    private final EntityReaders delegate;
    private final LinkedList<ReaderEntry<EntityReader<?>>> readers;
    private final ReadWriteLock readersLock;
    private final LinkedList<ReaderEntry<EntityStreamReader<?>>> streamReaders;
    private final ReadWriteLock streamReadersLock;

    /**
     * Create a new instance.
     */
    public EntityReaders() {
        super();
        this.delegate = null;
        this.readers = new LinkedList<>();
        this.readersLock = new ReentrantReadWriteLock();
        this.streamReaders = new LinkedList<>();
        this.streamReadersLock = new ReentrantReadWriteLock();
    }

    /**
     * Create a new delegated instance.
     * @param delegate support delegate
     */
    public EntityReaders(EntityReaders delegate) {
        super(delegate);
        this.delegate = delegate;
        this.readers = new LinkedList<>();
        this.readersLock = new ReentrantReadWriteLock();
        this.streamReaders = new LinkedList<>();
        this.streamReadersLock = new ReentrantReadWriteLock();
    }

    @Override
    public void registerStreamReader(EntityStreamReader<?> streamReader) {
        Objects.requireNonNull(streamReader, "streamReader is null!");
        try {
            streamReadersLock.writeLock().lock();
            streamReaders.addFirst(new ReaderEntry<>(
                    streamReader.getClass(), streamReader));
        } finally {
            streamReadersLock.writeLock().unlock();
        }
    }

    @Override
    public void registerReader(EntityReader<?> reader) {
        Objects.requireNonNull(reader, "reader is null!");
        try {
            readersLock.writeLock().lock();
            readers.addFirst(new ReaderEntry<>(reader.getClass(),
                    reader));
        } finally {
            readersLock.writeLock().unlock();
        }
    }

    @SuppressWarnings("unchecked")
    public <T> EntityReader<T> getReader(
            Class<? extends EntityReader<T>> readerClass,
            EntityReaders delegate) {

        try {
            readersLock.readLock().lock();
            for (ReaderEntry<EntityReader<?>> readerEntry : readers) {
                if (readerEntry.readerClass.equals(readerClass)){
                    return (EntityReader<T>) readerEntry.reader;
                }
            }
        } finally {
            readersLock.readLock().unlock();
        }
        if (this.delegate != null) {
            return this.delegate.getReader(readerClass, delegate);
        }
        return null;
    }

    @SuppressWarnings("unchecked")
    public <T> EntityStreamReader<T> getStreamReader(
            Class<? extends EntityStreamReader<T>> streamReaderClass,
            EntityReaders delegate) {

        try {
            streamReadersLock.readLock().lock();
            for (ReaderEntry<EntityStreamReader<?>> readerEntry
                    : streamReaders) {
                if (readerEntry.readerClass.equals(streamReaderClass)){
                    return (EntityStreamReader<T>) readerEntry.reader;
                }
            }
        } finally {
            streamReadersLock.readLock().unlock();
        }
        if (this.delegate != null) {
            return this.delegate.getStreamReader(streamReaderClass, delegate);
        }
        return null;
    }

    @SuppressWarnings("unchecked")
    public <T> EntityReader<T> selectReader(Class<T> type, ContentInfo info,
            EntityReaders delegate) {

        try {
            readersLock.readLock().lock();
            for (ReaderEntry<EntityReader<?>> readerEntry : readers) {
                if (readerEntry.reader.accept(type, info)){
                    return (EntityReader<T>) readerEntry.reader;
                }
            }
        } finally {
            readersLock.readLock().unlock();
        }
        if (this.delegate != null) {
            return this.delegate.selectReader(type, info, delegate);
        }
        return null;
    }

    @SuppressWarnings("unchecked")
    public <T> EntityReader<T> selectReader(GenericType<T> type,
            ContentInfo info, EntityReaders delegate) {

        try {
            readersLock.readLock().lock();
            for (ReaderEntry<EntityReader<?>> readerEntry : readers) {
                if (readerEntry.reader.accept(type, info)){
                    return (EntityReader<T>) readerEntry.reader;
                }
            }
        } finally {
            readersLock.readLock().unlock();
        }
        if (this.delegate != null) {
            return this.delegate.selectReader(type, info, delegate);
        }
        return null;
    }

    @SuppressWarnings("unchecked")
    public <T> EntityStreamReader<T> selectStreamReader(Class<T> type,
            ContentInfo info, EntityReaders delegate) {

        try {
            streamReadersLock.readLock().lock();
            for (ReaderEntry<EntityStreamReader<?>> readerEntry
                    : streamReaders) {
                if (readerEntry.reader.accept(type, info)){
                    return (EntityStreamReader<T>) readerEntry.reader;
                }
            }
        } finally {
            streamReadersLock.readLock().unlock();
        }
        if (this.delegate != null) {
            return this.delegate.selectStreamReader(type, info, delegate);
        }
        return null;
    }

    @SuppressWarnings("unchecked")
    public <T> EntityStreamReader<T> selectStreamReader(GenericType<T> type,
            ContentInfo info, EntityReaders delegate) {

        try {
            streamReadersLock.readLock().lock();
            for (ReaderEntry<EntityStreamReader<?>> readerEntry
                    : streamReaders) {
                if (readerEntry.reader.accept(type, info)){
                    return (EntityStreamReader<T>) readerEntry.reader;
                }
            }
        } finally {
            streamReadersLock.readLock().unlock();
        }
        if (this.delegate != null) {
            return this.delegate.selectStreamReader(type, info, delegate);
        }
        return null;
    }

    private static final class ReaderEntry<T> {

        final Class<?> readerClass;
        final T reader;

        ReaderEntry(Class<?> readerClass, T reader) {
            this.readerClass = readerClass;
            this.reader = reader;
        }
    }
}