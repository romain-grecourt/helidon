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
package io.helidon.media.multipart.server;

import io.helidon.media.multipart.common.BodyPartStreamReader;
import io.helidon.media.multipart.common.BodyPartStreamWriter;
import io.helidon.media.multipart.common.MultiPartReader;
import io.helidon.media.multipart.common.MultiPartWriter;
import io.helidon.webserver.Handler;
import io.helidon.webserver.Routing;
import io.helidon.webserver.ServerRequest;
import io.helidon.webserver.ServerResponse;
import io.helidon.webserver.Service;


/**
 * Multi part support service.
 */
public final class MultiPartSupport implements Service, Handler {

    private final MultiPartWriter multiPartWriter;
    private final MultiPartReader multiPartReader;
    private final BodyPartStreamWriter bodyPartWriter;
    private final BodyPartStreamReader bodyPartReader;

    /**
     * Forces the use of {@link #create()}.
     */
    private MultiPartSupport(){
        multiPartReader = MultiPartReader.create();
        multiPartWriter = MultiPartWriter.create();
        bodyPartReader = BodyPartStreamReader.create();
        bodyPartWriter = BodyPartStreamWriter.create();
    }

    /**
     * Create a new instance of {@link MultiPartSupport}.
     * @return MultiPartSupport
     */
    public static MultiPartSupport create(){
        return new MultiPartSupport();
    }

    @Override
    public void update(Routing.Rules rules) {
        rules.any(this);
    }

    @Override
    public void accept(ServerRequest req, ServerResponse res) {
        req.content().registerReader(multiPartReader)
                .registerStreamReader(bodyPartReader);
        res.registerWriter(multiPartWriter)
                .registerStreamWriter(bodyPartWriter);
        req.next();
    }
}
