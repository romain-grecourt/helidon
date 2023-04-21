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

import io.helidon.security.spi.OutboundSecurityProvider;

import static io.helidon.security.AuditEvent.AuditParam.plain;
import static io.helidon.security.internal.SecurityAuditEvent.error;
import static io.helidon.security.internal.SecurityAuditEvent.failure;
import static io.helidon.security.internal.SecurityAuditEvent.success;

/**
 * Outbound security builder and executor.
 * <br/>
 * <p>
 * See {@link #submit()}.
 */
final class OutboundSecurityClientImpl implements SecurityClient<OutboundSecurityResponse> {

    private static final String AUDIT_SUCCESS_FMT = "Provider %s. Request %s. Subject %s";
    private static final String AUDIT_ERROR_FMT = "Provider %s, Description %s, Request %s. Subject %s";
    private static final String AUDIT_FAILURE_FMT = "Provider %s, Description %s, Request %s. Subject %s";

    private final Security security;
    private final SecurityContextImpl context;
    private final String providerName;
    private final ProviderRequest providerRequest;
    private final SecurityEnvironment env;
    private final EndpointConfig config;

    OutboundSecurityClientImpl(Security security,
                               SecurityContextImpl context,
                               SecurityRequest request,
                               String providerName,
                               SecurityEnvironment env,
                               EndpointConfig config) {

        this.security = security;
        this.context = context;
        this.providerName = providerName;
        this.providerRequest = new ProviderRequest(context, request.resources());
        this.env = env;
        this.config = config;
    }

    @Override
    public OutboundSecurityResponse submit() {
        OutboundSecurityProvider provider = findProvider();

        if (null == provider) {
            return OutboundSecurityResponse.empty();
        }

        try {
            OutboundSecurityResponse response = provider.outboundSecurity(providerRequest, env, config);
            if (response.status().isSuccess()) {
                //Audit success
                context.audit(success(AuditEvent.OUTBOUND_TYPE_PREFIX + ".outbound", AUDIT_SUCCESS_FMT)
                        .addParam(plain("provider", provider.getClass().getName()))
                        .addParam(plain("request", this))
                        .addParam(plain("subject", context.user().orElse(SecurityContext.ANONYMOUS))));
            } else {
                context.audit(failure(AuditEvent.OUTBOUND_TYPE_PREFIX + ".outbound", AUDIT_FAILURE_FMT)
                        .addParam(plain("provider", provider.getClass().getName()))
                        .addParam(plain("request", this))
                        .addParam(plain("message", response.description().orElse(null)))
                        .addParam(plain("exception", response.throwable().orElse(null)))
                        .addParam(plain("subject", context.user().orElse(SecurityContext.ANONYMOUS))));
            }
            return response;
        } catch (Throwable e) {
            context.audit(error(AuditEvent.OUTBOUND_TYPE_PREFIX + ".outbound", AUDIT_ERROR_FMT)
                    .addParam(plain("provider", provider.getClass().getName()))
                    .addParam(plain("request", this))
                    .addParam(plain("message", e.getMessage()))
                    .addParam(plain("exception", e))
                    .addParam(plain("subject", context.user().orElse(SecurityContext.ANONYMOUS))));
            throw new SecurityException("Failed to process security", e);
        }
    }

    private OutboundSecurityProvider findProvider() {
        return security.resolveOutboundProvider(providerName)
                       .stream()
                       .filter(p -> p.isOutboundSupported(providerRequest, env, config))
                       .findFirst()
                       .orElse(null);
    }
}
