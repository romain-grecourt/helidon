/*
 * Copyright (c) 2018, 2021 Oracle and/or its affiliates.
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

package io.helidon.security;

import io.helidon.security.spi.AuthorizationProvider;

import static io.helidon.security.AuditEvent.AuditParam.plain;
import static io.helidon.security.internal.SecurityAuditEvent.error;
import static io.helidon.security.internal.SecurityAuditEvent.failure;
import static io.helidon.security.internal.SecurityAuditEvent.success;

/**
 * Authorizer.
 */
final class AuthorizationClientImpl implements SecurityClient<AuthorizationResponse> {
    private static final AuthorizationProvider DEFAULT_PROVIDER = new DefaultAtzProvider();
    private static final String AUDIT_SUCCESS_FMT = "Path %s. Provider %s. Subject %s";
    private static final String AUDIT_ERROR_FMT =  "Path %s. Provider %s, Description %s, Request %s. Subject %s. %s: %s";
    private static final String AUDIT_FAILURE_FMT = "Path %s. Provider %s, Description %s, Request %s. Subject %s";

    private final Security security;
    private final SecurityContextImpl context;
    private final SecurityRequest request;
    private final String providerName;
    private final ProviderRequest providerRequest;

    AuthorizationClientImpl(Security security,
                            SecurityContextImpl context,
                            SecurityRequest request,
                            String providerName) {
        this.security = security;
        this.context = context;
        this.request = request;
        this.providerName = providerName;
        this.providerRequest = new ProviderRequest(context, request.resources());
    }

    @Override
    public AuthorizationResponse submit() {
        // TODO ABAC - if annotated with Attribute meta annot, make sure that all are processed

        AuthorizationProvider provider = security.resolveAtzProvider(providerName).orElse(DEFAULT_PROVIDER);

        try {
            AuthorizationResponse response = provider.authorize(providerRequest);

            if (response.status().isSuccess()) {
                context.audit(success(AuditEvent.AUTHZ_TYPE_PREFIX + ".authorize", AUDIT_SUCCESS_FMT)
                        .addParam(plain("path", providerRequest.env().path()))
                        .addParam(plain("provider", provider.getClass().getName()))
                        .addParam(plain("subject", context.user())));
            } else {
                context.audit(failure(AuditEvent.AUTHZ_TYPE_PREFIX + ".authorize", AUDIT_FAILURE_FMT)
                        .addParam(plain("path", providerRequest.env().path()))
                        .addParam(plain("provider", provider.getClass().getName()))
                        .addParam(plain("request", this))
                        .addParam(plain("subject", context.user()))
                        .addParam(plain("message", response.description().orElse(null)))
                        .addParam(plain("exception", response.throwable().orElse(null))));
            }

            return response;
        } catch (Throwable throwable) {
            context.audit(error(AuditEvent.AUTHZ_TYPE_PREFIX + ".authorize", AUDIT_ERROR_FMT)
                    .addParam(plain("path", providerRequest.env().path()))
                    .addParam(plain("provider", provider.getClass().getName()))
                    .addParam(plain("description", "Audit failure"))
                    .addParam(plain("request", this))
                    .addParam(plain("subject", context.user()))
                    .addParam(plain("message", throwable.getMessage()))
                    .addParam(plain("exception", throwable)));
            throw new SecurityException(throwable);
        }
    }

    private static class DefaultAtzProvider implements AuthorizationProvider {
        @Override
        public AuthorizationResponse authorize(ProviderRequest context) {
            return AuthorizationResponse.permit();
        }
    }
}
