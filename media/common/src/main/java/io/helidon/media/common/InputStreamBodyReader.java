package io.helidon.media.common;

import io.helidon.common.GenericType;
import io.helidon.common.http.DataChunk;
import io.helidon.common.reactive.Flow.Publisher;
import java.io.InputStream;
import io.helidon.common.reactive.Mono;

/**
 * Message body reader for {@link InputStream}.
 */
public class InputStreamBodyReader implements MessageBodyReader<InputStream> {

    private InputStreamBodyReader() {
    }

    @Override
    public boolean accept(GenericType<?> type, MessageBodyReaderContext context) {
        return InputStream.class.isAssignableFrom(type.rawType());
    }

    @Override
    @SuppressWarnings("unchecked")
    public <U extends InputStream> Mono<U> read(
            Publisher<DataChunk> publisher, GenericType<U> type,
            MessageBodyReaderContext context) {

        return (Mono<U>) read(publisher);
    }

    public static Mono<InputStream> read(Publisher<DataChunk> publisher) {
        return Mono.just(new PublisherInputStream(publisher));
    }

    public static InputStreamBodyReader create() {
        return new InputStreamBodyReader();
    }
}
