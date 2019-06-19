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

import io.helidon.common.http.HttpContent;

/**
 * Body part base entity.
 *
 * @param <T> the type of {@link HttpContent}. In-bound parts use
 * {@link InBoundContent}, out-bound parts use {@link OutBoundContent}.
 */
public abstract class BodyPart<T extends HttpContent> {

    protected final T content;
    private final InBoundBodyPartHeaders headers;

    protected BodyPart(T content, InBoundBodyPartHeaders headers) {
        this.content = content;
        this.headers = headers;
    }

    /**
     * Get the reactive representation of the part content.
     * @return {@link Content}, never {@code null}
     */
    public T content() {
        return content;
    }

    /**
     * Returns http part headers.
     * @return BodyPartHeaders, never {@code null}
     */
    public InBoundBodyPartHeaders headers(){
        return headers;
    }

    /**
     * Get the control name.
     *
     * @return the {@code name} parameter of the {@code Content-Disposition}
     * header, or {@code null} if not present.
     */
    public String name() {
        return headers().contentDisposition().name().orElse(null);
    }

    /**
     * Get the file name.
     *
     * @return the {@code filename} parameter of the {@code Content-Disposition}
     * header, or {@code null} if not present.
     */
    public String filename() {
        return headers().contentDisposition().filename().orElse(null);
    }

    /**
     * Builder base class for creating {@link BodyPart} instances.
     */
    protected static abstract class Builder
            implements io.helidon.common.Builder {

        /**
         * Set the headers for this part.
         * @param headers headers
         * @return this builder instance
         */
        public abstract Builder headers(InBoundBodyPartHeaders headers);
    }
}
