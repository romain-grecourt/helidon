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
 * Entity writer for {@code CharSequence}.
 */
public final class CharSequenceWriter
        implements EntityWriter<CharSequence> {

    @Override
    public Ack accept(Object entity, Class<?> type, OutBoundScope scope) {
        if (CharSequence.class.isAssignableFrom(type)){
            return new Ack(MediaType.TEXT_PLAIN);
        }
        return null;
    }

    @Override
    public Publisher<DataChunk> writeEntity(CharSequence cs,
            OutBoundScope scope) {

        try {
            return write(cs, scope.charset());
        } catch (IllegalStateException ex) {
            return new FailedPublisher<>(ex);
        }
    }

    public static Publisher<DataChunk> write(CharSequence cs, Charset charset) {
        if (cs == null || cs.length() == 0) {
            return new EmptyPublisher<>();
        }
        DataChunk chunk = DataChunk.create(false, charset
                .encode(cs.toString()));
        return new SingleItemPublisher<>(chunk);
    }
}
