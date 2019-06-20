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
package io.helidon.common.http;

import io.helidon.common.reactive.Flow.Publisher;
import io.helidon.common.reactive.Flow.Subscriber;
import java.util.LinkedList;
import java.util.Objects;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * Content filter support.
 */
public abstract class ContentFilters implements ContentFiltersRegistry {

    private final ContentFilters parent;
    private final LinkedList<ContentFilter> filters;
    private final ReadWriteLock filtersLock;

    /**
     * Create a new content support instance.
     */
    protected ContentFilters() {
        this.parent = null;
        this.filters = new LinkedList<>();
        this.filtersLock = new ReentrantReadWriteLock();
    }

    /**
     * Create a new parented content support instance.
     * @param parent content filters parent
     */
    protected ContentFilters(ContentFilters parent) {
        this.parent = parent;
        this.filters = new LinkedList<>();
        this.filtersLock = new ReentrantReadWriteLock();
    }

    @Override
    public final ContentFilters registerFilter(ContentFilter filter) {
        Objects.requireNonNull(filter, "filter is null!");
        try {
            filtersLock.writeLock().lock();
            filters.addLast(filter);
            return this;
        } finally {
            filtersLock.writeLock().unlock();
        }
    }

    /**
     * Apply the filters by creating a publisher chain of each filter.
     *
     * @param publisher the initial publisher
     * @return the last publisher of the resulting chain
     */
    public final Publisher<DataChunk> applyFilters(
            Publisher<DataChunk> publisher) {

        return applyFilters(publisher, null);
    }

    /**
     * Apply the filters by creating a publisher chain of each filter.
     * @param publisher the initial publisher
     * @param ifc interceptor factory
     * @return the last publisher of the resulting chain
     */
    public final Publisher<DataChunk> applyFilters(
            Publisher<DataChunk> publisher, ContentInterceptor.Factory ifc) {

        Objects.requireNonNull(publisher, "publisher cannot be null!");
        Publisher<DataChunk> last = doApplyFilters(publisher);
        if (parent != null) {
            last = parent.doApplyFilters(last);
        }
        return new FilteredPublisher(last, ifc);
    }

    private Publisher<DataChunk> doApplyFilters(
            Publisher<DataChunk> publisher) {

        Publisher<DataChunk> lastPublisher = publisher;
        try {
            filtersLock.readLock().lock();
            for (ContentFilter filter : filters) {
                Publisher<DataChunk> p = filter.apply(lastPublisher);
                if (p != null) {
                    lastPublisher = p;
                }
            }
        } finally {
            filtersLock.readLock().unlock();
        }
        return lastPublisher;
    }

    /**
     * The publisher created as a result of {@link #applyFilters}.
     */
    private static final class FilteredPublisher
            implements Publisher<DataChunk> {

        private final Publisher<DataChunk> publisher;
        private final ContentInterceptor.Factory ifc;

        FilteredPublisher(Publisher<DataChunk> publisher,
                ContentInterceptor.Factory ifc) {

            this.publisher = publisher;
            this.ifc = ifc;
        }

        @Override
        public void subscribe(Subscriber<? super DataChunk> subscriber) {
            ContentInterceptor ic = null;
            if (ifc != null) {
                ic = ifc.create(subscriber);
            }
            if (ic != null) {
                publisher.subscribe(ic);
            } else {
                publisher.subscribe(subscriber);
            }
        }
    }
}
