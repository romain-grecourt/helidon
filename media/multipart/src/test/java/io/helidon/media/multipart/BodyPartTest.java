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

import io.helidon.common.http.DataChunk;
import io.helidon.common.http.MediaType;
import io.helidon.common.reactive.Flow;
import io.helidon.common.reactive.Flow.Publisher;
import io.helidon.common.reactive.Flow.Subscriber;
import io.helidon.media.common.ContentReaders;
import io.helidon.media.common.ContentWriters;
import io.helidon.media.multipart.MultiPartDecoderTest.DataChunkPublisher;
import io.helidon.webserver.internal.InBoundContext;
import io.helidon.webserver.internal.InBoundMediaSupport;
import io.helidon.webserver.internal.OutBoundContent;
import io.helidon.webserver.internal.OutBoundContext;
import io.helidon.webserver.internal.OutBoundMediaSupport;
import io.opentracing.Span;
import io.opentracing.Tracer;
import java.nio.charset.Charset;
import java.util.concurrent.atomic.AtomicBoolean;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.fail;
import io.helidon.common.http.EntityReader;
import io.helidon.common.http.EntityWriter;

/**
 * Tests {@link BodyPart}.
 */
public class BodyPartTest {

    private static final EntityReader<String> STRING_READER =
            ContentReaders.stringReader(Charset.defaultCharset());

    private static final EntityReader<byte[]> BYTES_READER =
            ContentReaders.byteArrayReader();

    private static final EntityWriter<CharSequence> STRING_WRITER =
            ContentWriters.charSequenceWriter(Charset.defaultCharset());

    static final OutBoundMediaSupport OUTBOUND_MEDIA_SUPPORT =
            getOutBoundMediaSupport();

    static final InBoundMediaSupport INBOUND_MEDIA_SUPPORT =
            getInBoundMediaSupport();

    @Test
    public void testContentFromPublisher() {
        Publisher<DataChunk> publisher = STRING_WRITER
                .apply("body part data");
        InBoundBodyPart bodyPart = InBoundBodyPart.builder()
                .publisher(publisher, INBOUND_MEDIA_SUPPORT)
                .build();
        final AtomicBoolean acceptCalled = new AtomicBoolean(false);
        bodyPart.content().as(String.class).thenAccept(str -> {
            acceptCalled.set(true);
            assertThat(str, is(equalTo("body part data")));
        }).exceptionally((Throwable ex) -> {
            fail(ex);
            return null;
        });
        assertThat(acceptCalled.get(), is(equalTo(true)));
    }

    @Test
    public void testContentFromEntity() {
        OutBoundBodyPart bodyPart = OutBoundBodyPart.create("body part data");
//        Content content = bodyPart.content();
//        assertThat(content, is(instanceOf(OutBoundContent.class)));
//        ((OutBoundContent) content)
//                .mediaSupport(OUTBOUND_MEDIA_SUPPORT);
        final AtomicBoolean acceptCalled = new AtomicBoolean(false);
        STRING_READER.apply(bodyPart.content()).thenAccept(str -> {
            acceptCalled.set(true);
            assertThat(str, is(equalTo("body part data")));
        }).exceptionally((Throwable ex) -> {
            fail(ex);
            return null;
        });
        assertThat(acceptCalled.get(), is(equalTo(true)));
    }

    @Test
    public void testBufferedPart() {
        Publisher<DataChunk> publisher = new DataChunkPublisher(
                "abc".getBytes());
        InBoundBodyPart bodyPart = InBoundBodyPart.builder()
                .publisher(publisher, INBOUND_MEDIA_SUPPORT)
                .buffered()
                .build();
        assertThat(bodyPart.isBuffered(), is(equalTo(true)));
        assertThat(bodyPart.as(String.class), is(equalTo("abc")));
    }

    @Test
    public void testNonBufferedPart() {
        Publisher<DataChunk> publisher = new DataChunkPublisher(
                "abc".getBytes());
        InBoundBodyPart bodyPart = InBoundBodyPart.builder()
                .publisher(publisher, INBOUND_MEDIA_SUPPORT)
                .build();
        assertThat(bodyPart.isBuffered(), is(equalTo(false)));
        assertThrows(IllegalStateException.class, () -> {
            bodyPart.as(String.class);
        });
    }

    @Test
    public void testBadBufferedPart() {
        UncompletablePublisher publisher = new UncompletablePublisher(
                "abc".getBytes(), "def".getBytes());
        InBoundBodyPart bodyPart = InBoundBodyPart.builder()
                .publisher(publisher, INBOUND_MEDIA_SUPPORT)
                .buffered()
                .build();
        assertThat(bodyPart.isBuffered(), is(equalTo(true)));
        try {
            bodyPart.as(String.class);
            fail("exception should be thrown");
        } catch (IllegalStateException ex) {
            assertThat(ex.getMessage(),
                    is(equalTo("Unable to convert part content synchronously")));
        }
    }

    @Test
    public void testBuildingPartWithNoContent() {
        assertThrows(IllegalStateException.class, ()-> {
            InBoundBodyPart.builder().build();
        });
    }

    @Test
    public void testOutBoundPublisher() {
        Publisher<DataChunk> publisher = new DataChunkPublisher(
                "abc".getBytes());
        OutBoundBodyPart bodyPart = OutBoundBodyPart.builder()
                .publisher(publisher)
                .build();
        assertThat(bodyPart.content(), is(instanceOf(OutBoundContent.class)));
    }

    @Test
    public void testName() {
        OutBoundBodyPart bodyPart = OutBoundBodyPart.builder()
                .headers(BodyPartHeaders.builder()
                        .contentDisposition(ContentDisposition.builder()
                                .name("foo")
                                .build())
                        .build())
                .entity("abc")
                .build();
        assertThat(bodyPart.name(), is(equalTo("foo")));
        assertThat(bodyPart.filename(), is(nullValue()));
    }

    @Test
    public void testFilename() {
        BodyPart bodyPart = OutBoundBodyPart.builder()
                .headers(BodyPartHeaders.builder()
                        .contentDisposition(ContentDisposition.builder()
                                .filename("foo.txt")
                                .build())
                        .build())
                .entity("abc")
                .build();
        assertThat(bodyPart.filename(), is(equalTo("foo.txt")));
        assertThat(bodyPart.name(), is(nullValue()));
    }

    @SuppressWarnings("unchecked")
    private static OutBoundMediaSupport getOutBoundMediaSupport(){
        OutBoundMediaSupport mediaSupport = new OutBoundMediaSupport(
                new OutBoundContext() {
            @Override
            public void setContentType(MediaType mediaType) {
            }

            @Override
            public void setContentLength(long size) {
            }
        });
        mediaSupport.registerWriter(CharSequence.class, MediaType.TEXT_PLAIN,
                STRING_WRITER);
        return mediaSupport;
    }

    @SuppressWarnings("unchecked")
    private static InBoundMediaSupport getInBoundMediaSupport() {
        InBoundMediaSupport mediaSupport = new InBoundMediaSupport(
                mockInBoundContext());
        mediaSupport.registerReader(String.class, STRING_READER);
        mediaSupport.registerReader(byte[].class, BYTES_READER);
        return mediaSupport;
    }

    private static InBoundContext mockInBoundContext() {
        return new InBoundContext() {
            @Override
            public Tracer.SpanBuilder createSpanBuilder(String operationName) {
                Tracer.SpanBuilder spanBuilderMock
                        = Mockito.mock(Tracer.SpanBuilder.class);
                Span spanMock = Mockito.mock(Span.class);
                Mockito.doReturn(spanMock).when(spanBuilderMock).start();
                return spanBuilderMock;
            }

            @Override
            public Charset charset() {
                return Charset.defaultCharset();
            }
        };
    }

    /**
     * A publisher that never invokes {@link Subscriber#onComplete()}.
     */
    static class UncompletablePublisher extends DataChunkPublisher {

        UncompletablePublisher(byte[]... chunksData) {
            super(chunksData);
        }

        @Override
        public void subscribe(Subscriber<? super DataChunk> subscriber) {
            super.subscribe(new Subscriber<DataChunk> () {
                @Override
                public void onSubscribe(Flow.Subscription subscription) {
                    subscriber.onSubscribe(subscription);
                }

                @Override
                public void onNext(DataChunk item) {
                    subscriber.onNext(item);
                }

                @Override
                public void onError(Throwable error) {
                    subscriber.onError(error);
                }

                @Override
                public void onComplete() {
                }
            });
        }
    }
}
