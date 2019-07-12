package io.helidon.media.common;

import io.helidon.common.GenericType;
import io.helidon.common.http.DataChunk;
import io.helidon.common.http.MediaType;
import io.helidon.common.reactive.Flow.Publisher;
import io.helidon.common.reactive.Mono;
import io.helidon.common.reactive.RetrySchema;
import java.nio.channels.ReadableByteChannel;
import java.util.function.Function;

/**
 * Message body writer for {@link ReadableByteChannel}.
 */
public final class ByteChannelWriter
        implements MessageBodyWriter<ReadableByteChannel> {

    static final RetrySchema DEFAULT_RETRY_SCHEMA =
            RetrySchema.linear(0, 10, 250);

    private final Mapper mapper;

    private ByteChannelWriter(RetrySchema schema) {
        this.mapper = new Mapper(schema);
    }

    @Override
    public boolean accept(GenericType<?> type,
            MessageBodyWriterContext context) {

        return ReadableByteChannel.class.isAssignableFrom(type.rawType());
    }

    @Override
    public Publisher<DataChunk> write(Mono<ReadableByteChannel> content,
            GenericType<? extends ReadableByteChannel> type,
            MessageBodyWriterContext context) {

            context.contentType(MediaType.APPLICATION_OCTET_STREAM);
        return content.flatMapMany(mapper);
    }

    public static ByteChannelWriter create(RetrySchema schema) {
        return new ByteChannelWriter(schema);
    }

    public static ByteChannelWriter create() {
        return new ByteChannelWriter(DEFAULT_RETRY_SCHEMA);
    }

    private static final class Mapper
            implements Function<ReadableByteChannel, Publisher<DataChunk>> {

        private final RetrySchema schema;

        Mapper(RetrySchema schema) {
            this.schema = schema;
        }

        @Override
        public Publisher<DataChunk> apply(ReadableByteChannel channel) {
            return new ReadableByteChannelPublisher(channel, schema);
        }
    }
}
