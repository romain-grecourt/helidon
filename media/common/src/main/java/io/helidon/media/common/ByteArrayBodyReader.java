package io.helidon.media.common;

import io.helidon.common.GenericType;
import io.helidon.common.http.DataChunk;
import io.helidon.common.http.Utils;
import io.helidon.common.reactive.Flow.Publisher;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import io.helidon.common.reactive.Multi;
import io.helidon.common.reactive.Mono;
import java.util.function.BiConsumer;
import java.util.function.Supplier;

/**
 * Message body reader for {@code ByteArrayOutputStream}.
 */
public final class ByteArrayBodyReader
        implements MessageBodyReader<ByteArrayOutputStream> {

    private static final Supplier<ByteArrayOutputStream> BAOS_SUPPLIER =
            ByteArrayOutputStream::new;

    private static final BiConsumer<ByteArrayOutputStream, DataChunk> COLLECTOR =
            new Collector();

    private ByteArrayBodyReader() {
    }

    @Override
    public boolean accept(GenericType<?> type,
            MessageBodyReaderContext context) {

        return ByteArrayOutputStream.class.isAssignableFrom(type.rawType());
    }

    @Override
    @SuppressWarnings("unchecked")
    public <U extends ByteArrayOutputStream> Mono<U> read(
            Publisher<DataChunk> publisher, GenericType<U> type,
            MessageBodyReaderContext context) {

        return (Mono<U>) read(publisher);
    }

    public static ByteArrayBodyReader create() {
        return new ByteArrayBodyReader();
    }

    public static Mono<ByteArrayOutputStream> read(Publisher<DataChunk> chunks) {
        return Multi.from(chunks).collect(BAOS_SUPPLIER, COLLECTOR);
    }

    private static final class Collector
            implements BiConsumer<ByteArrayOutputStream, DataChunk> {

        @Override
        public void accept(ByteArrayOutputStream baos, DataChunk chunk) {
            try {
                Utils.write(chunk.data(), baos);
            } catch (IOException e) {
                throw new IllegalArgumentException(
                        "Cannot convert byte buffer to a byte array!", e);
            } finally {
                chunk.release();
            }
        }
    }
}
