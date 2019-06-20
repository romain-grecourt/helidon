package io.helidon.common.http;

import io.helidon.common.GenericType;
import io.helidon.common.reactive.Flow.Publisher;
import io.helidon.common.reactive.Flow.Subscriber;
import java.util.Objects;
import java.util.function.Function;
import java.util.function.Predicate;

/**
 * Out-bound reactive payload that can converted from entities.
 */
public final class OutBoundContent
        implements HttpContent, EntityWritersRegistry {

    private final Object entity;
    private final Publisher<Object> stream;
    private final Class<Object> type;
    private final GenericType<Object> gType;
    private final Publisher<DataChunk> publisher;
    private final OutBoundScope scope;
    private final EntityWriters writers;
    private final ContentInterceptor.Factory interceptorFactory;

    public OutBoundContent(Object entity, OutBoundScope scope,
            ContentInterceptor.Factory interceptorFactory) {

        Objects.requireNonNull(entity, "entity cannot be null!");
        Objects.requireNonNull(scope, "scope cannot be null!");
        this.publisher = null;
        this.stream = null;
        this.type = null;
        this.gType = null;
        this.entity = entity;
        this.scope = scope;
        this.writers = scope.writers();
        this.interceptorFactory = interceptorFactory;
    }

    public OutBoundContent(Object entity, OutBoundScope scope) {
        this(entity, scope, /* interceptorFactory */ null);
    }

    public OutBoundContent(Publisher<Object> stream,
            Class<? extends Object> type, OutBoundScope scope,
            ContentInterceptor.Factory interceptorFactory) {

        Objects.requireNonNull(stream, "stream cannot be null!");
        Objects.requireNonNull(type, "type cannot be null!");
        Objects.requireNonNull(scope, "scope cannot be null!");
        this.stream = stream;
        this.type = (Class<Object>) type;
        this.gType = null;
        this.publisher = null;
        this.entity = null;
        this.scope = scope;
        this.writers = scope.writers();
        this.interceptorFactory = interceptorFactory;
    }

    public OutBoundContent(Publisher<Object> stream,
            Class<? extends Object> type, OutBoundScope scope) {

        this(stream, type, scope, /* interceptorFactory */ null);
    }

    public OutBoundContent(Publisher<Object> stream,
            GenericType<? extends Object> type, OutBoundScope scope,
            ContentInterceptor.Factory interceptorFactory) {

        Objects.requireNonNull(stream, "stream cannot be null!");
        Objects.requireNonNull(type, "type cannot be null!");
        Objects.requireNonNull(scope, "scope cannot be null!");
        this.stream = stream;
        this.gType = (GenericType<Object>) type;
        this.entity = null;
        this.publisher = null;
        this.type = null;
        this.scope = scope;
        this.writers = scope.writers();
        this.interceptorFactory = interceptorFactory;
    }

    public OutBoundContent(Publisher<Object> stream,
            GenericType<? extends Object> type, OutBoundScope scope) {

        this(stream, type, scope, /* interceptorFactory */ null);
    }

    public OutBoundContent(Publisher<DataChunk> publisher, OutBoundScope scope,
            ContentInterceptor.Factory interceptorFactory) {

        Objects.requireNonNull(publisher, "publisher cannot be null!");
        Objects.requireNonNull(scope, "scope cannot be null!");
        this.publisher = publisher;
        this.entity = null;
        this.stream = null;
        this.type = null;
        this.gType = null;
        this.scope = scope;
        this.writers = scope.writers();
        this.interceptorFactory = interceptorFactory;
    }

    public OutBoundContent(Publisher<DataChunk> publisher,
            OutBoundScope scope) {

        this(publisher, scope, /* interceptorFactory */ null);
    }

    OutBoundScope scope() {
        return scope;
    }

    @Override
    public void subscribe(Subscriber<? super DataChunk> subscriber) {
        toPublisher(null, null).subscribe(subscriber);
    }

    @SuppressWarnings("unchecked")
    public Publisher<DataChunk> toPublisher(EntityWriters fallback,
            HashParameters headers) {

        Publisher<DataChunk> pub;
        ContentInterceptor.Factory ifac = null;
        if (publisher != null) {
            pub = publisher;
            ifac = interceptorFactory;
        } else {
            if (entity != null) {
                pub = writers.marshall(entity, scope, fallback, headers, ifac);
                if (interceptorFactory != null) {
                    ifac = interceptorFactory
                            .forType(entity.getClass().getTypeName());
                }
            } else {
                if (type != null) {
                    pub = writers.marshallStream(stream, type, scope, fallback,
                            headers, ifac);
                    if (interceptorFactory != null) {
                        ifac = interceptorFactory.forType(type.getTypeName());
                    }
                } else {
                    pub = writers.marshallStream(stream, gType, scope, fallback,
                            headers, ifac);
                    if (interceptorFactory != null) {
                        ifac = interceptorFactory.forType(gType.getTypeName());
                    }
                }
            }
        }
        return writers.applyFilters(pub, ifac);
    }

    @Override
    public OutBoundContent registerFilter(ContentFilter filter) {
        writers.registerFilter(filter);
        return this;
    }

    @Override
    public OutBoundContent registerWriter(EntityWriter<?> writer) {
        writers.registerWriter(writer);
        return this;
    }

    @Override
    public OutBoundContent registerStreamWriter(EntityStreamWriter<?> writer) {
        writers.registerStreamWriter(writer);
        return this;
    }

    @Override
    public <T> EntityWritersRegistry registerWriter(Class<T> type,
            Function<T, Publisher<DataChunk>> function) {

        return writers.registerWriter(type, function);
    }

    @Override
    public <T> EntityWritersRegistry registerWriter(Predicate<?> predicate,
            Function<T, Publisher<DataChunk>> function) {

        return writers.registerWriter(predicate, function);
    }

    @Override
    public <T> EntityWritersRegistry registerWriter(Class<T> type,
            MediaType contentType,
            Function<? extends T, Publisher<DataChunk>> function) {

        return writers.registerWriter(type, contentType, function);
    }

    @Override
    public <T> EntityWritersRegistry registerWriter(Predicate<?> accept,
            MediaType contentType, Function<T, Publisher<DataChunk>> function) {

        return writers.registerWriter(accept, contentType, function);
    }
}
