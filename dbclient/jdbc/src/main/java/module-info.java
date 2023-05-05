/*
 * Copyright (c) 2019, 2023 Oracle and/or its affiliates.
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

import io.helidon.common.features.api.Aot;
import io.helidon.common.features.api.Feature;
import io.helidon.common.features.api.HelidonFlavor;
import io.helidon.dbclient.jdbc.JdbcDbClientProvider;
import io.helidon.dbclient.jdbc.spi.HikariCpExtensionProvider;
import io.helidon.dbclient.spi.DbClientProvider;

/**
 * Helidon Common Mapper.
 */
@Feature(value = "JDBC",
        description = "DB Client over JDBC",
        in = HelidonFlavor.SE,
        path = {"DbClient", "JDBC"}
)
@Aot(description = "Tested with Helidon Oracle and H2 drivers (see examples)")
module io.helidon.dbclient.jdbc {
    requires static io.helidon.common.features.api;

    uses HikariCpExtensionProvider;
    requires java.sql;
    requires com.zaxxer.hikari;

    requires transitive io.helidon.common;
    requires transitive io.helidon.common.configurable;
    requires transitive io.helidon.dbclient;

    exports io.helidon.dbclient.jdbc;
    exports io.helidon.dbclient.jdbc.spi;

    provides DbClientProvider with JdbcDbClientProvider;
}
