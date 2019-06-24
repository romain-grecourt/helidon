package io.helidon.media.jsonb.common;

import io.helidon.common.http.DataChunk;
import io.helidon.common.http.EntityReader;
import io.helidon.common.http.InBoundScope;
import io.helidon.common.reactive.Flow.Publisher;
import io.helidon.media.common.ByteArrayReader;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Objects;
import java.util.concurrent.CompletionStage;
import javax.json.bind.Jsonb;
import javax.json.bind.JsonbException;

/**
 * JSON-B entity reader.
 */
public class JsonBindingEntityReader implements EntityReader<Object> {

    private final Jsonb jsonb;

    public JsonBindingEntityReader(Jsonb jsonb) {
        Objects.requireNonNull(jsonb);
        this.jsonb = jsonb;
    }

    @Override
    public boolean accept(Class<?> type, InBoundScope scope) {
        return !CharSequence.class.isAssignableFrom(type);
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

        T read(byte[] bytes) throws JsonbException {
            try (InputStream inputStream = new ByteArrayInputStream(bytes)) {
                return jsonb.fromJson(inputStream, type);
            } catch (final IOException ioException) {
                throw new JsonbException(ioException.getMessage(), ioException);
            }
        }
    }
}
