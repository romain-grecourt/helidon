package io.helidon.common.reactive;

import io.helidon.common.reactive.FailedPublisher;
import io.helidon.common.reactive.Flow;
import io.helidon.common.reactive.Flow.Publisher;
import io.helidon.common.reactive.Flow.Subscriber;
import io.helidon.common.reactive.Flow.Subscription;

/**
 * Single input delegating processor.
 * @param <T> the subscribed item type
 * @param <U> the published item type
 */
public abstract class SingleInputDelegatingProcessor<T, U>
        implements Flow.Processor<T, U> {

    private Subscription subscription;
    private Subscriber<? super U> subscriber;
    private Publisher<U> delegate;

    @Override
    public void onSubscribe(Subscription subscription) {
        this.subscription = subscription;
        subscription.request(1);
    }

    protected abstract Publisher<U> delegate(T item);

    @Override
    public final void onNext(T item) {
        try {
            delegate = delegate(item);
        } catch (Throwable ex) {
            delegate = new FailedPublisher<>(ex);
        }
        subscription.cancel();
        onComplete();
    }

    @Override
    public final void onError(Throwable throwable) {
        error(throwable);
    }

    public final void error(Throwable error) {
        if (subscriber != null) {
            subscriber.onError(error);
        }
    }

    @Override
    public void onComplete() {
    }

    @Override
    public final void subscribe(Flow.Subscriber<? super U> subscriber) {
        if (delegate == null) {
            subscriber.onError(new IllegalStateException("Not ready"));
        }
        this.subscriber = subscriber;
        delegate.subscribe(subscriber);
    }
}
