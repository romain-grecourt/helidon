package io.helidon.media.common;

import io.helidon.common.reactive.SingleInputProcessor;
import io.helidon.common.GenericType;
import io.helidon.common.http.DataChunk;
import io.helidon.common.http.MediaType;
import io.helidon.common.http.MessageBody.Writer;
import io.helidon.common.http.MessageBody.WriterContext;
import io.helidon.common.reactive.Flow.Publisher;
import java.io.ByteArrayOutputStream;

/**
 * Writer for {@link ByteArrayOutputStream}.
 */
public final class ByteArrayWriter implements Writer<ByteArrayOutputStream> {

    private final boolean copy;

    private ByteArrayWriter(boolean copy) {
        this.copy = copy;
    }

    @Override
    public boolean accept(GenericType<?> type, WriterContext context) {
        return byte[].class.isAssignableFrom(type.rawType());
    }

    @Override
    public <U extends ByteArrayOutputStream> Publisher<DataChunk> write(
            Publisher<U> content, GenericType<U> type, WriterContext context) {

        context.contentType(MediaType.APPLICATION_OCTET_STREAM);
        return write(content, copy);
    }

    public static Publisher<DataChunk> write(
            Publisher<? extends ByteArrayOutputStream> content, boolean copy) {

        Processor processor = new Processor(copy);
        content.subscribe(processor);
        return processor;
    }

    public static ByteArrayWriter create(boolean copy) {
        return new ByteArrayWriter(copy);
    }

    private static final class Processor
            extends SingleInputProcessor<ByteArrayOutputStream, DataChunk> {

        private final boolean copy;

        Processor(boolean copy) {
            this.copy = copy;
        }

        @Override
        protected DataChunk wrap(ByteArrayOutputStream data) {
            byte[] bytes;
            if (copy) {
                int size = data.size();
                bytes = new byte[size];
                System.arraycopy(data.toByteArray(), 0, bytes, 0, size);
            } else {
                bytes = data.toByteArray();
            }
            return DataChunk.create(false, bytes);
        }

        @Override
        public void onComplete() {
            complete();
        }
    }
}
