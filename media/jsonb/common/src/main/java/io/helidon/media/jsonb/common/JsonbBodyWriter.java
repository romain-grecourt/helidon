package io.helidon.media.jsonb.common;

import io.helidon.common.GenericType;
import io.helidon.common.http.DataChunk;
import io.helidon.common.http.MediaType;
import io.helidon.common.reactive.Flow.Publisher;
import io.helidon.media.common.CharBuffer;
import io.helidon.media.common.CharBufferBodyWriter;
import java.util.Objects;
import javax.json.bind.Jsonb;
import javax.json.bind.JsonbException;
import io.helidon.common.reactive.Mono;
import io.helidon.media.common.MessageBodyWriter;
import io.helidon.media.common.MessageBodyWriterContext;
import java.nio.charset.Charset;
import java.util.function.Function;

/**
 * Message body writer supporting object binding with JSON-B.
 */
public class JsonbBodyWriter implements MessageBodyWriter<Object> {

    private final Jsonb jsonb;

    public JsonbBodyWriter(Jsonb jsonb) {
        Objects.requireNonNull(jsonb);
        this.jsonb = jsonb;
    }

    @Override
    public boolean accept(GenericType<?> type,
            MessageBodyWriterContext context) {

        return !CharSequence.class.isAssignableFrom(type.rawType());
    }

    @Override
    public Publisher<DataChunk> write(Mono<Object> content,
            GenericType<? extends Object> type,
            MessageBodyWriterContext context) {

        MediaType contentType = context.findAccepted(MediaType.JSON_PREDICATE,
                MediaType.APPLICATION_JSON);
        context.contentType(contentType);
        return content.flatMapMany(new Mapper(jsonb, context.charset()));
    }

    private static final class Mapper
            implements Function<Object, Publisher<DataChunk>> {

        private final Jsonb jsonb;
        private final Charset charset;

        Mapper(Jsonb jsonb, Charset charset) {
            this.jsonb = jsonb;
            this.charset = charset;
        }

        @Override
        public Publisher<DataChunk> apply(Object item) {
            CharBuffer buffer = new CharBuffer();
            try {
                jsonb.toJson(item, buffer);
                return CharBufferBodyWriter
                        .write(Mono.just(buffer), charset);
            } catch (IllegalStateException | JsonbException ex) {
                return Mono.<DataChunk>error(ex);
            }
        }
    }
}
