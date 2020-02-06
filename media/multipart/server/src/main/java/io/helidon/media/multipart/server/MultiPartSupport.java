/*
 * Copyright (c) 2020 Oracle and/or its affiliates. All rights reserved.
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
package io.helidon.media.multipart.server;

import io.helidon.media.multipart.common.BodyPartBodyStreamReader;
import io.helidon.media.multipart.common.BodyPartBodyStreamWriter;
import io.helidon.media.multipart.common.MultiPartBodyReader;
import io.helidon.media.multipart.common.MultiPartBodyWriter;
import io.helidon.webserver.Handler;
import io.helidon.webserver.Routing;
import io.helidon.webserver.ServerRequest;
import io.helidon.webserver.ServerResponse;
import io.helidon.webserver.Service;


/**
 * Multi part support service.
 */
public final class MultiPartSupport implements Service, Handler {

    private final MultiPartBodyWriter multiPartWriter;
    private final MultiPartBodyReader multiPartReader;
    private final BodyPartBodyStreamWriter bodyPartWriter;
    private final BodyPartBodyStreamReader bodyPartReader;

    /**
     * Forces the use of {@link #create()}.
     */
    private MultiPartSupport(){
        multiPartReader = MultiPartBodyReader.get();
        multiPartWriter = MultiPartBodyWriter.get();
        bodyPartReader = BodyPartBodyStreamReader.get();
        bodyPartWriter = BodyPartBodyStreamWriter.get();
    }

    @Override
    public void update(Routing.Rules rules) {
        rules.any(this);
    }

    @Override
    public void accept(ServerRequest req, ServerResponse res) {
        req.content().registerReader(multiPartReader)
                .registerReader(bodyPartReader);
        res.registerWriter(multiPartWriter)
                .registerWriter(bodyPartWriter);
        req.next();
    }

    /**
     * Create a new instance of {@link MultiPartSupport}.
     * @return MultiPartSupport
     */
    public static MultiPartSupport create(){
        return new MultiPartSupport();
    }
}
