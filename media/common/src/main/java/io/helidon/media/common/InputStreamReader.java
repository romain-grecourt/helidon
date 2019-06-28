package io.helidon.media.common;

import io.helidon.common.GenericType;
import io.helidon.common.http.DataChunk;
import io.helidon.common.reactive.Flow.Publisher;
import java.io.InputStream;
import io.helidon.common.http.MessageBody.Reader;
import io.helidon.common.http.MessageBody.ReaderContext;
import io.helidon.common.reactive.SingleItemPublisher;

/**
 * Entity reader for {@code InputStream}.
 */
public class InputStreamReader implements Reader<InputStream> {

    private InputStreamReader() {
    }

    @Override
    public boolean accept(GenericType<?> type, ReaderContext context) {
        return InputStream.class.isAssignableFrom(type.rawType());
    }

    @Override
    @SuppressWarnings("unchecked")
    public <U extends InputStream> Publisher<U> read(
            Publisher<DataChunk> publisher, GenericType<U> type,
            ReaderContext context) {

        return new SingleItemPublisher<>((U) new PublisherInputStream(publisher));
    }

    public static InputStreamReader create() {
        return new InputStreamReader();
    }
}
