/*
 * Copyright (c) 2017, 2019 Oracle and/or its affiliates. All rights reserved.
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

package io.helidon.webserver;

import java.net.URI;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.StringTokenizer;
import io.helidon.common.http.ContextualRegistry;
import io.helidon.common.http.Http;
import io.helidon.common.http.MediaType;
import io.helidon.common.http.Parameters;
import io.helidon.common.http.Reader;
import io.helidon.common.reactive.Flow;
import io.helidon.media.common.ContentReaders;
import io.helidon.tracing.config.SpanTracingConfig;
import io.helidon.tracing.config.TracingConfigUtil;

import io.opentracing.Span;
import io.opentracing.SpanContext;
import io.opentracing.Tracer;
import io.opentracing.tag.Tags;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;

import io.helidon.common.GenericType;

import io.helidon.common.http.MessageBody.ReadableContent;
import io.helidon.common.http.MessageBodyContextBase;
import io.helidon.common.http.MessageBodyReadableContent;
import io.helidon.common.http.MessageBodyReaderContext;

import static io.helidon.common.CollectionsHelper.mapOf;

/**
 * The basic abstract implementation of {@link ServerRequest}.
 */
abstract class Request implements ServerRequest {
    private static final String TRACING_CONTENT_READ_NAME = "content-read";

    /**
     * The default charset to use in case that no charset or no mime-type is
     * defined in the content type header.
     */
    static final Charset DEFAULT_CHARSET = StandardCharsets.UTF_8;

    private final BareRequest bareRequest;
    private final WebServer webServer;
    private final ContextualRegistry context;
    private final Parameters queryParams;
    private final HashRequestHeaders headers;
    private final ReadableContent content;
    private final MessageBodyEventListener eventListener;

    /**
     * Creates new instance.
     *
     * @param req bare request from HTTP SPI implementation.
     * @param webServer relevant server.
     */
    Request(BareRequest req, WebServer webServer, HashRequestHeaders headers) {
        this.bareRequest = req;
        this.webServer = webServer;
        this.headers = headers;
        this.context = ContextualRegistry.create(webServer.context());
        this.queryParams = UriComponent.decodeQuery(req.uri().getRawQuery(),
                /* decode */ true);
        this.eventListener = new MessageBodyEventListener();
        MessageBodyReaderContext readerContext = MessageBodyReaderContext
                .create(webServer.mediaSupport().readerContext(),
                        eventListener, headers, headers.contentType());
        this.content = MessageBodyReadableContent.create(req.bodyPublisher(),
                readerContext);
    }

    /**
     * Creates clone of existing instance.
     *
     * @param request a request to clone.
     */
    Request(Request request) {
        this.bareRequest = request.bareRequest;
        this.webServer = request.webServer;
        this.context = request.context;
        this.queryParams = request.queryParams;
        this.headers = request.headers;
        this.content = request.content;
        this.eventListener = request.eventListener;
    }

    /**
     * Obtain the charset from the request.
     *
     * @param request the request to extract the charset from
     * @return the charset or {@link #DEFAULT_CHARSET} if none found
     */
    static Charset contentCharset(ServerRequest request) {
        return request.headers()
                      .contentType()
                      .flatMap(MediaType::charset)
                      .map(Charset::forName)
                      .orElse(DEFAULT_CHARSET);
    }

    @Override
    public WebServer webServer() {
        return webServer;
    }

    @Override
    public ContextualRegistry context() {
        return context;
    }

    @Override
    public Http.RequestMethod method() {
        return bareRequest.method();
    }

    @Override
    public Http.Version version() {
        return bareRequest.version();
    }

    @Override
    public URI uri() {
        return bareRequest.uri();
    }

    @Override
    public String query() {
        return bareRequest.uri().getRawQuery();
    }

    @Override
    public Parameters queryParams() {
        return queryParams;
    }

    @Override
    public String fragment() {
        return bareRequest.uri().getFragment();
    }

    @Override
    public String localAddress() {
        return bareRequest.localAddress();
    }

    @Override
    public int localPort() {
        return bareRequest.localPort();
    }

    @Override
    public String remoteAddress() {
        return bareRequest.remoteAddress();
    }

    @Override
    public int remotePort() {
        return bareRequest.remotePort();
    }

    @Override
    public boolean isSecure() {
        return bareRequest.isSecure();
    }

    @Override
    public RequestHeaders headers() {
        return headers;
    }

    @Override
    public ReadableContent content() {
        return this.content;
    }

    @Override
    public long requestId() {
        return bareRequest.requestId();
    }

    private final class MessageBodyEventListener
            implements MessageBodyContextBase.EventListener {

        private Span readSpan;

        @Override
        public void onEvent(MessageBodyContextBase.Event event) {
            switch(event.eventType()) {
                case BEFORE_ONSUBSCRIBE:
                    Tracer.SpanBuilder spanBuilder = tracer()
                            .buildSpan("content-read");
                    Span span = span();
                    if (span != null) {
                        spanBuilder.asChildOf(span);
                    }
                    GenericType<?> type = event.entityType().orElse(null);
                    if (type != null) {
                        spanBuilder.withTag("requested.type",
                                type.getTypeName());
                    }
                    readSpan = spanBuilder.start();
                    break;

                case AFTER_ONERROR:
                    if (readSpan != null) {
                        Tags.ERROR.set(readSpan, Boolean.TRUE);
                        Throwable ex = event.asErrorEvent().error();
                        readSpan.log(mapOf("event", "error",
                                "error.kind", "Exception",
                                "error.object", ex,
                                "message", ex.toString()));
                        readSpan.finish();
                    }
                    break;
                case AFTER_ONCOMPLETE:
                    if (readSpan != null) {
                        readSpan.finish();
                    }
                    break;
                default:
                    // do nothing
            }
        }
    }

    /**
     * {@link ServerRequest.Path} implementation.
     */
    static class Path implements ServerRequest.Path {

        private final String path;
        private final String rawPath;
        private final Map<String, String> params;
        private final Path absolutePath;
        private List<String> segments;

        /**
         * Creates new instance.
         *
         * @param path actual relative URI path.
         * @param rawPath actual relative URI path without any decoding.
         * @param params resolved path parameters.
         * @param absolutePath absolute path.
         */
        Path(String path, String rawPath, Map<String, String> params,
                Path absolutePath) {

            this.path = path;
            this.rawPath = rawPath;
            this.params = params == null ? Collections.emptyMap() : params;
            this.absolutePath = absolutePath;
        }

        @Override
        public String param(String name) {
            return params.get(name);
        }

        @Override
        public List<String> segments() {
            List<String> result = segments;
            // No synchronisation needed, worth case is multiple splitting.
            if (result == null) {
                StringTokenizer stok = new StringTokenizer(path, "/");
                result = new ArrayList<>();
                while (stok.hasMoreTokens()) {
                    result.add(stok.nextToken());
                }
                this.segments = result;
            }
            return result;
        }

        @Override
        public String toString() {
            return path;
        }

        @Override
        public String toRawString() {
            return rawPath;
        }

        @Override
        public Path absolute() {
            return absolutePath == null ? this : absolutePath;
        }

        static Path create(Path contextual, String path,
                Map<String, String> params) {

            return create(contextual, path, path, params);
        }

        static Path create(Path contextual, String path, String rawPath,
                Map<String, String> params) {

            if (contextual == null) {
                return new Path(path, rawPath, params, null);
            } else {
                return contextual.createSubpath(path, rawPath, params);
            }
        }

        Path createSubpath(String path, String rawPath,
                Map<String, String> params) {

            if (params == null) {
                params = Collections.emptyMap();
            }
            if (absolutePath == null) {
                HashMap<String, String> map =
                        new HashMap<>(this.params.size() + params.size());
                map.putAll(this.params);
                map.putAll(params);
                return new Path(path, rawPath, params,
                        new Path(this.path, this.rawPath, map, null));
            } else {
                int size = this.params.size() + params.size()
                        + absolutePath.params.size();
                HashMap<String, String> map = new HashMap<>(size);
                map.putAll(absolutePath.params);
                map.putAll(this.params);
                map.putAll(params);
                return new Path(path, rawPath, params,
                        new Path(absolutePath.path, absolutePath.rawPath, map,
                                /* absolute path */ null));
            }
        }
    }
}
