package io.helidon.media.jsonp.common;

import io.helidon.common.GenericType;
import io.helidon.common.http.DataChunk;
import io.helidon.common.reactive.Flow.Publisher;
import io.helidon.common.reactive.Mono;
import io.helidon.media.common.ByteArrayBodyReader;
import io.helidon.media.common.MessageBodyReader;
import io.helidon.media.common.MessageBodyReaderContext;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.util.Objects;
import java.util.function.Function;
import javax.json.JsonReader;
import javax.json.JsonReaderFactory;
import javax.json.JsonStructure;

/**
 * Message body reader for {@link JsonStructure} sub-classes (JSON-P).
 */
public final class JsonpBodyReader implements MessageBodyReader<JsonStructure> {

    private final JsonReaderFactory jsonFactory;

    public JsonpBodyReader(JsonReaderFactory jsonFactory) {
        Objects.requireNonNull(jsonFactory);
        this.jsonFactory = jsonFactory;
    }

    @Override
    public boolean accept(GenericType<?> type,
            MessageBodyReaderContext context) {

        return JsonStructure.class.isAssignableFrom(type.rawType());
    }

    @Override
    public <U extends JsonStructure> Mono<U> read(
            Publisher<DataChunk> publisher, GenericType<U> type,
            MessageBodyReaderContext context) {

        return ByteArrayBodyReader.read(publisher)
                .flatMap(new Mapper<>(jsonFactory, context.charset()));
    }

    private static final class Mapper<T extends JsonStructure>
            implements Function<ByteArrayOutputStream, Mono<T>> {

        private final JsonReaderFactory jsonFactory;
        private final Charset charset;

        Mapper(JsonReaderFactory jsonFactory, Charset charset) {
            this.jsonFactory = jsonFactory;
            this.charset = charset;
        }

        @Override
        @SuppressWarnings("unchecked")
        public Mono<T> apply(ByteArrayOutputStream baos) {
            InputStream is = new ByteArrayInputStream(baos.toByteArray());
            JsonReader reader = jsonFactory.createReader(is, charset);
            return Mono.just((T) reader.read());
        }
    }
}
