package io.helidon.media.jsonb.common;

import io.helidon.common.GenericType;
import io.helidon.common.http.DataChunk;
import io.helidon.common.http.MediaType;
import io.helidon.common.reactive.FailedPublisher;
import io.helidon.common.reactive.Flow.Publisher;
import io.helidon.media.common.CharBuffer;
import io.helidon.media.common.CharBufferWriter;
import java.util.Objects;
import javax.json.bind.Jsonb;
import javax.json.bind.JsonbException;
import io.helidon.common.http.MessageBody.Writer;
import io.helidon.common.http.MessageBody.WriterContext;
import io.helidon.common.reactive.SingleItemPublisher;
import io.helidon.common.reactive.SingleInputDelegatingProcessor;
import java.nio.charset.Charset;

/**
 * JSON-B writer.
 */
public class JsonbWriter implements Writer<Object> {

    private final Jsonb jsonb;

    public JsonbWriter(Jsonb jsonb) {
        Objects.requireNonNull(jsonb);
        this.jsonb = jsonb;
    }

    @Override
    public boolean accept(GenericType<?> type, WriterContext context) {
        return !CharSequence.class.isAssignableFrom(type.rawType());
    }

    @Override
    public <U> Publisher<DataChunk> write(Publisher<U> content,
            GenericType<U> type, WriterContext context) {

        MediaType contentType = context.findAccepted(MediaType.JSON_PREDICATE,
                MediaType.APPLICATION_JSON);
        context.contentType(contentType);
        Processor processor = new Processor(jsonb, context.charset());
        content.subscribe(processor);
        return processor;
    }

    private static final class Processor
            extends SingleInputDelegatingProcessor<Object, DataChunk> {

        private final Jsonb jsonb;
        private final Charset charset;

        Processor(Jsonb jsonb, Charset charset) {
            this.jsonb = jsonb;
            this.charset = charset;
        }

        @Override
        protected Publisher<DataChunk> delegate(Object item) {
            CharBuffer buffer = new CharBuffer();
            try {
                jsonb.toJson(item, buffer);
                return CharBufferWriter
                        .write(new SingleItemPublisher<>(buffer), charset);
            } catch (IllegalStateException | JsonbException ex) {
                return new FailedPublisher<>(ex);
            }
        }

    }
}
