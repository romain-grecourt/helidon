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

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.function.Supplier;

/**
 * Multipart entity.
 */
public class MultiPart {

    private final List<BodyPart> parts;

    MultiPart(List<BodyPart> parts) {
        this.parts = parts;
    }

    /**
     * Get all the nested body parts.
     *
     * @return list of {@link BodyPart}
     */
    public List<BodyPart> bodyParts() {
        return parts;
    }

    /**
     * Short-hand for creating {@link MultiPart} instances with the
     * specified entities as body parts.
     *
     * @param entities the body part entities
     * @return created MultiPart
     */
    public static MultiPart create(BodyPart... entities) {
        Builder builder = builder();
        for (BodyPart entity : entities) {
            builder.bodyPart(BodyPart.builder().entity(entity));
        }
        return builder.build();
    }

    /**
     * Short-hand for creating {@link MultiPart} instances with the
     * specified entities as body parts.
     *
     * @param entities the body part entities
     * @return created MultiPart
     */
    public static MultiPart create(Collection<BodyPart> entities) {
        Builder builder = builder();
        for (BodyPart entity : entities) {
            builder.bodyPart(BodyPart.builder().entity(entity));
        }
        return builder.build();
    }

    /**
     * Create a new builder instance.
     *
     * @return Builder
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Builder class for creating {@link MultiPart} instances.
     */
    public static final class Builder implements io.helidon.common.Builder<Builder, MultiPart> {

        private final ArrayList<BodyPart> bodyParts = new ArrayList<>();

        /**
         * Force the use of {@link MultiPart#builder()}.
         */
        private Builder() {
        }

        /**
         * Add a body part.
         *
         * @param bodyPart body part to add
         * @return this builder instance
         */
        public Builder bodyPart(BodyPart bodyPart) {
            bodyParts.add(bodyPart);
            return this;
        }

        /**
         * Add a body part.
         *
         * @param supplier body part supplier
         * @return this builder instance
         */
        public Builder bodyPart(Supplier<BodyPart> supplier) {
            bodyParts.add(supplier.get());
            return this;
        }

        /**
         * Add a new body part based on the name entity.
         *
         * @param name   body part name
         * @param entity body part entity
         * @return this builder instance
         */
        public Builder bodyPart(String name, Object entity) {
            bodyParts.add(BodyPart.builder()
                                  .name(name)
                                  .entity(entity)
                                  .build());
            return this;
        }

        /**
         * Add a new body part based on the name, filename and {@link Path} to the file.
         *
         * @param name     body part name
         * @param filename body part filename
         * @param file     file path
         * @return this builder instance
         */
        public Builder bodyPart(String name, String filename, Path file) {
            bodyParts.add(BodyPart.builder()
                                    .name(name)
                                    .filename(filename)
                                    .entity(file)
                                    .build());
            return this;
        }

        /**
         * Add a new body part based on the name and {@link Path} to the files.
         * <p>
         * Filename for each file is set as actual file name.
         *
         * @param name  body part name
         * @param files file path
         * @return this builder instance
         */
        public Builder bodyPart(String name, Path... files) {
            for (Path file : files) {
                Path fileName = file.getFileName();
                if (fileName != null) {
                    bodyPart(name, fileName.toString(), file);
                }
            }
            return this;
        }

        /**
         * Add body parts.
         *
         * @param bodyParts body parts to add
         * @return this builder instance
         */
        public Builder bodyParts(Collection<BodyPart> bodyParts) {
            this.bodyParts.addAll(bodyParts);
            return this;
        }

        @Override
        public MultiPart build() {
            return new MultiPart(bodyParts);
        }
    }
}
