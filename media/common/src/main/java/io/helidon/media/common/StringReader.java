package io.helidon.media.common;

import io.helidon.common.GenericType;
import io.helidon.common.http.DataChunk;
import io.helidon.common.http.MessageBody.Reader;
import io.helidon.common.http.MessageBody.ReaderContext;
import io.helidon.common.reactive.Flow.Publisher;
import io.helidon.common.reactive.Mono;
import java.io.ByteArrayOutputStream;
import java.nio.charset.Charset;
import java.util.function.Function;

/**
 * Reader for String.
 */
public final class StringReader implements Reader<String> {

    private StringReader() {
    }

    @Override
    public boolean accept(GenericType<?> type, ReaderContext context) {
        return String.class.isAssignableFrom(type.rawType());
    }

    @Override
    @SuppressWarnings("unchecked")
    public <U extends String> Mono<U> read(Publisher<DataChunk> publisher,
            GenericType<U> type, ReaderContext context) {

        return (Mono<U>) read(publisher, context.charset());
    }

    public static Mono<String> read(
            Publisher<DataChunk> publisher, Charset charset) {

        return ByteArrayReader.read(publisher).flatMap(new Mapper(charset));
    }

    public static StringReader create() {
        return new StringReader();
    }

    private static final class Mapper
            implements Function<ByteArrayOutputStream, Mono<String>> {

        private final Charset charset;

        Mapper(Charset charset) {
            this.charset = charset;
        }

        @Override
        public Mono<String> apply(ByteArrayOutputStream baos) {
            return Mono.just(new String(baos.toByteArray(), charset));
        }
    }
}
