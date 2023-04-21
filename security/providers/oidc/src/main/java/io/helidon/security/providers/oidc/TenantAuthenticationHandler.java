/*
 * Copyright (c) 2023 Oracle and/or its affiliates.
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

package io.helidon.security.providers.oidc;

import java.lang.System.Logger.Level;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.function.BiConsumer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import io.helidon.common.Errors;
import io.helidon.common.http.Http;
import io.helidon.common.http.HttpMediaType;
import io.helidon.common.parameters.Parameters;
import io.helidon.nima.webclient.http1.Http1ClientRequest;
import io.helidon.security.AuthenticationResponse;
import io.helidon.security.EndpointConfig.AnnotationScope;
import io.helidon.security.Grant;
import io.helidon.security.Principal;
import io.helidon.security.ProviderRequest;
import io.helidon.security.Role;
import io.helidon.security.Security;
import io.helidon.security.SecurityEnvironment;
import io.helidon.security.SecurityLevel;
import io.helidon.security.SecurityResponse.SecurityStatus;
import io.helidon.security.Subject;
import io.helidon.security.abac.scope.ScopeValidator.Scope;
import io.helidon.security.abac.scope.ScopeValidator.Scopes;
import io.helidon.security.jwt.Jwt;
import io.helidon.security.jwt.JwtException;
import io.helidon.security.jwt.JwtUtil;
import io.helidon.security.jwt.SignedJwt;
import io.helidon.security.jwt.jwk.JwkKeys;
import io.helidon.security.providers.common.TokenCredential;
import io.helidon.security.providers.oidc.common.OidcConfig;
import io.helidon.security.providers.oidc.common.OidcConfig.RequestType;
import io.helidon.security.providers.oidc.common.Tenant;
import io.helidon.security.providers.oidc.common.TenantConfig;
import io.helidon.security.util.TokenHandler;

import jakarta.json.JsonObject;

import static io.helidon.common.http.Http.Header.WWW_AUTHENTICATE;
import static io.helidon.common.http.Http.Status.UNAUTHORIZED_401;
import static io.helidon.security.providers.oidc.common.OidcConfig.postJsonResponse;
import static io.helidon.security.providers.oidc.common.spi.TenantConfigFinder.DEFAULT_TENANT_ID;

/**
 * Authentication handler.
 */
class TenantAuthenticationHandler {
    private static final System.Logger LOGGER = System.getLogger(TenantAuthenticationHandler.class.getName());
    private static final TokenHandler PARAM_HEADER_HANDLER = TokenHandler.forHeader(OidcConfig.PARAM_HEADER_NAME);

    private final boolean optional;
    private final OidcConfig oidcConfig;
    private final TenantConfig tenantConfig;
    private final Tenant tenant;
    private final boolean useJwtGroups;
    private final Errors.Collector collector = Errors.collector();
    private final BiConsumer<StringBuilder, String> scopeAppender;
    private final Pattern attemptPattern;

    TenantAuthenticationHandler(OidcConfig oidcConfig, Tenant tenant, boolean useJwtGroups, boolean optional) {
        this.oidcConfig = oidcConfig;
        this.tenant = tenant;
        this.tenantConfig = tenant.tenantConfig();
        this.useJwtGroups = useJwtGroups;
        this.optional = optional;
        this.attemptPattern = Pattern.compile(".*?" + oidcConfig.redirectAttemptParam() + "=(\\d+).*");

        // clean the scope audience - must end with / if exists
        String audience = tenantConfig.scopeAudience();
        if (audience == null || audience.isEmpty()) {
            this.scopeAppender = StringBuilder::append;
        } else {
            if (audience.endsWith("/")) {
                this.scopeAppender = (sb, scope) -> sb.append(audience).append(scope);
            } else {
                this.scopeAppender = (sb, scope) -> sb.append(audience).append("/").append(scope);
            }
        }
    }

    private void validateJwtWithJwk(SignedJwt signedJwt) {
        JwkKeys jwk = tenant.signJwk();
        Errors errors = signedJwt.verifySignature(jwk);
        errors.forEach(errorMessage -> {
            switch (errorMessage.getSeverity()) {
                case FATAL -> collector.fatal(errorMessage.getSource(), errorMessage.getMessage());
                case WARN -> collector.warn(errorMessage.getSource(), errorMessage.getMessage());
                default -> collector.hint(errorMessage.getSource(), errorMessage.getMessage());
            }
        });
    }

    private void validateJwtWithPost(SignedJwt signedJwt) {

        Http1ClientRequest post = tenant.appWebClient()
                                        .post()
                                        .uri(tenant.introspectUri())
                                        .accept(HttpMediaType.APPLICATION_JSON)
                                        .header(Http.Header.CACHE_CONTROL, "no-cache, no-store, must-revalidate");

        Parameters.Builder form = Parameters.builder("oidc-form-params")
                                            .add("token", signedJwt.tokenContent());

        OidcUtil.updateRequest(RequestType.INTROSPECT_JWT, tenantConfig, form);

        try {
            JsonObject json = postJsonResponse(post, form.build());
            if (!json.getBoolean("active")) {
                this.collector.fatal(json, "Token is not active");
            }
        } catch (OidcConfig.OidcResponseException ex) {
            collector.fatal(ex.status(), String.format(
                    "Failed to validate token, response status: %s, entity: %s", ex.status(), ex.entity()));
        } catch (Throwable ex) {
            collector.fatal(ex, "Failed to validate token, request failed: " + ex.getMessage());
        }
    }

    AuthenticationResponse authenticate(String tenantId, ProviderRequest providerRequest) {
        /*
          1. Get token from request - if available, validate it and continue
          2. If not - Redirect to login page
         */
        List<String> missingLocations = new LinkedList<>();

        Optional<String> token = Optional.empty();
        try {
            if (oidcConfig.useHeader()) {
                token = token.or(() -> oidcConfig.headerHandler().extractToken(providerRequest.env().headers()));

                if (token.isEmpty()) {
                    missingLocations.add("header");
                }
            }

            if (oidcConfig.useParam()) {
                token = token.or(() -> PARAM_HEADER_HANDLER.extractToken(providerRequest.env().headers()));

                if (token.isEmpty()) {
                    token = token.or(() -> providerRequest.env().queryParams().first(oidcConfig.paramName()));
                }

                if (token.isEmpty()) {
                    missingLocations.add("query-param");
                }
            }

            if (oidcConfig.useCookie()) {
                if (token.isEmpty()) {
                    // only do this for cookies
                    Optional<String> cookie = oidcConfig.tokenCookieHandler()
                                                        .findCookie(providerRequest.env().headers());
                    if (cookie.isEmpty()) {
                        missingLocations.add("cookie");
                    } else {
                        try {
                            return validateToken(tenantId, providerRequest, cookie.get());
                        } catch (Throwable throwable) {
                            String msg = "Invalid token in cookie";
                            LOGGER.log(Level.DEBUG, msg, throwable);
                            return errorResponse(providerRequest, UNAUTHORIZED_401, null, msg, tenantId);
                        }
                    }
                }
            }
        } catch (SecurityException e) {
            String msg = "Failed to extract token from one of the configured locations";
            LOGGER.log(Level.DEBUG, msg, e);
            return failOrAbstain(msg + e);
        }

        if (token.isPresent()) {
            return validateToken(tenantId, providerRequest, token.get());
        } else {
            String msg = "Missing token, could not find in either of: " + missingLocations;
            LOGGER.log(Level.DEBUG, msg);
            return errorResponse(providerRequest, UNAUTHORIZED_401, null, msg, tenantId);
        }
    }

    private Set<String> expectedScopes(ProviderRequest request) {
        Set<String> result = new HashSet<>();

        AnnotationScope[] allScopes = AnnotationScope.values();
        for (SecurityLevel securityLevel : request.endpointConfig().securityLevels()) {
            List<Scopes> expectedScopes = securityLevel.combineAnnotations(Scopes.class, allScopes);
            expectedScopes.stream()
                          .map(Scopes::value)
                          .map(Arrays::asList)
                          .map(List::stream)
                          .forEach(stream -> stream.map(Scope::value)
                                                   .forEach(result::add));

            List<Scope> annotations = securityLevel.combineAnnotations(Scope.class, allScopes);
            annotations.stream()
                       .map(Scope::value)
                       .forEach(result::add);
        }

        return result;
    }

    private AuthenticationResponse errorResponse(ProviderRequest providerRequest,
                                                 Http.Status status,
                                                 String code,
                                                 String description,
                                                 String tenantId) {
        if (oidcConfig.shouldRedirect()) {
            // make sure we do not exceed redirect limit
            String state = origUri(providerRequest);
            int redirectAttempt = redirectAttempt(state);
            if (redirectAttempt >= oidcConfig.maxRedirects()) {
                return errorResponseNoRedirect(code, description, status);
            }

            Set<String> expectedScopes = expectedScopes(providerRequest);

            StringBuilder scopes = new StringBuilder(tenantConfig.baseScopes());

            for (String expectedScope : expectedScopes) {
                if (scopes.length() > 0) {
                    // space after base scopes
                    scopes.append(' ');
                }
                String scope = expectedScope;
                if (scope.startsWith("/")) {
                    scope = scope.substring(1);
                }
                scopeAppender.accept(scopes, scope);
            }

            String scopeString;
            scopeString = URLEncoder.encode(scopes.toString(), StandardCharsets.UTF_8);

            String authorizationEndpoint = tenant.authorizationEndpointUri();
            String nonce = UUID.randomUUID().toString();
            String redirectUri;
            if (DEFAULT_TENANT_ID.equals(tenantId)) {
                redirectUri = encode(redirectUri(providerRequest.env()));
            } else {
                redirectUri = encode(redirectUri(providerRequest.env()) + "?"
                        + encode(oidcConfig.tenantParamName()) + "=" + encode(tenantId));
            }

            String queryString = "?" + "client_id=" + tenantConfig.clientId() + "&" +
                    "response_type=code&" +
                    "redirect_uri=" + redirectUri + "&" +
                    "scope=" + scopeString + "&" +
                    "nonce=" + nonce + "&" +
                    "state=" + encode(state);

            // must redirect
            return AuthenticationResponse
                    .builder()
                    .status(SecurityStatus.FAILURE_FINISH)
                    .statusCode(Http.Status.TEMPORARY_REDIRECT_307.code())
                    .description("Redirecting to identity server: " + description)
                    .responseHeader("Location", authorizationEndpoint + queryString)
                    .build();
        }
        return errorResponseNoRedirect(code, description, status);
    }

    private String redirectUri(SecurityEnvironment env) {
        for (Map.Entry<String, List<String>> entry : env.headers().entrySet()) {
            if (entry.getKey().equalsIgnoreCase("host") && !entry.getValue().isEmpty()) {
                String firstHost = entry.getValue().get(0);
                return oidcConfig.redirectUriWithHost(oidcConfig.forceHttpsRedirects() ? "https" : env.transport()
                        + "://" + firstHost);
            }
        }

        return oidcConfig.redirectUriWithHost();
    }

    private AuthenticationResponse failOrAbstain(String message) {
        return AuthenticationResponse.builder()
                                     .status(optional ? SecurityStatus.ABSTAIN : SecurityStatus.FAILURE)
                                     .description(message)
                                     .build();
    }

    private AuthenticationResponse errorResponseNoRedirect(String code, String description, Http.Status status) {
        if (optional) {
            return AuthenticationResponse.builder()
                                         .status(SecurityStatus.ABSTAIN)
                                         .description(description)
                                         .build();
        }
        if (null == code) {
            String headerValue = "Bearer realm=\"" + tenantConfig.realm() + "\"";
            return AuthenticationResponse.builder()
                                         .status(SecurityStatus.FAILURE)
                                         .statusCode(UNAUTHORIZED_401.code())
                                         .responseHeader(WWW_AUTHENTICATE.defaultCase(), headerValue)
                                         .description(description)
                                         .build();
        }
        return AuthenticationResponse.builder()
                                     .status(SecurityStatus.FAILURE)
                                     .statusCode(status.code())
                                     .responseHeader(WWW_AUTHENTICATE.defaultCase(), errorHeader(code, description))
                                     .description(description)
                                     .build();
    }

    private int redirectAttempt(String state) {
        if (state.contains("?")) {
            // there are parameters
            Matcher matcher = attemptPattern.matcher(state);
            if (matcher.matches()) {
                return Integer.parseInt(matcher.group(1));
            }
        }
        return 1;
    }

    private String errorHeader(String code, String description) {
        return "Bearer realm=\"" + tenantConfig.realm() + "\", error=\"" + code + "\", error_description=\"" + description + "\"";
    }

    private String origUri(ProviderRequest providerRequest) {
        List<String> origUri = providerRequest.env()
                                              .headers()
                                              .getOrDefault(Security.HEADER_ORIG_URI, List.of());

        if (origUri.isEmpty()) {
            origUri = List.of(providerRequest.env().targetUri().getPath());
        }

        return origUri.get(0);
    }

    private String encode(String state) {
        return URLEncoder.encode(state, StandardCharsets.UTF_8);
    }

    private AuthenticationResponse validateToken(String tenantId, ProviderRequest providerRequest, String token) {
        SignedJwt signedJwt;
        try {
            signedJwt = SignedJwt.parseToken(token);
        } catch (Exception e) {
            LOGGER.log(Level.DEBUG, "Could not parse inbound token", e);
            return AuthenticationResponse.failed("Invalid token", e);
        }

        try {
            if (tenantConfig.validateJwtWithJwk()) {
                validateJwtWithJwk(signedJwt);
            } else {
                validateJwtWithPost(signedJwt);
            }
            return processValidationResult(providerRequest, signedJwt, tenantId, collector);
        } catch (Throwable ex) {
            LOGGER.log(Level.DEBUG, "Failed to validate request", ex);
            return AuthenticationResponse.failed("Failed to validate JWT", ex);
        }
    }

    private AuthenticationResponse processValidationResult(ProviderRequest providerRequest,
                                                           SignedJwt signedJwt,
                                                           String tenantId,
                                                           Errors.Collector collector) {
        Jwt jwt = signedJwt.getJwt();
        Errors errors = collector.collect();
        Errors validationErrors = jwt.validate(tenant.issuer(), tenantConfig.audience());

        if (errors.isValid() && validationErrors.isValid()) {

            errors.log(LOGGER);
            Subject subject = buildSubject(jwt, signedJwt);

            Set<String> scopes = subject.grantsByType("scope")
                                        .stream()
                                        .map(Grant::getName)
                                        .collect(Collectors.toSet());

            // make sure we have the correct scopes
            Set<String> expectedScopes = expectedScopes(providerRequest);
            List<String> missingScopes = new LinkedList<>();
            for (String expectedScope : expectedScopes) {
                if (!scopes.contains(expectedScope)) {
                    missingScopes.add(expectedScope);
                }
            }

            if (missingScopes.isEmpty()) {
                return AuthenticationResponse.success(subject);
            } else {
                return errorResponse(providerRequest,
                        Http.Status.FORBIDDEN_403,
                        "insufficient_scope",
                        "Scopes " + missingScopes + " are missing",
                        tenantId);
            }
        } else {
            if (LOGGER.isLoggable(Level.DEBUG)) {
                // only log errors when details requested
                errors.log(LOGGER);
                validationErrors.log(LOGGER);
            }
            return errorResponse(providerRequest,
                    UNAUTHORIZED_401,
                    "invalid_token",
                    "Token not valid",
                    tenantId);
        }
    }

    private Subject buildSubject(Jwt jwt, SignedJwt signedJwt) {
        Principal principal = buildPrincipal(jwt);

        TokenCredential.Builder builder = TokenCredential.builder();
        jwt.issueTime().ifPresent(builder::issueTime);
        jwt.expirationTime().ifPresent(builder::expTime);
        jwt.issuer().ifPresent(builder::issuer);
        builder.token(signedJwt.tokenContent());
        builder.addToken(Jwt.class, jwt);
        builder.addToken(SignedJwt.class, signedJwt);

        Subject.Builder subjectBuilder = Subject.builder()
                                                .principal(principal)
                                                .addPublicCredential(TokenCredential.class, builder.build());

        if (useJwtGroups) {
            Optional<List<String>> userGroups = jwt.userGroups();
            userGroups.ifPresent(groups -> groups.forEach(group -> subjectBuilder.addGrant(Role.create(group))));
        }

        Optional<List<String>> scopes = jwt.scopes();
        scopes.ifPresent(scopeList -> scopeList.forEach(scope -> subjectBuilder.addGrant(Grant.builder()
                                                                                              .name(scope)
                                                                                              .type("scope")
                                                                                              .build())));

        return subjectBuilder.build();

    }

    private Principal buildPrincipal(Jwt jwt) {
        String subject = jwt.subject()
                            .orElseThrow(() -> new JwtException("JWT does not contain subject claim, cannot create principal."));

        String name = jwt.preferredUsername()
                         .orElse(subject);

        Principal.Builder builder = Principal.builder()
                                             .name(name)
                                             .id(subject);

        jwt.payloadClaims().forEach((key, jsonValue) -> builder.addAttribute(key, JwtUtil.toObject(jsonValue)));
        jwt.email().ifPresent(value -> builder.addAttribute("email", value));
        jwt.emailVerified().ifPresent(value -> builder.addAttribute("email_verified", value));
        jwt.locale().ifPresent(value -> builder.addAttribute("locale", value));
        jwt.familyName().ifPresent(value -> builder.addAttribute("family_name", value));
        jwt.givenName().ifPresent(value -> builder.addAttribute("given_name", value));
        jwt.fullName().ifPresent(value -> builder.addAttribute("full_name", value));

        return builder.build();
    }
}
