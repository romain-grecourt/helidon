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

import io.helidon.dbclient.DbClientContext;

/**
 * Stuff needed by each and every statement.
 */
final class JdbcExecuteContext extends DbClientContext {

    private final String dbType;
    private final Connection connection;

    private JdbcExecuteContext(Builder builder) {
        super(builder);
        this.dbType = builder.dbType;
        this.connection = builder.connection;
    }

    /**
     * Builder to create new instances.
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
