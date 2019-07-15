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

import io.helidon.common.CollectionsHelper;
import io.helidon.common.http.MediaType;
import io.helidon.common.http.DataChunk;
import io.helidon.common.reactive.Flow.Subscriber;
import io.helidon.common.reactive.Multi;
import io.helidon.media.common.ContentReaders;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.Test;
import java.util.concurrent.CompletionException;
import java.time.Duration;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import io.helidon.media.multipart.common.MultiPartDecoderTest.DataChunkSubscriber;

import static io.helidon.media.multipart.common.BodyPartTest.MEDIA_SUPPORT;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.fail;

/**
 * Test {@link MultiPartEncoder}.
 */
public class MultiPartEncoderTest {

    private static final List<OutboundBodyPart> EMPTY_OUTBOUND_PARTS =
            CollectionsHelper.<OutboundBodyPart>listOf();

    // TODO test throttling

    @Test
    public void testEncodeOnePart() {
        String boundary = "boundary";
        String message = encodeParts(boundary,
                OutboundBodyPart.builder()
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
                OutboundBodyPart.builder()
                        .headers(OutboundBodyPartHeaders.builder()
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
                OutboundBodyPart.builder()
                        .entity("part1")
                        .build(),
                OutboundBodyPart.builder()
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
        MultiPartEncoder encoder = MultiPartEncoder
                .create("boundary", MEDIA_SUPPORT.writerContext());
        Multi.just(EMPTY_OUTBOUND_PARTS).subscribe(encoder);
        try {
            Multi.just(EMPTY_OUTBOUND_PARTS).subscribe(encoder);
            fail("exception should be thrown");
        } catch(IllegalStateException ex) {
            assertThat(ex.getMessage(),
                    is(equalTo("Input subscription already set")));
        }
    }

    @Test
    public void testUpstreamError() {
        MultiPartEncoder decoder = MultiPartEncoder
                .create("boundary", MEDIA_SUPPORT.writerContext());
        Multi.<OutboundBodyPart>error(new IllegalStateException("oops"))
                .subscribe(decoder);
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
        MultiPartEncoder decoder = MultiPartEncoder
                .create("boundary", MEDIA_SUPPORT.writerContext());
        Multi.just(OutboundBodyPart.builder()
                .publisher((Subscriber<? super DataChunk> subscriber) -> {
                    subscriber.onError(new IllegalStateException("oops"));
                })
                .build()).subscribe(decoder);
        DataChunkSubscriber subscriber = new DataChunkSubscriber();
        decoder.subscribe(subscriber);
        CompletableFuture<String> future = subscriber.content()
                .toCompletableFuture();
        assertThat(future.isCompletedExceptionally(), is(equalTo(true)));
        try {
            future.getNow(null);
            fail("exception should be thrown");
        } catch(CompletionException ex) {
            assertThat(ex.getCause(),
                    is(instanceOf(IllegalStateException.class)));
            assertThat(ex.getCause().getMessage(), is(equalTo("oops")));
        }
    }

    private static String encodeParts(String boundary,
            OutboundBodyPart... parts) {

        MultiPartEncoder encoder = MultiPartEncoder.create(boundary,
                MEDIA_SUPPORT.writerContext());
        Multi.just(parts).subscribe(encoder);
        return ContentReaders.readString(encoder, StandardCharsets.UTF_8)
                .block(Duration.ofSeconds(10));
    }
}
