package io.helidon.media.common;

import io.helidon.common.reactive.SingleInputProcessor;
import io.helidon.common.GenericType;
import io.helidon.common.http.DataChunk;
import io.helidon.common.http.MessageBody.Reader;
import io.helidon.common.http.MessageBody.ReaderContext;
import io.helidon.common.reactive.Flow.Publisher;
import java.nio.charset.Charset;

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
    public <U extends String> Publisher<U> read(Publisher<DataChunk> publisher,
            GenericType<U> type, ReaderContext context) {

        return read(publisher, context.charset());
    }

    public static <U extends String> Publisher<U> read(
            Publisher<DataChunk> publisher, Charset charset) {

        Processor processor = new Processor(charset);
        ByteArrayReader.read(publisher).subscribe(processor);
        return processor;
    }

    public static StringReader create() {
        return new StringReader();
    }

    private static final class Processor <U extends String>
            extends SingleInputProcessor<byte[], U> {

        private final Charset charset;

        Processor(Charset charset) {
            this.charset = charset;
        }

        @Override
        protected U wrap(byte[] data) {
            return (U) new String(data, charset);
        }

        @Override
        public void onComplete() {
            complete();
        }
    }
}
