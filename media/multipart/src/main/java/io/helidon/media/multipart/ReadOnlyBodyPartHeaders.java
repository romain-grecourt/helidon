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

import java.util.List;
import java.util.Map;

import io.helidon.common.http.Http;
import io.helidon.common.http.ReadOnlyParameters;

/**
 * Read-only body part headers.
 */
final class ReadOnlyBodyPartHeaders extends ReadOnlyParameters implements BodyPartHeaders {

    private final Object internalLock = new Object();
    private ContentDisposition contentDisposition;

    /**
     * Create a new instance.
     *
     * @param params headers map
     */
    ReadOnlyBodyPartHeaders(Map<String, List<String>> params) {
        super(params);
    }

    @Override
    public ContentDisposition contentDisposition() {
        if (contentDisposition == null) {
            synchronized (internalLock) {
                contentDisposition = first(Http.Header.CONTENT_DISPOSITION)
                        .map(ContentDisposition::parse)
                        .orElse(ContentDisposition.EMPTY);
            }
        }
        return contentDisposition;
    }
}
