package io.helidon.media.jackson.common;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.helidon.common.http.DataChunk;
import io.helidon.common.http.EntityWriter;
import io.helidon.common.http.MediaType;
import io.helidon.common.http.OutBoundScope;
import io.helidon.common.reactive.FailedPublisher;
import io.helidon.common.reactive.Flow.Publisher;
import io.helidon.media.common.CharBuffer;
import io.helidon.media.common.CharBufferWriter;
import java.io.IOException;
import java.util.Objects;

/**
 * Jackson entity writer.
 */
public class JacksonEntityWriter implements EntityWriter<Object> {

    private final ObjectMapper objectMapper;

    public JacksonEntityWriter(ObjectMapper objectMapper) {
        Objects.requireNonNull(objectMapper);
        this.objectMapper = objectMapper;
    }

    @Override
    public Ack accept(Object entity, Class<?> type, OutBoundScope scope) {
        if (entity != null
                && !(entity instanceof CharSequence)
                && objectMapper.canSerialize(entity.getClass())) {

            MediaType contentType = scope.findAccepted(MediaType.JSON_PREDICATE,
                    MediaType.APPLICATION_JSON);
            if (contentType != null) {
                return new Ack(contentType);
            }
        }
        return null;
    }

    @Override
    public Publisher<DataChunk> writeEntity(Object entity,
            OutBoundScope scope) {

        try {
            CharBuffer buffer = new CharBuffer();
            objectMapper.writeValue(buffer, entity);
            return CharBufferWriter.write(buffer, scope.charset());
        } catch (IOException wrapMe) {
            return new FailedPublisher<>(new JacksonRuntimeException(
                    wrapMe.getMessage(), wrapMe));
        } catch (IllegalStateException ex) {
            return new FailedPublisher<>(ex);
        }
    }
}
