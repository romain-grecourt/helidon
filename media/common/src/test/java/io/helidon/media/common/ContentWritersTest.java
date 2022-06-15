/*
 * Copyright (c) 2018, 2022 Oracle and/or its affiliates.
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

package io.helidon.media.common;

import java.util.concurrent.Flow.Publisher;

import io.helidon.common.http.DataChunk;

import org.junit.jupiter.api.Test;

import static java.nio.charset.StandardCharsets.ISO_8859_1;
import static java.nio.charset.StandardCharsets.UTF_8;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

/**
 * A test for {@link io.helidon.media.common.ContentWriters}.
 */
public class ContentWritersTest {

    @Test
    public void testWriteBytes() {
        byte[] bytes = "abc".getBytes(ISO_8859_1);
        Publisher<DataChunk> publisher = ContentWriters.writeBytes(bytes, false);
        byte[] result = ContentReaders.readBytes(publisher).await();
        assertThat(bytes, is(result));
    }

    @Test
    public void testWriteBytesCopy() {
        byte[] bytes = "abc".getBytes(ISO_8859_1);
        Publisher<DataChunk> publisher = ContentWriters.writeBytes(bytes, true);
        System.arraycopy("xxx".getBytes(ISO_8859_1), 0, bytes, 0, bytes.length);
        byte[] result = ContentReaders.readBytes(publisher).await();
        assertThat("abc".getBytes(ISO_8859_1), is(result));
    }

    @Test
    public void testWriteBytesEmpty() {
        byte[] bytes = new byte[0];
        Publisher<DataChunk> publisher = ContentWriters.writeBytes(bytes, false);
        byte[] result = ContentReaders.readBytes(publisher).await();
        assertThat(result.length, is(0));
    }

    @Test
    public void testWriteCharSequence() {
        String data = "abc";
        Publisher<DataChunk> publisher = ContentWriters.writeCharSequence(data, UTF_8);
        byte[] result = ContentReaders.readBytes(publisher).await();
        assertThat(new String(result, UTF_8), is(data));
    }
}
