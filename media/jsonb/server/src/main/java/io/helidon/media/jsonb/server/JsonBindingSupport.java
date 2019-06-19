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

package io.helidon.media.jsonb.server;

import java.util.Objects;

import javax.json.bind.Jsonb;
import javax.json.bind.JsonbBuilder;

import io.helidon.webserver.Handler;
import io.helidon.webserver.ServerRequest;
import io.helidon.webserver.ServerResponse;
import io.helidon.webserver.Service;

import io.helidon.media.jsonb.common.JsonBindingEntityReader;
import io.helidon.media.jsonb.common.JsonBindingEntityWriter;
import io.helidon.webserver.Routing;

/**
 * A {@link Service} and a {@link Handler} that provides <a
 * href="http://json-b.net/">JSON-B</a> support to Helidon.
 */
public final class JsonBindingSupport implements Service, Handler {

    private final JsonBindingEntityReader reader;
    private final JsonBindingEntityWriter writer;

    private JsonBindingSupport(final Jsonb jsonb) {
        this.reader = new JsonBindingEntityReader(jsonb);
        this.writer = new JsonBindingEntityWriter(jsonb);
    }

    @Override
    public void update(Routing.Rules rules) {
        rules.any(this);
    }

    @Override
    public void accept(final ServerRequest request, final ServerResponse response) {
        request.content().registerReader(reader);
        response.registerWriter(writer);
        request.next();
    }

    /**
     * Creates a new {@link JsonBindingSupport}.
     *
     * @param jsonb the Jsonb} to use; must not be {@code null}
     *
     * @return a new {@link JsonBindingSupport}
     *
     * @exception NullPointerException if {@code jsonb} is {@code
     * null}
     */
    public static JsonBindingSupport create(final Jsonb jsonb) {
        Objects.requireNonNull(jsonb);
        return new JsonBindingSupport(jsonb);
    }

    /**
     * Creates a new {@link JsonBindingSupport}.
     *
     * @return a new {@link JsonBindingSupport}
     */
    public static JsonBindingSupport create() {
        final Jsonb jsonb = JsonbBuilder.create();
        return create(jsonb);
    }

}
