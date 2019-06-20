/*
 * Copyright (c) 2019 Oracle and/or its affiliates. All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.helidon.media.multipart.common;

import io.helidon.common.reactive.Flow.Publisher;
import io.helidon.common.reactive.Flow.Subscriber;
import io.helidon.common.reactive.Flow.Subscription;
import java.util.Collection;
import java.util.LinkedList;
import java.util.Queue;

/**
 * A reactive publisher of {@link BodyPart} that publishes all the items of a
 * given {@code Collection<BodyPart>}.
 */
final class BodyPartPublisher<T extends BodyPart> implements Publisher<T> {

    private final Queue<T> queue;
    private long requested;
    private boolean delivering;
    private boolean canceled;
    private boolean complete;

    BodyPartPublisher(Collection<T> bodyParts) {
        this.queue = new LinkedList<>(bodyParts);
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
                    T part = queue.poll();
                    if (part != null) {
                        requested--;
                        if (queue.isEmpty()) {
                            complete = true;
                        }
                        subscriber.onNext(part);
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
