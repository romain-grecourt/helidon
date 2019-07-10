package io.helidon.media.jackson.common;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.helidon.common.GenericType;
import io.helidon.common.http.DataChunk;
import io.helidon.common.http.MessageBody.Reader;
import io.helidon.common.http.MessageBody.ReaderContext;
import io.helidon.common.reactive.Flow.Publisher;
import io.helidon.common.reactive.Mono;
import io.helidon.media.common.ByteArrayReader;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Objects;
import java.util.function.Function;

/**
 * Jackson content reader.
 */
public final class JacksonReader implements Reader<Object> {

    private final ObjectMapper objectMapper;

    public JacksonReader(ObjectMapper objectMapper) {
        Objects.requireNonNull(objectMapper);
        this.objectMapper = objectMapper;
    }

    @Override
    public boolean accept(GenericType<?> type, ReaderContext context) {
        Class<?> clazz = type.rawType();
        return !CharSequence.class.isAssignableFrom(clazz)
                && objectMapper.canDeserialize(
                        objectMapper.constructType(clazz));
    }

    @Override
    public <U extends Object> Mono<U> read(Publisher<DataChunk> publisher,
            GenericType<U> type, ReaderContext context) {

        return ByteArrayReader.read(publisher)
                .flatMap(new Mapper<>(type, objectMapper));
    }

    private static final class Mapper<T>
            implements Function<ByteArrayOutputStream, Mono<T>> {

        private final GenericType<? super T> type;
        private final ObjectMapper objectMapper;

        Mapper(GenericType<? super T> type, ObjectMapper objectMapper) {
            this.type = type;
            this.objectMapper = objectMapper;
        }

        @Override
        public Mono<T> apply(ByteArrayOutputStream baos) {
            try {
                return Mono.just(objectMapper.readValue(baos.toByteArray(),
                        (Class<T>) type.rawType()));
            } catch (final IOException wrapMe) {
                return Mono.<T>error(
                        new JacksonRuntimeException(wrapMe.getMessage(),
                        wrapMe));
            }
        }
    }
}
