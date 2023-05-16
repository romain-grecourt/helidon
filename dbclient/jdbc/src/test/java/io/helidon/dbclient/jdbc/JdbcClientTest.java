/*
 * Copyright (c) 2020, 2023 Oracle and/or its affiliates.
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
import java.sql.PreparedStatement;
import java.sql.SQLException;

import io.helidon.dbclient.DbExecute;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.jupiter.api.Assertions.fail;

@SuppressWarnings({"SqlDialectInspection", "SqlNoDataSourceInspection"})
public class JdbcClientTest {

    private static final System.Logger LOGGER = System.getLogger(JdbcClientTest.class.getName());

    private static final ConnectionPool POOL = Mockito.mock(ConnectionPool.class);
    private static final Connection CONN = Mockito.mock(Connection.class);
    private static final PreparedStatement PREP_STATEMENT = Mockito.mock(PreparedStatement.class);

    @BeforeAll
    static void beforeAll() throws SQLException {
        Mockito.when(CONN.prepareStatement("SELECT NULL FROM DUAL")).thenReturn(PREP_STATEMENT);
        Mockito.when(POOL.connection()).thenReturn(CONN);
        Mockito.when(PREP_STATEMENT.executeLargeUpdate()).thenReturn(1L);
    }

    @Test
    void txResultHandling() {
        JdbcDbClient dbClient = (JdbcDbClient) JdbcDbClientProviderBuilder.create()
                .connectionPool(POOL)
                .build();
        long result = dbClient.transaction(exec -> exec.dml("SELECT NULL FROM DUAL"));
        assertThat(result, is(equalTo(1L)));
    }

    @Test
    void testUnwrapClientConnection() {
        JdbcDbClient dbClient = (JdbcDbClient) JdbcDbClientProviderBuilder.create()
                .connectionPool(POOL)
                .build();
        Connection connection = dbClient.unwrap(Connection.class);
        assertThat(connection, notNullValue());
    }

    @Test
    void testUnsupportedUnwrapClientClass() {
        JdbcDbClient dbClient = (JdbcDbClient) JdbcDbClientProviderBuilder.create()
                .connectionPool(POOL)
                .build();
        try {
            dbClient.unwrap(PreparedStatement.class);
            fail("Unsupported unwrap call must throw UnsupportedOperationException");
        } catch (UnsupportedOperationException ex) {
            LOGGER.log(Level.DEBUG, () -> String.format("Caught expected UnsupportedOperationException: %s", ex.getMessage()));
        }
    }

    @Test
    void testUnwrapExecutorConnection() {
        JdbcDbClient dbClient = (JdbcDbClient) JdbcDbClientProviderBuilder.create()
                .connectionPool(POOL)
                .build();
        try (DbExecute exec = dbClient.execute()) {
            Connection connection = exec.unwrap(Connection.class);
            assertThat(connection, notNullValue());
            exec.dml("SELECT NULL FROM DUAL");
        }
    }

    @Test
    void testUnsupportedUnwrapExecutorClass() {
        JdbcDbClient dbClient = (JdbcDbClient) JdbcDbClientProviderBuilder.create()
                .connectionPool(POOL)
                .build();
        try (DbExecute exec = dbClient.execute()) {
            try {
                exec.unwrap(PreparedStatement.class);
                fail("Unsupported unwrap call must throw UnsupportedOperationException");
            } catch (UnsupportedOperationException ex) {
                LOGGER.log(Level.DEBUG, () -> String.format("Caught expected UnsupportedOperationException: %s",
                        ex.getMessage()));
            }
            exec.query("{\"operation\": \"command\", \"query\": { ping: 1 }}");
        }
    }

}
