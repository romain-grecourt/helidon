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
package io.helidon.media.multipart;

import io.helidon.common.reactive.Flow.Publisher;
import io.helidon.common.reactive.Flow.Subscriber;
import io.helidon.common.reactive.Flow.Subscription;
import java.util.Collection;
import java.util.Iterator;
import io.helidon.common.http.EntityReaders;
import io.helidon.common.http.EntityWriters;
import io.helidon.media.common.MediaSupportBase;

/**
 * Multi part support service.
 */
public final class MultiPartSupport extends MediaSupportBase {

    /**
     * The default boundary used for encoding multipart messages.
     */
    static final String DEFAULT_BOUNDARY = "[^._.^]==>boundary<==[^._.^]";

    /**
     * Force the use of {@link #create()}.
     */
    private MultiPartSupport(){
    }

    /**
     * Create a new instance of {@link MultiPartSupport}.
     * @return MultiPartSupport
     */
    public static MultiPartSupport create(){
        return new MultiPartSupport();
    }

    @Override
    protected void registerWriters(EntityWriters writers) {
        writers.registerWriter(new MultiPartEntityWriter());
        writers.registerStreamWriter(new BodyPartEntityStreamWriter());
    }

    @Override
    protected void registerReaders(EntityReaders readers) {
        readers.registerReader(new MultiPartEntityReader());
    }

    /**
     * A reactive publisher of {@link BodyPart} that publishes all the items
     * of a given {@code Collection<BodyPart>}.
     */
    static final class BodyPartPublisher<T extends BodyPart>
            implements Publisher<T> {

        private final Collection<T> bodyParts;

        BodyPartPublisher(Collection<T> bodyParts) {
            this.bodyParts = bodyParts;
        }

        @Override
        public void subscribe(Subscriber<? super T> subscriber) {
            final Iterator<T> bodyPartsIt = bodyParts.iterator();
            subscriber.onSubscribe(new Subscription() {

                volatile boolean canceled = false;

                @Override
                public void request(long n) {
                    if (canceled) {
                        subscriber.onError(new IllegalStateException(
                                "Subscription canceled"));
                        return;
                    }
                    while (bodyPartsIt.hasNext() && --n >= 0) {
                        subscriber.onNext(bodyPartsIt.next());
                    }
                    if (!bodyPartsIt.hasNext()) {
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
}
