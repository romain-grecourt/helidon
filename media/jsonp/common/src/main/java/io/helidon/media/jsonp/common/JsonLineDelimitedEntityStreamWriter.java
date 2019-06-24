package io.helidon.media.jsonp.common;

import io.helidon.common.http.MediaType;
import io.helidon.common.http.OutBoundScope;
import io.helidon.common.reactive.Flow.Publisher;
import javax.json.JsonStructure;
import javax.json.JsonWriterFactory;

/**
 *
 * JSON-P line delimited stream writer.
 */
public class JsonLineDelimitedEntityStreamWriter
        extends JsonEntityStreamWriter {

    public JsonLineDelimitedEntityStreamWriter(JsonWriterFactory factory) {
        super(factory, /* begin */ null, "\r\n", /* end */ null);
    }

    @Override
    public Ack accept(Publisher<Object> stream, Class<?> type,
            OutBoundScope scope) {

        if (JsonStructure.class.isAssignableFrom(type)) {
            MediaType contentType = scope.findAccepted(
                    MediaType.APPLICATION_STREAM_JSON);
            if (contentType != null) {
                return new Ack(contentType);
            }
        }
        return null;
    }
}
