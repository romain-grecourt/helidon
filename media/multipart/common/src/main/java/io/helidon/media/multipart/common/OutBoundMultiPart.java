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
package io.helidon.media.multipart.common;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * Out-bound multipart message.
 */
public final class OutBoundMultiPart implements MultiPart<OutBoundBodyPart>{

    private final List<OutBoundBodyPart> parts;

    /**
     * Create a new out-bound multipart message.
     * @param parts body parts
     */
    private OutBoundMultiPart(List<OutBoundBodyPart> parts) {
        this.parts = parts;
    }

    @Override
    public List<OutBoundBodyPart> bodyParts() {
        return parts;
    }

    /**
     * Short-hand for creating an {@link OutBoundMultiPart} instances with the
     * specified entities as body parts.
     *
     * @param <T> the type of the entities
     * @param entities the body part entities
     * @return created MultiPart
     */
    public static <T> OutBoundMultiPart create(Collection<T> entities) {
        Builder builder = builder();
        for(T entity : entities){
            builder.bodyPart(OutBoundBodyPart.create(entity));
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
     * Builder class for creating {@link OutBoundMultiPart} instances.
     */
    public static final class Builder
            implements io.helidon.common.Builder<OutBoundMultiPart> {

        private final ArrayList<OutBoundBodyPart> bodyParts = new ArrayList<>();

        /**
         * Force the use of {@link OutBoundMultiPart#builder()}.
         */
        private Builder() {
        }

        public Builder bodyPart(OutBoundBodyPart bodyPart) {
            bodyParts.add(bodyPart);
            return this;
        }

        public Builder bodyParts(Collection<OutBoundBodyPart> bodyParts) {
            this.bodyParts.addAll(bodyParts);
            return this;
        }

        @Override
        public OutBoundMultiPart build() {
            return new OutBoundMultiPart(bodyParts);
        }
    }
}
