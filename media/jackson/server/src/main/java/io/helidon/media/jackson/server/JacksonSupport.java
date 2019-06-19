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
package io.helidon.media.jackson.server;

import java.util.Objects;
import io.helidon.media.jackson.common.JacksonEntityReader;
import io.helidon.media.jackson.common.JacksonEntityWriter;
import io.helidon.webserver.Handler;
import io.helidon.webserver.ServerRequest;
import io.helidon.webserver.ServerResponse;
import io.helidon.webserver.Service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.fasterxml.jackson.module.paramnames.ParameterNamesModule;

import io.helidon.webserver.Routing;

/**
 * A {@link Service} and a {@link Handler} that provides Jackson
 * support to Helidon.
 */
public final class JacksonSupport implements Service, Handler {

    private final JacksonEntityReader reader;
    private final JacksonEntityWriter writer;

    /**
     * Creates a new {@link JacksonSupport}.
     *
     * @param objectMapper mapper, must not be {@code null}
     *
     * @exception NullPointerException if {@code objectMapper} is {@code null}
     */
    private JacksonSupport(final ObjectMapper objectMapper) {
        this.reader = new JacksonEntityReader(objectMapper);
        this.writer = new JacksonEntityWriter(objectMapper);
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
     * Creates a new {@link JacksonSupport}.
     *
     * @return a new {@link JacksonSupport}
     */
    public static JacksonSupport create() {
        final ObjectMapper mapper = new ObjectMapper()
            .registerModule(new ParameterNamesModule())
            .registerModule(new Jdk8Module())
            .registerModule(new JavaTimeModule());
        return create(mapper);
    }

    /**
     * Creates a new {@link JacksonSupport}.
     *
     * @param objectMapper, must not be {@code null}
     * @return a new {@link JacksonSupport}
     *
     * @exception NullPointerException if {@code objectMapper}
     * is {@code null}
     */
    public static JacksonSupport create(ObjectMapper objectMapper) {
        return new JacksonSupport(objectMapper);
    }
}
