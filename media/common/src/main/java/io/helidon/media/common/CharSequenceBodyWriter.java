package io.helidon.media.common;

import io.helidon.common.GenericType;
import io.helidon.common.http.DataChunk;
import io.helidon.common.http.MediaType;
import io.helidon.common.reactive.Flow.Publisher;
import io.helidon.common.reactive.Mono;
import io.helidon.common.reactive.MultiMapper;
import java.nio.charset.Charset;

/**
 * Writer for {@code CharSequence}.
 */
public final class CharSequenceBodyWriter
        implements MessageBodyWriter<CharSequence> {

    /**
     * Singleton instance.
     */
    private static final CharSequenceBodyWriter INSTANCE =
            new CharSequenceBodyWriter();

    /**
     * Enforce the use of {@link #get()}.
     */
    private CharSequenceBodyWriter() {
    }

    @Override
    public boolean accept(GenericType<?> type,
            MessageBodyWriterContext context) {

        return CharSequence.class.isAssignableFrom(type.rawType());
    }

    @Override
    public Publisher<DataChunk> write(Mono<CharSequence> content,
            GenericType<? extends CharSequence> type,
            MessageBodyWriterContext context) {

        context.contentType(MediaType.TEXT_PLAIN);
        // TODO cache per charset.
        return content.mapMany(new CharSequenceToChunks(
                context.charset()));
    }

    /**
     * Get the {@link CharSequenceBodyWriter} singleton.
     * @return CharSequenceBodyWriter
     */
    public static CharSequenceBodyWriter get() {
        return INSTANCE;
    }

    /**
     * Implementation of {@link MultiMapper} to convert {@link CharSequence} to
     * a publisher of {@link DataChunk}.
     */
    private static final class CharSequenceToChunks
            implements MultiMapper<CharSequence, DataChunk> {

        private final Charset charset;

        CharSequenceToChunks(Charset charset) {
            this.charset = charset;
        }

        @Override
        public Publisher<DataChunk> map(CharSequence cs) {
            return ContentWriters.writeCharSequence(cs, charset);
        }
    }
}