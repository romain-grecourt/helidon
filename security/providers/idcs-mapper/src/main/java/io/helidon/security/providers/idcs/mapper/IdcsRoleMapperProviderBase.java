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
package io.helidon.security.providers.idcs.mapper;

import java.lang.System.Logger.Level;
import java.net.URI;
import java.time.Duration;
import java.time.Instant;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;

import io.helidon.common.context.Context;
import io.helidon.common.context.Contexts;
import io.helidon.common.http.Http;
import io.helidon.common.http.HttpMediaType;
import io.helidon.common.parameters.Parameters;
import io.helidon.config.Config;
import io.helidon.config.metadata.Configured;
import io.helidon.config.metadata.ConfiguredOption;
import io.helidon.nima.webclient.http1.Http1Client;
import io.helidon.nima.webclient.http1.Http1ClientRequest;
import io.helidon.security.AuthenticationResponse;
import io.helidon.security.Grant;
import io.helidon.security.ProviderRequest;
import io.helidon.security.Role;
import io.helidon.security.Subject;
import io.helidon.security.SubjectType;
import io.helidon.security.integration.common.RoleMapTracing;
import io.helidon.security.jwt.Jwt;
import io.helidon.security.jwt.SignedJwt;
import io.helidon.security.jwt.Validator;
import io.helidon.security.providers.oidc.common.OidcConfig;
import io.helidon.security.providers.oidc.common.OidcConfig.OidcResponseException;
import io.helidon.security.spi.SubjectMappingProvider;

import jakarta.json.JsonArray;
import jakarta.json.JsonObject;

import static io.helidon.security.providers.oidc.common.OidcConfig.postJsonResponse;

/**
 * Common functionality for IDCS role mapping using {@link Http1Client}.
 */
public abstract class IdcsRoleMapperProviderBase implements SubjectMappingProvider {
    /**
     * User subject type used when requesting roles from IDCS.
     * An attempt is made to obtain it from JWT claim {@code sub_type}. If not defined,
     * default is used as configured in {@link IdcsRoleMapperProviderBase.Builder}.
     */
    public static final String IDCS_SUBJECT_TYPE_USER = "user";
    /**
     * Client subject type used when requesting roles from IDCS.
     * An attempt is made to obtain it from JWT claim {@code sub_type}. If not defined,
     * default is used as configured in {@link IdcsRoleMapperProviderBase.Builder}.
     */
    public static final String IDCS_SUBJECT_TYPE_CLIENT = "client";
    /**
     * Json key for group roles to be retrieved from IDCS response.
     */
    protected static final String ROLE_GROUP = "groups";
    /**
     * Json key for app roles to be retrieved from IDCS response.
     */
    protected static final String ROLE_APPROLE = "appRoles";
    /**
     * Json key for token to be retrieved from IDCS response when requesting application token.
     */
    protected static final String ACCESS_TOKEN_KEY = "access_token";
    /**
     * Property sent with JAX-RS requests to override parent span context in outbound calls.
     * We cannot use the constant declared in {@code ClientTracingFilter}, as it is not a required dependency.
     */
    protected static final String PARENT_CONTEXT_CLIENT_PROPERTY = "io.helidon.tracing.span-context";
    private static final System.Logger LOGGER = System.getLogger(IdcsRoleMapperProviderBase.class.getName());

    private final Set<SubjectType> supportedTypes = EnumSet.noneOf(SubjectType.class);
    private final OidcConfig oidcConfig;
    private final String defaultIdcsSubjectType;

    /**
     * Configures the needed fields from the provided builder.
     *
     * @param builder builder with oidcConfig and other needed fields.
     */
    protected IdcsRoleMapperProviderBase(Builder<?> builder) {
        this.oidcConfig = builder.oidcConfig;
        this.oidcConfig.tokenEndpointUri(); //Remove once IDCS is rewritten to be lazily loaded
        this.defaultIdcsSubjectType = builder.defaultIdcsSubjectType;
        if (builder.supportedTypes.isEmpty()) {
            this.supportedTypes.add(SubjectType.USER);
        } else {
            this.supportedTypes.addAll(builder.supportedTypes);
        }
    }

    @Override
    public AuthenticationResponse map(ProviderRequest request, AuthenticationResponse previous) {

        Optional<Subject> maybeUser = previous.user();
        Optional<Subject> maybeService = previous.service();
        if (maybeService.isEmpty() && maybeUser.isEmpty()) {
            return previous;
        }

        // create a new response
        AuthenticationResponse.Builder builder = AuthenticationResponse.builder()
                                                                       .requestHeaders(previous.requestHeaders());
        previous.description().ifPresent(desc -> builder.description(desc));

        if (maybeUser.isPresent()) {
            if (supportedTypes.contains(SubjectType.USER)) {
                // service will be done after use
                builder.user(enhance(request, previous, maybeUser.get()));
            } else {
                builder.service(maybeUser.get());
            }
        }

        if (maybeService.isPresent()) {
            if (supportedTypes.contains(SubjectType.SERVICE)) {
                builder.user(enhance(request, previous, maybeService.get()));
            } else {
                builder.service(maybeService.get());
            }
        }
        return builder.build();
    }

    /**
     * Enhance subject with IDCS roles.
     *
     * @param request  provider request
     * @param previous previous authenticated response
     * @param subject  subject to enhance
     * @return enhanced subject
     */
    protected abstract Subject enhance(ProviderRequest request, AuthenticationResponse previous, Subject subject);

    /**
     * Updates original subject with the list of grants.
     *
     * @param originalSubject as was created by authentication provider
     * @param grants          grants added by this role mapper
     * @return new subject
     */
    protected Subject buildSubject(Subject originalSubject, List<? extends Grant> grants) {
        Subject.Builder builder = Subject.builder();
        builder.update(originalSubject);

        grants.forEach(builder::addGrant);

        return builder.build();
    }

    protected List<? extends Grant> processRoleRequest(Http1ClientRequest request, Object entity, String subject) {
        try {
            JsonObject json = postJsonResponse(request, entity);
            return processServerResponse(json, subject);
        } catch (Throwable th) {
            if (th instanceof OidcResponseException rex) {
                if (rex.status() == Http.Status.UNAUTHORIZED_401) {
                    LOGGER.log(Level.WARNING,
                            "Cannot read groups for user \"{0}\"." +
                                    " Response code: {1}," +
                                    " make sure your IDCS client has role \"Authenticator Client\"" +
                                    " added on the client configuration page," +
                                    " error entity: {2}"
                            , subject, rex.status(), rex.entity());
                } else {
                    LOGGER.log(Level.WARNING,
                            "Cannot read groups for user \"{0}\". Response code: {1}, error entity: {2}"
                            , subject, rex.status(), rex.entity());
                }
            } else {
                LOGGER.log(Level.WARNING,
                        "Cannot read groups for user \"{0} \". Error message: {1}",
                        subject, th.getMessage());
            }
            return List.of();
        }
    }

    /**
     * Access to {@link io.helidon.security.providers.oidc.common.OidcConfig} so the field is not duplicated by
     * classes that extend this provider.
     *
     * @return open ID Connect configuration (also used to configure access to IDCS)
     */
    protected OidcConfig oidcConfig() {
        return oidcConfig;
    }

    /**
     * Default subject type to use when requesting data from IDCS.
     *
     * @return configured default subject type or {@link #IDCS_SUBJECT_TYPE_USER}
     */
    protected String defaultIdcsSubjectType() {
        return defaultIdcsSubjectType;
    }

    private List<? extends Grant> processServerResponse(JsonObject jsonObject, String subjectName) {
        JsonArray groups = jsonObject.getJsonArray("groups");
        JsonArray appRoles = jsonObject.getJsonArray("appRoles");

        if ((null == groups) && (null == appRoles)) {
            LOGGER.log(Level.TRACE, () -> "Neither groups nor app roles found for user " + subjectName);
            return List.of();
        }

        List<Role> result = new LinkedList<>();
        for (String type : Arrays.asList(ROLE_GROUP, ROLE_APPROLE)) {
            JsonArray types = jsonObject.getJsonArray(type);
            if (null != types) {
                for (int i = 0; i < types.size(); i++) {
                    JsonObject typeJson = types.getJsonObject(i);
                    String name = typeJson.getString("display");
                    String id = typeJson.getString("value");
                    String ref = typeJson.getString("$ref");

                    Role role = Role.builder()
                                    .name(name)
                                    .addAttribute("type", type)
                                    .addAttribute("id", id)
                                    .addAttribute("ref", ref)
                                    .build();

                    result.add(role);
                }
            }
        }
        return result;
    }

    /**
     * Fluent API builder for {@link IdcsRoleMapperProviderBase}.
     *
     * @param <B> Type of the extending builder
     */
    @SuppressWarnings({"unused", "UnusedReturnValue"})
    @Configured
    public static class Builder<B extends Builder<B>> {

        private final Set<SubjectType> supportedTypes = EnumSet.noneOf(SubjectType.class);
        @SuppressWarnings("unchecked")
        private final B me = (B) this;
        private String defaultIdcsSubjectType = IDCS_SUBJECT_TYPE_USER;
        private OidcConfig oidcConfig;

        /**
         * Default constructor.
         */
        protected Builder() {
        }

        /**
         * Update this builder state from configuration.
         * Expects:
         * <ul>
         * <li>oidc-config to load an instance of {@link io.helidon.security.providers.oidc.common.OidcConfig}</li>
         * <li>cache-config (optional) to load instances of {@link io.helidon.security.providers.common.EvictableCache} for
         * caching</li>
         * <li>default-idcs-subject-type to use when not defined in a JWT, either {@value #IDCS_SUBJECT_TYPE_USER} or
         *      {@link #IDCS_SUBJECT_TYPE_CLIENT}, defaults to {@value #IDCS_SUBJECT_TYPE_USER}</li>
         * </ul>
         *
         * @param config current node must have "oidc-config" as one of its children
         * @return updated builder instance
         */
        public B config(Config config) {
            config.get("oidc-config").ifExists(it -> {
                OidcConfig.Builder builder = OidcConfig.builder();
                // we do not need JWT validation at all
                builder.validateJwtWithJwk(false);
                // this is an IDCS specific extension
                builder.serverType("idcs");
                builder.config(it);

                oidcConfig(builder.build());
            });

            config.get("subject-types")
                  .asList(String.class)
                  .ifPresent(list -> list.forEach(s -> addSubjectType(SubjectType.valueOf(s))));

            config.get("default-idcs-subject-type").asString().ifPresent(this::defaultIdcsSubjectType);
            return me;
        }

        /**
         * Use explicit {@link io.helidon.security.providers.oidc.common.OidcConfig} instance, e.g. when using it also for OIDC
         * provider.
         *
         * @param config oidc specific configuration, must have at least identity endpoint and client credentials configured
         * @return updated builder instance
         */
        @ConfiguredOption
        public B oidcConfig(OidcConfig config) {
            this.oidcConfig = config;
            return me;
        }

        /**
         * Get the configuration to access IDCS instance.
         *
         * @return oidc config
         */
        protected OidcConfig oidcConfig() {
            return oidcConfig;
        }

        /**
         * Configure supported subject types.
         * By default {@link io.helidon.security.SubjectType#USER} is used if none configured.
         *
         * @param types types to configure as supported for mapping
         * @return updated builder instance
         */
        public B subjectTypes(SubjectType... types) {
            this.supportedTypes.clear();
            this.supportedTypes.addAll(Arrays.asList(types));
            return me;
        }

        /**
         * Configure subject type to use when requesting roles from IDCS.
         * Can be either {@link #IDCS_SUBJECT_TYPE_USER} or {@link #IDCS_SUBJECT_TYPE_CLIENT}.
         * Defaults to {@link #IDCS_SUBJECT_TYPE_USER}.
         *
         * @param subjectType type of subject to use when requesting roles from IDCS
         * @return updated builder instance
         */
        @ConfiguredOption(IDCS_SUBJECT_TYPE_USER)
        public B defaultIdcsSubjectType(String subjectType) {
            this.defaultIdcsSubjectType = subjectType;
            return me;
        }

        /**
         * Add a supported subject type.
         * If none added, {@link io.helidon.security.SubjectType#USER} is used.
         * If any added, only the ones added will be used (e.g. if you want to use
         * both {@link io.helidon.security.SubjectType#USER} and {@link io.helidon.security.SubjectType#SERVICE},
         * both need to be added.
         *
         * @param type subject type to add to the list of supported types
         * @return updated builder instance
         */
        @ConfiguredOption(key = "subject-types", kind = ConfiguredOption.Kind.LIST, value = "USER")
        public B addSubjectType(SubjectType type) {
            this.supportedTypes.add(type);
            return me;
        }
    }

    /**
     * Token for app access to IDCS.
     */
    protected static class AppToken {
        private static final List<Validator<Jwt>> TIME_VALIDATORS = Jwt.defaultTimeValidators();

        private final AtomicReference<AppTokenData> token = new AtomicReference<>();
        private final Http1Client webClient;
        private final URI tokenEndpointUri;
        private final Duration tokenRefreshSkew;

        protected AppToken(Http1Client webClient, URI tokenEndpointUri, Duration tokenRefreshSkew) {
            this.webClient = webClient;
            this.tokenEndpointUri = tokenEndpointUri;
            this.tokenRefreshSkew = tokenRefreshSkew;
        }

        protected Optional<String> getToken(RoleMapTracing tracing) {
            final AppTokenData currentTokenData = token.get();
            if (currentTokenData == null) {
                AppTokenData tokenData = fromServer(tracing);
                if (token.compareAndSet(null, tokenData)) {
                    return tokenData.maybeTokenContent();
                }
                // another thread "stole" the data, return its content
                return token.get().maybeTokenContent();
            }
            // there is an existing value
            Jwt jwt = currentTokenData.appJwt;
            if (jwt == null || !jwt.validate(TIME_VALIDATORS).isValid() || isNearExpiration(jwt)) {
                // it is not valid or is very close to expiration - we must get a new value
                AppTokenData tokenData = fromServer(tracing);
                if (token.compareAndSet(currentTokenData, tokenData)) {
                    return tokenData.maybeTokenContent();
                }
                return token.get().maybeTokenContent();
            } else {
                // present and valid
                return currentTokenData.maybeTokenContent();
            }
        }

        private boolean isNearExpiration(Jwt jwt) {
            return jwt.expirationTime()
                      .map(exp -> exp.minus(tokenRefreshSkew).isBefore(Instant.now()))
                      .orElse(false);
        }

        private AppTokenData fromServer(RoleMapTracing tracing) {
            Parameters params = Parameters.builder("idcs-form-params")
                                          .add("grant_type", "client_credentials")
                                          .add("scope", "urn:opc:idm:__myscopes__")
                                          .build();

            // use current span context as a parent for client outbound
            // using a custom child context, so we do not replace the parent in the current context
            Context parentContext = Contexts.context().orElseGet(Contexts::globalContext);
            Context childContext = Context.builder()
                                          .parent(parentContext)
                                          .build();

            tracing.findParent().ifPresent(childContext::register);

            return Contexts.runInContext(childContext, () -> {

                Http1ClientRequest request = webClient.post()
                                                      .uri(tokenEndpointUri)
                                                      .accept(HttpMediaType.APPLICATION_JSON);

                JsonObject json = postJsonResponse(request, params);
                String accessToken = json.getString(ACCESS_TOKEN_KEY);
                LOGGER.log(Level.TRACE, () -> "Access token: " + accessToken);
                SignedJwt signedJwt = SignedJwt.parseToken(accessToken);
                try {
                    return new AppTokenData(accessToken, signedJwt.getJwt());
                } catch (OidcResponseException ex) {
                    LOGGER.log(Level.ERROR,
                            "Failed to obtain access token for application to read groups from IDCS." +
                                    " Status: {0}, error message: {1}", ex.status(), ex.entity());
                    return new AppTokenData(null, null);
                } catch (Throwable ex) {
                    LOGGER.log(Level.ERROR, "Failed to obtain access token for application to read groups from IDCS." +
                            " Failed with exception: " + ex.getMessage(), ex);
                    return new AppTokenData(null, null);
                }
            });
        }
    }

    private record AppTokenData(String tokenContent, Jwt appJwt) {

        Optional<String> maybeTokenContent() {
            return Optional.ofNullable(tokenContent);
        }
    }
}
