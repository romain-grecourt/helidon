package io.helidon.common.reactive;

import io.helidon.common.reactive.Flow.Subscription;

/**
 * Empty subscription singleton.
 */
final class EmptySubscription implements Subscription {

    static final EmptySubscription INSTANCE = new EmptySubscription();

    EmptySubscription() {
    }

    @Override
    public void request(long n) {
    }

    @Override
    public void cancel() {
    }
}
