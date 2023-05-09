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
import io.helidon.common.features.api.Preview;
import io.helidon.dbclient.DbClient;
import io.helidon.dbclient.spi.DbClientProvider;
import io.helidon.dbclient.spi.DbClientServiceProvider;
import io.helidon.dbclient.spi.DbMapperProvider;

/**
 * Helidon DB Client.
 *
 * @see DbClient
 */
@Preview
@Feature(value = "Db Client",
        description = "Helidon Database client",
        in = HelidonFlavor.SE,
        path = "DbClient"
)
module io.helidon.dbclient {
    requires static io.helidon.common.features.api;

    requires java.logging;
    requires transitive io.helidon.common;
    requires transitive io.helidon.common.config;
    requires transitive io.helidon.common.context;
    requires transitive io.helidon.common.mapper;
    requires java.sql;

    exports io.helidon.dbclient;
    exports io.helidon.dbclient.spi;

    uses DbClientProvider;
    uses DbMapperProvider;
    uses DbClientServiceProvider;
}
