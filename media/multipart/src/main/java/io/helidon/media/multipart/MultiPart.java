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

import java.util.Collection;
import java.util.LinkedList;

/**
 * Multi part entity.
 */
public final class MultiPart {

    private final Collection<BodyPart> bodyParts;

    private MultiPart(Collection<BodyPart> bodyParts) {
        this.bodyParts = bodyParts;
    }

    public Collection<BodyPart> bodyParts(){
        return bodyParts;
    }

    public static <T> MultiPart create(T ... entities){
        Builder builder = builder();
        for(T entity : entities){
            builder.bodyPart(BodyPart.create(entity));
        }
        return builder.build();
    }

    public static <T> MultiPart create(Collection<T> entities){
        Builder builder = builder();
        for(T entity : entities){
            builder.bodyPart(BodyPart.create(entity));
        }
        return builder.build();
    }

    public static Builder builder(){
        return new Builder();
    }

    public static final class Builder
            implements io.helidon.common.Builder<MultiPart> {

        private final LinkedList<BodyPart> bodyParts = new LinkedList<>();

        public Builder bodyPart(BodyPart bodyPart){
            bodyParts.add(bodyPart);
            return this;
        }

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
