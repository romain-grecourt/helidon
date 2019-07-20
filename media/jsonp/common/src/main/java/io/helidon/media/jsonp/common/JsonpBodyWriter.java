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
package io.helidon.media.jsonp.common;


import javax.json.JsonStructure;
import javax.json.JsonWriter;
import javax.json.JsonWriterFactory;

import io.helidon.common.GenericType;
import io.helidon.common.http.DataChunk;
import io.helidon.common.http.MediaType;
import io.helidon.common.reactive.Flow.Publisher;
import io.helidon.media.common.CharBuffer;
import io.helidon.media.common.ContentWriters;
import io.helidon.media.common.MessageBodyWriter;
import io.helidon.media.common.MessageBodyWriterContext;

/**
 * Message body writer for {@link JsonStructure} sub-classes (JSON-P).
 */
public class JsonpBodyWriter implements MessageBodyWriter<JsonStructure> {

    private final JsonWriterFactory jsonWriterFactory;

    JsonpBodyWriter(JsonWriterFactory jsonWriterFactory) {
        this.jsonWriterFactory = jsonWriterFactory;
    }

    @Override
    public boolean accept(GenericType<?> type,
            MessageBodyWriterContext context) {

        return JsonStructure.class.isAssignableFrom(type.rawType());
    }

    @Override
    public Publisher<DataChunk> write(JsonStructure content,
            GenericType<? extends JsonStructure> type,
            MessageBodyWriterContext context) {

        MediaType contentType = context.findAccepted(MediaType.JSON_PREDICATE,
                MediaType.APPLICATION_JSON);
        context.contentType(contentType);
        CharBuffer buffer = new CharBuffer();
        try (JsonWriter writer = jsonWriterFactory.createWriter(buffer)) {
            if (writer != null) {
                writer.write(content);
            }
            return ContentWriters.writeCharBuffer(buffer, context.charset());
        }
    }
}
