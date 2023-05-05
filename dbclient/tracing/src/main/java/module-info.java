/*
 * Copyright (c) 2019, 2022 Oracle and/or its affiliates.
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

import io.helidon.common.features.api.Feature;
import io.helidon.common.features.api.HelidonFlavor;
import io.helidon.dbclient.spi.DbClientServiceProvider;
import io.helidon.dbclient.tracing.DbClientTracingProvider;

/**
 * Helidon DB Client Tracing.
 */
@Feature(value = "Tracing",
        description = "DB Client tracing support",
        in = HelidonFlavor.SE,
        path = {"DbClient", "Tracing"}
)
module io.helidon.dbclient.tracing {
    requires static io.helidon.common.features.api;

    requires java.logging;

    requires io.helidon.dbclient;
    requires io.helidon.tracing.config;

    requires io.opentracing.api;
    requires io.opentracing.util;

    exports io.helidon.dbclient.tracing;

    provides DbClientServiceProvider with DbClientTracingProvider;
}
