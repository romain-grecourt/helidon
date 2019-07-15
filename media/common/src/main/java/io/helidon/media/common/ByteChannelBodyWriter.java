package io.helidon.media.common;

import io.helidon.common.GenericType;
import io.helidon.common.http.DataChunk;
import io.helidon.common.http.MediaType;
import io.helidon.common.reactive.Flow.Publisher;
import io.helidon.common.reactive.Mono;
import io.helidon.common.reactive.MultiMapper;
import io.helidon.common.reactive.RetrySchema;
import java.nio.channels.ReadableByteChannel;

/**
 * Message body writer for {@link ReadableByteChannel}.
 */
public final class ByteChannelBodyWriter
        implements MessageBodyWriter<ReadableByteChannel> {

    static final RetrySchema DEFAULT_RETRY_SCHEMA =
            RetrySchema.linear(0, 10, 250);

    private static final ByteChannelBodyWriter DEFAULT_INSTANCE =
            new ByteChannelBodyWriter(DEFAULT_RETRY_SCHEMA);

    private final ByteChannelToChunks mapper;

    /**
     * Enforce the use of the static factory method.
     *
     * @param schema retry schema
     */
    private ByteChannelBodyWriter(RetrySchema schema) {
        this.mapper = new ByteChannelToChunks(schema);
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
        return content.mapMany(mapper);
    }

    /**
     * Create a new writer instance.
     * @param schema retry schema
     * @return ByteChannelWriter
     */
    public static ByteChannelBodyWriter create(RetrySchema schema) {
        return new ByteChannelBodyWriter(schema);
    }

    /**
     * Get the singleton instance that uses the default retry schema.
     * @return ByteChannelBodyWriter
     */
    public static ByteChannelBodyWriter get() {
        return DEFAULT_INSTANCE;
    }

    /**
     * Implementation of {@link MultiMapper} that converts a
     * {@link ReadableByteChannel} to a publisher of {@link DataChunk}.
     */
    private static final class ByteChannelToChunks
            implements MultiMapper<ReadableByteChannel, DataChunk> {

        private final RetrySchema schema;

        ByteChannelToChunks(RetrySchema schema) {
            this.schema = schema;
        }

        @Override
        public Publisher<DataChunk> map(ReadableByteChannel channel) {
            return new ReadableByteChannelPublisher(channel, schema);
        }
    }
}
