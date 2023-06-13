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
package io.helidon.dbclient.jdbc;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import io.helidon.dbclient.DbClientContext;

/**
 * Stuff needed by each and every statement.
 */
final class JdbcExecuteContext extends DbClientContext {

    private static final System.Logger LOGGER = System.getLogger(JdbcExecuteContext.class.getName());
    private final String dbType;
    private final Connection connection;
    private final List<Runnable> closeHandlers = new ArrayList<>();

    private JdbcExecuteContext(Builder builder) {
        super(builder);
        this.dbType = builder.dbType;
        this.connection = builder.connection;
    }

    /**
     * Builder to create new instances.
     *
     * @return a new builder instance
     */
    static Builder jdbcBuilder() {
        return new Builder();
    }

    String dbType() {
        return dbType;
    }

    Connection connection() {
        return connection;
    }

    void close() {
        try {
            closeHandlers.forEach(Runnable::run);
            connection.close();
        } catch (SQLException ex) {
            LOGGER.log(System.Logger.Level.WARNING,
                    String.format("Could not close execute context: %s", ex.getMessage()),
                    ex);
        }
    }

    void onClose(Runnable closeHandler) {
        closeHandlers.add(closeHandler);
    }

    static class Builder extends BuilderBase<Builder> implements io.helidon.common.Builder<Builder, JdbcExecuteContext> {
        private String dbType;
        private Connection connection;

        @Override
        public JdbcExecuteContext build() {
            return new JdbcExecuteContext(this);
        }

        Builder dbType(String dbType) {
            this.dbType = dbType;
            return this;
        }

        Builder connection(Connection connection) {
            this.connection = connection;
            return this;
        }
    }
}
