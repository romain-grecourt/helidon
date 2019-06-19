package io.helidon.media.jsonp.common;

import io.helidon.common.http.DataChunk;
import io.helidon.common.http.EntityStreamReader;
import io.helidon.common.http.InBoundScope;
import io.helidon.common.reactive.Flow.Processor;
import io.helidon.common.reactive.Flow.Publisher;
import io.helidon.common.reactive.Flow.Subscriber;
import io.helidon.common.reactive.Flow.Subscription;
import java.util.Objects;
import javax.json.JsonReaderFactory;
import javax.json.JsonStructure;

/**
 * JSON-P entity stream reader.
 */
public class JsonEntityStreamReader implements EntityStreamReader<JsonStructure> {

    private final JsonReaderFactory jsonReaderFactory;

    public JsonEntityStreamReader(JsonReaderFactory jsonReaderFactory) {
        Objects.requireNonNull(jsonReaderFactory);
        this.jsonReaderFactory = jsonReaderFactory;
    }

    @Override
    public boolean accept(Class<?> type, InBoundScope scope) {
        return JsonStructure.class.isAssignableFrom(type);
    }

    @Override
    public Publisher<? extends JsonStructure> readEntityStream(
            Publisher<DataChunk> publisher, Class<? super JsonStructure> type,
            InBoundScope scope) {

        return new JsonArrayStreamProcessor(publisher);
    }

    class JsonArrayStreamProcessor<T> implements Processor<DataChunk, T> {

        private long itemsRequested;
        private boolean first = true;
        private Subscriber<? super T> itemSubscriber;
        private final Publisher<DataChunk> chunkPublisher;
        private Subscription chunkSubscription;

        JsonArrayStreamProcessor(Publisher<DataChunk> chunkPublisher) {
            this.chunkPublisher = chunkPublisher;
        }

        @Override
        public void subscribe(Subscriber<? super T> itemSubscriber) {
            this.itemSubscriber = itemSubscriber;
            this.itemSubscriber.onSubscribe(new Subscription() {
                @Override
                public void request(long n) {
                    itemsRequested = n;
                    chunkPublisher.subscribe(JsonArrayStreamProcessor.this);
                }

                @Override
                public void cancel() {
                    if (chunkSubscription != null) {
                        chunkSubscription.cancel();
                    }
                    itemsRequested = 0;
                }
            });
        }

        @Override
        public void onSubscribe(Subscription chunkSubscription) {
            this.chunkSubscription = chunkSubscription;
            chunkSubscription.request(itemsRequested);
        }

        @Override
        public void onNext(DataChunk chunk) {
            // Should parse using regular JSON parser
        }

        @Override
        public void onError(Throwable throwable) {
        }

        @Override
        public void onComplete() {
        }
    }
}
