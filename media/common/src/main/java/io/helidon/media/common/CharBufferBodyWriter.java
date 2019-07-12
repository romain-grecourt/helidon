package io.helidon.media.common;

import io.helidon.common.GenericType;
import io.helidon.common.http.DataChunk;
import io.helidon.common.http.MediaType;
import io.helidon.common.reactive.Flow.Publisher;
import io.helidon.common.reactive.Mono;
import java.nio.charset.Charset;
import java.util.function.Function;

/**
 * Message body writer for {@link CharBuffer}.
 */
public final class CharBufferBodyWriter
        implements MessageBodyWriter<CharBuffer> {

    private CharBufferBodyWriter() {
    }

    @Override
    public boolean accept(GenericType<?> type,
            MessageBodyWriterContext context) {

        return CharBuffer.class.isAssignableFrom(type.rawType());
    }

    @Override
    public Publisher<DataChunk> write(Mono<CharBuffer> content,
            GenericType<? extends CharBuffer> type,
            MessageBodyWriterContext context) {

        context.contentType(MediaType.TEXT_PLAIN);
        return write(content, context.charset());
    }

    public static Publisher<DataChunk> write(Mono<CharBuffer> content,
            Charset charset) {

        return content.flatMapMany(new Mapper(charset));
    }

    static Publisher<DataChunk> write(CharBuffer buffer, Charset charset) {
        return Mono.just(DataChunk.create(false, buffer.encode(charset)));
    }

    public static CharBufferBodyWriter create() {
        return new CharBufferBodyWriter();
    }

    private static final class Mapper
            implements Function<CharBuffer, Publisher<DataChunk>> {

        private final Charset charset;

        Mapper(Charset charset) {
            this.charset = charset;
        }

        @Override
        public Publisher<DataChunk> apply(CharBuffer buffer) {
            return write(buffer, charset);
        }
    }
}
