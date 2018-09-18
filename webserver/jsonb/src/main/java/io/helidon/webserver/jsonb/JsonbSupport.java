/*
 * Copyright (c) 2017, 2018 Oracle and/or its affiliates. All rights reserved.
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

package io.helidon.webserver.jsonb;

import java.lang.reflect.Type;
import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.util.List;
import java.util.Objects;
import java.util.function.Function;

import io.helidon.common.http.DataChunk;
import io.helidon.common.http.Http;
import io.helidon.common.http.MediaType;
import io.helidon.common.http.Parameters;
import io.helidon.common.http.Reader;
import io.helidon.common.reactive.Flow;
import io.helidon.common.reactive.Flow.Publisher;

import io.helidon.webserver.ContentReaders;
import io.helidon.webserver.ContentWriters;
import io.helidon.webserver.Handler;
import io.helidon.webserver.Routing;
import io.helidon.webserver.ServerRequest;
import io.helidon.webserver.ServerResponse;
import io.helidon.webserver.Service;

import javax.json.bind.Jsonb;
import javax.json.bind.JsonbBuilder;
import javax.json.bind.JsonbConfig;

/**
 *
 * @author rgrecour
 */
public class JsonbSupport implements Service, Handler {

    /**
     * JSONP (JSON with Pending) can have this weird type.
     */
    private static final MediaType APPLICATION_JAVASCRIPT = new MediaType("application", "javascript");

    private final Jsonb jsonb;
    private final Class type;
    private final Type runtimeType;

    private JsonbSupport(Class type, Type runtimeType, JsonbConfig config){
        Objects.requireNonNull(type, "type is null");
        this.type = type;
        this.runtimeType = runtimeType;
        if(config != null){
            jsonb = JsonbBuilder.create(config);
        } else {
            jsonb = JsonbBuilder.create();
        }
    }

    @Override
    public void update(Routing.Rules routingRules) {
        routingRules.any(this);
    }

    @Override
    public void accept(ServerRequest req, ServerResponse res) {
        // Reader
        req.content()
               .registerReader(type::isAssignableFrom, (publisher, clazz) -> {
                   Charset charset = determineCharset(req.headers());
                   return reader(charset).apply(publisher);
               });

        // Writer
        res.registerWriter(obj -> (type.isAssignableFrom(obj.getClass()))
                && testOrSetContentType(req, res),
                bean -> {
                    Charset charset = determineCharset(res.headers());
                    return writer(charset).apply(bean);
                });
        req.next();
    }

    private Charset determineCharset(Parameters headers) {
        return headers.first(Http.Header.CONTENT_TYPE)
                .map(MediaType::parse)
                .flatMap(MediaType::getCharset)
                .map(sch -> {
                    try {
                        return Charset.forName(sch);
                    } catch (Exception e) {
                        return Charset.defaultCharset();
                    }
                })
                .orElse(Charset.defaultCharset());
    }

    private boolean testOrSetContentType(ServerRequest request, ServerResponse response) {
        MediaType mt = response.headers().contentType().orElse(null);
        if (mt == null) {
            // Find if accepts any JSON compatible type
            List<MediaType> acceptedTypes = request.headers().acceptedTypes();
            MediaType preferredType;
            if (acceptedTypes.isEmpty()) {
                preferredType = MediaType.APPLICATION_JSON;
            } else {
                preferredType = acceptedTypes
                        .stream()
                        .map(mediaType -> {
                            if (mediaType.test(MediaType.APPLICATION_JSON)) {
                                return MediaType.APPLICATION_JSON;
                            } else if (mediaType.test(APPLICATION_JAVASCRIPT)) {
                                return APPLICATION_JAVASCRIPT;
                            } else if (mediaType.hasSuffix("json")) {
                                return new MediaType(mediaType.getType(), mediaType.getSubtype());
                            } else {
                                return null;
                            }
                        })
                        .filter(Objects::nonNull)
                        .findFirst()
                        .orElse(null);
            }
            if (preferredType == null) {
                return false;
            } else {
                response.headers().contentType(preferredType);
                return true;
            }
        } else {
            return MediaType.JSON_PREDICATE.test(mt);
        }
    }

    /**
     * Returns a function (reader) converting {@link Flow.Publisher Publisher} of {@link ByteBuffer}s to
     * {@link Object}.
     * <p>
     * It is intended for derivation of others, more specific readers.
     *
     * @param charset a charset to use or {@code null} for default charset
     * @return the byte array content reader that transforms a publisher of byte buffers to a completion stage that
     *         might end exceptionally with a {@link IllegalArgumentException} in case of I/O error or
     *         a {@link javax.json.bind.JsonbException}
     */
    public Reader<Object> reader(Charset charset) {
        return (publisher, clazz) -> {
            return ContentReaders.stringReader(charset)
                    .apply(publisher)
                    .thenApply(str -> {
                        if (runtimeType != null) {
                            return jsonb.fromJson(str, runtimeType);
                        }
                        return jsonb.fromJson(str, type);
                    });
        };
    }

    public Reader<Object> reader(){
        return reader(Charset.defaultCharset());
    }

    /**
     * Returns a function (writer) converting {@link Object} to the {@link Flow.Publisher Publisher}
     * of {@link DataChunk}s.
     *
     * @param charset a charset to use or {@code null} for default charset
     * @return created function
     */
    public Function<Object, Publisher<DataChunk>> writer(Charset charset) {
        return obj -> {
            String str;
            if(runtimeType != null){
                str = jsonb.toJson(obj, runtimeType);
            } else {
                str = jsonb.toJson(obj, type);
            }
            return ContentWriters.charSequenceWriter(charset).apply(str);
        };
    }

    public Function<Object, Publisher<DataChunk>> writer() {
        return writer(Charset.defaultCharset());
    }

    public static class Builder {

        private Class type;
        private Type runtimeType;
        private JsonbConfig config;

        public Builder type(Class type){
            this.type = type;
            return this;
        }

        public Builder runtimeType(Type type){
            this.runtimeType = type;
            return this;
        }

        public Builder config(JsonbConfig config){
            this.config = config;
            return this;
        }

        public JsonbSupport build(){
            return new JsonbSupport(type, runtimeType, config);
        }
    }

    public static Builder builder(){
        return new Builder();
    }
}
