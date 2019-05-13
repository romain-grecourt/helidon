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
package io.helidon.media.multipart;

import io.helidon.common.GenericType;
import io.helidon.common.http.Content;
import io.helidon.common.http.DataChunk;
import io.helidon.common.http.MediaType;
import io.helidon.common.http.Reader;
import io.helidon.common.reactive.Flow;
import io.helidon.common.reactive.Flow.Publisher;
import io.helidon.media.common.ContentWriters;
import io.helidon.webserver.Request;
import io.helidon.webserver.Request.InternalReader;
import io.helidon.webserver.Response;
import io.helidon.webserver.Response.Writer;
import io.helidon.webserver.ServerRequest;
import io.helidon.webserver.ServerResponse;
import java.io.File;
import java.io.IOException;
import java.nio.channels.FileChannel;
import java.nio.channels.ReadableByteChannel;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.function.Function;
import java.util.function.Predicate;

/**
 * Body part entity.
 */
public final class BodyPart {

    private final BodyPartContent content;
    private final BodyPartHeaders headers;

    private BodyPart(Object entity, BodyPartHeaders headers) {
        this.content = new EntityContent(entity);
        this.headers = headers;
    }

    private BodyPart(Publisher<DataChunk> publisher, BodyPartHeaders headers) {
        this.content = new RawContent(publisher);
        this.headers = headers;
    }

    void registerRequestReaders(ServerRequest request){
        content.registerReaders(
                ((Request.Content)request.content()).getReaders());
    }

    void registerResponseWriters(ServerResponse response){
        content.registerWriters(((Response)response).getWriters());
    }

    public Content content() {
        return content;
    }

    public BodyPartHeaders headers(){
        return headers;
    }

    public static <T> BodyPart create(T entity){
        return builder()
                .entity(entity)
                .build();
    }

    static BodyPart create(BodyPartHeaders headers,
            Publisher<DataChunk> content) {

        return builder()
                .headers(headers)
                .publisher(content)
                .build();
    }

    public static Builder builder(){
        return new Builder();
    }

    public static final class Builder
            implements io.helidon.common.Builder<BodyPart> {

        private Object entity;
        private BodyPartHeaders headers;
        private Publisher<DataChunk> publisher;

        public Builder entity(Object entity) {
            if (publisher != null) {
                throw new IllegalStateException(
                        "The body part content source is already set");
            }
            this.entity = entity;
            return this;
        }

        public Builder headers(BodyPartHeaders header) {
            this.headers = header;
            return this;
        }

        Builder publisher(Publisher<DataChunk> publisher) {
            if (entity != null) {
                throw new IllegalStateException(
                        "The body part content source is already set");
            }
            this.publisher = publisher;
            return this;
        }

        @Override
        public BodyPart build() {
            if (entity != null) {
                return new BodyPart(entity, headers);
            } else if (publisher != null) {
                return new BodyPart(publisher, headers);
            }
            throw new IllegalStateException(
                    "No body content source is set");
        }
    }

    static abstract class BodyPartContent implements Content {

        private final ArrayList<InternalReader> readers = new ArrayList<>();
        private final ReadWriteLock readersLock = new ReentrantReadWriteLock();
        protected final ArrayList<Writer> writers = new ArrayList<>();
        protected final ReadWriteLock writersLock = new ReentrantReadWriteLock();

        /**
         * Register writers needed to serialize the content.
         * @param writers writers to register
         */
        void registerWriters(List<Writer> writers){
            try {
                writersLock.writeLock().lock();
                this.writers.addAll(writers);
            } finally {
                writersLock.writeLock().unlock();
            }
        }

        /**
         * Register readers needed to de-serialize the content.
         * @param readers readers to register
         */
        void registerReaders(Deque<InternalReader<?>> readers){
            try {
                readersLock.writeLock().lock();
                this.readers.addAll(readers);
            } finally {
                readersLock.writeLock().unlock();
            }
        }

        /**
         * Get or create the content publisher
         * @return publisher
         */
        protected abstract Publisher<DataChunk> getOrCreatePublisher();

        @Override
        public void subscribe(Flow.Subscriber<? super DataChunk> subscriber) {
            getOrCreatePublisher().equals(subscriber);
        }

        @Override
        public void registerFilter(Function<Publisher<DataChunk>, Publisher<DataChunk>> function) {
            throw new UnsupportedOperationException();
        }

        @Override
        public <T> void registerReader(Class<T> type, Reader<T> reader) {
            registerReader(aClass -> aClass.isAssignableFrom(type), reader);
        }

        @Override
        public <T> void registerReader(Predicate<Class<?>> predicate, Reader<T> reader) {
            try {
                readersLock.writeLock().lock();
                readers.add(new InternalReader<>(predicate, reader));
            } finally {
                readersLock.writeLock().unlock();
            }
        }

        @Override
        public <T> CompletionStage<T> as(Class<T> type) {
            CompletionStage<T> result;
            try {
                readersLock.readLock().lock();
                result = (CompletionStage<T>) readerFor(type).apply(getOrCreatePublisher(), type);
            } catch (IllegalArgumentException e) {
                result = failedFuture(e);
            } catch (Exception e) {
                result = failedFuture(new IllegalArgumentException("Transformation failed!", e));
            } finally {
                readersLock.readLock().unlock();
            }
            return result;
        }

        @SuppressWarnings("unchecked")
        private <T> Reader<T> readerFor(final Class<T> type) {
            return (Reader<T>) readers.stream()
                                      .filter(reader -> reader.accept(type))
                                      .findFirst()
                                      .orElseThrow(() -> new IllegalArgumentException("No reader found for class: " + type));
        }

        private static CompletableFuture failedFuture(Throwable t) {
            CompletableFuture result = new CompletableFuture<>();
            result.completeExceptionally(t);
            return result;
        }

        @Override
        public <T> Publisher<T> asPublisherOf(Class<T> type) {
            throw new UnsupportedOperationException();
        }

        @Override
        public <T> Publisher<T> asPublisherOf(GenericType<T> type) {
            throw new UnsupportedOperationException();
        }

        @Override
        public <T> void registerStreamReader(Class<T> type,
                Function<Publisher<DataChunk>, Publisher<T>> reader) {

            throw new UnsupportedOperationException();
        }
    }

    private static final class EntityContent extends BodyPartContent {

        private final Object entity;
        private Publisher<DataChunk> publisher;

        EntityContent(Object entity) {
            this.entity = entity;
        }

        @Override
        protected Publisher<DataChunk> getOrCreatePublisher() {
            if (publisher != null) {
                return publisher;
            }
            try {
                writersLock.readLock().lock();
                Publisher<DataChunk> pub = null;
                for (int i = writers.size() - 1; i >= 0; i--) {
                    Writer<Object> writer = writers.get(i);
                    if (writer.accept(entity)) {
                        pub = writer.getFunction().apply(entity);
                    }
                }
                if (pub == null) {
                    pub = createDefaultPublisher(entity);
                }
                if (pub == null) {
                    throw new IllegalStateException(
                            "Cannot write! No registered stream writer for '"
                            + entity.getClass().toString() + "'.");
                }
                publisher = pub;
                return publisher;
            } finally {
                writersLock.readLock().unlock();
            }
        }

        private Flow.Publisher<DataChunk> createDefaultPublisher(Object content) {
            final Class<?> type = content.getClass();
            if (File.class.isAssignableFrom(type)) {
                return toPublisher(((File) content).toPath());
            } else if (Path.class.isAssignableFrom(type)) {
                return toPublisher((Path) content);
            } else if (ReadableByteChannel.class.isAssignableFrom(type)) {
                return ContentWriters.byteChannelWriter().apply((ReadableByteChannel) content);
            } else if (CharSequence.class.isAssignableFrom(type)) {
                return toPublisher((CharSequence) content);
            } else if (byte[].class.isAssignableFrom(type)) {
                return ContentWriters.byteArrayWriter(true).apply((byte[]) content);
            }
            return null;
        }

        private Flow.Publisher<DataChunk> toPublisher(CharSequence s) {
            MediaType mediaType = MediaType.TEXT_PLAIN;
            String charset = mediaType.charset().orElse(StandardCharsets.UTF_8.name());
            return ContentWriters.charSequenceWriter(Charset.forName(charset)).apply(s);
        }

        private Flow.Publisher<DataChunk> toPublisher(Path path) {
            // Set response length - if possible
            try {
                // Is it existing and readable file
                if (!Files.exists(path)) {
                    throw new IllegalArgumentException("File path argument doesn't exist!");
                }
                if (!Files.isRegularFile(path)) {
                    throw new IllegalArgumentException("File path argument isn't a file!");
                }
                if (!Files.isReadable(path)) {
                    throw new IllegalArgumentException("File path argument isn't readable!");
                }
                // Try to write length
//            try {
//                bodyPart.headers.contentLength(Files.size(path));
//            } catch (Exception e) {
//                // Cannot get length or write length, not a big deal
//            }
                // And write
                FileChannel fc = FileChannel.open(path, StandardOpenOption.READ);
                return ContentWriters.byteChannelWriter().apply(fc);
            } catch (IOException e) {
                throw new IllegalArgumentException("Cannot read a file!", e);
            }
        }
    }

    private static final class RawContent extends BodyPartContent {

        private final Publisher<DataChunk> publisher;

        RawContent(Publisher<DataChunk> publisher) {
            this.publisher = publisher;
        }

        @Override
        protected Publisher<DataChunk> getOrCreatePublisher() {
            return publisher;
        }
    }
}
