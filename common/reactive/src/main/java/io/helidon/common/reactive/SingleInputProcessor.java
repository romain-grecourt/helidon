package io.helidon.common.reactive;

import io.helidon.common.reactive.Flow;
import io.helidon.common.reactive.OriginThreadPublisher;

/**
 * Single input processor.
 * @param <T> the subscribed item type
 * @param <U> the published item type
 */
public abstract class SingleInputProcessor<T, U>
            extends OriginThreadPublisher<U, T>
            implements Flow.Processor<T, U> {

    private Flow.Subscription subscription;

    @Override
    public final void onSubscribe(Flow.Subscription subscription) {
        this.subscription = subscription;
        subscription.request(1);
    }

    @Override
    public final void onNext(T item) {
        submit(item);
        subscription.cancel();
        onComplete();
    }

    @Override
    public final void onError(Throwable error) {
        error(error);
    }
}
