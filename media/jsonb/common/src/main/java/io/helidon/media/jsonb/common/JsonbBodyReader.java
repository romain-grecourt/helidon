package io.helidon.media.jsonb.common;

import io.helidon.common.GenericType;
import io.helidon.common.http.DataChunk;
import io.helidon.common.reactive.Flow.Publisher;
import io.helidon.common.reactive.Mono;
import io.helidon.media.common.ByteArrayBodyReader;
import io.helidon.media.common.MessageBodyReader;
import io.helidon.media.common.MessageBodyReaderContext;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Objects;
import java.util.function.Function;
import javax.json.bind.Jsonb;
import javax.json.bind.JsonbException;

/**
 * Message body writer supporting object binding with JSON-B.
 */
public class JsonbBodyReader implements MessageBodyReader<Object> {

    private final Jsonb jsonb;

    public JsonbBodyReader(Jsonb jsonb) {
        Objects.requireNonNull(jsonb);
        this.jsonb = jsonb;
    }

    @Override
    public boolean accept(GenericType<?> type,
            MessageBodyReaderContext context) {

        return !CharSequence.class.isAssignableFrom(type.rawType());
    }

    @Override
    public <U extends Object> Mono<U> read(Publisher<DataChunk> publisher,
            GenericType<U> type, MessageBodyReaderContext context) {

        return ByteArrayBodyReader.read(publisher)
                .flatMap(new Mapper<>(type, jsonb));
    }

    private static final class Mapper<T>
            implements Function<ByteArrayOutputStream, Mono<T>> {

        private final GenericType<? super T> type;
        private final Jsonb jsonb;

        Mapper(GenericType<? super T> type, Jsonb jsonb) {
            this.type = type;
            this.jsonb = jsonb;
        }

        @Override
        public Mono<T> apply(ByteArrayOutputStream baos) {
            try (InputStream inputStream =
                    new ByteArrayInputStream(baos.toByteArray())) {
                T object = jsonb.fromJson(inputStream, type.type());
                return Mono.just(object);
            } catch (IOException ex) {
                return Mono.<T>error(new JsonbException(ex.getMessage(), ex));
            }
        }
    }
}
