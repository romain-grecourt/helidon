package io.helidon.common.reactive;

import io.helidon.common.reactive.Flow;
import io.helidon.common.reactive.OriginThreadPublisher;

/**
 * Single output processor.
 * @param <T> the published item type
 * @param <U> the subscribed item type
 */
public abstract class SingleOutputProcessor<T, U>
        extends OriginThreadPublisher<T, T>
        implements Flow.Processor<U, T>{

    @Override
    public final void submit(T data) {
        super.submit(data);
        super.complete();
    }

    @Override
    public final void complete() {
        throw new UnsupportedOperationException("Complete cannot be invoked");
    }

    @Override
    protected final T wrap(T data) {
        return data;
    }

    @Override
    public final void onSubscribe(Flow.Subscription subscription) {
        subscription.request(Long.MAX_VALUE);
    }

    @Override
    public final void onError(Throwable throwable) {
        error(throwable);
    }

    @Override
    public void onComplete() {
    }
}
