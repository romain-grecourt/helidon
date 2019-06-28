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
 * Writer for {@code CharSequence}.
 */
public final class CharSequenceWriter implements Writer<CharSequence> {

    private CharSequenceWriter() {
    }

    @Override
    public boolean accept(GenericType<?> type, WriterContext context) {
        return CharSequence.class.isAssignableFrom(type.rawType());
    }

    @Override
    public <U extends CharSequence> Publisher<DataChunk> write(
            Publisher<U> content, GenericType<U> type, WriterContext context) {

        context.contentType(MediaType.TEXT_PLAIN);
        return write(content, context.charset());
    }

    public static Publisher<DataChunk> write(
            Publisher<? extends CharSequence> content, Charset charset) {

        Processor processor = new Processor(charset);
        content.subscribe(processor);
        return processor;
    }

    public static CharSequenceWriter create() {
        return new CharSequenceWriter();
    }

    private static final class Processor
            extends SingleInputProcessor<CharSequence, DataChunk> {

        private final Charset charset;

        Processor(Charset charset) {
            this.charset = charset;
        }

        @Override
        protected DataChunk wrap(CharSequence data) {
            return DataChunk.create(false, charset.encode(data.toString()));
        }

        @Override
        public void onComplete() {
            complete();
        }
    }
}
