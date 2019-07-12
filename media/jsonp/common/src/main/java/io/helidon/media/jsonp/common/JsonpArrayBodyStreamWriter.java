package io.helidon.media.jsonp.common;

import javax.json.JsonWriterFactory;

/**
 * JSON-P array message body stream writer.
 */
public final class JsonpArrayBodyStreamWriter extends JsonpBodyStreamWriter {

    public JsonpArrayBodyStreamWriter(JsonWriterFactory writerFactory) {
        super(writerFactory, "[", ",", "]");
    }
}
