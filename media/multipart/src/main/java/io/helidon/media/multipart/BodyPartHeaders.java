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

import io.helidon.common.http.Headers;
import io.helidon.common.http.ReadOnlyParameters;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/**
 * Body part headers.
 */
public final class BodyPartHeaders extends ReadOnlyParameters
        implements Headers {

    BodyPartHeaders(Map<String, List<String>> data) {
        super(data);
        
    }

    // API for form-data content-disposition

    public static Builder builder() {
        return new Builder();
    }

    public static final class Builder
            implements io.helidon.common.Builder<BodyPartHeaders> {

        private final Map<String, List<String>> headers =
                new TreeMap<>(String.CASE_INSENSITIVE_ORDER);

        public Builder header(String name, String value) {
            List<String> values = headers.get(name);
            if (values == null) {
                values = new ArrayList<>();
                headers.put(name, values);
            }
            values.add(value);
            return this;
        }

        @Override
        public BodyPartHeaders build() {
            return new BodyPartHeaders(headers);
        }
    }
}
