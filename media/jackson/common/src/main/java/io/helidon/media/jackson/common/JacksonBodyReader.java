package io.helidon.media.jackson.common;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.helidon.common.GenericType;
import io.helidon.common.http.DataChunk;
import io.helidon.common.reactive.Flow.Publisher;
import io.helidon.common.reactive.Mapper;
import io.helidon.common.reactive.Mono;
import io.helidon.media.common.ContentReaders;
import io.helidon.media.common.MessageBodyReader;
import io.helidon.media.common.MessageBodyReaderContext;
import java.io.IOException;
import java.util.Objects;

/**
 * Message body reader supporting object binding with Jackson.
 */
public final class JacksonBodyReader implements MessageBodyReader<Object> {

    private final ObjectMapper objectMapper;

    public JacksonBodyReader(ObjectMapper objectMapper) {
        Objects.requireNonNull(objectMapper);
        this.objectMapper = objectMapper;
    }

    @Override
    public boolean accept(GenericType<?> type,
            MessageBodyReaderContext context) {

        Class<?> clazz = type.rawType();
        return !CharSequence.class.isAssignableFrom(clazz)
                && objectMapper.canDeserialize(
                        objectMapper.constructType(clazz));
    }

    @Override
    public <U extends Object> Mono<U> read(Publisher<DataChunk> publisher,
            GenericType<U> type, MessageBodyReaderContext context) {

        return ContentReaders.readBytes(publisher)
                .map(new BytesToObject<>(type, objectMapper));
    }

    private static final class BytesToObject<T>
            implements Mapper<byte[], T> {

        private final GenericType<? super T> type;
        private final ObjectMapper objectMapper;

        BytesToObject(GenericType<? super T> type,
                ObjectMapper objectMapper) {

            this.type = type;
            this.objectMapper = objectMapper;
        }

        @Override
        public T map(byte[] bytes) {
            try {
                return objectMapper.readValue(bytes, (Class<T>) type.rawType());
            } catch (final IOException wrapMe) {
                throw new JacksonRuntimeException(wrapMe.getMessage(), wrapMe);
            }
        }
    }
}
