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

package io.helidon.examples.metrics.kpi;

import io.helidon.config.Config;
import io.helidon.logging.common.LogConfig;
import io.helidon.metrics.api.KeyPerformanceIndicatorMetricsSettings;
import io.helidon.metrics.api.MetricsSettings;
import io.helidon.nima.observe.metrics.MetricsFeature;
import io.helidon.nima.webserver.WebServer;
import io.helidon.nima.webserver.http.HttpRouting;

/**
 * The application main class.
 */
public final class Main {

    static final String USE_CONFIG_PROPERTY_NAME = "useConfig";

    static final boolean USE_CONFIG = Boolean.getBoolean(USE_CONFIG_PROPERTY_NAME);

    /**
     * Cannot be instantiated.
     */
    private Main() {
    }

    /**
     * Application main entry point.
     *
     * @param args command line arguments.
     */
    public static void main(final String[] args) {
        WebServer.Builder builder = WebServer.builder();
        setup(builder);
        WebServer server = builder.start();
        System.out.println("WEB server is up! http://localhost:" + server.port() + "/greet");
    }

    /**
     * Setup the server.
     *
     * @param server server builder
     */
    static void setup(WebServer.Builder server) {
        // load logging configuration
        LogConfig.configureRuntime();

        // By default this will pick up application.yaml from the classpath
        Config config = Config.create();

        server.routing(r -> routing(r, config))
              .config(config.get("server"));

    }

    /**
     * Setup routing.
     *
     * @param routing routing builder
     * @param config  configuration of this server
     */
    private static void routing(HttpRouting.Builder routing, Config config) {
        /*
         * For purposes of illustration, the key performance indicator settings for the
         * MetricsSupport instance are set up according to a system property so you can see,
         * in one example, how to code each approach. Normally, you would choose one
         * approach to use in an application.
         */
        MetricsFeature metricsSupport = USE_CONFIG
                ? metricsSupportWithConfig(config.get("metrics"))
                : metricsSupportWithoutConfig();

        GreetService greetService = new GreetService(config);

        routing.addFeature(metricsSupport)
               .register("/greet", greetService);
    }

    /**
     * Creates a {@link MetricsFeature} instance using a "metrics" configuration node.
     *
     * @param metricsConfig {@link Config} node with key "metrics" if present; an empty node otherwise
     * @return {@code MetricsSupport} object with metrics (including KPI) set up using the config node
     */
    private static MetricsFeature metricsSupportWithConfig(Config metricsConfig) {
        return MetricsFeature.create(metricsConfig);
    }

    /**
     * Creates a {@link MetricsFeature} instance explicitly turning on extended KPI metrics.
     *
     * @return {@code MetricsSupport} object with extended KPI metrics enabled
     */
    private static MetricsFeature metricsSupportWithoutConfig() {

        KeyPerformanceIndicatorMetricsSettings.Builder settingsBuilder =
                KeyPerformanceIndicatorMetricsSettings.builder()
                        .extended(true)
                        .longRunningRequestThresholdMs(2000);
        return MetricsFeature.builder()
                .metricsSettings(MetricsSettings.builder()
                                         .keyPerformanceIndicatorSettings(settingsBuilder))
                .build();
    }
}
