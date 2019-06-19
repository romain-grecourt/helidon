package io.helidon.media.jsonp.common;

import io.helidon.common.http.DataChunk;
import io.helidon.common.http.EntityReader;
import io.helidon.common.http.InBoundScope;
import io.helidon.common.reactive.Flow.Publisher;
import io.helidon.media.common.ByteArrayEntityReader;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.util.Objects;
import java.util.concurrent.CompletionStage;
import javax.json.JsonException;
import javax.json.JsonReader;
import javax.json.JsonReaderFactory;
import javax.json.JsonStructure;

/**
 * JSON-P entity reader.
 */
public class JsonEntityReader implements EntityReader<JsonStructure> {

    private final JsonReaderFactory jsonReaderFactory;

    public JsonEntityReader(JsonReaderFactory jsonReaderFactory) {
        Objects.requireNonNull(jsonReaderFactory);
        this.jsonReaderFactory = jsonReaderFactory;
    }

    @Override
    public boolean accept(Class<?> type, InBoundScope scope) {
        return JsonStructure.class.isAssignableFrom(type);
    }

    @Override
    public CompletionStage<? extends JsonStructure> readEntity(
            Publisher<DataChunk> publisher, Class<? super JsonStructure> type,
            InBoundScope scope) {

        ValueReader<? extends JsonStructure> valueReader =
                new ValueReader<>(scope.charset());
        return ByteArrayEntityReader.read(publisher)
                .thenApply(valueReader::read);
    }

    private final class ValueReader<T> {

        private final Charset charset;

        ValueReader(Charset charset) {
            this.charset = charset;
        }

        @SuppressWarnings("")
        T read(byte[] bytes) throws JsonException {
            InputStream is = new ByteArrayInputStream(bytes);
            JsonReader reader = jsonReaderFactory.createReader(is, charset);
            return (T) reader.read();
        }
    }
}
