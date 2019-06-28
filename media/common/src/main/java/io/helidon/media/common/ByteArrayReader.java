package io.helidon.media.common;

import io.helidon.common.reactive.SingleOutputProcessor;
import io.helidon.common.GenericType;
import io.helidon.common.http.DataChunk;
import io.helidon.common.http.Utils;
import io.helidon.common.reactive.Flow.Publisher;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import io.helidon.common.http.MessageBody.Reader;
import io.helidon.common.http.MessageBody.ReaderContext;

/**
 * Reader for {@code ByteArrayOutputStream}.
 */
public final class ByteArrayReader implements Reader<ByteArrayOutputStream> {

    private ByteArrayReader() {
    }

    @Override
    public boolean accept(GenericType<?> type, ReaderContext context) {
        return byte[].class.isAssignableFrom(type.rawType());
    }

    @Override
    public <U extends ByteArrayOutputStream> Publisher<U> read(
            Publisher<DataChunk> publisher, GenericType<U> type,
            ReaderContext context) {

        return read(publisher);
    }

    public static ByteArrayReader create() {
        return new ByteArrayReader();
    }

    public static <U extends ByteArrayOutputStream> Publisher<U> read(
            Publisher<DataChunk> publisher) {

        Processor processor = new Processor();
        publisher.subscribe(processor);
        return processor;
    }

    private static final class Processor<U extends ByteArrayOutputStream>
            extends SingleOutputProcessor<ByteArrayOutputStream, DataChunk> {

        private final ByteArrayOutputStream bytes = new ByteArrayOutputStream();

        @Override
        public void onNext(DataChunk item) {
            try {
                Utils.write(item.data(), bytes);
            } catch (IOException e) {
                onError(new IllegalArgumentException(
                        "Cannot convert byte buffer to a byte array!", e));
            } finally {
                item.release();
            }
        }

        @Override
        public void onComplete() {
            submit(bytes);
        }
    }
}
