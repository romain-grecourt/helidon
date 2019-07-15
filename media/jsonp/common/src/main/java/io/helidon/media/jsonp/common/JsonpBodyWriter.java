package io.helidon.media.jsonp.common;

import io.helidon.common.GenericType;
import io.helidon.common.http.DataChunk;
import io.helidon.common.http.MediaType;
import io.helidon.common.reactive.Flow.Publisher;
import io.helidon.common.reactive.Mono;
import io.helidon.common.reactive.MultiMapper;
import io.helidon.media.common.CharBuffer;
import io.helidon.media.common.ContentWriters;
import io.helidon.media.common.MessageBodyWriter;
import io.helidon.media.common.MessageBodyWriterContext;
import java.nio.charset.Charset;
import javax.json.JsonStructure;
import javax.json.JsonWriter;
import javax.json.JsonWriterFactory;

/**
 * Message body writer for {@link JsonStructure} sub-classes (JSON-P).
 */
public class JsonpBodyWriter implements MessageBodyWriter<JsonStructure> {

    private final JsonWriterFactory jsonWriterFactory;

    public JsonpBodyWriter(JsonWriterFactory jsonWriterFactory) {
        this.jsonWriterFactory = jsonWriterFactory;
    }

    @Override
    public boolean accept(GenericType<?> type,
            MessageBodyWriterContext context) {

        return JsonStructure.class.isAssignableFrom(type.rawType());
    }

    @Override
    public Publisher<DataChunk> write(Mono<JsonStructure> content,
            GenericType<? extends JsonStructure> type,
            MessageBodyWriterContext context) {

        MediaType contentType = context.findAccepted(MediaType.JSON_PREDICATE,
                MediaType.APPLICATION_JSON);
        context.contentType(contentType);
        return content.mapMany(new JsonStructureToChunks(jsonWriterFactory,
                context.charset()));
    }

    static final class JsonStructureToChunks
            implements MultiMapper<JsonStructure, DataChunk> {

        private final JsonWriterFactory factory;
        private final Charset charset;

        JsonStructureToChunks(JsonWriterFactory factory, Charset charset) {
            this.factory = factory;
            this.charset = charset;
        }

        @Override
        public Publisher<DataChunk> map(JsonStructure item) {
            CharBuffer buffer = new CharBuffer();
            try (JsonWriter writer = factory.createWriter(buffer)) {
                writer.write(item);
                return ContentWriters.writeCharBuffer(buffer, charset);
            }
        }
    }
}
