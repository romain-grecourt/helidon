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

import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;

/**
 * Generator for multipart MIME message.
 */
final class MIMEGenerator {

    /**
     * The boundary string.
     */
    private final String bnd;

    /**
     * Create a new instance.
     * @param boundary the boundary string, must not be {@code null}
     * @throws NullPointerException if boundary is null
     */
    MIMEGenerator(String boundary) {
        Objects.requireNonNull(boundary, "boundary cannot be null");
        bnd = boundary;
    }

    /**
     * Add a header to the part.
     * @param header header name
     * @param values header values
     */
    String newPart(Map<String, List<String>> headers) {
        StringBuilder sb = new StringBuilder();

        // start boundary
        sb.append(bnd).append("\r\n");

        // headers lines
        for (Entry<String, List<String>> headerEntry : headers.entrySet()) {
            String headerName = headerEntry.getKey();
            for (String headerValue : headerEntry.getValue()) {
                sb.append(headerName)
                        .append(":")
                        .append(headerValue)
                        .append("\r\n");
            }
        }

        // end of headers empty line
        if (!headers.isEmpty()) {
            sb.append("\r\n");
        }
        return sb.toString();
    }

    /**
     * Signals the end of the generated multipart message.
     */
    String endMessage() {
        return bnd + "--";
    }
}
