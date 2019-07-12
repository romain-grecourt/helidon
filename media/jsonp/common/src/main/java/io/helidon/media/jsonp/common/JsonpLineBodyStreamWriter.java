package io.helidon.media.jsonp.common;

import io.helidon.common.GenericType;
import io.helidon.common.http.DataChunk;
import io.helidon.common.http.MediaType;
import io.helidon.common.reactive.Flow.Publisher;
import io.helidon.media.common.MessageBodyWriterContext;
import javax.json.JsonStructure;
import javax.json.JsonWriterFactory;

/**
 * JSON-P line delimited stream writer.
 */
public final class JsonpLineBodyStreamWriter extends JsonpBodyStreamWriter {

    public JsonpLineBodyStreamWriter(JsonWriterFactory factory) {
        super(factory, /* begin */ null, "\r\n", /* end */ null);
    }

    @Override
    public Publisher<DataChunk> write(Publisher<JsonStructure> publisher,
            GenericType<? extends JsonStructure> type,
            MessageBodyWriterContext context) {

         MediaType contentType = context.findAccepted(MediaType.JSON_PREDICATE,
                MediaType.APPLICATION_STREAM_JSON);
         context.contentType(contentType);
         return new JsonArrayStreamProcessor(publisher, context.charset());
    }
}
