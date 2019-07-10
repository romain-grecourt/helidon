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
import io.helidon.common.reactive.Flow.Publisher;
import io.helidon.common.reactive.Flow.Subscriber;
import io.helidon.media.common.CharSequenceWriter;
import io.helidon.media.common.MediaSupport;
import io.helidon.common.http.MessageBodyReadableContent;
import io.helidon.common.http.MessageBodyWriteableContent;
import io.helidon.common.reactive.Flow.Subscription;
import io.helidon.common.reactive.Mono;
import io.helidon.media.common.StringReader;
import org.junit.jupiter.api.Test;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.fail;

import static io.helidon.media.multipart.common.MultiPartDecoderTest.chunksPublisher;

/**
 * Tests {@link BodyPart}.
 */
public class BodyPartTest {

    static final MediaSupport MEDIA_SUPPORT = MediaSupport.createWithDefaults();
    static final Charset DEFAULT_CHARSET = StandardCharsets.UTF_8;

    @Test
    public void testContentFromPublisher() {
        InboundBodyPart bodyPart = InboundBodyPart.builder()
                .content(readableContent(CharSequenceWriter
                        .write(Mono.just("body part data"), DEFAULT_CHARSET)))
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
        Publisher<DataChunk> publisher = MessageBodyWriteableContent
                .of(OutboundBodyPart.create("body part data").content())
                .toPublisher(MEDIA_SUPPORT.writerContext());
        String result = StringReader.read(publisher, DEFAULT_CHARSET).block();
        assertThat(result, is(equalTo("body part data")));
    }

    @Test
    public void testBufferedPart() {
        InboundBodyPart bodyPart = InboundBodyPart.builder()
                .content(readableContent(
                        chunksPublisher("abc".getBytes())))
                .buffered()
                .build();
        assertThat(bodyPart.isBuffered(), is(equalTo(true)));
        assertThat(bodyPart.as(String.class), is(equalTo("abc")));
    }

    @Test
    public void testNonBufferedPart() {
        InboundBodyPart bodyPart = InboundBodyPart.builder()
                .content(readableContent(
                        chunksPublisher("abc".getBytes())))
                .build();
        assertThat(bodyPart.isBuffered(), is(equalTo(false)));
        assertThrows(IllegalStateException.class, () -> {
            bodyPart.as(String.class);
        });
    }

    @Test
    public void testBadBufferedPart() {
        InboundBodyPart bodyPart = InboundBodyPart.builder()
                .content(readableContent(new UncompletablePublisher(
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
            InboundBodyPart.builder().build();
        });
    }

    @Test
    public void testName() {
        OutboundBodyPart bodyPart = OutboundBodyPart.builder()
                .headers(OutboundBodyPartHeaders.builder()
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
        OutboundBodyPart bodyPart = OutboundBodyPart.builder()
                .headers(OutboundBodyPartHeaders.builder()
                        .contentDisposition(ContentDisposition.builder()
                                .filename("foo.txt")
                                .build())
                        .build())
                .entity("abc")
                .build();
        assertThat(bodyPart.filename(), is(equalTo("foo.txt")));
        assertThat(bodyPart.name(), is(nullValue()));
    }

    static MessageBodyReadableContent readableContent(
            Publisher<DataChunk> chunks) {

        return MessageBodyReadableContent.create(chunks,
                MEDIA_SUPPORT.readerContext());
    }

    /**
     * A publisher that never invokes {@link Subscriber#onComplete()}.
     */
    static class UncompletablePublisher implements Publisher<DataChunk> {

        private final Publisher<DataChunk> delegate;

        UncompletablePublisher(byte[]... chunksData) {
            delegate = chunksPublisher(chunksData);
        }

        @Override
        public void subscribe(Subscriber<? super DataChunk> subscriber) {
            delegate.subscribe(new Subscriber<DataChunk> () {
                @Override
                public void onSubscribe(Subscription subscription) {
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
