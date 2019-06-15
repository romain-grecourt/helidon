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

import io.helidon.common.http.Content;
import io.helidon.common.http.ContentInfo;
import io.helidon.common.http.DataChunk;
import io.helidon.common.http.MediaType;
import io.helidon.common.reactive.Flow.Publisher;
import io.helidon.common.reactive.Flow.Subscriber;
import io.helidon.common.reactive.Flow.Subscription;
import io.helidon.webserver.Response;
import io.helidon.webserver.Routing;
import io.helidon.webserver.ServerRequest;
import io.helidon.webserver.ServerResponse;
import java.util.Collection;
import java.util.Iterator;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import io.helidon.common.http.EntityReader;
import io.helidon.common.http.EntityReaders;
import io.helidon.common.http.EntityStreamReader;
import io.helidon.common.http.EntityWriter;
import io.helidon.common.http.EntityStreamWriter;
import io.helidon.common.http.EntityStreamWriter.Promise;
import io.helidon.common.http.EntityWriters;
import io.helidon.common.http.InBoundContext;
import io.helidon.media.common.MediaSupportBase;
import java.nio.charset.Charset;
import java.util.List;

/**
 * Multi part support service.
 */
public final class MultiPartSupport extends MediaSupportBase {

    /**
     * The default boundary used for encoding multipart messages.
     */
    private static final String DEFAULT_BOUNDARY = 
            "[^._.^]==>boundary<==[^._.^]";

    /**
     * Force the use of {@link #create()}.
     */
    private MultiPartSupport(){
    }

    /**
     * Create a new instance of {@link MultiPartSupport}.
     * @return MultiPartSupport
     */
    public static MultiPartSupport create(){
        return new MultiPartSupport();
    }

    @Override
    protected void registerWriters(EntityWriters writers) {
        writers.registerWriter(new MultiPartWriter(DEFAULT_BOUNDARY, writers));
        writers.registerStreamWriter(new BodyPartStreamWriter(DEFAULT_BOUNDARY,
                writers));
    }

    @Override
    protected void registerReaders(EntityReaders readers) {
        readers.registerReader(new MultiPartReader(null, readers));
    }

    /**
     * {@link BodyPart} stream writer.
     */
    private static final class BodyPartStreamWriter
            implements EntityStreamWriter<OutBoundBodyPart> {

        private final String boundary;
        private final EntityWriters writers;

        BodyPartStreamWriter(String boundary, EntityWriters writers) {
            this.boundary = boundary;
            this.writers = writers;
        }

        @Override
        public Promise accept(Class<?> type, List<MediaType> acceptedTypes) {
            if (OutBoundBodyPart.class.isAssignableFrom(type)) {
                return new Promise<>(new ContentInfo(
                        MediaType.MULTIPART_FORM_DATA), this);
            }
            return null;
        }

        @Override
        public Publisher<DataChunk> writeEntityStream(
                Publisher<OutBoundBodyPart> parts, Class<OutBoundBodyPart> type,
                ContentInfo info, List<MediaType> acceptedTypes,
                Charset defaultCharset) {

            MultiPartEncoder encoder = new MultiPartEncoder(boundary, writers);
            parts.subscribe(encoder);
            return encoder;
        }
    }

    /**
     * {@link MultiPart} entity writer.
     */
    private static final class MultiPartWriter
            implements EntityWriter<OutBoundMultiPart> {

        private final String boundary;
        private final EntityWriters writers;

        MultiPartWriter(String boundary, EntityWriters writers) {
            this.boundary = boundary;
            this.writers = writers;
        }

        @Override
        public Promise accept(Object entity, List<MediaType> acceptedTypes) {
            if (OutBoundMultiPart.class.isAssignableFrom(entity.getClass())) {
                return new Promise<>(new ContentInfo(
                        MediaType.MULTIPART_FORM_DATA), this);
            }
            return null;
        }

        @Override
        public Publisher<DataChunk> writeEntity(OutBoundMultiPart multiPart,
                ContentInfo info, List<MediaType> acceptedTypes,
                Charset defaultCharset) {

            MultiPartEncoder encoder = new MultiPartEncoder(boundary,
                    writers);
            new BodyPartPublisher<>(multiPart.bodyParts()).subscribe(encoder);
            return encoder;
        }
    }

    /**
     * A reactive publisher of {@link BodyPart} that publishes all the items
     * of a given {@code Collection<BodyPart>}.
     */
    static final class BodyPartPublisher<T extends BodyPart>
            implements Publisher<T> {

        private final Collection<T> bodyParts;

        BodyPartPublisher(Collection<T> bodyParts) {
            this.bodyParts = bodyParts;
        }

        @Override
        public void subscribe(Subscriber<? super T> subscriber) {
            final Iterator<T> bodyPartsIt = bodyParts.iterator();
            subscriber.onSubscribe(new Subscription() {

                volatile boolean canceled = false;

                @Override
                public void request(long n) {
                    if (canceled) {
                        subscriber.onError(new IllegalStateException(
                                "Subscription canceled"));
                        return;
                    }
                    while (bodyPartsIt.hasNext() && --n >= 0) {
                        subscriber.onNext(bodyPartsIt.next());
                    }
                    if (!bodyPartsIt.hasNext()) {
                        subscriber.onComplete();
                    }
                }

                @Override
                public void cancel() {
                    canceled = true;
                }
            });
        }
    }

    /**
     * {@link BodyPart} stream reader.
     */
    private static final class BodyPartStreamReader
            implements EntityStreamReader<InBoundBodyPart> {

        private final InBoundContext context;
        private final EntityReaders readers;

        BodyPartStreamReader(InBoundContext context, EntityReaders readers) {
            this.context = context;
            this.readers = readers;
        }

        @Override
        public boolean accept(Class<?> type, ContentInfo info) {
            return BodyPart.class.isAssignableFrom(type);
        }

        @Override
        public Publisher<? extends InBoundBodyPart> readEntityStream(
                Publisher<DataChunk> chunks,
                Class<? super InBoundBodyPart> type,ContentInfo info,
                Charset defaultCharset) {

            MultiPartDecoder decoder = new MultiPartDecoder(readers, context);
            chunks.subscribe(decoder);
            return decoder;
        }
    }

    /**
     * {@link MultiPart} entity reader.
     */
    private static final class MultiPartReader
            implements EntityReader<MultiPart> {

        private final InBoundContext context;
        private final EntityReaders readers;

        MultiPartReader(InBoundContext context, EntityReaders readers) {
            this.context = context;
            this.readers = readers;
        }

        @Override
        public boolean accept(Class<?> type, ContentInfo info) {
            return MultiPart.class.isAssignableFrom(type); 
        }

        @Override
        public CompletionStage<? extends MultiPart> readEntity(
                Publisher<DataChunk> chunks, Class<? super MultiPart> type,
                ContentInfo info, Charset defaultCharset) {

            MultiPartDecoder decoder = new MultiPartDecoder(readers, context);
            chunks.subscribe(decoder);
            BufferingBodyPartSubscriber bodyPartSubscriber =
                    new BufferingBodyPartSubscriber();
            decoder.subscribe(bodyPartSubscriber);
            CompletableFuture<MultiPart> future = new CompletableFuture<>();
            bodyPartSubscriber.getFuture().thenAccept(bodyParts -> {
                future.complete(new InBoundMultiPart(bodyParts));
            }).exceptionally((Throwable error) -> {
                future.completeExceptionally(error);
                return null;
            });
            return future;
        }
    }
}
