package io.helidon.common.reactive;

import io.helidon.common.reactive.Flow.Publisher;
import io.helidon.common.reactive.Flow.Subscriber;
import io.helidon.common.reactive.Flow.Subscription;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Failed publisher that invokes {@link Subscriber#onError} once with the given
 * exception during {@link Subscription#request}.
 * @param <T> item type
 */
public final class FailedPublisher<T> implements Publisher<T> {

    private final Throwable ex;
    private final AtomicBoolean done;

    public FailedPublisher(Throwable ex) {
        this.ex = ex;
        this.done = new AtomicBoolean(false);
    }

    @Override
    public void subscribe(Subscriber<? super T> subscriber) {
        subscriber.onSubscribe(new Subscription() {
            @Override
            public void request(long n) {
                if (done.compareAndSet(false, true)) {
                    subscriber.onError(ex);
                }
            }

            @Override
            public void cancel() {
            }
        });
    }
}
