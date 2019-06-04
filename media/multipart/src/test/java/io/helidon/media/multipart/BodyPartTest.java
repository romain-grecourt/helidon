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
import io.helidon.common.http.DataChunk;
import io.helidon.common.http.MediaType;
import io.helidon.common.http.Reader;
import io.helidon.common.http.Writer;
import io.helidon.common.reactive.Flow.Publisher;
import io.helidon.media.common.ContentReaders;
import io.helidon.media.common.ContentWriters;
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
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.fail;

/**
 * Tests {@link BodyPart}.
 */
public class BodyPartTest {

    private static final Reader<String> STRING_READER =
            ContentReaders.stringReader(Charset.defaultCharset());

    private static final Reader<byte[]> BYTES_READER =
            ContentReaders.byteArrayReader();

    private static final Writer<CharSequence> STRING_WRITER =
            ContentWriters.charSequenceWriter(Charset.defaultCharset());

    static final OutBoundMediaSupport OUTBOUND_MEDIA_SUPPORT =
            getOutBoundMediaSupport();

    static final InBoundMediaSupport INBOUND_MEDIA_SUPPORT =
            getInBoundMediaSupport();

    @Test
    public void testContentFromPublisher() {
        Publisher<DataChunk> publisher = STRING_WRITER
                .apply("body part data");
        BodyPart bodyPart = BodyPart.builder()
                .inBoundPublisher(publisher, INBOUND_MEDIA_SUPPORT)
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
        BodyPart bodyPart = BodyPart.create("body part data");
        Content content = bodyPart.content();
        assertThat(content, is(instanceOf(OutBoundContent.class)));
        ((OutBoundContent) content)
                .mediaSupport(OUTBOUND_MEDIA_SUPPORT);
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
}
