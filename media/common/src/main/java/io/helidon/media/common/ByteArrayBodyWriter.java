package io.helidon.media.common;

import io.helidon.common.GenericType;
import io.helidon.common.http.DataChunk;
import io.helidon.common.http.MediaType;
import io.helidon.common.reactive.Flow.Publisher;
import io.helidon.common.reactive.Mono;
import java.io.ByteArrayOutputStream;
import java.util.function.Function;

/**
 * Message body writer for {@link ByteArrayOutputStream}.
 */
public final class ByteArrayBodyWriter
        implements MessageBodyWriter<ByteArrayOutputStream> {

    private static final Mapper MAPPER = new Mapper(false);
    private static final Mapper COPY_MAPPER = new Mapper(true);

    private final boolean copy;

    private ByteArrayBodyWriter(boolean copy) {
        this.copy = copy;
    }

    @Override
    public boolean accept(GenericType<?> type,
            MessageBodyWriterContext context) {

        return ByteArrayOutputStream.class.isAssignableFrom(type.rawType());
    }

    @Override
    public Publisher<DataChunk> write(Mono<ByteArrayOutputStream> content,
            GenericType<? extends ByteArrayOutputStream> type,
            MessageBodyWriterContext context) {

        context.contentType(MediaType.APPLICATION_OCTET_STREAM);
        return write(content, copy);
    }

    public static Publisher<DataChunk> write(
            Mono<ByteArrayOutputStream> content, boolean copy) {

        Mapper mapper;
        if (copy) {
            mapper = COPY_MAPPER;
        } else {
            mapper = MAPPER;
        }
        return content.flatMapMany(mapper);
    }

    public static ByteArrayBodyWriter create(boolean copy) {
        return new ByteArrayBodyWriter(copy);
    }

    static Publisher<DataChunk> write(byte[] bytes, boolean copy) {
        byte[] data;
        if (copy) {
            data = new byte[bytes.length];
            System.arraycopy(bytes, 0, data, 0, bytes.length);
        } else {
            data = bytes;
        }
        return Mono.just(DataChunk.create(false, data));
    }

    private static final class Mapper
            implements Function<ByteArrayOutputStream, Publisher<DataChunk>> {

        private final boolean copy;

        Mapper(boolean copy) {
            this.copy = copy;
        }

        @Override
        public Publisher<DataChunk> apply(ByteArrayOutputStream baos) {
            return write(baos.toByteArray(), copy);
        }
    }
}
