/*
 * Copyright (c) 2017, 2022 Oracle and/or its affiliates.
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

import java.nio.charset.Charset;
import java.nio.charset.UnsupportedCharsetException;

import io.helidon.common.http.MediaType;

import org.junit.jupiter.api.Test;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.fail;

/**
 * Test content type charset.
 */
public class ContentCharsetTest {

    @Test
    public void requestContentCharset() {
        assertThat(readerContext("application/json; charset=cp1250").charset(),
                is(Charset.forName("cp1250")));
    }

    @Test
    public void invalidRequestContentCharset() {
        try {
            readerContext("application/json; charset=invalid-charset-name")
                    .charset();
            fail("an exception should be thrown");
        } catch (IllegalStateException ex) {
            assertThat(ex.getCause(),
                    is(instanceOf(UnsupportedCharsetException.class)));
        }
    }

    @Test
    public void nonexistentCharset() {
        assertThat(readerContext("application/json").charset(),
                is(equalTo(EntitySupport.ReaderContext.DEFAULT_CHARSET)));
    }

    @Test
    public void missingContentType() {
        assertThat(readerContext(null).charset(),
                is(equalTo(EntitySupport.ReaderContext.DEFAULT_CHARSET)));
    }

    /**
     * Create a reader context with the specified {@code Content-Type} value.
     *
     * @param contentTypeValue {@code Content-Type} value
     * @return MessageBodyReaderContext
     */
    private EntitySupport.ReaderContext readerContext(String contentTypeValue) {
        MediaType contentType = contentTypeValue != null ? MediaType.parse(contentTypeValue) : null;
        return EntitySupport.ReaderContext.create().createChild(null, null, contentType);
    }
}
