package io.helidon.media.jsonp.common;

import io.helidon.common.GenericType;
import io.helidon.common.http.DataChunk;
import io.helidon.common.http.MediaType;
import io.helidon.common.http.MessageBody.Writer;
import io.helidon.common.http.MessageBody.WriterContext;
import io.helidon.common.reactive.Flow.Publisher;
import io.helidon.common.reactive.Mono;
import io.helidon.media.common.CharBuffer;
import io.helidon.media.common.CharBufferWriter;
import java.nio.charset.Charset;
import java.util.function.Function;
import javax.json.JsonStructure;
import javax.json.JsonWriter;
import javax.json.JsonWriterFactory;

/**
 * JSON-P entity writer.
 */
public class JsonpWriter implements Writer<JsonStructure> {

    private final JsonWriterFactory jsonWriterFactory;

    public JsonpWriter(JsonWriterFactory jsonWriterFactory) {
        this.jsonWriterFactory = jsonWriterFactory;
    }

    @Override
    public boolean accept(GenericType<?> type, WriterContext context) {
        return JsonStructure.class.isAssignableFrom(type.rawType());
    }

    @Override
    public Publisher<DataChunk> write(Mono<JsonStructure> content,
            GenericType<? extends JsonStructure> type, WriterContext context) {

        MediaType contentType = context.findAccepted(MediaType.JSON_PREDICATE,
                MediaType.APPLICATION_JSON);
        context.contentType(contentType);
        return content.flatMapMany(new Mapper(jsonWriterFactory,
                context.charset()));
    }

    static Publisher<DataChunk> write(JsonWriterFactory factory,
            JsonStructure entity, Charset charset) {

        CharBuffer buffer = new CharBuffer();
        try (JsonWriter writer = factory.createWriter(buffer)) {
            writer.write(entity);
            return CharBufferWriter.write(Mono.just(buffer), charset);
        }
    }

    private static final class Mapper
            implements Function<JsonStructure, Publisher<DataChunk>> {

        private final JsonWriterFactory jsonWriterFactory;
        private final Charset charset;

        Mapper(JsonWriterFactory jsonWriterFactory, Charset charset) {
            this.jsonWriterFactory = jsonWriterFactory;
            this.charset = charset;
        }

        @Override
        public Publisher<DataChunk> apply(JsonStructure item) {
            return write(jsonWriterFactory, item, charset);
        }
    }
}
