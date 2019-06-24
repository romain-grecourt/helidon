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
import java.util.Objects;

/**
 * Content filter support.
 */
public abstract class ContentFilters implements ContentFiltersRegistry {

    private final ContentOperatorRegistry<ContentFilter> filters;

    /**
     * Create a new content support instance.
     */
    protected ContentFilters() {
        filters = new ContentOperatorRegistry<>(null);
    }

    /**
     * Create a new parented content support instance.
     * @param parent content filters parent
     */
    protected ContentFilters(ContentFilters parent) {
        Objects.requireNonNull(parent, "parent cannot be null!");
        filters = new ContentOperatorRegistry<>(parent.filters);
    }

    @Override
    public final ContentFilters registerFilter(ContentFilter filter) {
        filters.registerLast(filter);
        return this;
    }

    /**
     * Apply the filters by creating a publisher chain of each filter.
     *
     * @param pub the initial publisher
     * @return the last publisher of the resulting chain
     */
    public final Publisher<DataChunk> applyFilters(Publisher<DataChunk> pub) {
        return applyFilters(pub, null);
    }

    /**
     * Apply the filters by creating a publisher chain of each filter.
     * @param pub the initial publisher
     * @param ifc interceptor factory
     * @return the last publisher of the resulting chain
     */
    public final Publisher<DataChunk> applyFilters(Publisher<DataChunk> pub,
            ContentInterceptor.Factory ifc) {

        Objects.requireNonNull(pub, "pub cannot be null!");
        try {
            Publisher<DataChunk> last = pub;
            for (ContentFilter filter : filters) {
                Publisher<DataChunk> filtered = filter.apply(last);
                if (filtered != null) {
                    last = filtered;
                }
            }
            return new FilteredPublisher(last, ifc);
        } finally {
            filters.close();
        }
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
