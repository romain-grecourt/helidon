package io.helidon.media.common;

import io.helidon.common.http.DataChunk;
import io.helidon.common.http.EntityWriter;
import io.helidon.common.http.MediaType;
import io.helidon.common.http.OutBoundScope;
import io.helidon.common.reactive.EmptyPublisher;
import io.helidon.common.reactive.Flow.Publisher;
import io.helidon.common.reactive.SingleItemPublisher;

/**
 * Entity writer for {@code byte[]}.
 */
public final class ByteArrayEntityWriter implements EntityWriter<byte[]> {

    @Override
    public Ack accept(Object entity, Class<?> type, OutBoundScope scope) {
        if (byte[].class.isAssignableFrom(type)) {
            return new Ack(MediaType.APPLICATION_OCTET_STREAM);
        }
        return null;
    }

    @Override
    public Publisher<DataChunk> writeEntity(byte[] bytes, OutBoundScope scope) {
        return write(bytes);
    }

    public static Publisher<DataChunk> write(byte[] bytes) {
        if (bytes == null || bytes.length == 0) {
            return new EmptyPublisher<>();
        }
        DataChunk chunk = DataChunk.create(false, bytes);
        return new SingleItemPublisher<>(chunk);
    }
}
