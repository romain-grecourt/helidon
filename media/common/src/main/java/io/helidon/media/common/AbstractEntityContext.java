/*
 * Copyright (c) 2020, 2022 Oracle and/or its affiliates.
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
package io.helidon.media.common;

import io.helidon.common.GenericType;
import io.helidon.common.http.DataChunk;
import io.helidon.common.reactive.Single;
import io.helidon.media.common.EntitySupport.Filter;
import io.helidon.media.common.EntitySupport.Operator;

import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.Flow.Publisher;
import java.util.concurrent.Flow.Subscriber;
import java.util.concurrent.Flow.Subscription;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Base implementation for {@link EntitySupport.Context}
 */
public abstract class AbstractEntityContext<T extends AbstractEntityContext<T>> implements EntitySupport.Context {

    private static final Logger LOGGER = Logger.getLogger(AbstractEntityContext.class.getName());
    private static final Event BEFORE_ONSUBSCRIBE = new EventImpl(EventType.BEFORE_ONSUBSCRIBE, null);
    private static final Event BEFORE_ONNEXT = new EventImpl(EventType.BEFORE_ONNEXT, null);
    private static final Event BEFORE_ONCOMPLETE = new EventImpl(EventType.BEFORE_ONCOMPLETE, null);
    private static final Event AFTER_ONSUBSCRIBE = new EventImpl(EventType.AFTER_ONSUBSCRIBE, null);
    private static final Event AFTER_ONNEXT = new EventImpl(EventType.AFTER_ONNEXT, null);
    private static final Event AFTER_ONCOMPLETE = new EventImpl(EventType.AFTER_ONCOMPLETE, null);

    private final OperatorRegistry<FilterOperator<?>> filters;
    private final EventListener eventListener;

    /**
     * Create a instance.
     *
     * @param parent        content filters parent
     * @param eventListener event listener
     */
    protected AbstractEntityContext(AbstractEntityContext<T> parent, EventListener eventListener) {
        if (parent != null) {
            this.filters = new OperatorRegistry<>(parent.filters);
        } else {
            this.filters = new OperatorRegistry<>();
        }
        this.eventListener = eventListener;
    }

    @Override
    @SuppressWarnings("unchecked")
    public T registerFilter(Filter filter) {
        Objects.requireNonNull(filter, "filter is null!");
        filters.registerLast(new FilterOperator<>(filter));
        return (T) this;
    }

    @Override
    public final Publisher<DataChunk> applyFilters(Publisher<DataChunk> publisher) {
        return applyFilters(publisher, eventListener);
    }

    /**
     * Apply the filters on the given input publisher to form a publisher chain.
     *
     * @param publisher input publisher
     * @param type      type information associated with the input publisher
     * @return tail of the publisher chain
     */
    protected final Publisher<DataChunk> applyFilters(Publisher<DataChunk> publisher, GenericType<?> type) {
        Objects.requireNonNull(type, "type cannot be null!");
        if (eventListener != null) {
            return applyFilters(publisher, new TypedEventListener(eventListener, type));
        } else {
            return applyFilters(publisher, (EventListener) null);
        }
    }

    private Publisher<DataChunk> applyFilters(Publisher<DataChunk> publisher, EventListener listener) {
        if (publisher == null) {
            publisher = Single.empty();
        }
        try {
            Publisher<DataChunk> last = publisher;
            for (Filter filter : filters) {
                Publisher<DataChunk> p = filter.apply(last);
                if (p != null) {
                    last = p;
                }
            }
            return new EventingPublisher(last, listener);
        } finally {
            filters.close();
        }
    }

    private record EventingPublisher(Publisher<DataChunk> publisher, EventListener listener)
            implements Publisher<DataChunk> {

        @Override
        public void subscribe(Subscriber<? super DataChunk> subscriber) {
            publisher.subscribe(new EventingSubscriber(subscriber, listener));
        }
    }

    private record EventingSubscriber(Subscriber<? super DataChunk> delegate, EventListener listener)
            implements Subscriber<DataChunk> {

        private void fireEvent(Event event) {
            if (listener != null) {
                try {
                    listener.onEvent(event);
                } catch (Throwable ex) {
                    LOGGER.log(Level.WARNING, "An exception occurred in EventListener.onEvent", ex);
                }
            }
        }

        @Override
        public void onSubscribe(Subscription subscription) {
            fireEvent(BEFORE_ONSUBSCRIBE);
            try {
                delegate.onSubscribe(subscription);
            } finally {
                fireEvent(AFTER_ONSUBSCRIBE);
            }
        }

        @Override
        public void onNext(DataChunk item) {
            fireEvent(BEFORE_ONNEXT);
            try {
                delegate.onNext(item);
            } finally {
                fireEvent(AFTER_ONNEXT);
            }
        }

        @Override
        public void onError(Throwable error) {
            fireEvent(new ErrorEventImpl(error, EventType.BEFORE_ONERROR));
            try {
                delegate.onError(error);
            } finally {
                fireEvent(new ErrorEventImpl(error, EventType.AFTER_ONERROR));
            }
        }

        @Override
        public void onComplete() {
            fireEvent(BEFORE_ONCOMPLETE);
            try {
                delegate.onComplete();
            } finally {
                fireEvent(AFTER_ONCOMPLETE);
            }
        }
    }

    private record FilterOperator<T>(Filter filter) implements Operator<T>, Filter {

        @Override
        public PredicateResult accept(GenericType<?> type, T context) {
            return PredicateResult.SUPPORTED;
        }

        @Override
        public Publisher<DataChunk> apply(Publisher<DataChunk> publisher) {
            return filter.apply(publisher);
        }
    }

    private record TypedEventListener(EventListener delegate, GenericType<?> entityType) implements EventListener {

        @Override
        public void onEvent(Event event) {
            Event copy;
            if (event instanceof ErrorEventImpl) {
                copy = new ErrorEventImpl((ErrorEventImpl) event, entityType);
            } else if (event instanceof EventImpl) {
                copy = new EventImpl((EventImpl) event, entityType);
            } else {
                throw new IllegalStateException("Unknown event type " + event);
            }
            delegate.onEvent(copy);
        }
    }

    private static class EventImpl implements Event {

        private final EventType eventType;
        private final GenericType<?> entityType;

        EventImpl(EventImpl event, GenericType<?> entityType) {
            this(event.eventType, entityType);
        }

        EventImpl(EventType eventType, GenericType<?> entityType) {
            this.eventType = eventType;
            this.entityType = entityType;
        }

        @Override
        public Optional<GenericType<?>> entityType() {
            return Optional.ofNullable(entityType);
        }

        @Override
        public EventType eventType() {
            return eventType;
        }
    }

    private static final class ErrorEventImpl extends EventImpl implements ErrorEvent {

        private final Throwable error;

        ErrorEventImpl(ErrorEventImpl event, GenericType<?> type) {
            super(event.eventType(), type);
            error = event.error;
        }

        ErrorEventImpl(Throwable error, EventType eventType) {
            super(eventType, null);
            Objects.requireNonNull(error, "error cannot be null!");
            this.error = error;
        }

        @Override
        public Throwable error() {
            return error;
        }
    }
}
