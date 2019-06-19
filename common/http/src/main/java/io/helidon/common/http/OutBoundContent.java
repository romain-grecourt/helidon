package io.helidon.common.http;

import io.helidon.common.GenericType;
import io.helidon.common.reactive.Flow.Publisher;
import io.helidon.common.reactive.Flow.Subscriber;
import java.util.Objects;

/**
 * Out-bound reactive payload that can converted from entities.
 */
public final class OutBoundContent
        implements HttpContent, EntityWritersRegistry {

    private final Object entity;
    private final Publisher<? extends Object> stream;
    private final Class<?> type;
    private final GenericType<?> genericType;
    private final Publisher<DataChunk> publisher;
    final OutBoundScope scope;
    private final ContentInterceptor.Factory interceptorFactory;

    public OutBoundContent(Object entity, OutBoundScope scope,
            ContentInterceptor.Factory interceptorFactory) {

        Objects.requireNonNull(entity, "entity cannot be null!");
        Objects.requireNonNull(scope, "scope cannot be null!");
        this.publisher = null;
        this.stream = null;
        this.type = null;
        this.genericType = null;
        this.entity = entity;
        this.scope = scope;
        this.interceptorFactory = interceptorFactory;
    }

    public OutBoundContent(Object entity, OutBoundScope scope) {
        this(entity, scope, /* interceptorFactory */ null);
    }

    public OutBoundContent(Publisher<Object> stream, Class<?> type,
        OutBoundScope scope, ContentInterceptor.Factory interceptorFactory) {

        Objects.requireNonNull(stream, "stream cannot be null!");
        Objects.requireNonNull(type, "type cannot be null!");
        Objects.requireNonNull(scope, "scope cannot be null!");
        this.stream = stream;
        this.type = type;
        this.genericType = null;
        this.publisher = null;
        this.entity = null;
        this.scope = scope;
        this.interceptorFactory = interceptorFactory;
    }

    public OutBoundContent(Publisher<Object> stream, Class<?> type,
            OutBoundScope scope) {

        this(stream, type, scope, /* interceptorFactory */ null);
    }

    public OutBoundContent(Publisher<Object> stream, GenericType<?> type,
        OutBoundScope scope, ContentInterceptor.Factory interceptorFactory) {

        Objects.requireNonNull(stream, "stream cannot be null!");
        Objects.requireNonNull(type, "type cannot be null!");
        Objects.requireNonNull(scope, "scope cannot be null!");
        this.stream = stream;
        this.genericType = type;
        this.entity = null;
        this.publisher = null;
        this.type = null;
        this.scope = scope;
        this.interceptorFactory = interceptorFactory;
    }

    public OutBoundContent(Publisher<Object> stream, GenericType<?> type,
            OutBoundScope scope) {

        this(stream, type, scope, /* interceptorFactory */ null);
    }

    public OutBoundContent(Publisher<DataChunk> publisher,
            OutBoundScope scope, ContentInterceptor.Factory interceptorFactory) {

        Objects.requireNonNull(publisher, "publisher cannot be null!");
        Objects.requireNonNull(scope, "scope cannot be null!");
        this.publisher = publisher;
        this.entity = null;
        this.stream = null;
        this.type = null;
        this.genericType = null;
        this.scope = scope;
        this.interceptorFactory = interceptorFactory;
    }

    public OutBoundContent(Publisher<DataChunk> publisher,
            OutBoundScope scope) {

        this(publisher, scope, /* interceptorFactory */ null);
    }

    @Override
    public void subscribe(Subscriber<? super DataChunk> subscriber) {
        subscribe(subscriber, /* delegate */ null);
    }

    @SuppressWarnings("unchecked")
    public void subscribe(Subscriber<? super DataChunk> subscriber,
            EntityWriters delegate) {

        Publisher<DataChunk> pub;
        ContentInterceptor.Factory ifac = null;
        if (publisher != null) {
            pub = publisher;
            ifac = interceptorFactory;
        } else {
            if (entity != null) {
                EntityWriter.Promise promise = scope.writers
                        .selectWriter(entity, scope, delegate);
                pub = promise.writer.writeEntity(entity, promise, scope);
                if (interceptorFactory != null) {
                    ifac = interceptorFactory.forType(
                            entity.getClass().getTypeName());
                }
            } else {
                if (type != null) {
                    EntityStreamWriter.Promise promise = scope.writers
                        .selectStreamWriter(type, scope, delegate);
                    pub = promise.writer.writeEntityStream(stream, type,
                            promise, scope);
                    if (interceptorFactory != null) {
                        ifac = interceptorFactory.forType(type.getTypeName());
                    }
                } else {
                    EntityStreamWriter.Promise promise = scope.writers
                        .selectStreamWriter(genericType, scope, delegate);
                    pub = promise.writer.writeEntityStream(stream,
                            genericType, promise, scope);
                    if (interceptorFactory != null) {
                        ifac = interceptorFactory.forType(
                                genericType.getTypeName());
                    }
                }
            }
        }
        scope.writers.applyFilters(pub, ifac).subscribe(subscriber);
    }

    @Override
    public OutBoundContent registerFilter(ContentFilter filter) {
        scope.writers.registerFilter(filter);
        return this;
    }

    @Override
    public OutBoundContent registerWriter(EntityWriter<?> writer) {
        scope.writers.registerWriter(writer);
        return this;
    }

    @Override
    public OutBoundContent registerStreamWriter(EntityStreamWriter<?> writer) {
        scope.writers.registerStreamWriter(writer);
        return this;
    }
}
