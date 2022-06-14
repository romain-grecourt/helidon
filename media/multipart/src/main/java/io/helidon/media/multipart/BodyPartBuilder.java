/*
 * Copyright (c) 2022 Oracle and/or its affiliates.
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

import java.util.List;
import java.util.Map;

import io.helidon.common.GenericType;
import io.helidon.common.http.Parameters;
import io.helidon.media.common.Entity;
import io.helidon.media.common.EntityBuilder;

/**
 * Builder for creating {@link BodyPart} instances.
 */
public final class BodyPartBuilder extends EntityBuilder<BodyPartBuilder, BodyPart> {

    private BodyPartHeaders.Builder headersBuilder;

    /**
     * Create a new builder instance.
     */
    BodyPartBuilder() {
        headersBuilder = BodyPartHeaders.builder();
    }

    /**
     * Create a new body part backed by the specified entity.
     *
     * @param <T>    entity type
     * @param entity entity for the body part content
     * @return this builder instance
     */
    public <T> BodyPartBuilder entity(T entity) {
        return entity(entity, GenericType.<T>create(entity.getClass()));
    }

    /**
     * Set the headers for this part.
     *
     * @param builder headers builder
     * @return this builder instance
     */
    public BodyPartBuilder headers(BodyPartHeaders.Builder builder) {
        this.headersBuilder = builder;
        return this;
    }

    /**
     * Set the headers for this part.
     *
     * @param map headers map
     * @return this builder instance
     */
    public BodyPartBuilder headers(Map<String, List<String>> map) {
        headersBuilder.headers(map);
        return this;
    }

    /**
     * Set the headers for this part.
     *
     * @param parameters headers
     * @return this builder instance
     */
    public BodyPartBuilder headers(Parameters parameters) {
        return headers(parameters.toMap());
    }

    /**
     * {@link ContentDisposition} name.
     * <p>
     * This value will be ignored if an actual instance of {@link BodyPartHeaders} is set.
     *
     * @param name content disposition name parameter
     * @return this builder instance
     */
    public BodyPartBuilder name(String name) {
        headersBuilder.name(name);
        return this;
    }

    /**
     * {@link ContentDisposition} filename.
     * <p>
     * This value will be ignored if an actual instance of {@link BodyPartHeaders} is set.
     *
     * @param filename content disposition filename parameter
     * @return this builder instance
     */
    public BodyPartBuilder filename(String filename) {
        headersBuilder.name(filename);
        return this;
    }

    @Override
    public BodyPart build() {
        BodyPartHeaders headers = headersBuilder.build();
        Entity entity = buildEntity(headers);
        return new BodyPartImpl(entity, headers);
    }
}
