package io.helidon.media.jsonb.common;

import io.helidon.common.GenericType;
import io.helidon.common.http.DataChunk;
import io.helidon.common.http.MediaType;
import io.helidon.common.reactive.Flow.Publisher;
import io.helidon.media.common.CharBuffer;
import java.util.Objects;
import javax.json.bind.Jsonb;
import javax.json.bind.JsonbException;
import io.helidon.common.reactive.Mono;
import io.helidon.common.reactive.MultiMapper;
import io.helidon.media.common.ContentWriters;
import io.helidon.media.common.MessageBodyWriter;
import io.helidon.media.common.MessageBodyWriterContext;
import java.nio.charset.Charset;

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
        return content.mapMany(new ObjectToChunks(jsonb,
                context.charset()));
    }

    /**
     * Implementation of {@link MultiMapper} that converts objects into chunks.
     */
    private static final class ObjectToChunks
            implements MultiMapper<Object, DataChunk> {

        private final Jsonb jsonb;
        private final Charset charset;

        ObjectToChunks(Jsonb jsonb, Charset charset) {
            this.jsonb = jsonb;
            this.charset = charset;
        }

        @Override
        public Publisher<DataChunk> map(Object item) {
            CharBuffer buffer = new CharBuffer();
            try {
                jsonb.toJson(item, buffer);
                return ContentWriters.writeCharBuffer(buffer, charset);
            } catch (IllegalStateException | JsonbException ex) {
                return Mono.<DataChunk>error(ex);
            }
        }
    }
}
