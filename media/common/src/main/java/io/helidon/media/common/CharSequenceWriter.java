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
 * Writer for {@code CharSequence}.
 */
public final class CharSequenceWriter implements Writer<CharSequence> {

    private CharSequenceWriter() {
    }

    @Override
    public boolean accept(GenericType<?> type, WriterContext context) {
        return CharSequence.class.isAssignableFrom(type.rawType());
    }

    @Override
    public Publisher<DataChunk> write(Mono<CharSequence> content,
            GenericType<? extends CharSequence> type, WriterContext context) {

        context.contentType(MediaType.TEXT_PLAIN);
        return write(content, context.charset());
    }

    static Publisher<DataChunk> write(CharSequence content, Charset charset) {

        return Mono.just(DataChunk.create(false,
                charset.encode(content.toString())));
    }

    public static Publisher<DataChunk> write(Mono<CharSequence> content,
            Charset charset) {

        return content.flatMapMany(new Mapper(charset));
    }

    public static CharSequenceWriter create() {
        return new CharSequenceWriter();
    }

    private static final class Mapper
            implements Function<CharSequence, Publisher<DataChunk>> {

        private final Charset charset;

        Mapper(Charset charset) {
            this.charset = charset;
        }

        @Override
        public Publisher<DataChunk> apply(CharSequence cs) {
            return write(cs, charset);
        }
    }
}
