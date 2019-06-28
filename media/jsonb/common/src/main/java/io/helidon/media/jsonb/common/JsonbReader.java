package io.helidon.media.jsonb.common;

import io.helidon.common.GenericType;
import io.helidon.common.http.DataChunk;
import io.helidon.common.http.MessageBody.Reader;
import io.helidon.common.http.MessageBody.ReaderContext;
import io.helidon.common.reactive.Flow.Publisher;
import io.helidon.media.common.ByteArrayReader;
import io.helidon.common.reactive.SingleInputProcessor;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Objects;
import javax.json.bind.Jsonb;
import javax.json.bind.JsonbException;

/**
 * JSON-B reader.
 */
public class JsonbReader implements Reader<Object> {

    private final Jsonb jsonb;

    public JsonbReader(Jsonb jsonb) {
        Objects.requireNonNull(jsonb);
        this.jsonb = jsonb;
    }

    @Override
    public boolean accept(GenericType<?> type, ReaderContext scope) {
        return !CharSequence.class.isAssignableFrom(type.rawType());
    }

    @Override
    public <U extends Object> Publisher<U> read(Publisher<DataChunk> publisher,
            GenericType<U> type, ReaderContext context) {

        Processor<U> processor = new Processor<>(type, jsonb);
        ByteArrayReader.read(publisher).subscribe(processor);
        return processor;
    }

    private static final class Processor<T>
            extends SingleInputProcessor<ByteArrayOutputStream, T> {

        private final GenericType<? super T> type;
        private final Jsonb jsonb;

        Processor(GenericType<? super T> type, Jsonb jsonb) {
            this.type = type;
            this.jsonb = jsonb;
        }

        @Override
        protected T wrap(ByteArrayOutputStream bytes) {
            try (InputStream inputStream =
                    new ByteArrayInputStream(bytes.toByteArray())) {
                return jsonb.fromJson(inputStream, type);
            } catch (final IOException ioException) {
                throw new JsonbException(ioException.getMessage(), ioException);
            }
        }

        @Override
        public void onComplete() {
            complete();
        }
    }
}
