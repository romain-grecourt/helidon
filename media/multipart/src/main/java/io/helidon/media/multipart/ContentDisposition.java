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

import static io.helidon.common.CollectionsHelper.listOf;
import io.helidon.common.http.ReadOnlyParameters;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * A representation of the {@code Content-Disposition} header as described in
 * <a href="https://tools.ietf.org/html/rfc2183">RFC 2183</a>.
 */
public final class ContentDisposition extends ReadOnlyParameters {

    private final CharSequence type;

    private ContentDisposition(CharSequence type, Map<String, List<String>> params) {
        super(params);
        this.type = type;
    }

    public static final class Builder
            implements io.helidon.common.Builder<ContentDisposition> {

        private String type;
        private Map<String, List<String>> params = new HashMap<>();

        /**
         * Set the type to {@code form-data}.
         * @return the builder instance
         */
        public Builder formDataType() {
            return type("form-data");
        }

        /**
         * Set the type.
         * @param type 
         * @return the builder instance
         */
        public Builder type(String type) {
            this.type = type;
            return this;
        }

        /**
         * Set the {@code filename} parameter.
         * @param filename filename value
         * @return the builder instance
         */
        public Builder filename(String filename) {
            return param("filename", filename);
        }

        /**
         * Add a parameter to the header.
         * @param name parameter name
         * @param value parameter value
         * @return the builder instance
         */
        public Builder param(String name, String value){
            params.put(name, listOf(value));
            return this;
        }

        @Override
        public ContentDisposition build() {
            return new ContentDisposition(type, params);
        }
    }
}
