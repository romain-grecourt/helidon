/*
 * Copyright (c) 2022 Oracle and/or its affiliates.
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
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import io.helidon.dbclient.DbClient;
import io.helidon.dbclient.DbClientException;

import io.helidon.dbclient.DbExecute;
import io.helidon.dbclient.DbTransaction;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static org.mockito.Mockito.anyString;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class JdbcClientMultipleDMLOperationTest {

    private static final PreparedStatement PREP_STATEMENT = mock(PreparedStatement.class);
    private enum DmlOperation {insert, update, delete}

    @BeforeAll
    static void beforeAll() throws SQLException {
        doAnswer(invocationOnMock -> {
            // Put a delay to simulate a statement processing
            delay(10);
            return 1L;
        }).when(PREP_STATEMENT).executeLargeUpdate();
    }

    @Test
    void testMultipleInsert() {
        multipleDMLOperationExecution(false, DmlOperation.insert);
    }

    @Test
    void testMultipleTxInsert() {
        multipleDMLOperationExecution(true, DmlOperation.insert);
    }

    @Test
    void testMultipleUpdate() {
        multipleDMLOperationExecution(false, DmlOperation.update);
    }

    @Test
    void testMultipleTxUpdate() {
        multipleDMLOperationExecution(true, DmlOperation.update);
    }

    @Test
    void testMultipleDelete() {
        multipleDMLOperationExecution(false, DmlOperation.delete);
    }

    @Test
    void testMultipleTxDelete() {
        multipleDMLOperationExecution(true, DmlOperation.delete);
    }

    void multipleDMLOperationExecution(boolean tx, DmlOperation dmlOperation) {
        int maxIteration = 100;

        DbClient dbClient = JdbcDbClientProviderBuilder.create()
                                                       .connectionPool(new MockConnectionPool())
                                                       .build();
        switch (dmlOperation) {
            case insert -> {
                for (int i = 0; i < maxIteration; i++) {
                    if (tx) {
                        try (DbTransaction exec = dbClient.transaction()) {
                            exec.createInsert("INSERT INTO pokemons (name, type) VALUES ('name', 'type')").execute();
                        }
                    } else {
                        try (DbExecute exec = dbClient.execute()) {
                            exec.createInsert("INSERT INTO pokemons (name, type) VALUES ('name', 'type')").execute();
                        }
                    }
                }
            }
            case update -> {
                for (int i = 0; i < maxIteration; i++) {
                    if (tx) {
                        try (DbTransaction exec = dbClient.transaction()) {
                            exec.createUpdate("UPDATE pokemons SET type = 'type' WHERE name = 'name'").execute();
                        }
                    } else {
                        try (DbExecute exec = dbClient.execute()) {
                            exec.createUpdate("UPDATE pokemons SET type = 'type' WHERE name = 'name'").execute();
                        }
                    }
                }
            }
            case delete -> {
                for (int i = 0; i < maxIteration; i++) {
                    if (tx) {
                        try (DbTransaction exec = dbClient.transaction()) {
                            exec.createDelete("DELETE FROM pokemons WHERE name = name").execute();
                        }
                    } else {
                        try (DbExecute exec = dbClient.execute()) {
                            exec.createDelete("DELETE FROM pokemons WHERE name = name").execute();
                        }
                    }
                }
            }
        }
    }

    static class MockConnectionPool implements ConnectionPool {
        int maxPoolCount = 10;
        List<Connection> connectionPool = Collections.synchronizedList(new ArrayList<>(maxPoolCount));
        List<Connection> usedConnections = Collections.synchronizedList(new ArrayList<>(maxPoolCount));

        int actualConnection = 0;

        @Override
        public Connection connection() {
            Connection conn;
            this.actualConnection++;
            // get connection from the pool if it is not empty,
            if (!connectionPool.isEmpty()) {
                conn = this.connectionPool.remove(0);
            } else {
                // If usedConnections reach maxPoolCount, wait for a few seconds until it recedes.
                // Otherwise throw an exception.
                Timer timer = new Timer(2);
                while (this.usedConnections.size() >= maxPoolCount) {
                    if (timer.expired()) {
                        throw new DbClientException(
                                String.format("Unable to acquire a connection after %d sec", timer.getTimeout()));
                    }
                    delay(50);
                }
                conn = mock(Connection.class);
                try {
                    when(conn.prepareStatement(anyString())).thenReturn(PREP_STATEMENT);
                    doAnswer(invocationOnMock -> {
                        if (this.usedConnections.remove(conn)) {
                            connectionPool.add(conn);
                        }
                        return null;
                    }).when(conn).close();
                } catch (Exception ignored) {
                }
            }
            this.usedConnections.add(conn);
            // Put delay to simulate an instantiation of a connection
            delay(10);
            return conn;
        }
    }

    private static class Timer {
        private final long endTime;
        private final int timeOut;

        public Timer(int timeOut) {
            this.timeOut = timeOut;
            this.endTime = System.currentTimeMillis() + 1_000L * timeOut;
        }

        public boolean expired() {
            return System.currentTimeMillis() >= this.endTime;
        }

        int getTimeout() {
            return this.timeOut;
        }
    }

    private static void delay(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException ignore) {
        }
    }
}
