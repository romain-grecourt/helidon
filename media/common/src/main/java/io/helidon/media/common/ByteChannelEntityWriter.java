package io.helidon.media.common;

import io.helidon.common.http.ContentInfo;
import io.helidon.common.http.DataChunk;
import io.helidon.common.http.EntityWriter;
import io.helidon.common.http.MediaType;
import io.helidon.common.reactive.Flow.Publisher;
import io.helidon.common.reactive.RetrySchema;
import java.nio.channels.ReadableByteChannel;
import java.nio.charset.Charset;
import java.util.List;

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
    public Promise accept(Object entity, List<MediaType> acceptedTypes) {
        if (ReadableByteChannel.class.isAssignableFrom(entity.getClass())) {
            return new Promise<>(new ContentInfo(
                    MediaType.APPLICATION_OCTET_STREAM), this);
        }
        return null;
    }

    @Override
    public Publisher<DataChunk> writeEntity(ReadableByteChannel channel,
            ContentInfo info, List<MediaType> acceptedTypes,
            Charset defaultCharset) {

        return new ReadableByteChannelPublisher(channel, schema);
    }
}
