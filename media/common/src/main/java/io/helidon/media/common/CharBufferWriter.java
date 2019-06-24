package io.helidon.media.common;

import io.helidon.common.http.DataChunk;
import io.helidon.common.http.EntityWriter;
import io.helidon.common.http.MediaType;
import io.helidon.common.http.OutBoundScope;
import io.helidon.common.reactive.EmptyPublisher;
import io.helidon.common.reactive.FailedPublisher;
import io.helidon.common.reactive.Flow.Publisher;
import io.helidon.common.reactive.SingleItemPublisher;
import java.nio.charset.Charset;

/**
 * Entity writer for {@link CharBuffer}.
 */
public final class CharBufferWriter implements EntityWriter<CharBuffer> {

    @Override
    public Ack accept(Object entity, Class<?> type, OutBoundScope scope) {
        if (CharBuffer.class.isAssignableFrom(type)){
            return new Ack(MediaType.TEXT_PLAIN);
        }
        return null;
    }

    @Override
    public Publisher<DataChunk> writeEntity(CharBuffer buffer,
            OutBoundScope scope) {

        if (buffer == null || buffer.size() == 0) {
            return new EmptyPublisher<>();
        }
        try {
            return write(buffer, scope.charset());
        } catch (IllegalStateException ex) {
            return new FailedPublisher<>(ex);
        }
    }

    public static Publisher<DataChunk> write(CharBuffer buffer,
            Charset charset) {

        if (buffer == null || buffer.size() == 0) {
            return new EmptyPublisher<>();
        }
        DataChunk chunk = DataChunk.create(false,buffer.encode(charset));
        return new SingleItemPublisher<>(chunk);
    }
}
