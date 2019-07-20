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
package io.helidon.media.common;

import io.helidon.common.GenericType;
import io.helidon.common.http.DataChunk;
import io.helidon.common.http.MediaType;
import io.helidon.common.reactive.Flow.Publisher;

/**
 * Writer for {@code CharSequence}.
 */
public final class CharSequenceBodyWriter
        implements MessageBodyWriter<CharSequence> {

    /**
     * Singleton instance.
     */
    private static final CharSequenceBodyWriter INSTANCE =
            new CharSequenceBodyWriter();

    /**
     * Enforce the use of {@link #get()}.
     */
    private CharSequenceBodyWriter() {
    }

    @Override
    public boolean accept(GenericType<?> type,
            MessageBodyWriterContext context) {

        return CharSequence.class.isAssignableFrom(type.rawType());
    }

    @Override
    public Publisher<DataChunk> write(CharSequence content,
            GenericType<? extends CharSequence> type,
            MessageBodyWriterContext context) {

        context.contentType(MediaType.TEXT_PLAIN);
        return ContentWriters.writeCharSequence(content, context.charset());
    }

    /**
     * Get the {@link CharSequenceBodyWriter} singleton.
     * @return CharSequenceBodyWriter
     */
    public static CharSequenceBodyWriter get() {
        return INSTANCE;
    }
}
