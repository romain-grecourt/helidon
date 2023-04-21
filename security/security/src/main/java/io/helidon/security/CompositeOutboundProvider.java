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

import java.lang.annotation.Annotation;
import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import io.helidon.security.spi.OutboundSecurityProvider;

/**
 * A security outbound provider building a result from one or more outbound providers.
 */
final class CompositeOutboundProvider implements OutboundSecurityProvider {
    private final List<OutboundSecurityProvider> providers = new LinkedList<>();

    CompositeOutboundProvider(List<OutboundSecurityProvider> providers) {
        this.providers.addAll(providers);
    }

    @Override
    public boolean isOutboundSupported(ProviderRequest providerRequest,
                                       SecurityEnvironment outboundEnv,
                                       EndpointConfig outboundConfig) {
        return providers.stream()
                .anyMatch(provider -> provider.isOutboundSupported(providerRequest, outboundEnv, outboundConfig));
    }

    @Override
    public Collection<Class<? extends Annotation>> supportedAnnotations() {
        Set<Class<? extends Annotation>> result = new HashSet<>();
        providers.forEach(provider -> result.addAll(provider.supportedAnnotations()));
        return result;
    }

    @Override
    public OutboundSecurityResponse outboundSecurity(ProviderRequest providerRequest,
                                                                        SecurityEnvironment outboundEnv,
                                                                        EndpointConfig outboundConfig) {

        OutboundCall call = new OutboundCall(OutboundSecurityResponse.abstain(), providerRequest,
                outboundEnv, outboundConfig);

        for (OutboundSecurityProvider provider : providers) {
            if (call.response.status() == SecurityResponse.SecurityStatus.ABSTAIN) {
                // previous call(s) did not care, I don't have to update request
                if (provider.isOutboundSupported(call.inboundContext, call.outboundEnv, call.outboundConfig)) {
                    OutboundSecurityResponse outboundSecurityResponse = provider.outboundSecurity(call.inboundContext, call.outboundEnv, call.outboundConfig);
                    SecurityEnvironment nextEnv = updateRequestHeaders(call.outboundEnv, outboundSecurityResponse);
                    call = new OutboundCall(outboundSecurityResponse, call.inboundContext, nextEnv, call.outboundConfig);
                    continue;
                }

                // continue with existing result
                continue;
            }
            // construct a new request
            if (call.response.status().isSuccess()) {
                // invoke current
                OutboundSecurityResponse thisResponse = provider.outboundSecurity(call.inboundContext, call.outboundEnv, call.outboundConfig);
                OutboundSecurityResponse prevResponse = call.response;

                // combine
                OutboundSecurityResponse.Builder builder = OutboundSecurityResponse.builder();
                prevResponse.requestHeaders().forEach(builder::requestHeader);
                prevResponse.responseHeaders().forEach(builder::responseHeader);
                thisResponse.requestHeaders().forEach(builder::requestHeader);
                thisResponse.responseHeaders().forEach(builder::responseHeader);
                SecurityEnvironment nextEnv = updateRequestHeaders(call.outboundEnv, thisResponse);

                builder.status(thisResponse.status());
                call = new OutboundCall(builder.build(), call.inboundContext, nextEnv, call.outboundConfig);
            }
        }

        return call.response;
    }

    private SecurityEnvironment updateRequestHeaders(SecurityEnvironment env, OutboundSecurityResponse response) {
        SecurityEnvironment.Builder builder = env.derive();

        response.requestHeaders().forEach(builder::header);

        return builder.build();
    }

    private record OutboundCall(OutboundSecurityResponse response,
                                ProviderRequest inboundContext,
                                SecurityEnvironment outboundEnv,
                                EndpointConfig outboundConfig) {
    }
}
