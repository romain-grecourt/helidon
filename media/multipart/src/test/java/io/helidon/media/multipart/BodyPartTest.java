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
import io.helidon.common.http.Reader;
import io.helidon.common.reactive.Flow;
import io.helidon.common.reactive.Flow.Publisher;
import io.helidon.media.common.ContentReaders;
import io.helidon.media.common.ContentWriters;
import io.helidon.webserver.Request.InternalReader;
import io.helidon.webserver.Response.Writer;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Deque;
import java.util.LinkedList;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Function;
import org.junit.jupiter.api.Test;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.fail;

/**
 * Tests {@link BodyPart}.
 */
public class BodyPartTest {

    static final BodyPartHeaders EMPTY_HEADERS =
            BodyPartHeaders.builder().build();

    static final Reader<String> STRING_READER =
            ContentReaders.stringReader(Charset.defaultCharset());

    static final Reader<byte[]> BYTES_READER =
            ContentReaders.byteArrayReader();

    static final Function<CharSequence,Publisher<DataChunk>> STRING_WRITER =
            ContentWriters.charSequenceWriter(Charset.defaultCharset());

    static final ArrayList<Writer> WRITERS = getWriters();

    static final Deque<InternalReader<?>> READERS = getReaders();

    @Test
    public void testContentFromPublisher() {
        Flow.Publisher<DataChunk> publisher = STRING_WRITER
                .apply("body part data");
        BodyPart bodyPart = BodyPart.create(EMPTY_HEADERS, publisher);
        bodyPart.registerReaders(READERS);
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
        bodyPart.registerWriters(WRITERS);
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
    private static ArrayList<Writer> getWriters(){
        Writer writer = new Writer(o -> CharSequence.class.equals(o.getClass()),
                MediaType.TEXT_PLAIN, STRING_WRITER);
        ArrayList<Writer> writers = new ArrayList<>();
        writers.add(writer);
        return writers;
    }

    @SuppressWarnings("unchecked")
    private static Deque<InternalReader<?>> getReaders() {
        LinkedList<InternalReader<?>> readers = new LinkedList<>();
        readers.add(new InternalReader(
                aClass -> aClass.equals(String.class), STRING_READER));
        readers.add(new InternalReader(
                aClass -> aClass.equals(byte[].class), BYTES_READER));
        return readers;
    }
}
