package io.helidon.media.jsonp.common;

import io.helidon.common.GenericType;
import io.helidon.common.http.DataChunk;
import io.helidon.common.http.MediaType;
import io.helidon.common.http.MessageBody.WriterContext;
import io.helidon.common.reactive.Flow.Publisher;
import javax.json.JsonStructure;
import javax.json.JsonWriterFactory;

/**
 *
 * JSON-P line delimited stream writer.
 */
public class JsonpLineDelimitedStreamWriter
        extends JsonpStreamWriter {

    public JsonpLineDelimitedStreamWriter(JsonWriterFactory factory) {
        super(factory, /* begin */ null, "\r\n", /* end */ null);
    }

    @Override
    public <U extends JsonStructure> Publisher<DataChunk> write(
            Publisher<U> content, GenericType<U> type, WriterContext context) {

         MediaType contentType = context.findAccepted(MediaType.JSON_PREDICATE,
                MediaType.APPLICATION_STREAM_JSON);
         context.contentType(contentType);
         return new JsonArrayStreamProcessor(content, context.charset());
    }
}
