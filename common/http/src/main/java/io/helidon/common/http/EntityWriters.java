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
import java.util.function.Function;
import java.util.function.Predicate;

/**
 * Entity writers support.
 */
public final class EntityWriters extends ContentFilters
        implements EntityWritersRegistry {

    private final ContentOperatorRegistry<EntityWriter<?>> writers;
    private final ContentOperatorRegistry<EntityStreamWriter<?>> swriters;

    public EntityWriters() {
        super();
        this.writers = new ContentOperatorRegistry<>();
        this.swriters = new ContentOperatorRegistry<>();
    }

    public EntityWriters(EntityWriters parent) {
        super();
        this.writers = new ContentOperatorRegistry<>(parent.writers);
        this.swriters = new ContentOperatorRegistry<>(parent.swriters);
    }

    @Override
    public EntityWriters registerWriter(EntityWriter<?> writer) {
        writers.registerFirst(writer);
        return this;
    }

    @Override
    public EntityWriters registerStreamWriter(EntityStreamWriter<?> writer) {
        swriters.registerFirst(writer);
        return this;
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
    public <T> Publisher<DataChunk> marshall(T entity, OutBoundScope scope,
            EntityWriters fallback, HashParameters headers,
            ContentInterceptor.Factory ifc) {

        WriterPredicate predicate = new WriterPredicate(entity, scope);
        ContentOperatorRegistry<EntityWriter<?>> writersFb;
        if (fallback != null) {
            writersFb = fallback.writers;
        } else {
            writersFb = null;
        }
        EntityWriter<T> writer = writers.select(predicate, writersFb);
        if (writer == null) {
            throw new IllegalArgumentException(
                    "No writer found for class: "
                    + entity.getClass().getTypeName());
        }
        predicate.ack.processHeaders(headers);
        Publisher<DataChunk> publisher = writer.writeEntity(entity, scope);
        if (publisher == null) {
            publisher = new EmptyPublisher<>();
        }
        String type = entity.getClass().getTypeName();
        ifc = ContentInterceptor.Factory.forType(ifc, type);
        return applyFilters(publisher, ifc);
    }

    @SuppressWarnings("unchecked")
    public <T> Publisher<DataChunk> marshall(
            Class<? extends EntityWriter<T>> writerCls, T entity,
            OutBoundScope scope, EntityWriters fallback, HashParameters headers,
            ContentInterceptor.Factory ifc) {

        WriterClassPredicate predicate =
                new WriterClassPredicate<>(writerCls, entity, scope);
        ContentOperatorRegistry<EntityWriter<?>> writersFb;
        if (fallback != null) {
            writersFb = fallback.writers;
        } else {
            writersFb = null;
        }
        EntityWriter<T> writer = writers.select(predicate, writersFb);
        if (writer == null) {
            throw new IllegalArgumentException(
                    "No writer found of type: "
                    + entity.getClass().getTypeName());
        }
        predicate.ack.processHeaders(headers);
        Publisher<DataChunk> publisher = writer.writeEntity(entity, scope);
        if (publisher == null) {
            publisher = new EmptyPublisher<>();
        }
        String type = entity.getClass().getTypeName();
        ifc = ContentInterceptor.Factory.forType(ifc, type);
        return applyFilters(publisher, ifc);
    }

    @SuppressWarnings("unchecked")
    public <T> Publisher<DataChunk> marshall(T entity, GenericType<T> gtype,
            OutBoundScope scope, EntityWriters fallback, HashParameters headers,
            ContentInterceptor.Factory ifc) {

        WriterPredicate predicate = new WriterPredicate(entity, gtype, scope);
        ContentOperatorRegistry<EntityWriter<?>> writersFb;
        if (fallback != null) {
            writersFb = fallback.writers;
        } else {
            writersFb = null;
        }
        EntityWriter<T> writer = writers.select(predicate, writersFb);
        if (writer == null) {
            throw new IllegalArgumentException(
                    "No writer found for class: "
                    + entity.getClass().getTypeName());
        }
        predicate.ack.processHeaders(headers);
        Publisher<DataChunk> publisher = writer.writeEntity(entity, scope);
        if (publisher == null) {
            publisher = new EmptyPublisher<>();
        }
        ifc = ContentInterceptor.Factory.forType(ifc, gtype.getTypeName());
        return applyFilters(publisher, ifc);
    }

    @SuppressWarnings("unchecked")
    public <T> Publisher<DataChunk> marshall(
            Class<? extends EntityWriter<T>> writerCls, T entity,
            GenericType<T> gtype, OutBoundScope scope, EntityWriters fallback,
            HashParameters headers, ContentInterceptor.Factory ifc) {

        WriterClassPredicate predicate =
                new WriterClassPredicate<>(writerCls, entity, gtype, scope);
        ContentOperatorRegistry<EntityWriter<?>> writersFb;
        if (fallback != null) {
            writersFb = fallback.writers;
        } else {
            writersFb = null;
        }
        EntityWriter<T> writer = writers.select(predicate, writersFb);
        if (writer == null) {
            throw new IllegalArgumentException(
                    "No writer found of type: "
                    + entity.getClass().getTypeName());
        }
        predicate.ack.processHeaders(headers);
        Publisher<DataChunk> publisher = writer.writeEntity(entity, scope);
        if (publisher == null) {
            publisher = new EmptyPublisher<>();
        }
        ifc = ContentInterceptor.Factory.forType(ifc, gtype.getTypeName());
        return applyFilters(publisher, ifc);
    }

    @SuppressWarnings("unchecked")
    public <T> Publisher<DataChunk> marshallStream(Publisher<T> stream,
            Class<T> type, OutBoundScope scope, EntityWriters fallback,
            HashParameters headers, ContentInterceptor.Factory ifc) {

        WriterPredicate predicate = new WriterPredicate(stream, type, scope);
        ContentOperatorRegistry<EntityStreamWriter<?>> writersFb;
        if (fallback != null) {
            writersFb = fallback.swriters;
        } else {
            writersFb = null;
        }
        EntityStreamWriter<T> writer = swriters.select(predicate, writersFb);
        if (writer == null) {
            throw new IllegalArgumentException(
                    "No writer found for class: "
                    + type.getTypeName());
        }
        predicate.ack.processHeaders(headers);
        Publisher<DataChunk> publisher = writer.writeEntityStream(stream, type,
                scope);
        if (publisher == null) {
            publisher = new EmptyPublisher<>();
        }
        ifc = ContentInterceptor.Factory.forType(ifc, type.getTypeName());
        return applyFilters(publisher, ifc);
    }

    @SuppressWarnings("unchecked")
    public <T> Publisher<DataChunk> marshallStream(
            Class<? extends EntityStreamWriter<T>> writerCls,
            Publisher<T> stream, Class<T> type, OutBoundScope scope,
            EntityWriters fallback, HashParameters headers,
            ContentInterceptor.Factory ifc) {

        WriterClassPredicate predicate = new WriterClassPredicate(writerCls,
                stream, type, scope);
        ContentOperatorRegistry<EntityStreamWriter<?>> writersFb;
        if (fallback != null) {
            writersFb = fallback.swriters;
        } else {
            writersFb = null;
        }
        EntityStreamWriter<T> writer = swriters.select(predicate, writersFb);
        if (writer == null) {
            throw new IllegalArgumentException(
                    "No writer found of type: "
                    + type.getTypeName());
        }
        predicate.ack.processHeaders(headers);
        Publisher<DataChunk> publisher = writer.writeEntityStream(stream, type,
                scope);
        if (publisher == null) {
            publisher = new EmptyPublisher<>();
        }
        ifc = ContentInterceptor.Factory.forType(ifc, type.getTypeName());
        return applyFilters(publisher, ifc);
    }

    @SuppressWarnings("unchecked")
    public <T> Publisher<DataChunk> marshallStream(Publisher<T> stream,
            GenericType<T> type, OutBoundScope scope, EntityWriters fallback,
            HashParameters headers, ContentInterceptor.Factory ifc) {

        WriterPredicate predicate = new WriterPredicate(stream, type, scope);
        ContentOperatorRegistry<EntityStreamWriter<?>> writersFb;
        if (fallback != null) {
            writersFb = fallback.swriters;
        } else {
            writersFb = null;
        }
        EntityStreamWriter<T> writer = swriters.select(predicate, writersFb);
        if (writer == null) {
            throw new IllegalArgumentException(
                    "No writer found for class: "
                    + type.getTypeName());
        }
        predicate.ack.processHeaders(headers);
        Publisher<DataChunk> publisher = writer.writeEntityStream(stream, type,
                scope);
        if (publisher == null) {
            publisher = new EmptyPublisher<>();
        }
        ifc = ContentInterceptor.Factory.forType(ifc, type.getTypeName());
        return applyFilters(publisher, ifc);
    }

    @SuppressWarnings("unchecked")
    public <T> Publisher<DataChunk> marshallStream(
            Class<? extends EntityStreamWriter<T>> writerCls,
            Publisher<T> stream, GenericType<T> type, OutBoundScope scope,
            EntityWriters fallback, HashParameters headers,
            ContentInterceptor.Factory ifc) {

        WriterClassPredicate predicate = new WriterClassPredicate(writerCls,
                stream, type, scope);
        ContentOperatorRegistry<EntityStreamWriter<?>> writersFb;
        if (fallback != null) {
            writersFb = fallback.swriters;
        } else {
            writersFb = null;
        }
        EntityStreamWriter<T> writer = swriters.select(predicate, writersFb);
        if (writer == null) {
            throw new IllegalArgumentException(
                    "No writer found of type: "
                    + type.getTypeName());
        }
        predicate.ack.processHeaders(headers);
        Publisher<DataChunk> publisher = writer.writeEntityStream(stream, type,
                scope);
        if (publisher == null) {
            publisher = new EmptyPublisher<>();
        }
        ifc = ContentInterceptor.Factory.forType(ifc, type.getTypeName());
        return applyFilters(publisher, ifc);
    }

    /**
     * Predicate to select writer by class.
     */
    private static final class WriterClassPredicate<T extends ContentWriter<U>, U>
            extends WriterPredicate<T, U> {

        private final Class clazz;

        WriterClassPredicate(Class clazz, U entity, OutBoundScope scope) {
            super(entity, scope);
            Objects.requireNonNull(clazz, "class cannot be null!");
            this.clazz = clazz;
        }

        WriterClassPredicate(Class clazz, U entity, Class<?> type,
                OutBoundScope scope) {

            super(entity, type, scope);
            this.clazz = clazz;
        }

        WriterClassPredicate(Class clazz, U entity, GenericType<?> gtype,
                OutBoundScope scope) {

            super(entity, gtype, scope);
            this.clazz = clazz;
        }

        @Override
        public boolean test(T writer) {
            if (clazz.equals(writer.getClass())) {
                return super.test(writer);
            }
            return false;
        }
    }

    /**
     * Predicate of {@link EntityWriter} or {@link EntityStreamWriter}.
     * @param <T> entity type
     */
    private static class WriterPredicate<T extends ContentWriter<U>, U>
            implements Predicate<T> {

        private final U entity;
        private final Class<?> type;
        private final GenericType<?> gtype;
        private final OutBoundScope scope;
        volatile ContentWriter.Ack ack;

        WriterPredicate(U entity, OutBoundScope scope) {
            Objects.requireNonNull(entity, "entity cannot be null");
            Objects.requireNonNull(scope, "scope cannot be null");
            this.entity = entity;
            this.type = entity.getClass();
            this.gtype = null;
            this.scope = scope;
        }

        WriterPredicate(U entity, Class<?> type, OutBoundScope scope) {
            Objects.requireNonNull(entity, "entity cannot be null");
            Objects.requireNonNull(scope, "scope cannot be null");
            this.entity = entity;
            this.type = type;
            this.gtype = null;
            this.scope = scope;
        }

        WriterPredicate(U entity, GenericType<?> gtype,
                OutBoundScope scope) {

            Objects.requireNonNull(entity, "entity cannot be null");
            Objects.requireNonNull(gtype, "type cannot be null");
            Objects.requireNonNull(scope, "scope cannot be null");
            this.entity = entity;
            this.type = null;
            this.gtype = gtype;
            this.scope = scope;
        }

        @Override
        public boolean test(T writer) {
            if (type != null) {
                ack = writer.accept(entity, type, scope);
            } else {
                ack = writer.accept(entity, gtype, scope);
            }
            return ack != null;
        }
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
        public Ack accept(Object entity, Class<?> type, OutBoundScope scope) {
            if (predicate.test(entity)) {
                return new Ack(contentType);
            }
            return null;
        }

        @Override
        public Publisher<DataChunk> writeEntity(T entity, OutBoundScope scope) {
            return function.apply(entity);
        }
    }
}
