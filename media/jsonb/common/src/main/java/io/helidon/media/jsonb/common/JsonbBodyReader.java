package io.helidon.media.jsonb.common;

import io.helidon.common.GenericType;
import io.helidon.common.http.DataChunk;
import io.helidon.common.reactive.Flow.Publisher;
import io.helidon.common.reactive.Mapper;
import io.helidon.common.reactive.Mono;
import io.helidon.media.common.ContentReaders;
import io.helidon.media.common.MessageBodyReader;
import io.helidon.media.common.MessageBodyReaderContext;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Objects;
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

        return ContentReaders.readBytes(publisher)
                .map(new BytesToObject<>(type, jsonb));
    }

    private static final class BytesToObject<T>
            implements Mapper<byte[], T> {

        private final GenericType<? super T> type;
        private final Jsonb jsonb;

        BytesToObject(GenericType<? super T> type, Jsonb jsonb) {
            this.type = type;
            this.jsonb = jsonb;
        }

        @Override
        public T map(byte[] bytes) {
            try (InputStream inputStream = new ByteArrayInputStream(bytes)) {
                return jsonb.fromJson(inputStream, type.type());
            } catch (IOException ex) {
                throw new JsonbException(ex.getMessage(), ex);
            }
        }
    }
}
