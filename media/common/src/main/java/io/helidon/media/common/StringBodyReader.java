package io.helidon.media.common;

import io.helidon.common.GenericType;
import io.helidon.common.http.DataChunk;
import io.helidon.common.reactive.Flow.Publisher;
import io.helidon.common.reactive.Mono;
import java.io.ByteArrayOutputStream;
import java.nio.charset.Charset;
import java.util.function.Function;

/**
 * Message body reader for {@link String}.
 */
public final class StringBodyReader implements MessageBodyReader<String> {

    /**
     * Create a new {@link StringBodyReader} instance.
     * @return StringBodyReader
     */
    public static StringBodyReader create() {
        return new StringBodyReader();
    }

    /**
     * Private to enforce the use of {@link #create()}.
     */
    private StringBodyReader() {
    }

    @Override
    public boolean accept(GenericType<?> type,
            MessageBodyReaderContext context) {

        return String.class.isAssignableFrom(type.rawType());
    }

    @Override
    @SuppressWarnings("unchecked")
    public <U extends String> Mono<U> read(Publisher<DataChunk> publisher,
            GenericType<U> type, MessageBodyReaderContext context) {

        return (Mono<U>) read(publisher, context.charset());
    }

    public static Mono<String> read(
            Publisher<DataChunk> publisher, Charset charset) {

        return ByteArrayBodyReader.read(publisher).flatMap(new Mapper(charset));
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
