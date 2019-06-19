package io.helidon.media.jsonp.common;

import io.helidon.common.http.MediaType;
import io.helidon.common.http.OutBoundScope;
import java.util.List;
import javax.json.JsonStructure;
import javax.json.JsonWriterFactory;

/**
 *
 * JSON-P line delimited stream writer.
 */
public class JsonLineDelimitedEntityStreamWriter
        extends JsonEntityStreamWriter {

    public JsonLineDelimitedEntityStreamWriter(JsonWriterFactory writerFactory) {
        super(writerFactory, /* begin */ null, "\r\n", /* end */ null);
    }

    @Override
    public Promise accept(Class<?> type, OutBoundScope scope) {
        if (JsonStructure.class.isAssignableFrom(type)) {
            MediaType contentType = findAccepted(scope.acceptedTypes);
            if (contentType != null) {
                return new Promise(this, contentType);
            }
        }
        return null;
    }

    private static MediaType findAccepted(List<MediaType> acceptedTypes) {
        if (acceptedTypes != null) {
            for (MediaType acceptedType : acceptedTypes) {
                if (MediaType.APPLICATION_STREAM_JSON.equals(acceptedType)) {
                    return acceptedType;
                }
            }
        }
        return null;
    }
}
