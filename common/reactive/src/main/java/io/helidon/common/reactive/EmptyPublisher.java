package io.helidon.common.reactive;

import io.helidon.common.reactive.Flow.Publisher;
import static io.helidon.common.reactive.ReactiveStreamsAdapter.publisherToFlow;
import reactor.core.publisher.Mono;

/**
 * A publisher that is empty.
 * @param <T> item type
 */
public final class EmptyPublisher<T> implements Publisher<T> {

    @Override
    public void subscribe(Flow.Subscriber<? super T> subscriber) {
        publisherToFlow(Mono.<T>empty()).subscribe(subscriber);
    }
}
