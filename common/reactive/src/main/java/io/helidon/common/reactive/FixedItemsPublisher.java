package io.helidon.common.reactive;

import io.helidon.common.reactive.Flow.Publisher;
import io.helidon.common.reactive.Flow.Subscriber;
import io.helidon.common.reactive.Flow.Subscription;
import java.util.Collection;
import java.util.LinkedList;
import java.util.Queue;

public final class FixedItemsPublisher<T> implements Publisher<T> {

    private final Queue<T> queue;
    private long requested;
    private boolean delivering;
    private boolean canceled;
    private boolean complete;

    public FixedItemsPublisher(Collection<T> items) {
        this.queue = new LinkedList<>(items);
        canceled = false;
        requested = 0;
    }

    @Override
    public void subscribe(Subscriber<? super T> subscriber) {
        subscriber.onSubscribe(new Subscription() {
            @Override
            public void request(long n) {
                if (n <= 0 || canceled || complete) {
                    return;
                }
                requested += n;
                if (delivering) {
                    return;
                }
                delivering = true;
                while (!complete && requested > 0) {
                    T item = queue.poll();
                    if (item != null) {
                        requested--;
                        if (queue.isEmpty()) {
                            complete = true;
                        }
                        subscriber.onNext(item);
                    }
                }
                delivering = false;
                if (complete) {
                    subscriber.onComplete();
                }
            }

            @Override
            public void cancel() {
                canceled = true;
            }
        });
    }
}