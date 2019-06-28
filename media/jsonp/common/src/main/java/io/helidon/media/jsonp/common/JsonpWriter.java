package io.helidon.media.jsonp.common;

import io.helidon.common.GenericType;
import io.helidon.common.http.DataChunk;
import io.helidon.common.http.MediaType;
import io.helidon.common.http.MessageBody.Writer;
import io.helidon.common.http.MessageBody.WriterContext;
import io.helidon.common.reactive.FailedPublisher;
import io.helidon.common.reactive.Flow.Publisher;
import io.helidon.common.reactive.SingleItemPublisher;
import io.helidon.media.common.CharBuffer;
import io.helidon.media.common.CharBufferWriter;
import io.helidon.common.reactive.SingleInputDelegatingProcessor;
import java.nio.charset.Charset;
import javax.json.JsonException;
import javax.json.JsonStructure;
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
    public <U extends JsonStructure> Publisher<DataChunk> write(
            Publisher<U> content, GenericType<U> type, WriterContext context) {

        MediaType contentType = context.findAccepted(MediaType.JSON_PREDICATE,
                MediaType.APPLICATION_JSON);
        context.contentType(contentType);
        Processor processor = new Processor(jsonWriterFactory, context.charset());
        content.subscribe(processor);
        return processor;
    }

    static Publisher<DataChunk> write(JsonWriterFactory factory,
            JsonStructure entity, Charset charset) {

        CharBuffer buffer = new CharBuffer();
        try (javax.json.JsonWriter writer = factory.createWriter(buffer)) {
            writer.write(entity);
            return CharBufferWriter
                    .write(new SingleItemPublisher<>(buffer), charset);
        } catch (IllegalStateException | JsonException ex) {
            return new FailedPublisher<>(ex);
        }
    }

    private static final class Processor
            extends SingleInputDelegatingProcessor<JsonStructure, DataChunk> {

        private final JsonWriterFactory jsonWriterFactory;
        private final Charset charset;

        Processor(JsonWriterFactory jsonWriterFactory, Charset charset) {
            this.jsonWriterFactory = jsonWriterFactory;
            this.charset = charset;
        }

        @Override
        protected Publisher<DataChunk> delegate(JsonStructure item) {
            return write(jsonWriterFactory, item, charset);
        }
    }
}
