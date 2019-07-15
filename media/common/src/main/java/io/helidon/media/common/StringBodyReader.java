package io.helidon.media.common;

import io.helidon.common.GenericType;
import io.helidon.common.http.DataChunk;
import io.helidon.common.reactive.Flow.Publisher;
import io.helidon.common.reactive.Mono;

/**
 * Message body reader for {@link String}.
 */
public final class StringBodyReader implements MessageBodyReader<String> {

    /**
     * Singleton instance.
     */
    private static final StringBodyReader INSTANCE = new StringBodyReader();

    /**
     * Private to enforce the use of {@link #get()}.
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

        return (Mono<U>) ContentReaders.readString(publisher,
                context.charset());
    }

    /**
     * Create a new {@link StringBodyReader} instance.
     * @return StringBodyReader
     */
    public static StringBodyReader get() {
        return INSTANCE;
    }
}
