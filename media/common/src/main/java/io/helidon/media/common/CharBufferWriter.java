package io.helidon.media.common;

import io.helidon.common.reactive.SingleInputProcessor;
import io.helidon.common.GenericType;
import io.helidon.common.http.DataChunk;
import io.helidon.common.http.MediaType;
import io.helidon.common.http.MessageBody.Writer;
import io.helidon.common.http.MessageBody.WriterContext;
import io.helidon.common.reactive.Flow.Publisher;
import java.nio.charset.Charset;

/**
 * Writer for {@link CharBuffer}.
 */
public final class CharBufferWriter implements Writer<CharBuffer> {

    private CharBufferWriter() {
    }

    @Override
    public boolean accept(GenericType<?> type, WriterContext context) {
        return CharBuffer.class.isAssignableFrom(type.rawType());
    }

    @Override
    public <U extends CharBuffer> Publisher<DataChunk> write(
            Publisher<U> content, GenericType<U> type, WriterContext context) {

        context.contentType(MediaType.TEXT_PLAIN);
        return write(content, context.charset());
    }

    public static Publisher<DataChunk> write(Publisher<? extends CharBuffer>
            content, Charset charset) {

        Processor processor = new Processor(charset);
        content.subscribe(processor);
        return processor;
    }

    public static CharBufferWriter create() {
        return new CharBufferWriter();
    }

    private static final class Processor
            extends SingleInputProcessor<CharBuffer, DataChunk> {

        private final Charset charset;

        Processor(Charset charset) {
            this.charset = charset;
        }

        @Override
        protected DataChunk wrap(CharBuffer buffer) {
            return DataChunk.create(false, buffer.encode(charset));
        }

        @Override
        public void onComplete() {
            complete();
        }
    }
}
