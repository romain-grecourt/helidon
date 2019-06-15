package io.helidon.common.http;

import io.helidon.common.GenericType;
import io.helidon.common.reactive.Flow.Publisher;
import io.helidon.common.reactive.Flow.Subscriber;
import java.nio.charset.Charset;
import java.util.List;
import java.util.Objects;

/**
 * Out-bound reactive payload that can converted from entities.
 */
public final class OutBoundContent
        implements HttpContent, EntityWritersRegistry {

    private final Object entity;
    private final Publisher<? extends Object> entityStream;
    private final Class<?> entityStreamType;
    private final GenericType<?> entityStreamGenericType;
    private final Publisher<DataChunk> publisher;
    private final EntityWriters writers;
    private final OutBoundContext context;

    public OutBoundContent(Object entity, EntityWriters writers,
        OutBoundContext context) {

        Objects.requireNonNull(entity, "entity cannot be null!");
        Objects.requireNonNull(context,
                "context cannot be null!");
        this.publisher = null;
        this.entityStream = null;
        this.entityStreamType = null;
        this.entityStreamGenericType = null;
        this.entity = entity;
        this.writers = writers;
        this.context = context;
    }

    public OutBoundContent(Publisher<Object> entityStream, Class type,
        EntityWriters writers, OutBoundContext context) {

        Objects.requireNonNull(entityStream, "entityStream cannot be null!");
        Objects.requireNonNull(type, "type cannot be null!");
        Objects.requireNonNull(context,
                "context cannot be null!");
        this.entityStream = entityStream;
        this.entityStreamType = type;
        this.entityStreamGenericType = null;
        this.publisher = null;
        this.entity = null;
        this.writers = writers;
        this.context = context;
    }

    public OutBoundContent(Publisher<Object> entityStream, GenericType type,
        EntityWriters writers, OutBoundContext context) {

        Objects.requireNonNull(entityStream, "entityStream cannot be null!");
        Objects.requireNonNull(type, "type cannot be null!");
        Objects.requireNonNull(context,
                "context cannot be null!");
        this.entityStream = entityStream;
        this.entityStreamGenericType = type;
        this.entity = null;
        this.publisher = null;
        this.entityStreamType = null;
        this.writers = writers;
        this.context = context;
    }

    public OutBoundContent(Publisher<DataChunk> publisher,
            EntityWriters writers, OutBoundContext context) {

        Objects.requireNonNull(publisher, "publisher cannot be null!");
        Objects.requireNonNull(context,
                "context cannot be null!");
        this.publisher = publisher;
        this.entity = null;
        this.entityStream = null;
        this.entityStreamType = null;
        this.entityStreamGenericType = null;
        this.writers = writers;
        this.context = context;
    }

    @Override
    public void subscribe(Subscriber<? super DataChunk> subscriber) {
        subscribe(subscriber, /* delegate */ null);
    }

    @SuppressWarnings("unchecked")
    void subscribe(Subscriber<? super DataChunk> subscriber,
            EntityWriters delegate) {

        Publisher<DataChunk> pub;
        if (publisher != null) {
            pub = publisher;
        } else {
            List<MediaType> acceptedTypes = context.acceptedTypes();
            Charset defaultCharset = context.defaultCharset();
            if (entity != null) {
                EntityWriter.Promise promise = writers
                        .selectWriter(entity, acceptedTypes, delegate);
                pub = promise.writer.writeEntity(entity, promise.info,
                        acceptedTypes, defaultCharset);
            } else {
                if (entityStreamType != null) {
                    EntityStreamWriter.Promise promise = writers
                        .selectStreamWriter(entityStreamType, acceptedTypes,
                                delegate);
                    pub = promise.writer.writeEntityStream(entityStream,
                            entityStreamType, promise.info, acceptedTypes,
                            defaultCharset);
                } else {
                    EntityStreamWriter.Promise promise = writers
                        .selectStreamWriter(entityStreamGenericType,
                                acceptedTypes, delegate);
                    pub = promise.writer.writeEntityStream(entityStream,
                            entityStreamGenericType, promise.info,
                            acceptedTypes, defaultCharset);
                }
            }
        }
        writers.applyFilters(pub, context)
                .subscribe(subscriber);
    }

    @Override
    public void registerFilter(ContentFilter filter) {
        writers.registerFilter(filter);
    }

    @Override
    public void registerWriter(EntityWriter<?> writer) {
        writers.registerWriter(writer);
    }

    @Override
    public void registerStreamWriter(EntityStreamWriter<?> streamWriter) {
        writers.registerStreamWriter(streamWriter);
    }
}
