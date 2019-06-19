package io.helidon.media.jsonp.common;

import io.helidon.common.http.DataChunk;
import io.helidon.common.http.EntityStreamWriter;
import io.helidon.common.http.MediaType;
import io.helidon.common.http.OutBoundScope;
import io.helidon.common.reactive.FailedPublisher;
import io.helidon.common.reactive.Flow.Processor;
import io.helidon.common.reactive.Flow.Publisher;
import io.helidon.common.reactive.Flow.Subscriber;
import io.helidon.common.reactive.Flow.Subscription;
import io.helidon.media.common.JsonHelper;
import static io.helidon.media.jsonp.common.JsonEntityWriter.write;
import java.nio.charset.Charset;
import java.util.Objects;
import javax.json.JsonStructure;
import javax.json.JsonWriterFactory;

/**
 * JSON-P entity stream writer.
 */
public abstract class JsonEntityStreamWriter
        implements EntityStreamWriter<JsonStructure> {

    private final JsonWriterFactory jsonWriterFactory;
    private final String begin;
    private final String separator;
    private final String end;

    public JsonEntityStreamWriter(JsonWriterFactory jsonWriterFactory,
            String begin, String separator, String end) {

        Objects.requireNonNull(jsonWriterFactory);
        Objects.requireNonNull(separator);
        this.jsonWriterFactory = jsonWriterFactory;
        this.begin = begin;
        this.separator = separator;
        this.end = end;
    }

    @Override
    public Promise accept(Class<?> type, OutBoundScope scope) {
        if (JsonStructure.class.isAssignableFrom(type)) {
            MediaType contentType = JsonHelper.getOutBoundContentType(scope);
            if (contentType != null) {
                return new Promise<>(this, contentType);
            }
        }
        return null;
    }

    @Override
    public Publisher<DataChunk> writeEntityStream(
            Publisher<JsonStructure> entityStream, Class<JsonStructure> type,
            Promise<JsonStructure> promise, OutBoundScope scope) {

        try {
            return new JsonArrayStreamProcessor(entityStream, scope.charset());
        } catch (IllegalStateException ex) {
            return new FailedPublisher<>(ex);
        }
    }

    class JsonArrayStreamProcessor
            implements Processor<JsonStructure, DataChunk> {

        private long itemsRequested;
        private boolean first = true;
        private Subscriber<? super DataChunk> chunkSubscriber;
        private final Publisher<JsonStructure> itemPublisher;
        private Subscription itemSubscription;
        private final DataChunk beginChunk;
        private final DataChunk separatorChunk;
        private final DataChunk endChunk;
        private final Charset charset;

        JsonArrayStreamProcessor(Publisher<JsonStructure> itemPublisher,
                Charset charset) {

            this.itemPublisher = itemPublisher;
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

            Publisher<DataChunk> itemChunkPublisher =
                    write(jsonWriterFactory, item, charset);

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
