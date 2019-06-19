package io.helidon.media.jsonp.common;

import javax.json.JsonWriterFactory;

/**
 * JSON-P array entity stream writer.
 */
public class JsonArrayEntityStreamWriter extends JsonEntityStreamWriter {

    public JsonArrayEntityStreamWriter(JsonWriterFactory writerFactory) {
        super(writerFactory, "[", ",", "]");
    }
}
