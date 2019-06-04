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

package io.helidon.media.jsonp.server;

import java.nio.charset.Charset;

import io.helidon.common.http.DataChunk;
import io.helidon.webserver.ServerRequest;
import io.helidon.webserver.ServerResponse;

import static io.helidon.media.common.ContentTypeCharset.determineCharset;

/**
 * Class JsonArrayStreamWriter.
 */
public class JsonArrayStreamWriter<T> extends JsonStreamWriter<T> {

    public JsonArrayStreamWriter(ServerRequest request, ServerResponse response, Class<T> type) {
        super(response, type);
        Charset charset = determineCharset(request.headers());
        beginChunk(DataChunk.create("[".getBytes(charset)));
        separatorChunk(DataChunk.create(",".getBytes(charset)));
        endChunk(DataChunk.create("]".getBytes(charset)));
    }
}
