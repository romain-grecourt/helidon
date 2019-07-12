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
 * Outbound multipart message.
 */
public final class OutboundMultiPart implements MultiPart<OutboundBodyPart>{

    private final List<OutboundBodyPart> parts;

    /**
     * Private to enforce the use of {@link #create(java.util.Collection)}
     * or {@link #builder()}.
     */
    private OutboundMultiPart(List<OutboundBodyPart> parts) {
        this.parts = parts;
    }

    @Override
    public List<OutboundBodyPart> bodyParts() {
        return parts;
    }

    /**
     * Short-hand for creating an {@link OutboundMultiPart} instances with the
     * specified entities as body parts.
     *
     * @param <T> the type of the entities
     * @param entities the body part entities
     * @return created MultiPart
     */
    public static <T> OutboundMultiPart create(Collection<T> entities) {
        Builder builder = builder();
        for(T entity : entities){
            builder.bodyPart(OutboundBodyPart.create(entity));
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
     * Builder class for creating {@link OutboundMultiPart} instances.
     */
    public static final class Builder
            implements io.helidon.common.Builder<OutboundMultiPart> {

        private final ArrayList<OutboundBodyPart> bodyParts = new ArrayList<>();

        /**
         * Force the use of {@link OutboundMultiPart#builder()}.
         */
        private Builder() {
        }

        public Builder bodyPart(OutboundBodyPart bodyPart) {
            bodyParts.add(bodyPart);
            return this;
        }

        public Builder bodyParts(Collection<OutboundBodyPart> bodyParts) {
            this.bodyParts.addAll(bodyParts);
            return this;
        }

        @Override
        public OutboundMultiPart build() {
            return new OutboundMultiPart(bodyParts);
        }
    }
}
