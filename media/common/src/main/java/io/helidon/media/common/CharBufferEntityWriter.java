package io.helidon.media.common;

import io.helidon.common.http.ContentInfo;
import io.helidon.common.http.DataChunk;
import io.helidon.common.http.EntityWriter;
import io.helidon.common.http.MediaType;
import io.helidon.common.reactive.EmptyPublisher;
import io.helidon.common.reactive.FailedPublisher;
import io.helidon.common.reactive.Flow.Publisher;
import io.helidon.common.reactive.SingleItemPublisher;
import java.nio.charset.Charset;
import java.util.List;

/**
 * Entity writer for {@link CharBuffer}.
 */
public final class CharBufferEntityWriter implements EntityWriter<CharBuffer> {

    @Override
    public Promise accept(Object entity, List<MediaType> acceptedTypes) {
        if (CharBuffer.class.isAssignableFrom(entity.getClass())){
            return new Promise<>(new ContentInfo(MediaType.TEXT_PLAIN), this);
        }
        return null;
    }

    @Override
    public Publisher<DataChunk> writeEntity(CharBuffer buffer, ContentInfo info,
            List<MediaType> acceptedTypes, Charset defaultCharset) {

        if (buffer == null || buffer.size() == 0) {
            return new EmptyPublisher<>();
        }
        try {
            DataChunk chunk = DataChunk.create(false, buffer.encode(
                    info.charset(defaultCharset)));
            return new SingleItemPublisher<>(chunk);
        } catch (IllegalStateException ex) {
            return new FailedPublisher<>(ex);
        }
    }
}
