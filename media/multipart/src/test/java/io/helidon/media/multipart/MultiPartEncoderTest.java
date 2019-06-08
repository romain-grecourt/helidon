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
import io.helidon.media.multipart.MultiPartSupport.BodyPartPublisher;
import io.helidon.media.common.ContentReaders;
import io.helidon.common.http.MediaType;

import static io.helidon.common.CollectionsHelper.listOf;
import static io.helidon.media.multipart.BodyPartTest.OUTBOUND_MEDIA_SUPPORT;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

/**
 * Test {@link MultiPartEncoder}.
 */
public class MultiPartEncoderTest {

    // TODO test with two parts
    // TODO test throttling

//    @Test
    public void testEncodeOnePart() {
        String boundary = "boundary";
        String message = encodeParts(boundary, BodyPart.builder()
                .entity("part1")
                .build());
        assertThat(message, is(equalTo(
                "--" + boundary + "\r\n"
                + "\r\n"
                + "part1\n"
                + "--" + boundary + "--")));
    }

//    @Test
    public void testEncodeOnePartWithHeaders() {
        String boundary = "boundary";
        String message = encodeParts(boundary, BodyPart.builder()
                .headers(BodyPartHeaders.builder()
                        .contentType(MediaType.TEXT_PLAIN)
                        .build())
                .entity("part1")
                .build());
        assertThat(message, is(equalTo(
                "--" + boundary + "\r\n"
                + "ContentType:text/plain\r\n"
                + "\r\n"
                + "part1\n"
                + "--" + boundary + "--")));
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
        } catch (InterruptedException | ExecutionException ex) {
            return null;
        }
    }
}
