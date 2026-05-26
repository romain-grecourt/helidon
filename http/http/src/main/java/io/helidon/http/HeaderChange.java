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

/**
 * A change made to HTTP headers.
 */
public sealed interface HeaderChange permits HeaderChange.Added, HeaderChange.Replaced, HeaderChange.Removed,
        HeaderChange.Cleared {

    /**
     * Listener of header changes.
     */
    @FunctionalInterface
    interface Listener {
        /**
         * Invoked when headers change.
         *
         * @param change header change
         */
        void onChange(HeaderChange change);
    }

    /**
     * Header was added.
     *
     * @param current current header
     */
    record Added(Header current) implements HeaderChange {
        /**
         * Create a new instance.
         */
        public Added {
            Objects.requireNonNull(current);
        }
    }

    /**
     * Header was replaced.
     *
     * @param previous previous header
     * @param current current header
     */
    record Replaced(Header previous, Header current) implements HeaderChange {
        /**
         * Create a new instance.
         */
        public Replaced {
            Objects.requireNonNull(previous);
            Objects.requireNonNull(current);
        }
    }

    /**
     * Header was removed.
     *
     * @param previous previous header
     */
    record Removed(Header previous) implements HeaderChange {
        /**
         * Create a new instance.
         */
        public Removed {
            Objects.requireNonNull(previous);
        }
    }

    /**
     * Headers were cleared.
     *
     * @param previous previous headers
     */
    record Cleared(Headers previous) implements HeaderChange {
        /**
         * Create a new instance.
         */
        public Cleared {
            Objects.requireNonNull(previous);
        }
    }
}
