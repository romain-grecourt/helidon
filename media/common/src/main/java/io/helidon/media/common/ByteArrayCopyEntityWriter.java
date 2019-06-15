package io.helidon.media.common;

import io.helidon.common.http.ContentInfo;
import io.helidon.common.http.DataChunk;
import io.helidon.common.http.EntityWriter;
import io.helidon.common.http.MediaType;
import io.helidon.common.reactive.EmptyPublisher;
import io.helidon.common.reactive.Flow.Publisher;
import io.helidon.common.reactive.SingleItemPublisher;
import java.nio.charset.Charset;
import java.util.List;

/**
 * Entity writer for {@code byte[]} that copies the input byte array.
 */
public final class ByteArrayCopyEntityWriter implements EntityWriter<byte[]> {

    @Override
    public Promise accept(Object entity, List<MediaType> acceptedTypes) {
        if (entity.getClass().isAssignableFrom(byte[].class)) {
            return new Promise<>(new ContentInfo(
                    MediaType.APPLICATION_OCTET_STREAM), this);
        }
        return null;
    }

    @Override
    public Publisher<DataChunk> writeEntity(byte[] bytes,
            ContentInfo info, List<MediaType> acceptedTypes,
            Charset defaultCharset) {

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
