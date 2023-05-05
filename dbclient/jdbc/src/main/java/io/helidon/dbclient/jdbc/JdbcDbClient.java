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
package io.helidon.dbclient.jdbc;

import java.lang.System.Logger.Level;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;
import java.util.function.Function;

import io.helidon.common.mapper.MapperManager;
import io.helidon.dbclient.DbClient;
import io.helidon.dbclient.DbClientException;
import io.helidon.dbclient.DbClientService;
import io.helidon.dbclient.DbExecute;
import io.helidon.dbclient.DbMapperManager;
import io.helidon.dbclient.DbStatementDml;
import io.helidon.dbclient.DbStatementGet;
import io.helidon.dbclient.DbStatementQuery;
import io.helidon.dbclient.DbStatements;
import io.helidon.dbclient.DbTransaction;
import io.helidon.dbclient.AbstractDbExecute;
import io.helidon.dbclient.DbStatementContext;

import static io.helidon.dbclient.DbStatementType.DELETE;
import static io.helidon.dbclient.DbStatementType.DML;
import static io.helidon.dbclient.DbStatementType.GET;
import static io.helidon.dbclient.DbStatementType.INSERT;
import static io.helidon.dbclient.DbStatementType.QUERY;
import static io.helidon.dbclient.DbStatementType.UPDATE;

/**
 * Helidon DB implementation for JDBC drivers.
 */
class JdbcDbClient implements DbClient {

    private static final System.Logger LOGGER = System.getLogger(DbClient.class.getName());

    private final ConnectionPool connectionPool;
    private final DbStatements statements;
    private final DbMapperManager dbMapperManager;
    private final MapperManager mapperManager;
    private final List<DbClientService> clientServices;

    JdbcDbClient(JdbcDbClientProviderBuilder builder) {
        this.connectionPool = builder.connectionPool();
        this.statements = builder.statements();
        this.dbMapperManager = builder.dbMapperManager();
        this.mapperManager = builder.mapperManager();
        this.clientServices = builder.clientServices();
    }

    @Override
    public JdbcTxExecute transaction() {
        return new JdbcTxExecute(
                statements,
                clientServices,
                connectionPool,
                dbMapperManager,
                mapperManager);
    }

    @Override
    public <T> T transaction(Function<DbTransaction, T> executor) {
        JdbcTxExecute execute = transaction();
        T result;
        try {
            result = executor.apply(execute);
            execute.doCommit();
            return result;
        } catch (Throwable t1) {
            LOGGER.log(Level.WARNING, () -> String.format("Transaction rollback: %s", t1.getMessage()), t1);
            try {
                execute.doRollback();
            } catch (Throwable t2) {
                LOGGER.log(Level.WARNING, () -> String.format("Transaction rollback failed: %s", t2.getMessage()), t2);
            }
            throw t1;
        }
    }

    @Override
    public DbExecute execute() {
        return new JdbcExecute(statements,
                JdbcExecute.createContext(statements,
                        clientServices,
                        connectionPool,
                        dbMapperManager,
                        mapperManager));
    }

    @Override
    public String dbType() {
        return connectionPool.dbType();
    }

    @Override
    public <C> C unwrap(Class<C> cls) {
        if (Connection.class.isAssignableFrom(cls)) {
            return cls.cast(connectionPool.connection());
        }
        throw new UnsupportedOperationException(String.format(
                "Class %s is not supported for unwrap",
                cls.getName()));
    }

    private static final class JdbcTxExecute extends JdbcExecute implements DbTransaction {

        private volatile boolean setRollbackOnly = false;

        private JdbcTxExecute(DbStatements statements,
                              List<DbClientService> clientServices,
                              ConnectionPool connectionPool,
                              DbMapperManager dbMapperManager,
                              MapperManager mapperManager) {

            super(statements, JdbcExecuteContext.jdbcBuilder()
                                                .statements(statements)
                                                .clientServices(clientServices)
                                                .dbType(connectionPool.dbType())
                                                .connection(createConnection(connectionPool))
                                                .dbMapperManager(dbMapperManager)
                                                .mapperManager(mapperManager)
                                                .build());
        }

        private static Connection createConnection(ConnectionPool connectionPool) {
            Connection conn = connectionPool.connection();
            try {
                conn.setAutoCommit(false);
            } catch (SQLException e) {
                throw new DbClientException("Failed to set autocommit to false", e);
            }
            return conn;
        }

        @Override
        public void rollback() {
            setRollbackOnly = true;
        }

        private void doRollback() {
            Connection conn = context().connection();
            try {
                conn.rollback();
                conn.close();
            } catch (SQLException e) {
                throw new DbClientException("Failed to rollback a transaction, or close a connection", e);
            }
        }

        private void doCommit() {
            if (setRollbackOnly) {
                doRollback();
            } else {
                Connection conn = context().connection();
                try {
                    conn.commit();
                    conn.close();
                } catch (SQLException e) {
                    throw new DbClientException("Failed to commit a transaction, or close a connection", e);
                }
            }
        }
    }

    private static class JdbcExecute extends AbstractDbExecute {

        private final JdbcExecuteContext context;

        private JdbcExecute(DbStatements statements, JdbcExecuteContext context) {
            super(statements);
            this.context = context;
        }

        private static JdbcExecuteContext createContext(DbStatements statements,
                                                        List<DbClientService> clientServices,
                                                        ConnectionPool connectionPool,
                                                        DbMapperManager dbMapperManager,
                                                        MapperManager mapperManager) {

            Connection conn = connectionPool.connection();
            try {
                conn.setAutoCommit(true);
            } catch (SQLException e) {
                throw new DbClientException("Failed to set autocommit to true", e);
            }
            return JdbcExecuteContext.jdbcBuilder()
                                     .statements(statements)
                                     .connection(conn)
                                     .clientServices(clientServices)
                                     .dbMapperManager(dbMapperManager)
                                     .mapperManager(mapperManager)
                                     .dbType(connectionPool.dbType())
                                     .build();
        }

        @Override
        public DbStatementQuery createNamedQuery(String name, String stmt) {
            return new JdbcStatementQuery(context, DbStatementContext.create(context, QUERY, name, stmt));

        }

        @Override
        public DbStatementGet createNamedGet(String name, String stmt) {
            return new JdbcStatementGet(context, DbStatementContext.create(context, GET, name, stmt));
        }

        @Override
        public DbStatementDml createNamedDmlStatement(String name, String stmt) {
            return new JdbcStatementDml(context, DbStatementContext.create(context, DML, name, stmt));
        }

        @Override
        public DbStatementDml createNamedInsert(String name, String stmt) {
            return new JdbcStatementDml(context, DbStatementContext.create(context, INSERT, name, stmt));
        }

        @Override
        public DbStatementDml createNamedUpdate(String name, String stmt) {
            return new JdbcStatementDml(context, DbStatementContext.create(context, UPDATE, name, stmt));
        }

        @Override
        public DbStatementDml createNamedDelete(String name, String stmt) {
            return new JdbcStatementDml(context, DbStatementContext.create(context, DELETE, name, stmt));
        }

        JdbcExecuteContext context() {
            return context;
        }

        @Override
        public void close() {
            try {
                context.connection().close();
            } catch (SQLException e) {
                LOGGER.log(Level.WARNING, String.format("Could not close connection: %s", e.getMessage()), e);
            }
        }

        @Override
        public <C> C unwrap(Class<C> cls) {
            if (Connection.class.isAssignableFrom(cls)) {
                return cls.cast(context.connection());
            }
            throw new UnsupportedOperationException(String.format(
                    "Class %s is not supported for unwrap",
                    cls.getName()));
        }

    }

}
