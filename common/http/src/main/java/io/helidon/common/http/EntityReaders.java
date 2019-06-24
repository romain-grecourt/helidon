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

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.function.Predicate;
import java.util.Objects;
import io.helidon.common.GenericType;
import io.helidon.common.http.ContentOperatorRegistry.ClassPredicate;
import io.helidon.common.reactive.FailedPublisher;
import io.helidon.common.reactive.Flow.Publisher;

/**
 * Entity readers support.
 */
public final class EntityReaders extends ContentFilters
        implements EntityReadersRegistry {

    private final ContentOperatorRegistry<EntityReader<?>> readers;
    private final ContentOperatorRegistry<EntityStreamReader<?>> sreaders;

    /**
     * Create a new instance.
     */
    public EntityReaders() {
        super();
        this.readers = new ContentOperatorRegistry<>();
        this.sreaders = new ContentOperatorRegistry<>();
    }

    /**
     * Create a new parented instance.
     * @param parent entity readers parent
     */
    public EntityReaders(EntityReaders parent) {
        super(parent);
        this.readers = new ContentOperatorRegistry<>(parent.readers);
        this.sreaders = new ContentOperatorRegistry<>(parent.sreaders);
    }

    @Override
    public EntityReaders registerStreamReader(EntityStreamReader<?> reader) {
        sreaders.registerFirst(reader);
        return this;
    }

    @Override
    public EntityReaders registerReader(EntityReader<?> reader) {
        readers.registerFirst(reader);
        return this;
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

    public <T> CompletionStage<T> unmarshall(Publisher<DataChunk> publisher,
            Class<T> type, InBoundScope scope, EntityReaders fallback,
            ContentInterceptor.Factory ifc) {

        try {
            ReaderPredicate predicate = new ReaderPredicate(type, scope);
            ContentOperatorRegistry<EntityReader<?>> readersFb;
            if (fallback != null) {
                readersFb = fallback.readers;
            } else {
                readersFb = null;
            }
            EntityReader<T> reader = (EntityReader<T>) readers
                    .select(predicate, readersFb);
            if (reader == null) {
                throw new IllegalArgumentException("No reader found for type: "
                        + type.getTypeName());
            }
            ifc = ContentInterceptor.Factory.forType(ifc, type.getTypeName());
            Publisher<DataChunk> pub = applyFilters(publisher, ifc);
            return (CompletionStage<T>) reader.readEntity(pub, type, scope);
        } catch (Throwable ex) {
            return transformationFailed(ex);
        }
    }

    public <T> CompletionStage<T> unmarshall(Publisher<DataChunk> publisher,
            Class<? extends EntityReader<T>> readerClass, Class<T> type,
            InBoundScope scope, EntityReaders fallback,
            ContentInterceptor.Factory ifc) {

        try {
            ClassPredicate predicate = new ClassPredicate(readerClass);
            ContentOperatorRegistry<EntityReader<?>> readersFb;
            if (fallback != null) {
                readersFb = fallback.readers;
            } else {
                readersFb = null;
            }
            EntityReader<T> reader = (EntityReader<T>) readers
                    .select(predicate, readersFb);
            if (reader == null) {
                throw new IllegalArgumentException("No reader found of class: "
                        + readerClass.getTypeName());
            }
            ifc = ContentInterceptor.Factory.forType(ifc, type.getTypeName());
            Publisher<DataChunk> pub = applyFilters(publisher, ifc);
            return (CompletionStage<T>) reader.readEntity(pub, type, scope);
        } catch (Throwable ex) {
            return transformationFailed(ex);
        }
    }

    public <T> CompletionStage<T> unmarshall(Publisher<DataChunk> publisher,
            GenericType<T> gtype, InBoundScope scope,
            EntityReaders fallback, ContentInterceptor.Factory ifc) {

        try {
            ReaderPredicate predicate = new ReaderPredicate(gtype, scope);
            ContentOperatorRegistry<EntityReader<?>> readersFb;
            if (fallback != null) {
                readersFb = fallback.readers;
            } else {
                readersFb = null;
            }
            EntityReader<T> reader = (EntityReader<T>) readers
                    .select(predicate, readersFb);
            if (reader == null) {
                throw new IllegalArgumentException("No reader found for type: "
                        + gtype.getTypeName());
            }
            ifc = ContentInterceptor.Factory.forType(ifc, gtype.getTypeName());
            Publisher<DataChunk> pub = applyFilters(publisher, ifc);
            return (CompletionStage<T>) reader.readEntity(pub, gtype, scope);
        } catch (Throwable ex) {
            return transformationFailed(ex);
        }
    }

    public <T> CompletionStage<T> unmarshall(Publisher<DataChunk> publisher,
            Class<? extends EntityReader<T>> readerClass, GenericType<T> gtype,
            InBoundScope scope, EntityReaders fallback,
            ContentInterceptor.Factory ifc) {

        try {
            ClassPredicate predicate = new ClassPredicate(readerClass);
            ContentOperatorRegistry<EntityReader<?>> readersFb;
            if (fallback != null) {
                readersFb = fallback.readers;
            } else {
                readersFb = null;
            }
            EntityReader<T> reader = (EntityReader<T>) readers
                    .select(predicate, readersFb);
            if (reader == null) {
                throw new IllegalArgumentException("No reader found of class: "
                        + readerClass.getTypeName());
            }
            ifc = ContentInterceptor.Factory.forType(ifc, gtype.getTypeName());
            Publisher<DataChunk> pub = applyFilters(publisher, ifc);
            return (CompletionStage<T>) reader.readEntity(pub, gtype, scope);
        } catch (Throwable ex) {
            return transformationFailed(ex);
        }
    }

    public <T> Publisher<T> unmarshallStream(Publisher<DataChunk> publisher,
            Class<T> type, InBoundScope scope, EntityReaders fallback,
            ContentInterceptor.Factory ifc) {

        try {
            ReaderPredicate predicate = new ReaderPredicate(type, scope);
            ContentOperatorRegistry<EntityStreamReader<?>> readersFb;
            if (fallback != null) {
                readersFb = fallback.sreaders;
            } else {
                readersFb = null;
            }
            EntityStreamReader<T> reader = (EntityStreamReader<T>) sreaders
                    .select(predicate, readersFb);
            if (reader == null) {
                throw new IllegalArgumentException(
                        "No stream reader found for type: " + type.getTypeName());
            }
            ifc = ContentInterceptor.Factory.forType(ifc, type.getTypeName());
            Publisher<DataChunk> pub = applyFilters(publisher, ifc);
            return (Publisher<T>) reader.readEntityStream(pub, type, scope);
        } catch (Throwable ex) {
            return streamTransformationFailed(ex);
        }
    }

    public <T> Publisher<T> unmarshallStream(Publisher<DataChunk> publisher,
            Class<? extends EntityStreamReader<T>> readerClass, Class<T> type,
            InBoundScope scope, EntityReaders fallback,
            ContentInterceptor.Factory ifc) {

        try {
            ClassPredicate predicate = new ClassPredicate(readerClass);
            ContentOperatorRegistry<EntityStreamReader<?>> readersFb;
            if (fallback != null) {
                readersFb = fallback.sreaders;
            } else {
                readersFb = null;
            }
            EntityStreamReader<T> reader = (EntityStreamReader<T>) sreaders
                    .select(predicate, readersFb);
            if (reader == null) {
                throw new IllegalArgumentException(
                        "No stream reader found of class: "
                                + type.getTypeName());
            }
            ifc = ContentInterceptor.Factory.forType(ifc, type.getTypeName());
            Publisher<DataChunk> pub = applyFilters(publisher, ifc);
            return (Publisher<T>) reader.readEntityStream(pub, type, scope);
        } catch (Throwable ex) {
            return streamTransformationFailed(ex);
        }
    }

    public <T> Publisher<T> unmarshallStream(Publisher<DataChunk> publisher,
            GenericType<T> gtype, InBoundScope scope, EntityReaders fallback,
            ContentInterceptor.Factory ifc) {

        try {
            ReaderPredicate predicate = new ReaderPredicate(gtype, scope);
            ContentOperatorRegistry<EntityStreamReader<?>> readersFb;
            if (fallback != null) {
                readersFb = fallback.sreaders;
            } else {
                readersFb = null;
            }
            EntityStreamReader<T> reader = (EntityStreamReader<T>) sreaders
                    .select(predicate, readersFb);
            if (reader == null) {
                throw new IllegalArgumentException(
                        "No stream reader found for type: "
                                + gtype.getTypeName());
            }
            ifc = ContentInterceptor.Factory.forType(ifc, gtype.getTypeName());
            Publisher<DataChunk> pub = applyFilters(publisher, ifc);
            return (Publisher<T>) reader.readEntityStream(pub, gtype, scope);
        } catch (Throwable ex) {
            return streamTransformationFailed(ex);
        }
    }

   public <T> Publisher<T> unmarshallStream(Publisher<DataChunk> publisher,
            Class<? extends EntityStreamReader<T>> readerClass,
            GenericType<T> gtype, InBoundScope scope, EntityReaders fallback,
            ContentInterceptor.Factory ifc) {

        try {
            ClassPredicate predicate = new ClassPredicate(readerClass);
            ContentOperatorRegistry<EntityStreamReader<?>> readersFb;
            if (fallback != null) {
                readersFb = fallback.sreaders;
            } else {
                readersFb = null;
            }
            EntityStreamReader<T> reader = (EntityStreamReader<T>) sreaders
                    .select(predicate, readersFb);
            if (reader == null) {
                throw new IllegalArgumentException(
                        "No stream reader found of class: "
                                + gtype.getTypeName());
            }
            ifc = ContentInterceptor.Factory.forType(ifc, gtype.getTypeName());
            Publisher<DataChunk> pub = applyFilters(publisher, ifc);
            return (Publisher<T>) reader.readEntityStream(pub, gtype, scope);
        } catch (Throwable ex) {
            return streamTransformationFailed(ex);
        }
    }

    /**
     * Created a failed future for a failed reader transformation.
     * @param ex exception raised
     * @return CompletableFuture
     */
    private static CompletableFuture transformationFailed(Throwable ex) {
        CompletableFuture failedFuture = new CompletableFuture();
        if (ex instanceof IllegalArgumentException) {
            failedFuture.completeExceptionally(ex);
        } else {
            failedFuture.completeExceptionally(
                    new IllegalStateException("Transformation failed!", ex));
        }
        return failedFuture;
    }

    /**
     * Create a failed publisher for a failed stream reader transformation.
     * @param ex exception raised
     * @return FailedPublisher
     */
    private static FailedPublisher streamTransformationFailed(Throwable ex) {
        if (ex instanceof IllegalArgumentException) {
            return new FailedPublisher(ex);
        } else {
            return new FailedPublisher<>(
                    new IllegalStateException("Transformation failed!", ex));
        }
    }

    /**
     * Selector of {@link EntityReader} or {@link EntityStreamReader}.
     * @param <T> entity type
     */
    private static final class ReaderPredicate<T>
            implements Predicate<ContentReader> {

        private final Class<?> type;
        private final GenericType<?> gtype;
        private final InBoundScope scope;

        ReaderPredicate(Class<?> type, InBoundScope scope) {
            Objects.requireNonNull(type, "type cannot be null");
            Objects.requireNonNull(scope, "scope cannot be null");
            this.type = type;
            this.gtype = null;
            this.scope = scope;
        }

        ReaderPredicate(GenericType<?> gtype, InBoundScope scope) {
            Objects.requireNonNull(gtype, "type cannot be null");
            Objects.requireNonNull(scope, "scope cannot be null");
            this.type = null;
            this.gtype = gtype;
            this.scope = scope;
        }

        @Override
        public boolean test(ContentReader reader) {
            if (type != null) {
                return reader.accept(type, scope);
            } else {
                return reader.accept(gtype, scope);
            }
        }
    }

    /**
     * Static predicate for composite readers registered with a given class.
     */
    private static final class TypePredicate implements Predicate<Class<?>> {

        private final Class<?> clazz;

        TypePredicate(Class<?> clazz) {
            this.clazz = clazz;
        }

        @Override
        public boolean test(Class<?> cls) {
            return cls.isAssignableFrom(clazz);
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
}