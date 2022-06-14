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
package io.helidon.media.multipart;

import java.util.Objects;
import java.util.Optional;

import io.helidon.media.common.Entity;
import io.helidon.media.common.ReadableEntity;

/**
 * Body part entity.
 */
public interface BodyPart {

    /**
     * Get the reactive representation of the part content.
     *
     * @return {@link Entity}, never {@code null}
     */
    ReadableEntity content();

    /**
     * Returns HTTP part headers.
     *
     * @return BodyPartHeaders, never {@code null}
     */
    BodyPartHeaders headers();

    /**
     * Get the control name.
     *
     * @return the {@code name} parameter of the {@code Content-Disposition} header
     */
    default Optional<String> name() {
        return headers().contentDisposition().name();
    }

    /**
     * Test the control name.
     *
     * @param name name
     * @return {@code true} if the control name is equal to the given name, {@code false} otherwise
     * @throws NullPointerException if the given name is {@code null}
     */
    default boolean isNamed(String name) {
        Objects.requireNonNull(name, "name is null");
        return name().map(name::equals).orElse(false);
    }

    /**
     * Get the file name.
     *
     * @return the {@code filename} parameter of the {@code Content-Disposition} header
     */
    default Optional<String> filename() {
        return headers().contentDisposition().filename();
    }

    /**
     * Create a new builder.
     *
     * @return BodyPartBuilder
     */
    static BodyPartBuilder builder() {
        return new BodyPartBuilder();
    }
}
