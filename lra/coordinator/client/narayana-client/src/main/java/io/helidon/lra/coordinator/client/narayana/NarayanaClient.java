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
package io.helidon.lra.coordinator.client.narayana;

import java.lang.System.Logger.Level;
import java.net.URI;
import java.time.Duration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import io.helidon.common.http.ClientRequestHeaders;
import io.helidon.common.http.Http;
import io.helidon.common.http.WritableHeaders;
import io.helidon.common.socket.SocketOptions;
import io.helidon.lra.coordinator.client.CoordinatorClient;
import io.helidon.lra.coordinator.client.CoordinatorConnectionException;
import io.helidon.lra.coordinator.client.Participant;
import io.helidon.lra.coordinator.client.PropagatedHeaders;
import io.helidon.nima.faulttolerance.Retry;
import io.helidon.nima.http.media.MediaContext;
import io.helidon.nima.webclient.http1.Http1Client;
import io.helidon.nima.webclient.http1.Http1ClientRequest;
import io.helidon.nima.webclient.http1.Http1ClientResponse;

import org.eclipse.microprofile.lra.annotation.LRAStatus;
import org.eclipse.microprofile.lra.annotation.ws.rs.LRA;

/**
 * Narayana LRA coordinator client.
 */
public class NarayanaClient implements CoordinatorClient {
    private static final Http.HeaderName LRA_HTTP_CONTEXT_HEADER = Http.Header.create(LRA.LRA_HTTP_CONTEXT_HEADER);
    private static final Http.HeaderName LRA_HTTP_RECOVERY_HEADER = Http.Header.create(LRA.LRA_HTTP_RECOVERY_HEADER);

    private static final System.Logger LOGGER = System.getLogger(NarayanaClient.class.getName());

    private static final int RETRY_ATTEMPTS = 5;
    private static final String QUERY_PARAM_CLIENT_ID = "ClientID";
    private static final String QUERY_PARAM_TIME_LIMIT = "TimeLimit";
    private static final String QUERY_PARAM_PARENT_LRA = "ParentLRA";
    private static final Http.HeaderName HEADER_LINK = Http.Header.create("Link");
    private static final Pattern LRA_ID_PATTERN = Pattern.compile("(.*)/([^/?]+).*");

    private Supplier<URI> coordinatorUriSupplier;
    private Duration coordinatorTimeout;
    private Retry retry;

    @Override
    public void init(Supplier<URI> coordinatorUriSupplier, Duration timeout) {
        this.coordinatorUriSupplier = coordinatorUriSupplier;
        this.coordinatorTimeout = timeout;
        this.retry = Retry.builder()
                          .overallTimeout(timeout)
                          .retryPolicy(Retry.JitterRetryPolicy.builder()
                                                              .calls(RETRY_ATTEMPTS)
                                                              .build())
                          .build();
    }

    @Override
    public URI start(String clientID, PropagatedHeaders headers, long timeout) {
        return startInternal(null, clientID, headers, timeout);
    }

    @Override
    public URI start(URI parentLRAUri, String clientID, PropagatedHeaders headers, long timeout) {
        return startInternal(parentLRAUri, clientID, headers, timeout);
    }

    private URI startInternal(URI parentLRA, String clientID, PropagatedHeaders headers, long timeout) {
        // We need to call coordinator which knows parent LRA
        URI baseUri = Optional.ofNullable(parentLRA)
                              .map(p -> parseBaseUri(p.toASCIIString()))
                              .orElse(coordinatorUriSupplier.get());

        return invokeWithRetry(() -> {
            Http1ClientRequest req = newClient(baseUri)
                    .post()
                    .path("start")
                    .headers(copyHeaders(headers)) // header propagation
                    .queryParam(QUERY_PARAM_CLIENT_ID, Optional.ofNullable(clientID).orElse(""))
                    .queryParam(QUERY_PARAM_TIME_LIMIT, String.valueOf(timeout))
                    .queryParam(QUERY_PARAM_PARENT_LRA, parentLRA == null ? "" : parentLRA.toASCIIString());

            try (Http1ClientResponse res = req.request()) {
                Http.Status status = res.status();

                if (status.code() != 201) {
                    String cont = res.entity().as(String.class);
                    String message = String.format(
                            "Unexpected response %s from coordinator %s: %s",
                            status, req.resolvedUri(), cont);
                    LOGGER.log(Level.WARNING, message);
                    throw new CoordinatorConnectionException(message, null, 500);
                }

                //propagate supported headers from coordinator
                Map<String, List<String>> headersMap = new HashMap<>();
                res.headers().forEach(it -> headersMap.put(it.name(), it.allValues()));
                headers.scan(headersMap);

                URI lraId = res.headers()
                               .value(Http.Header.LOCATION)
                               // TMM doesn't send lraId as LOCATION
                               .or(() -> res.headers().first(LRA_HTTP_CONTEXT_HEADER))
                               .map(URI::create)
                               .orElseThrow(() -> new IllegalArgumentException(
                                       "Coordinator needs to return lraId either as 'Location' or "
                                               + "'Long-Running-Action' header."));

                logF("LRA started - LRAID: {0} parent: {1}", lraId, parentLRA);
                return lraId;
            }
        }, "Unable to start LRA");
    }

    @Override
    public void cancel(URI lraId, PropagatedHeaders headers) {
        invokeWithRetry(() -> {
            Http1ClientRequest req = newClient(lraId)
                    .put()
                    .path("/cancel")
                    .headers(copyHeaders(headers));// header propagation

            try (Http1ClientResponse res = req.request()) {

                Http.Status status = res
                        .status();
                //noinspection SwitchStatementWithTooFewBranches
                switch (status.family()) {
                    case SUCCESSFUL -> logF("LRA cancelled - LRAID: {0}", lraId);
                    default -> {
                        if (404 == status.code()) {
                            LOGGER.log(Level.WARNING, "Cancel LRA - Coordinator can't find id - LRAID: " + lraId);
                            break;
                        }
                        String message = "Unable to cancel lra " + lraId;
                        LOGGER.log(Level.WARNING, message);
                        throw new CoordinatorConnectionException(message, status.code());
                    }
                }
                return null;
            }
        }, "Unable to cancel LRA");
    }

    @Override
    public void close(URI lraId, PropagatedHeaders headers) {
        invokeWithRetry(() -> {
            Http1ClientRequest req = newClient(lraId)
                    .put()
                    .path("/close")
                    .headers(copyHeaders(headers)); // header propagation

            try (Http1ClientResponse res = req.request()) {
                Http.Status status = res.status();
                //noinspection SwitchStatementWithTooFewBranches
                switch (status.family()) {
                    case SUCCESSFUL -> logF("LRA closed - LRAID: {0}", lraId);
                    default -> {
                        // 404 can happen when coordinator already cleaned terminated lra's
                        if (List.of(410, 404).contains(status.code())) {
                            logF("LRA already closed - LRAID: {0}", lraId);
                            break;
                        }
                        String message = "Unable to close lra - LRAID: " + lraId;
                        LOGGER.log(Level.WARNING, message);
                        throw new CoordinatorConnectionException(message, status.code());
                    }
                }
            }
        }, "Unable to close LRA");
    }

    @Override
    public Optional<URI> join(URI lraId, PropagatedHeaders headers, long timeLimit, Participant p) {
        String links = compensatorLinks(p);
        return invokeWithRetry(() -> {
            Http1ClientRequest req = newClient(lraId)
                    .put()
                    .queryParam(QUERY_PARAM_TIME_LIMIT, String.valueOf(timeLimit))
                    .header(HEADER_LINK, links)
                    .headers(copyHeaders(headers)); // header propagation
            try (Http1ClientResponse res = req.submit(links)) {
                int statusCode = res.status().code();
                Optional<URI> maybeUri = switch (statusCode) {
                    case 412 -> {
                        String message = req.resolvedUri() + " Too late to join LRA - LRAID: " + lraId;
                        LOGGER.log(Level.WARNING, message);
                        throw new CoordinatorConnectionException(message, statusCode);
                    }
                    case 404, 410 -> {
                        // Narayana returns 404 for already terminated lras
                        String message = "Not found " + lraId;
                        LOGGER.log(Level.WARNING, message);
                        throw new CoordinatorConnectionException(message, statusCode);
                    }
                    case 200 -> res.headers()
                                   .first(LRA_HTTP_RECOVERY_HEADER)
                                   .map(URI::create);
                    default -> {
                        String message = "Unexpected coordinator response";
                        LOGGER.log(Level.WARNING, message);
                        throw new CoordinatorConnectionException(message, statusCode);
                    }
                };
                maybeUri.ifPresent(uri -> logF("Participant {0} joined - LRAID: {1}", p, lraId));
                return maybeUri;
            }

        }, "Unable to join LRA");
    }

    @Override
    public void leave(URI lraId, PropagatedHeaders headers, Participant p) {
        invokeWithRetry(() -> {
            Http1ClientRequest req = newClient(lraId)
                    .put()
                    .path("/remove")
                    .headers(copyHeaders(headers));// header propagation
            try (Http1ClientResponse res = req.submit(compensatorLinks(p))) {
                Http.Status status = res.status();
                switch (status.code()) {
                    case 404 -> LOGGER.log(Level.WARNING,
                            "Participant {0} leaving LRA - Coordinator can't find id - LRAID: {1}",
                            p, lraId);
                    case 200 -> logF("Participant {0} left - LRAID: {1}", p, lraId);
                    default -> throw new IllegalStateException("Unexpected coordinator response " + status);
                }
            }
        }, "Unable to leave LRA");
    }


    @Override
    public LRAStatus status(URI lraId, PropagatedHeaders headers) {
        return invokeWithRetry(() -> {
            Http1ClientRequest req = newClient(lraId)
                    .get()
                    .path("/status")
                    .headers(copyHeaders(headers)); // header propagation
            try (Http1ClientResponse res = req.request()) {
                switch (res.status().code()) {
                    case 404 -> {
                        LOGGER.log(Level.WARNING, "Status LRA - Coordinator can't find id - LRAID: " + lraId);
                        return LRAStatus.Closed;
                    }
                    case 200, 202 -> {
                        LRAStatus lraStatus = res.entity().as(LRAStatus.class);
                        logF("LRA status {0} retrieved - LRAID: {1}", lraStatus, lraId);
                        return lraStatus;
                    }
                    default -> throw new IllegalStateException("Unexpected coordinator response " + res.status());
                }
            }
        }, "Unable to retrieve LRA status of " + lraId);
    }

    private Http1Client newClient(URI uri) {
        return Http1Client.builder()
                          .baseUri(uri)
                          // Workaround for #3242
                          //.keepAlive(false)
                          .channelOptions(SocketOptions.builder()
                                                       .connectTimeout(coordinatorTimeout)
                                                       .readTimeout(coordinatorTimeout)
                                                       .build())
                          .mediaContext(MediaContext.builder()
                                                    .addMediaSupport(new LraStatusSupport())
                                                    .build())
                          .build();
    }

    /**
     * Narayana accepts participant's links as RFC 5988 {@code jakarta.ws.rs.core.Link}s delimited by commas.
     * <p>
     * Example:
     * <pre>{@code
     * <http://127.0.0.1:8080/lraresource/status>; rel="status"; title="status URI"; type="text/plain",
     * <http://127.0.0.1:8080/lraresource/compensate>; rel="compensate"; title="compensate URI"; type="text/plain",
     * <http://127.0.0.1:8080/lraresource/after>; rel="after"; title="after URI"; type="text/plain",
     * <http://127.0.0.1:8080/lraresource/complete>; rel="complete"; title="complete URI"; type="text/plain",
     * <http://127.0.0.1:8080/lraresource/forget>; rel="forget"; title="forget URI"; type="text/plain",
     * <http://127.0.0.1:8080/lraresource/leave>; rel="leave"; title="leave URI"; type="text/plain"
     * }</pre>
     *
     * @param p participant to serialize as links
     * @return links delimited by comma
     */
    private String compensatorLinks(Participant p) {
        return Map.of(
                          "compensate", p.compensate(),
                          "complete", p.complete(),
                          "forget", p.forget(),
                          "leave", p.leave(),
                          "after", p.after(),
                          "status", p.status()
                  )
                  .entrySet()
                  .stream()
                  .filter(e -> e.getValue().isPresent())
                  // rfc 5988
                  .map(e -> String.format("<%s>; rel=\"%s\"; title=\"%s\"; type=\"text/plain\"",
                          e.getValue().get(),
                          e.getKey(),
                          e.getKey() + " URI"))
                  .map(String::valueOf)
                  .collect(Collectors.joining(","));
    }

    static URI parseBaseUri(String lraUri) {
        Matcher m = LRA_ID_PATTERN.matcher(lraUri);
        if (!m.matches()) {
            //LRA id uri format
            throw new RuntimeException("Error when parsing lra uri: " + lraUri);
        }
        return URI.create(m.group(1));
    }

    private Function<ClientRequestHeaders, WritableHeaders<?>> copyHeaders(PropagatedHeaders headers) {
        return wcHeaders -> {
            headers.toMap().forEach((key, value) -> wcHeaders.set(Http.Header.create(key), value));
            return wcHeaders;
        };
    }

    private void invokeWithRetry(Runnable runnable, String message) {
        invokeWithRetry(() -> {
            runnable.run();
            return null;
        }, message);
    }

    private <T> T invokeWithRetry(Supplier<T> supplier, String message) {
        return retry.invoke(() -> {
            try {
                return supplier.get();
            } catch (Throwable t) {
                LOGGER.log(Level.WARNING, message, t);
                if (t instanceof CoordinatorConnectionException) {
                    throw t;
                }
                throw new CoordinatorConnectionException(message, t, 500);
            }
        });
    }

    private void logF(String msg, Object... params) {
        LOGGER.log(Level.DEBUG, msg, params);
    }
}

