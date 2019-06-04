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

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Multi part entity.
 */
public final class MultiPart {

    /**
     * The nested parts.
     */
    private final List<BodyPart> bodyParts;

    /**
     * Create a new instance.
     * @param parts list of parts
     */
    private MultiPart(List<BodyPart> parts) {
        bodyParts = parts;
    }

    /**
     * Get all the nested body parts.
     * @return list of {@link BodyPart}
     */
    public List<BodyPart> bodyParts(){
        return bodyParts;
    }

    /**
     * Get the first body part identified by the given control name. The control
     * name is the {@code name} parameter of the {@code Content-Disposition}
     * header for a body part with disposition type {@code form-data}.
     *
     * @param name control name
     * @return {@code Optional<BodyPart>}, never {@code null}
     */
    public Optional<BodyPart> field(String name) {
        if (name == null) {
            return Optional.empty();
        }
        for (BodyPart part : bodyParts) {
            String partName = part.name();
            if (partName == null) {
                continue;
            }
            if (name.equals(partName)) {
                return Optional.of(part);
            }
        }
        return Optional.empty();
    }

    /**
     * Get the body parts identified by the given control name. The control
     * name is the {@code name} parameter of the {@code Content-Disposition}
     * header for a body part with disposition type {@code form-data}.
     *
     * @param name control name
     * @return {@code List<BodyPart>}, never {@code null}
     */
    public List<BodyPart> fields(String name) {
        if (name == null) {
            return Collections.emptyList();
        }
        List<BodyPart> result = new ArrayList<>();
        for (BodyPart part : bodyParts) {
            String partName = part.name();
            if (partName == null) {
                continue;
            }
            if (partName.equals(partName)) {
                result.add(part);
            }
        }
        return result;
    }

    /**
     * Get all the body parts that are identified with form data control names.
     * @return map of control names to body parts,never {@code null}
     */
    public Map<String, List<BodyPart>> fields() {
        Map<String, List<BodyPart>> results = new HashMap<>();
        for (BodyPart part : bodyParts) {
            String name = part.name();
            if (name == null) {
                continue;
            }
            List<BodyPart> result = results.get(name);
            if (result == null) {
                result = new ArrayList<>();
            }
            result.add(part);
        }
        return results;
    }

    /**
     * Short-hand for creating a {@link MultiPart} instances with the specified
     * entities as body parts.
     * @param <T> the type of the entities
     * @param entities the body part entities
     * @return created MultiPart
     */
    public static <T> MultiPart create(Collection<T> entities){
        Builder builder = builder();
        for(T entity : entities){
            builder.bodyPart(BodyPart.create(entity));
        }
        return builder.build();
    }

    /**
     * Create a new builder instance.
     * @return Builder
     */
    public static Builder builder(){
        return new Builder();
    }

    /**
     * Builder for creating {@link MultiPart} instances.
     */
    public static final class Builder
            implements io.helidon.common.Builder<MultiPart> {

        private final ArrayList<BodyPart> bodyParts = new ArrayList<>();

        /**
         * Force the use of {@link MultiPart#builder() }.
         */
        private Builder() {
        }

        /**
         * Add a new body part.
         * @param bodyPart body part to add
         * @return this builder
         */
        public Builder bodyPart(BodyPart bodyPart){
            bodyParts.add(bodyPart);
            return this;
        }

        /**
         * Add new body parts.
         * @param bodyParts body parts to add
         * @return this builder instance
         */
        public Builder bodyParts(Collection<BodyPart> bodyParts){
            this.bodyParts.addAll(bodyParts);
            return this;
        }

        @Override
        public MultiPart build(){
            return new MultiPart(bodyParts);
        }
    }
}
