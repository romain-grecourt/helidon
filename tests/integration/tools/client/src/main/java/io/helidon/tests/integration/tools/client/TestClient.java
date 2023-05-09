/*
 * Copyright (c) 2021, 2023 Oracle and/or its affiliates.
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
package io.helidon.tests.integration.tools.client;

import java.io.StringReader;
import java.io.UnsupportedEncodingException;
import java.lang.System.Logger.Level;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import io.helidon.nima.webclient.http1.Http1Client;
import io.helidon.nima.webclient.http1.Http1ClientRequest;

import jakarta.json.Json;
import jakarta.json.JsonArray;
import jakarta.json.JsonException;
import jakarta.json.JsonObject;
import jakarta.json.JsonReader;
import jakarta.json.JsonValue;
import org.junit.jupiter.api.Assertions;

/**
 * Web client to access all services of remote test application.
 */
public class TestClient {

    private static final System.Logger LOGGER = System.getLogger(TestClient.class.getName());

    private static final String UTF_8_STR = StandardCharsets.UTF_8.toString();

    private final Http1Client client;

    TestClient(final Http1Client client) {
        this.client = client;
    }

    Http1ClientRequest clientGetBuilderWithPath(final String service, final String method) {
        final StringBuilder sb = new StringBuilder(service.length() + (method != null ? method.length() : 0) + 2);
        sb.append('/');
        sb.append(service);
        if (method != null) {
            sb.append('/');
            sb.append(method);
        }
        return client.get(sb.toString());
    }

    private static String encode(final String str) {
        try {
            return URLEncoder.encode(str, UTF_8_STR);
        } catch (UnsupportedEncodingException ex) {
            Assertions.fail(String.format("URL fragment encoding failed: %s", ex.getMessage()));
        }
        return "";
    }

    private static String logStackTrace(final JsonObject response) {
        JsonArray stacktrace = response.getJsonArray("stacktrace");
        List<JsonObject> tracesList = stacktrace.getValuesAs(JsonObject.class);
        String logMsg = null;
        for (JsonObject trace : tracesList) {
            String message = trace.getString("message");
            LOGGER.log(Level.WARNING,
                    () -> String.format("%s: %s", trace.getString("class"), trace.getString("message")));
            JsonArray lines = trace.getJsonArray("trace");
            List<JsonObject> linesList = lines.getValuesAs(JsonObject.class);
            linesList.forEach((line) -> LOGGER.log(Level.WARNING, () -> String.format("    at %s$%s (%s:%d)",
                    line.getString("class"),
                    line.getString("method"), line.getString("file"), line.getInt("line"))));
            if (logMsg == null) {
                logMsg = message;
            }
        }
        return logMsg;
    }

    /**
     * Creates new web client builder instance.
     *
     * @return new web client builder instance
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Call remote test service method and return its data.
     *
     * @param service remote service name
     * @param method  remote test method name
     * @param params  remote test method query parameters
     * @return data returned by remote test service method
     */
    public JsonValue callServiceAndGetData(final String service, final String method, final Map<String, String> params) {
        return evaluateServiceCallResult(callService(clientGetBuilderWithPath(service, method), params));
    }

    /**
     * Call remote test service method and return its data.
     * No query parameters are passed.
     *
     * @param service remote service name
     * @param method  remote test method name
     * @return data returned by remote test service method
     */
    public JsonValue callServiceAndGetData(final String service, final String method) {
        return callServiceAndGetData(service, method, null);
    }

    /**
     * Call remote service method and return its raw data as JSON object.
     * No response content check is done.
     *
     * @param service remote service name
     * @param method  remote test method name
     * @param params  remote test method query parameters
     * @return data returned by remote service
     */
    public JsonObject callServiceAndGetRawData(final String service, final String method, final Map<String, String> params) {
        Http1ClientRequest rb = clientGetBuilderWithPath(service, method);
        rb.header("Accept", "application/json");
        return callService(rb, params);
    }

    /**
     * Call remote service method and return its raw data as JSON object.
     * No response content check is done. No query parameters are passed.
     *
     * @param service remote service name
     * @param method  remote test method name
     * @return data returned by remote service
     */
    public JsonObject callServiceAndGetRawData(final String service, final String method) {
        return callServiceAndGetRawData(service, method, null);
    }

    /**
     * Call remote service method and return its raw data as JSON object.
     * No response content check is done. No query parameters are passed.
     *
     * @param service remote service name
     * @param method  remote test method name
     * @return data returned by remote service
     */
    public String callServiceAndGetString(final String service, final String method) {
        Http1ClientRequest rb = clientGetBuilderWithPath(service, method);
        return rb.request(String.class);
    }

    /**
     * Direct access to web client.
     *
     * @return web client instance
     */
    public Http1Client client() {
        return client;
    }

    JsonObject callService(Http1ClientRequest rb, Map<String, String> params) {
        if (params != null && !params.isEmpty()) {
            for (Map.Entry<String, String> entry : params.entrySet()) {
                rb = rb.queryParam(entry.getKey(), encode(entry.getValue()));
            }
        }
        final String response = rb.request(String.class);
        try {
            final JsonReader jsonReader = Json.createReader(new StringReader(response));
            final JsonValue jsonContent = jsonReader.read();
            if (Objects.requireNonNull(jsonContent.getValueType()) == JsonValue.ValueType.OBJECT) {
                return jsonContent.asJsonObject();
            }
            throw new HelidonTestException(
                    String.format(
                            "Expected JSON object, but got JSON %s",
                            jsonContent.getValueType().name().toLowerCase()));
        } catch (JsonException t) {
            LOGGER.log(Level.WARNING,
                    () -> String.format(
                            "Caught %s when parsing response: %s",
                            t.getClass().getSimpleName(),
                            response), t);
            throw new HelidonTestException(
                    String.format(
                            "Caught %s when parsing response: %s",
                            t.getClass().getSimpleName(),
                            response),
                    t);
        }
    }

    JsonValue evaluateServiceCallResult(final JsonObject testData) {
        String status = testData.getString("status");
        switch (status) {
            case "OK" -> {
                return testData.get("data");
            }
            case "exception" -> {
                logStackTrace(testData);
                throw new HelidonTestException("Remote test execution failed.", testData.getJsonArray("stacktrace"));
            }
            default -> throw new HelidonTestException(String.format("Unknown response content: %s", testData));
        }
    }

    /**
     * Remote test web client builder.
     */
    public static class Builder {

        private static final String HTTP_PREFIX = "http://";

        private String host;
        private int port;

        Builder() {
            this.host = "localhost";
            this.port = 8080;
        }

        /**
         * Set test application URL host.
         *
         * @param host test application URL host
         * @return updated {@code TestClient} builder instance
         */
        public Builder host(final String host) {
            this.host = host;
            return this;
        }

        /**
         * Set test application URL port.
         *
         * @param port test application URL port
         * @return updated {@code TestClient} builder instance
         */
        public Builder port(final int port) {
            this.port = port;
            return this;
        }

        /**
         * Set test application URL service.
         * Setting service name will result in building extended test client
         * with service name support.
         *
         * @param service test application URL service
         * @return updated {@code TestServiceClient} builder instance
         */
        public TestServiceClient.Builder service(final String service) {
            return new TestServiceClient.Builder(this, service);
        }

        /**
         * Constructs base URL of web client.
         *
         * @return base URL of web client
         */
        String baseUrl() {
            return HTTP_PREFIX + host + ':' + port;
        }

        /**
         * Builds web client initialized with parameters stored in this builder.
         *
         * @return new {@link Http1Client} instance
         */
        Http1Client buildWebClient() {
            return Http1Client.builder()
                              .baseUri(baseUrl())
                              .build();
        }

        /**
         * Builds test web client initialized with parameters stored in this builder.
         *
         * @return new {@link TestClient} instance
         */
        public TestClient build() {
            return new TestClient(buildWebClient());
        }

    }

}
