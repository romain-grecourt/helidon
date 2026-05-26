/*
 * Copyright (c) 2026 Oracle and/or its affiliates.
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

package io.helidon.webclient.api;

import java.util.Iterator;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;
import java.util.function.Supplier;

import io.helidon.http.ClientRequestHeaders;
import io.helidon.http.Header;
import io.helidon.http.HeaderChange;
import io.helidon.http.HeaderName;
import io.helidon.http.Headers;
import io.helidon.http.HttpMediaType;
import io.helidon.http.ObservableHeaders;
import io.helidon.http.WritableHeaders;

class ObservableClientRequestHeaders implements ClientRequestHeaders, ObservableHeaders {
    private final ClientRequestHeaders delegate;
    private final List<HeaderChange.Listener> listeners = new CopyOnWriteArrayList<>();

    ObservableClientRequestHeaders(ClientRequestHeaders delegate) {
        this.delegate = Objects.requireNonNull(delegate);
    }

    @Override
    public void addListener(HeaderChange.Listener listener) {
        listeners.add(Objects.requireNonNull(listener));
    }

    @Override
    public void removeListener(HeaderChange.Listener listener) {
        listeners.remove(Objects.requireNonNull(listener));
    }

    @Override
    public List<String> all(HeaderName name, Supplier<List<String>> defaultSupplier) {
        return delegate.all(name, defaultSupplier);
    }

    @Override
    public boolean contains(HeaderName name) {
        return delegate.contains(name);
    }

    @Override
    public boolean contains(Header headerWithValue) {
        return delegate.contains(headerWithValue);
    }

    @Override
    public Header get(HeaderName name) {
        return delegate.get(name);
    }

    @Override
    public int size() {
        return delegate.size();
    }

    @Override
    public List<HttpMediaType> acceptedTypes() {
        return delegate.acceptedTypes();
    }

    @Override
    public ClientRequestHeaders setIfAbsent(Header header) {
        if (!delegate.contains(header.headerName())) {
            delegate.set(header);
            fire(new HeaderChange.Added(delegate.get(header.headerName())));
        }
        return this;
    }

    @Override
    public ClientRequestHeaders add(Header header) {
        var previous = delegate.find(header.headerName());
        delegate.add(header);
        var current = delegate.get(header.headerName());
        if (previous.isPresent()) {
            fire(new HeaderChange.Replaced(previous.get(), current));
        } else {
            fire(new HeaderChange.Added(current));
        }
        return this;
    }

    @Override
    public ClientRequestHeaders remove(HeaderName name) {
        delegate.remove(name, it -> fire(new HeaderChange.Removed(it)));
        return this;
    }

    @Override
    public ClientRequestHeaders remove(HeaderName name, Consumer<Header> removedConsumer) {
        delegate.remove(name, it -> {
            removedConsumer.accept(it);
            fire(new HeaderChange.Removed(it));
        });
        return this;
    }

    @Override
    public ClientRequestHeaders set(Header header) {
        var previous = delegate.find(header.headerName());
        delegate.set(header);
        var current = delegate.get(header.headerName());
        if (previous.isPresent()) {
            fire(new HeaderChange.Replaced(previous.get(), current));
        } else {
            fire(new HeaderChange.Added(current));
        }
        return this;
    }

    @Override
    public ClientRequestHeaders clear() {
        if (delegate.size() == 0) {
            return this;
        }
        var previous = WritableHeaders.create(delegate);
        delegate.clear();
        fire(new HeaderChange.Cleared(previous));
        return this;
    }

    @Override
    public ClientRequestHeaders from(Headers headers) {
        headers.forEach(this::set);
        return this;
    }

    @Override
    public Iterator<Header> iterator() {
        return delegate.iterator();
    }

    @Override
    public String toString() {
        return delegate.toString();
    }

    private void fire(HeaderChange change) {
        for (HeaderChange.Listener listener : listeners) {
            listener.onChange(change);
        }
    }
}
