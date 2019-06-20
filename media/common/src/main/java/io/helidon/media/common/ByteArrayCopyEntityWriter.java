package io.helidon.media.common;

import io.helidon.common.http.DataChunk;
import io.helidon.common.http.EntityWriter;
import io.helidon.common.http.MediaType;
import io.helidon.common.http.OutBoundScope;
import io.helidon.common.reactive.EmptyPublisher;
import io.helidon.common.reactive.Flow.Publisher;
import io.helidon.common.reactive.SingleItemPublisher;

/**
 * Entity writer for {@code byte[]} that copies the input byte array.
 */
public final class ByteArrayCopyEntityWriter implements EntityWriter<byte[]> {

    @Override
    public Ack<byte[]> accept(Object entity, OutBoundScope scope) {
        if (entity.getClass().isAssignableFrom(byte[].class)) {
            return new Ack<>(this, MediaType.APPLICATION_OCTET_STREAM);
        }
        return null;
    }

    @Override
    public Publisher<DataChunk> writeEntity(byte[] bytes, Ack<byte[]> ack,
            OutBoundScope scope) {

        return write(bytes);
    }

    public static Publisher<DataChunk> write(byte[] bytes) {
        if (bytes == null || bytes.length == 0) {
            return new EmptyPublisher<>();
        }
        byte[] bytesCopy = new byte[bytes.length];
        System.arraycopy(bytes, 0, bytesCopy, 0, bytes.length);
        DataChunk chunk = DataChunk.create(false, bytesCopy);
        return new SingleItemPublisher<>(chunk);
    }
}
