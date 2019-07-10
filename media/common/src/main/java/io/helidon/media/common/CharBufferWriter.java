package io.helidon.media.common;

import io.helidon.common.GenericType;
import io.helidon.common.http.DataChunk;
import io.helidon.common.http.MediaType;
import io.helidon.common.http.MessageBody.Writer;
import io.helidon.common.http.MessageBody.WriterContext;
import io.helidon.common.reactive.Flow.Publisher;
import io.helidon.common.reactive.Mono;
import java.nio.charset.Charset;
import java.util.function.Function;

/**
 * Writer for {@link CharBuffer}.
 */
public final class CharBufferWriter implements Writer<CharBuffer> {

    private CharBufferWriter() {
    }

    @Override
    public boolean accept(GenericType<?> type, WriterContext context) {
        return CharBuffer.class.isAssignableFrom(type.rawType());
    }

    @Override
    public Publisher<DataChunk> write(Mono<CharBuffer> content,
            GenericType<? extends CharBuffer> type, WriterContext context) {

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

    public static CharBufferWriter create() {
        return new CharBufferWriter();
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
