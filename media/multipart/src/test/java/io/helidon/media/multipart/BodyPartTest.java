/*
 * Copyright (c) 2020, 2022 Oracle and/or its affiliates.
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

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.Flow.Publisher;
import java.util.concurrent.atomic.AtomicBoolean;

import io.helidon.common.http.DataChunk;
import io.helidon.media.common.ContentReaders;
import io.helidon.media.common.ContentWriters;
import io.helidon.media.common.MediaContext;
import io.helidon.media.common.Entity;
import io.helidon.media.common.ReadableEntity;

import org.junit.jupiter.api.Test;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.fail;

/**
 * Tests {@link BodyPart}.
 */
public class BodyPartTest {

    static final MediaContext MEDIA_CONTEXT = MediaContext.create();
    static final Charset DEFAULT_CHARSET = StandardCharsets.UTF_8;

    @Test
    public void testContentFromPublisher() {
        BodyPart bodyPart = BodyPart.builder()
                                    .entity(readableContent(ContentWriters
                                            .writeCharSequence("body part data", DEFAULT_CHARSET)))
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
    public void testContentFromEntity() throws Exception {
        ReadableEntity content = BodyPart.builder()
                                         .entity("body part data")
                                         .build()
                                         .content();
        Publisher<DataChunk> publisher = ((Entity) content).writerContext(MEDIA_CONTEXT.writerContext());
        String result = ContentReaders.readString(publisher, DEFAULT_CHARSET).get();
        assertThat(result, is(equalTo("body part data")));
    }

    @Test
    public void testBuildingPartWithNoContent() {
        assertThrows(IllegalStateException.class, () -> BodyPart.builder().build());
    }

    @Test
    public void testIsNamed() {
        BodyPart bodyPart = BodyPart.builder()
                                    .headers(BodyPartHeaders.builder()
                                                            .contentDisposition(ContentDisposition.builder()
                                                                                                  .name("foo")))
                                    .entity("abc")
                                    .build();
        assertThat(bodyPart.isNamed("foo"), is(true));
    }

    @Test
    public void testName() {
        BodyPart bodyPart = BodyPart.builder()
                                    .headers(BodyPartHeaders.builder()
                                                            .contentDisposition(ContentDisposition.builder()
                                                                                                  .name("foo")))
                                    .entity("abc")
                                    .build();
        assertThat(bodyPart.name().orElse(null), is(equalTo("foo")));
        assertThat(bodyPart.filename().isEmpty(), is(true));
    }

    @Test
    public void testFilename() {
        BodyPart bodyPart = BodyPart.builder()
                                    .headers(BodyPartHeaders.builder()
                                                            .contentDisposition(ContentDisposition.builder()
                                                                                                  .filename("foo.txt")))
                                    .entity("abc")
                                    .build();
        assertThat(bodyPart.filename().orElse(null), is(equalTo("foo.txt")));
        assertThat(bodyPart.name().isEmpty(), is(true));
    }

    static ReadableEntity readableContent(Publisher<DataChunk> chunks) {
        return Entity.create(ctx -> chunks, MEDIA_CONTEXT.readerContext(), null);
    }
}
