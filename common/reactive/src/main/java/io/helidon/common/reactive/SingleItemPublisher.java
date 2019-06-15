package io.helidon.common.reactive;

import io.helidon.common.reactive.Flow.Publisher;
import static io.helidon.common.reactive.ReactiveStreamsAdapter.publisherToFlow;
import reactor.core.publisher.Mono;

/**
 * A publisher that publishes a single item.
 * @param <T> item type
 */
public final class SingleItemPublisher<T> implements Publisher<T> {

    private final T item;

    public SingleItemPublisher(T item) {
        this.item = item;
    }

    @Override
    public void subscribe(Flow.Subscriber<? super T> subscriber) {
        publisherToFlow(Mono.<T>just(item)).subscribe(subscriber);
    }
}
