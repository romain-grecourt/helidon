package io.helidon.media.jsonp.common;

import io.helidon.common.GenericType;
import io.helidon.common.http.DataChunk;
import io.helidon.common.http.MessageBody.Reader;
import io.helidon.common.http.MessageBody.ReaderContext;
import io.helidon.common.reactive.Flow.Publisher;
import io.helidon.media.common.ByteArrayReader;
import io.helidon.common.reactive.SingleInputProcessor;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.util.Objects;
import javax.json.JsonReader;
import javax.json.JsonReaderFactory;
import javax.json.JsonStructure;

/**
 * JSON-P entity reader.
 */
public class JsonpReader implements Reader<JsonStructure> {

    private final JsonReaderFactory jsonFactory;

    public JsonpReader(JsonReaderFactory jsonFactory) {
        Objects.requireNonNull(jsonFactory);
        this.jsonFactory = jsonFactory;
    }

    @Override
    public boolean accept(GenericType<?> type, ReaderContext context) {
        return JsonStructure.class.isAssignableFrom(type.rawType());
    }

    @Override
    public <U extends JsonStructure> Publisher<U> read(
            Publisher<DataChunk> publisher, GenericType<U> type,
            ReaderContext context) {

        Processor processor = new Processor(jsonFactory, context.charset());
        ByteArrayReader.read(publisher).subscribe(processor);
        return processor;
    }

    private static final class Processor<T extends JsonStructure>
            extends SingleInputProcessor<ByteArrayOutputStream, T> {

        private final JsonReaderFactory jsonFactory;
        private final Charset charset;

        Processor(JsonReaderFactory jsonFactory, Charset charset) {
            this.jsonFactory = jsonFactory;
            this.charset = charset;
        }

        @Override
        @SuppressWarnings("unchecked")
        protected T wrap(ByteArrayOutputStream bytes) {
            InputStream is = new ByteArrayInputStream(bytes.toByteArray());
            JsonReader reader = jsonFactory.createReader(is, charset);
            return (T) reader.read();
        }

        @Override
        public void onComplete() {
            complete();
        }
    }
}
