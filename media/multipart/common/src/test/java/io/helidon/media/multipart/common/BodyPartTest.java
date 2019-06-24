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
package io.helidon.media.multipart.common;

import java.nio.charset.StandardCharsets;
import java.nio.charset.Charset;
import java.util.concurrent.atomic.AtomicBoolean;
import io.helidon.common.http.DataChunk;
import io.helidon.common.http.EntityReaders;
import io.helidon.common.http.EntityWriters;
import io.helidon.common.http.InBoundContent;
import io.helidon.common.http.InBoundScope;
import io.helidon.common.http.OutBoundContent;
import io.helidon.common.http.ReadOnlyParameters;
import io.helidon.common.reactive.Flow;
import io.helidon.common.reactive.Flow.Publisher;
import io.helidon.common.reactive.Flow.Subscriber;
import io.helidon.media.common.CharSequenceWriter;
import io.helidon.media.common.MediaSupport;
import io.helidon.media.common.StringReader;
import io.helidon.media.multipart.common.MultiPartDecoderTest.DataChunkPublisher;
import org.junit.jupiter.api.Test;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.fail;

/**
 * Tests {@link BodyPart}.
 */
public class BodyPartTest {

    static final MediaSupport MEDIA_SUPPORT = MediaSupport.createWithDefaults();
    static final EntityReaders DEFAULT_READERS = MEDIA_SUPPORT.readers();
    static final EntityWriters DEFAULT_WRITERS = MEDIA_SUPPORT.writers();
    static final Charset DEFAULT_CHARSET = StandardCharsets.UTF_8;

    @Test
    public void testContentFromPublisher() {
        InBoundBodyPart bodyPart = InBoundBodyPart.builder()
                .content(inBoundContent(CharSequenceWriter
                        .write("body part data", DEFAULT_CHARSET)))
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
        Publisher<DataChunk> publisher = bodyPart.content()
                .toPublisher(DEFAULT_WRITERS, null);
        final AtomicBoolean acceptCalled = new AtomicBoolean(false);
        StringReader.read(publisher, DEFAULT_CHARSET)
                .thenAccept(str -> {
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
        InBoundBodyPart bodyPart = InBoundBodyPart.builder()
                .content(inBoundContent(
                        new DataChunkPublisher("abc".getBytes())))
                .buffered()
                .build();
        assertThat(bodyPart.isBuffered(), is(equalTo(true)));
        assertThat(bodyPart.as(String.class), is(equalTo("abc")));
    }

    @Test
    public void testNonBufferedPart() {
        InBoundBodyPart bodyPart = InBoundBodyPart.builder()
                .content(inBoundContent(
                        new DataChunkPublisher("abc".getBytes())))
                .build();
        assertThat(bodyPart.isBuffered(), is(equalTo(false)));
        assertThrows(IllegalStateException.class, () -> {
            bodyPart.as(String.class);
        });
    }

    @Test
    public void testBadBufferedPart() {
        InBoundBodyPart bodyPart = InBoundBodyPart.builder()
                .content(inBoundContent(new UncompletablePublisher(
                        "abc".getBytes(), "def".getBytes())))
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
                .headers(OutBoundBodyPartHeaders.builder()
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
        OutBoundBodyPart bodyPart = OutBoundBodyPart.builder()
                .headers(OutBoundBodyPartHeaders.builder()
                        .contentDisposition(ContentDisposition.builder()
                                .filename("foo.txt")
                                .build())
                        .build())
                .entity("abc")
                .build();
        assertThat(bodyPart.filename(), is(equalTo("foo.txt")));
        assertThat(bodyPart.name(), is(nullValue()));
    }

    static InBoundContent inBoundContent(Publisher<DataChunk> chunks) {
        return new InBoundContent(chunks, new InBoundScope(
                ReadOnlyParameters.empty(),
                DEFAULT_CHARSET, /* contentType */ null,
                DEFAULT_READERS));
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
