package io.helidon.security.integration.nima;

import io.helidon.common.context.Context;
import io.helidon.common.context.Contexts;
import io.helidon.common.http.Http;
import io.helidon.nima.webclient.ClientService;
import io.helidon.nima.webclient.ClientServiceRequest;
import io.helidon.security.Security;
import io.helidon.security.EndpointConfig;
import io.helidon.security.OutboundSecurityClientBuilder;
import io.helidon.security.OutboundSecurityResponse;
import io.helidon.security.SecurityContext;
import io.helidon.security.SecurityEnvironment;
import io.helidon.security.providers.common.OutboundConfig;
import io.helidon.tracing.Span;
import io.helidon.tracing.SpanContext;
import io.helidon.tracing.Tracer;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static java.lang.System.Logger.Level.DEBUG;

public class ClientSecurity implements ClientService {

    private static final System.Logger LOGGER = System.getLogger(ClientSecurity.class.getName());
    private static final String PROVIDER_NAME = "io.helidon.security.rest.client.security.providerName";

    private final Security security;

    private ClientSecurity() {
        this(null);
    }

    private ClientSecurity(Security security) {
        this.security = security;
    }

    /**
     * Creates new instance of client security service.
     *
     * @return client security service
     */
    public static ClientSecurity create() {
        Context context = Contexts.context().orElseGet(Contexts::globalContext);
        return context.get(Security.class)
                      .map(ClientSecurity::new) // if available, use constructor with Security parameter
                      .orElseGet(ClientSecurity::new); // else use constructor without Security parameter
    }

    /**
     * Creates new instance of client security service base on {@link Security}.
     *
     * @param security security instance
     * @return client security service
     */
    public static ClientSecurity create(Security security) {
        // if we have one more configuration parameter, we need to switch to builder based pattern
        return new ClientSecurity(security);
    }

    @Override
    public ClientServiceRequest request(ClientServiceRequest request) {
        Map<String, String> properties = request.properties();
        if ("true".equalsIgnoreCase(properties.get(OutboundConfig.PROPERTY_DISABLE_OUTBOUND))) {
            return request;
        }

        Context requestContext = request.context();
        // context either from request or create a new one
        Optional<SecurityContext> maybeContext = requestContext.get(SecurityContext.class);

        SecurityContext context;

        if (null == security) {
            if (maybeContext.isEmpty()) {
                return request;
            } else {
                context = maybeContext.get();
            }
        } else {
            // we have our own security - we need to use this instance for outbound,
            // so we cannot re-use the context
            context = createContext(request);
        }

        Span span = context.tracer()
                           .spanBuilder("security:outbound")
                           .parent(context.tracingSpan())
                           .start();

        String explicitProvider = properties.get(PROVIDER_NAME);

        OutboundSecurityClientBuilder clientBuilder;

        try {
            SecurityEnvironment.Builder outboundEnv = context.env()
                                                             .derive()
                                                             .clearHeaders()
                                                             .clearQueryParams();

            outboundEnv.method(request.method())
                       .path(request.path())
                       .targetUri(request.resolvedUri())
                       .headers(request.headers())
                       .queryParams(request.query());

            EndpointConfig.Builder outboundEp = context.endpointConfig().derive();

            for (String name : properties.keySet()) {
                Optional.ofNullable(properties.get(name))
                        .ifPresent(property -> outboundEp.addAtribute(name, property));
            }

            clientBuilder = context.outboundClientBuilder()
                                   .outboundEnvironment(outboundEnv)
                                   .outboundEndpointConfig(outboundEp)
                                   .explicitProvider(explicitProvider);

        } catch (Exception e) {
            traceError(span, e, null);
            throw e;
        }

        OutboundSecurityResponse providerResponse = clientBuilder.submit();
        return processResponse(request, span, providerResponse);
    }

    private ClientServiceRequest processResponse(ClientServiceRequest request,
                                                 Span span,
                                                 OutboundSecurityResponse providerResponse) {
        try {
            switch (providerResponse.status()) {
                case FAILURE, FAILURE_FINISH -> traceError(span,
                        providerResponse.throwable().orElse(null),
                        providerResponse.description()
                                        .orElse(providerResponse.status().toString()));
            }

            Map<String, List<String>> newHeaders = providerResponse.requestHeaders();

            LOGGER.log(DEBUG, () -> "Client filter header(s). SIZE: " + newHeaders.size());

            request.headers(headers -> {
                for (Map.Entry<String, List<String>> entry : newHeaders.entrySet()) {
                    LOGGER.log(DEBUG, () -> "    + Header: " + entry.getKey() + ": " + entry.getValue());

                    //replace existing
                    Http.HeaderName headerName = Http.Header.create(entry.getKey());
                    headers.remove(headerName);
                    for (String value : entry.getValue()) {
                        headers.add(headerName, value);
                    }
                }
                return headers;
            });
            span.end();
            return request;
        } catch (Exception e) {
            traceError(span, e, null);
            throw e;
        }
    }

    private SecurityContext createContext(ClientServiceRequest request) {
        SecurityContext.Builder builder = security.contextBuilder(UUID.randomUUID().toString())
                                                  .endpointConfig(EndpointConfig.builder()
                                                                                .build())
                                                  .env(SecurityEnvironment.builder()
                                                                          .path(request.path().toString())
                                                                          .build());
        request.context().get(Tracer.class).ifPresent(builder::tracingTracer);
        request.context().get(SpanContext.class).ifPresent(builder::tracingSpan);
        return builder.build();
    }

    static void traceError(Span span, Throwable throwable, String description) {
        // failed
        span.status(Span.Status.ERROR);

        if (throwable == null) {
            span.addEvent("error", Map.of("message", description,
                    "error.kind", "SecurityException"));
            span.end();
        } else {
            span.end(throwable);
        }
    }
}
