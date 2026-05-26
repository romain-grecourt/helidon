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

package io.helidon.http;

import java.util.Objects;
import java.util.function.Supplier;

/**
 * Headers that can notify listeners about header changes.
 */
public interface ObservableHeaders {
    /**
     * Mirror source header changes to target headers while an action runs.
     * If source headers are not observable, this invokes the action directly.
     *
     * @param sourceHeaders source headers
     * @param targetHeaders target headers
     * @param action action to run
     * @param <T> action result type
     * @return action result
     * @throws NullPointerException if any parameter is {@code null}
     */
    static <T> T mirrorWhile(Headers sourceHeaders, WritableHeaders<?> targetHeaders, Supplier<T> action) {
        Objects.requireNonNull(sourceHeaders);
        Objects.requireNonNull(targetHeaders);
        Objects.requireNonNull(action);

        if (sourceHeaders instanceof ObservableHeaders observableHeaders) {
            return observableHeaders.mirrorWhile(targetHeaders, action);
        }
        return action.get();
    }

    /**
     * Add a listener.
     *
     * @param listener header change listener
     * @throws NullPointerException if listener is {@code null}
     */
    void addListener(HeaderChange.Listener listener);

    /**
     * Remove a listener.
     *
     * @param listener header change listener
     * @throws NullPointerException if listener is {@code null}
     */
    void removeListener(HeaderChange.Listener listener);

    /**
     * Mirror this header set's changes to target headers while an action runs.
     *
     * @param targetHeaders target headers
     * @param action action to run
     * @param <T> action result type
     * @return action result
     * @throws NullPointerException if any parameter is {@code null}
     */
    default <T> T mirrorWhile(WritableHeaders<?> targetHeaders, Supplier<T> action) {
        Objects.requireNonNull(targetHeaders);
        Objects.requireNonNull(action);

        HeaderChange.Listener listener = change -> mirrorHeaderChange(change, targetHeaders);
        addListener(listener);
        try {
            return action.get();
        } finally {
            removeListener(listener);
        }
    }

    private static void mirrorHeaderChange(HeaderChange change, WritableHeaders<?> headers) {
        WritableHeaders<?> targetHeaders = Objects.requireNonNull(headers);
        switch (Objects.requireNonNull(change)) {
            case HeaderChange.Added added -> targetHeaders.set(added.current());
            case HeaderChange.Replaced replaced -> targetHeaders.set(replaced.current());
            case HeaderChange.Removed removed -> targetHeaders.remove(removed.previous().headerName());
            case HeaderChange.Cleared cleared -> cleared.previous().forEach(header -> targetHeaders.remove(header.headerName()));
        }
    }
}
