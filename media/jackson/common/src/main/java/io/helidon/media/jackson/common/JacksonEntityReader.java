package io.helidon.media.jackson.common;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.helidon.common.http.DataChunk;
import io.helidon.common.http.EntityReader;
import io.helidon.common.http.InBoundScope;
import io.helidon.common.reactive.Flow.Publisher;
import io.helidon.media.common.ByteArrayReader;
import java.io.IOException;
import java.util.Objects;
import java.util.concurrent.CompletionStage;

/**
 * Jackson entity reader.
 */
public final class JacksonEntityReader implements EntityReader<Object> {

    private final ObjectMapper objectMapper;

    public JacksonEntityReader(ObjectMapper objectMapper) {
        Objects.requireNonNull(objectMapper);
        this.objectMapper = objectMapper;
    }

    @Override
    public boolean accept(Class<?> type, InBoundScope scope) {
        return !CharSequence.class.isAssignableFrom(type)
                && objectMapper.canDeserialize(
                        objectMapper.constructType(type));
    }

    @Override
    public CompletionStage<Object> readEntity(Publisher<DataChunk> publisher,
            Class<Object> type, InBoundScope scope) {

        ValueReader<Object> valueReader = new ValueReader<>(type);
        return ByteArrayReader.read(publisher)
                .thenApply(valueReader::read);
    }

    private final class ValueReader<T> {

        private final Class<T> type;

        ValueReader(Class<T> type) {
            this.type = type;
        }

        T read(byte[] bytes) throws JacksonRuntimeException {
            try {
                return objectMapper.readValue(bytes, type);
            } catch (final IOException wrapMe) {
                throw new JacksonRuntimeException(wrapMe.getMessage(), wrapMe);
            }
        }
    }
}
