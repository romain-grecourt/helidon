/*
 * Copyright (c) 2021, 2022 Oracle and/or its affiliates.
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
package io.helidon.integrations.micrometer;

import java.util.function.Supplier;

import io.helidon.common.http.Http;
import io.helidon.common.http.HttpMediaType;
import io.helidon.config.Config;
import io.helidon.config.ConfigSources;
import io.helidon.integrations.micrometer.MeterRegistryFactory.BuiltInRegistryType;
import io.helidon.nima.webclient.http1.Http1Client;
import io.helidon.nima.webclient.http1.Http1ClientResponse;
import io.helidon.nima.webserver.WebServer;
import io.helidon.nima.webserver.http.HttpRouting;

import org.hamcrest.MatcherAssert;
import org.junit.jupiter.api.Test;

import static org.hamcrest.Matchers.is;

public class MicrometerEndpointTests {

    private static final Config OVERALL_TEST_CONFIG = Config.create(ConfigSources.classpath("/micrometerTestData.json"));


    @Test
    public void testDefaultEndpoint() {
        runTest(MicrometerFeature.DEFAULT_CONTEXT, MicrometerFeature::create);
    }

    @Test
    public void testExplicitEndpointWithDefaultBuiltInRegistryViaConfig() {
        String context = "/aa";
        runTest(context, () -> MicrometerFeature.builder()
                                                .config(OVERALL_TEST_CONFIG.get("explicitContext").get("metrics.micrometer"))
                                                .build());
    }

    @Test
    public void testExplicitEndpointWithExplicitBuiltInRegistryViaBuilder() {
        String context = "/bb";
        runTest(context, () ->
                MicrometerFeature.builder()
                                 .meterRegistryFactory(MeterRegistryFactory.builder()
                                                                           .enrollBuiltInRegistry(BuiltInRegistryType.PROMETHEUS)
                                                                           .build())
                                 .webContext(context)
                                 .build());
    }

    @Test
    public void testExplicitEndpointWithExplicitBuiltInRegistryViaConfig() {
        String context = "/cc";
        runTest(context, () ->
                MicrometerFeature.builder()
                                 .config(OVERALL_TEST_CONFIG.get("explicitContextWithExplicitBuiltIn")
                                                            .get("metrics.micrometer"))
                                 .build());
    }

    private static void runTest(String contextForRequest, Supplier<MicrometerFeature> supplier) {

        WebServer webServer = null;

        try {
            webServer = WebServer.builder()
                                 .host("localhost")
                                 .port(-1)
                                 .routing(routing -> prepareRouting(routing, supplier))
                                 .start();

            Http1ClientResponse webClientResponse =
                    Http1Client.builder()
                               .baseUri(String.format("http://localhost:%d%s", webServer.port(), contextForRequest))
                               .build()
                               .get()
                               .accept(HttpMediaType.TEXT_PLAIN)
                               .request();

            MatcherAssert.assertThat(webClientResponse.status(), is(Http.Status.OK_200));
        } finally {
            if (webServer != null) {
                webServer.stop();
            }
        }
    }

    private static void prepareRouting(HttpRouting.Builder routing, Supplier<MicrometerFeature> supplier) {
        supplier.get().setup(routing);
    }

}
