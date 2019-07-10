package io.helidon.media.jsonb.common;

import io.helidon.common.GenericType;
import io.helidon.common.http.DataChunk;
import io.helidon.common.http.MediaType;
import io.helidon.common.reactive.Flow.Publisher;
import io.helidon.media.common.CharBuffer;
import io.helidon.media.common.CharBufferWriter;
import java.util.Objects;
import javax.json.bind.Jsonb;
import javax.json.bind.JsonbException;
import io.helidon.common.http.MessageBody.Writer;
import io.helidon.common.http.MessageBody.WriterContext;
import io.helidon.common.reactive.Mono;
import java.nio.charset.Charset;
import java.util.function.Function;

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
    public Publisher<DataChunk> write(Mono<Object> content,
            GenericType<? extends Object> type, WriterContext context) {

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
                return CharBufferWriter
                        .write(Mono.just(buffer), charset);
            } catch (IllegalStateException | JsonbException ex) {
                return Mono.<DataChunk>error(ex);
            }
        }
    }
}
