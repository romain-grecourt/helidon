package io.helidon.webserver.internal;

import io.helidon.common.GenericType;
import io.helidon.common.http.DataChunk;
import io.helidon.common.http.Filter;
import io.helidon.common.http.MediaType;
import io.helidon.common.http.Reader;
import io.helidon.common.http.StreamReader;
import io.helidon.common.reactive.Flow;
import io.helidon.common.reactive.Flow.Publisher;
import java.util.Objects;
import java.util.concurrent.CompletionStage;
import java.util.function.Predicate;

/**
 * Out-bound content. This content implementation only supports
 * {@link Flow.Publisher#subscribe(Flow.Subscriber)}. Every other method throws
 * {@link UnsupportedOperationException}. An instance can be created by passing
 * a {@link Flow.Publisher}, or by passing an entity to marshall with a content
 * type. When creating an instance from an entity a {@link Flow.Publisher} is
 * created on the first call to
 * {@link Flow.Publisher#subscribe(Flow.Subscriber)}, note that
 * {@link #mediaSupport} must be called before that, otherwise an
 * {@link IllegalStateException} is throws.
 *
 * @param <T> type of the marshalled entity
 */
public final class OutBoundContent<T> implements io.helidon.common.http.Content {

    /**
     * Out-bound media support used to marshall the entity.
     */
    private OutBoundMediaSupport mediaSupport;

    /**
     * The entity backing this content.
     */
    private final T entity;

    /**
     * The content type used to select the writer for marshalling, may be
     * {@code null}.
     */
    private final MediaType contentType;

    /**
     * The underlying publisher, either the result of the entity marshalling
     * or passed via the constructor.
     */
    private volatile Publisher<DataChunk> publisher;

    /**
     * Create a new out-bound content instance backed by an entity.
     *
     * @param entity entity to marshall
     * @param contentType content type to use for selecting the writer for
     * marshalling
     */
    public OutBoundContent(T entity, MediaType contentType) {
        Objects.requireNonNull(entity, "entity cannot be null!");
        this.entity = entity;
        this.contentType = contentType;
    }

    /**
     * Create a new out-bound content instance backed by a publisher.
     * @param publisher publisher backing this content
     */
    public OutBoundContent(Publisher<DataChunk> publisher) {
        Objects.requireNonNull(publisher, "publisher cannot be null!");
        this.publisher = publisher;
        this.entity = null;
        this.contentType = null;
    }

    /**
     * Set the media support.
     * @param mediaSupport out-bound media support
     * @return this instance
     */
    public OutBoundContent mediaSupport(OutBoundMediaSupport mediaSupport) {
        this.mediaSupport = mediaSupport;
        return this;
    }

    @Override
    public void subscribe(Flow.Subscriber<? super DataChunk> subscriber) {
        if (publisher == null) {
            if (mediaSupport == null) {
                throw new IllegalStateException(
                        "Cannot marshall entity without out-bound media support");
            }
            publisher = mediaSupport.marshall(entity, contentType);
        }
        publisher.subscribe(subscriber);
    }

    @Override
    public void registerFilter(Filter filter) {
        throw new UnsupportedOperationException();
    }

    @Override
    public <T> void registerReader(Class<T> type, Reader<T> reader) {
        throw new UnsupportedOperationException();
    }

    @Override
    public <T> void registerReader(Predicate<Class<T>> predicate,
            Reader<T> reader) {
        throw new UnsupportedOperationException();
    }

    @Override
    public <T> CompletionStage<T> as(Class<T> type) {
        throw new UnsupportedOperationException();
    }

    @Override
    public <T> Flow.Publisher<T> asPublisherOf(Class<T> type) {
        throw new UnsupportedOperationException();
    }

    @Override
    public <T> Flow.Publisher<T> asPublisherOf(GenericType<T> type) {
        throw new UnsupportedOperationException();
    }

    @Override
    public <T> void registerStreamReader(Class<T> type,
            StreamReader<T> reader) {
        throw new UnsupportedOperationException();
    }
}
