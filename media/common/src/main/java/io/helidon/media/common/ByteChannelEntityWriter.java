package io.helidon.media.common;

import io.helidon.common.http.DataChunk;
import io.helidon.common.http.EntityWriter;
import io.helidon.common.http.MediaType;
import io.helidon.common.http.OutBoundScope;
import io.helidon.common.reactive.Flow.Publisher;
import io.helidon.common.reactive.RetrySchema;
import java.nio.channels.ReadableByteChannel;

/**
 * EntityWriter for {@link ReadableByteChannel}.
 */
public final class ByteChannelEntityWriter
        implements EntityWriter<ReadableByteChannel> {

    static final RetrySchema DEFAULT_RETRY_SCHEMA =
            RetrySchema.linear(0, 10, 250);

    private final RetrySchema schema;

    public ByteChannelEntityWriter() {
        this.schema = DEFAULT_RETRY_SCHEMA;
    }

    public ByteChannelEntityWriter(RetrySchema schema) {
        this.schema = schema;
    }

    @Override
    public Ack<ReadableByteChannel> accept(Object entity, OutBoundScope scope) {
        if (ReadableByteChannel.class.isAssignableFrom(entity.getClass())) {
            return new Ack<>(this, MediaType.APPLICATION_OCTET_STREAM);
        }
        return null;
    }

    @Override
    public Publisher<DataChunk> writeEntity(ReadableByteChannel channel,
            Ack<ReadableByteChannel> ack, OutBoundScope scope) {

        return new ReadableByteChannelPublisher(channel, schema);
    }
}
