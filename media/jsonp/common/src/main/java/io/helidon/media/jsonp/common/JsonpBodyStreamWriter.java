package io.helidon.media.jsonp.common;

import io.helidon.common.GenericType;
import io.helidon.common.http.DataChunk;
import io.helidon.common.http.MediaType;
import io.helidon.common.reactive.Flow.Processor;
import io.helidon.common.reactive.Flow.Publisher;
import io.helidon.common.reactive.Flow.Subscriber;
import io.helidon.common.reactive.Flow.Subscription;
import io.helidon.media.common.MessageBodyStreamWriter;
import io.helidon.media.common.MessageBodyWriterContext;
import java.nio.charset.Charset;
import java.util.Objects;
import javax.json.JsonStructure;
import javax.json.JsonWriterFactory;

/**
 * Message body writer reader for {@link JsonStructure} sub-classes (JSON-P).
 */
public abstract class JsonpBodyStreamWriter
        implements MessageBodyStreamWriter<JsonStructure> {

    private final JsonWriterFactory jsonFactory;
    private final String begin;
    private final String separator;
    private final String end;

    protected JsonpBodyStreamWriter(JsonWriterFactory jsonFactory,
            String begin, String separator, String end) {

        Objects.requireNonNull(jsonFactory);
        Objects.requireNonNull(separator);
        this.jsonFactory = jsonFactory;
        this.begin = begin;
        this.separator = separator;
        this.end = end;
    }

    @Override
    public boolean accept(GenericType<?> type,
            MessageBodyWriterContext context) {

        return JsonStructure.class.isAssignableFrom(type.rawType());
    }

    @Override
    public Publisher<DataChunk> write(Publisher<JsonStructure> content,
            GenericType<? extends JsonStructure> type,
            MessageBodyWriterContext context) {

         MediaType contentType = context.findAccepted(MediaType.JSON_PREDICATE,
                MediaType.APPLICATION_JSON);
         context.contentType(contentType);
         return new JsonArrayStreamProcessor(content, context.charset());
    }

    class JsonArrayStreamProcessor
            implements Processor<JsonStructure, DataChunk> {

        private long itemsRequested;
        private boolean first = true;
        private Subscriber<? super DataChunk> chunkSubscriber;
        private final Publisher<? extends JsonStructure> itemPublisher;
        private Subscription itemSubscription;
        private final DataChunk beginChunk;
        private final DataChunk separatorChunk;
        private final DataChunk endChunk;
        private final Charset charset;

        JsonArrayStreamProcessor(Publisher<? extends JsonStructure> publisher,
                Charset charset) {

            this.itemPublisher = publisher;
            if (begin != null) {
                this.beginChunk = DataChunk.create(begin.getBytes(charset));
            } else {
                this.beginChunk = null;
            }
            this.separatorChunk = DataChunk.create(separator.getBytes(charset));
            if (end != null) {
                this.endChunk = DataChunk.create(end.getBytes(charset));
            } else {
                this.endChunk = null;
            }
            Objects.requireNonNull(charset);
            this.charset = charset;
        }

        @Override
        public void subscribe(Subscriber<? super DataChunk> chunkSubscriber) {
            this.chunkSubscriber = chunkSubscriber;
            this.chunkSubscriber.onSubscribe(new Subscription() {
                @Override
                public void request(long n) {
                    itemsRequested = n;
                    itemPublisher.subscribe(JsonArrayStreamProcessor.this);
                }

                @Override
                public void cancel() {
                    if (itemSubscription != null) {
                        itemSubscription.cancel();
                    }
                    itemsRequested = 0;
                }
            });
        }

        @Override
        public void onSubscribe(Subscription itemSubscription) {
            this.itemSubscription = itemSubscription;
            if (beginChunk != null) {
                chunkSubscriber.onNext(beginChunk);
            }
            itemSubscription.request(itemsRequested);
        }

        @Override
        public void onNext(JsonStructure item) {
            if (!first && separatorChunk != null) {
                chunkSubscriber.onNext(separatorChunk);
            } else {
                first = false;
            }

            Publisher<DataChunk> itemChunkPublisher = JsonpBodyWriter
                    .write(jsonFactory, item, charset);

            itemChunkPublisher.subscribe(new Subscriber<DataChunk>() {
                @Override
                public void onSubscribe(Subscription subscription) {
                    subscription.request(Long.MAX_VALUE);
                }

                @Override
                public void onNext(DataChunk item) {
                    chunkSubscriber.onNext(item);
                }

                @Override
                public void onError(Throwable throwable) {
                    chunkSubscriber.onError(throwable);
                }

                @Override
                public void onComplete() {
                    // no-op
                }
            });
        }

        @Override
        public void onError(Throwable throwable) {
            if (endChunk != null) {
                chunkSubscriber.onNext(endChunk);
            }
        }

        @Override
        public void onComplete() {
            if (endChunk != null) {
                chunkSubscriber.onNext(endChunk);
            }
            chunkSubscriber.onComplete();
        }
    }
}
