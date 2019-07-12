package io.helidon.media.jackson.common;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.helidon.common.GenericType;
import io.helidon.common.http.DataChunk;
import io.helidon.common.http.MediaType;
import io.helidon.common.reactive.Flow.Publisher;
import io.helidon.common.reactive.Mono;
import io.helidon.media.common.CharBuffer;
import io.helidon.media.common.CharBufferBodyWriter;
import io.helidon.media.common.MessageBodyWriter;
import io.helidon.media.common.MessageBodyWriterContext;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.Objects;
import java.util.function.Function;

/**
 * Message body writer supporting object binding with Jackson.
 */
public final class JacksonBodyWriter implements MessageBodyWriter<Object> {

    private final ObjectMapper objectMapper;

    public JacksonBodyWriter(ObjectMapper objectMapper) {
        Objects.requireNonNull(objectMapper);
        this.objectMapper = objectMapper;
    }

    @Override
    public boolean accept(GenericType<?> type,
            MessageBodyWriterContext context) {

        return ! CharSequence.class.isAssignableFrom(type.rawType())
                && objectMapper.canSerialize(type.rawType());
    }

    @Override
    public Publisher<DataChunk> write(Mono<Object> content,
            GenericType<? extends Object> type,
            MessageBodyWriterContext context) {

        MediaType contentType = context.findAccepted(MediaType.JSON_PREDICATE,
                MediaType.APPLICATION_JSON);
        context.contentType(contentType);
        return content.flatMapMany(new Mapper(objectMapper, context.charset()));
    }

    private static final class Mapper
            implements Function<Object, Publisher<DataChunk>> {

        private final ObjectMapper objectMapper;
        private final Charset charset;

        Mapper(ObjectMapper objectMapper, Charset charset) {
            this.objectMapper = objectMapper;
            this.charset = charset;
        }

        @Override
        public Publisher<DataChunk> apply(Object content) {
            try {
                CharBuffer buffer = new CharBuffer();
                objectMapper.writeValue(buffer, content);
                return CharBufferBodyWriter.write(Mono.just(buffer), charset);
            } catch (IOException wrapMe) {
                throw new JacksonRuntimeException(wrapMe.getMessage(), wrapMe);
            }
        }
    }
}
