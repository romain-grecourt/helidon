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

import java.util.concurrent.ExecutionException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import io.helidon.media.multipart.MultiPartSupport.BodyPartPublisher;
import io.helidon.media.common.ContentReaders;
import io.helidon.common.http.MediaType;
import io.helidon.media.multipart.MultiPartDecoderTest.DataChunkPublisher;
import io.helidon.media.multipart.MultiPartDecoderTest.DataChunkSubscriber;
import io.helidon.common.http.DataChunk;
import io.helidon.common.reactive.Flow.Publisher;
import io.helidon.common.reactive.Flow.Subscriber;
import org.junit.jupiter.api.Test;

import static io.helidon.common.CollectionsHelper.listOf;
import static io.helidon.media.multipart.BodyPartTest.INBOUND_MEDIA_SUPPORT;
import static io.helidon.media.multipart.BodyPartTest.OUTBOUND_MEDIA_SUPPORT;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.fail;

/**
 * Test {@link MultiPartEncoder}.
 */
public class MultiPartEncoderTest {

    // TODO test throttling

    @Test
    public void testEncodeOnePart() {
        String boundary = "boundary";
        String message = encodeParts(boundary,
                BodyPart.builder()
                        .entity("part1")
                        .build());
        assertThat(message, is(equalTo(
                "--" + boundary + "\r\n"
                + "\r\n"
                + "part1\n"
                + "--" + boundary + "--")));
    }

    @Test
    public void testEncodeOnePartWithHeaders() {
        String boundary = "boundary";
        String message = encodeParts(boundary,
                BodyPart.builder()
                        .headers(BodyPartHeaders.builder()
                                .contentType(MediaType.TEXT_PLAIN)
                                .build())
                        .entity("part1")
                        .build());
        assertThat(message, is(equalTo(
                "--" + boundary + "\r\n"
                + "Content-Type:text/plain\r\n"
                + "\r\n"
                + "part1\n"
                + "--" + boundary + "--")));
    }

    @Test
    public void testEncodeTwoParts() {
        String boundary = "boundary";
        String message = encodeParts(boundary,
                BodyPart.builder()
                        .entity("part1")
                        .build(),
                BodyPart.builder()
                        .entity("part2")
                        .build());
        assertThat(message, is(equalTo(
                "--" + boundary + "\r\n"
                + "\r\n"
                + "part1\n"
                + "--" + boundary + "\r\n"
                + "\r\n"
                + "part2\n"
                + "--" + boundary + "--")));
    }

    @Test
    public void testSubcribingMoreThanOnce() {
        MultiPartEncoder encoder = new MultiPartEncoder("boundary",
                OUTBOUND_MEDIA_SUPPORT);
        new BodyPartPublisher(listOf()).subscribe(encoder);
        try {
            new BodyPartPublisher(listOf()).subscribe(encoder);
            fail("exception should be thrown");
        } catch(IllegalStateException ex) {
            assertThat(ex.getMessage(),
                    is(equalTo("Input subscription already set")));
        }
    }

    @Test
    public void testInBoundBodyPart() {
        try {
            String boundary = "boundary";
            encodeParts(boundary, BodyPart.builder()
                    .inBoundPublisher(new DataChunkPublisher(
                            ("--" + boundary + "\r\n"
                                    + "Content-Type:text/plain\r\n"
                                    + "\r\n"
                                    + "part1\n"
                                    + "--" + boundary + "--").getBytes()),
                            INBOUND_MEDIA_SUPPORT)
                    .build());
        } catch (TestException ex) {
            assertThat(ex.getCause(),
                    is(instanceOf(IllegalStateException.class)));
            assertThat(ex.getCause().getMessage(),
                    is(equalTo("Not an out-bound body part")));
        }
    }

    @Test
    public void testUpstreamError() {
        MultiPartEncoder decoder = new MultiPartEncoder("boundary",
                OUTBOUND_MEDIA_SUPPORT);
        new Publisher<BodyPart>(){
            @Override
            public void subscribe(Subscriber<? super BodyPart> subscriber) {
                subscriber.onError(new IllegalStateException("oops"));
            }
        }.subscribe(decoder);
        DataChunkSubscriber subscriber = new DataChunkSubscriber();
        decoder.subscribe(subscriber);
        CompletableFuture<String> future = subscriber.content()
                .toCompletableFuture();
        assertThat(future.isCompletedExceptionally(), is(equalTo(true)));
        try {
            future.getNow(null);
            fail("exception should be thrown");
        } catch(CompletionException ex) {
            assertThat(ex.getCause(), is(instanceOf(IllegalStateException.class)));
            assertThat(ex.getCause().getMessage(), is(equalTo("oops")));
        }
    }

    @Test
    public void testPartContentPublisherError() {
        MultiPartEncoder decoder = new MultiPartEncoder("boundary",
                OUTBOUND_MEDIA_SUPPORT);
        new BodyPartPublisher(listOf(BodyPart.builder()
                .publisher((Subscriber<? super DataChunk> subscriber) -> {
                    subscriber.onError(new IllegalStateException("oops"));
                })
                .build())).subscribe(decoder);
        DataChunkSubscriber subscriber = new DataChunkSubscriber();
        decoder.subscribe(subscriber);
        CompletableFuture<String> future = subscriber.content()
                .toCompletableFuture();
        assertThat(future.isCompletedExceptionally(), is(equalTo(true)));
        try {
            future.getNow(null);
            fail("exception should be thrown");
        } catch(CompletionException ex) {
            assertThat(ex.getCause(), is(instanceOf(IllegalStateException.class)));
            assertThat(ex.getCause().getMessage(), is(equalTo("oops")));
        }
    }

    private static String encodeParts(String boundary, BodyPart... parts) {
        BodyPartPublisher publisher = new BodyPartPublisher(listOf(parts));
        MultiPartEncoder encoder = new MultiPartEncoder(boundary,
                OUTBOUND_MEDIA_SUPPORT);
        publisher.subscribe(encoder);
        try {
            return new String(ContentReaders.byteArrayReader()
                    .apply(encoder)
                    .toCompletableFuture()
                    .get());
        } catch (InterruptedException ex) {
            return null;
        } catch (ExecutionException ex) {
            throw new TestException(ex.getCause());
        }
    }

    private static final class TestException extends RuntimeException {

        private TestException(Throwable cause) {
            super(cause);
        }
    }
}
