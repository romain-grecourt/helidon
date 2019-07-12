package io.helidon.media.common;

import io.helidon.common.GenericType;
import io.helidon.common.http.DataChunk;
import io.helidon.common.http.MediaType;
import io.helidon.common.reactive.Flow.Publisher;
import io.helidon.common.reactive.Mono;
import java.nio.charset.Charset;
import java.util.function.Function;

/**
 * Message body writer for {@code CharSequence}.
 */
public final class CharSequenceBodyWriter
        implements MessageBodyWriter<CharSequence> {

    private CharSequenceBodyWriter() {
    }

    @Override
    public boolean accept(GenericType<?> type,
            MessageBodyWriterContext context) {

        return CharSequence.class.isAssignableFrom(type.rawType());
    }

    @Override
    public Publisher<DataChunk> write(Mono<CharSequence> content,
            GenericType<? extends CharSequence> type,
            MessageBodyWriterContext context) {

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

    public static CharSequenceBodyWriter create() {
        return new CharSequenceBodyWriter();
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
