/*
 * Copyright (c) 2019, 2022 Oracle and/or its affiliates.
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

import java.io.InputStream;
import java.net.URLEncoder;
import java.nio.charset.Charset;
import java.util.concurrent.Flow.Publisher;

import io.helidon.common.http.DataChunk;

import io.helidon.common.reactive.Single;
import org.junit.jupiter.api.Test;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

/**
 * Unit test for {@link ContentReaders}.
 */
class ContentReadersTest {

    @Test
    void testReadString() {
        Publisher<DataChunk> chunks = chunks((byte) 225, (byte) 226, (byte) 227);
        String s  = ContentReaders.readString(chunks, Charset.forName("cp1250")).await();
        assertThat(s, is("áâă"));
    }

    @Test
    void testReadBytes() {
        byte[] bytes = "Popokatepetl".getBytes(UTF_8);
        byte[] actualBytes = ContentReaders.readBytes(chunks(bytes)).await();
        assertThat(actualBytes, is(bytes));
    }

    @Test
    void testReadInputStream() throws Exception {
        byte[] bytes = "Popokatepetl".getBytes(UTF_8);
        InputStream inputStream = ContentReaders.readInputStream(chunks(bytes));
        byte[] actualBytes = inputStream.readAllBytes();
        assertThat(actualBytes, is(bytes));
    }

    @Test
    void testreadURLEncodedString() {
        String original = "myParam=\"Now@is'the/time";
        String encoded = URLEncoder.encode(original, UTF_8);
        String s = ContentReaders.readURLEncodedString(chunks(encoded), UTF_8).await();
        assertThat(s, is(original));
    }

    private static Publisher<DataChunk> chunks(byte... bytes) {
        return Single.just(DataChunk.create(bytes));
    }

    private static Publisher<DataChunk> chunks(String s) {
        return chunks(s.getBytes(UTF_8));
    }
}
