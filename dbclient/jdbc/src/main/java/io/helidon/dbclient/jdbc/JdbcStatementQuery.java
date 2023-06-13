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
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;
import java.util.Spliterator;
import java.util.Spliterators.AbstractSpliterator;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import io.helidon.common.GenericType;
import io.helidon.common.mapper.MapperException;
import io.helidon.common.mapper.MapperManager;
import io.helidon.dbclient.DbClient;
import io.helidon.dbclient.DbClientException;
import io.helidon.dbclient.DbClientServiceContext;
import io.helidon.dbclient.DbColumn;
import io.helidon.dbclient.DbMapperManager;
import io.helidon.dbclient.DbRow;
import io.helidon.dbclient.DbStatementQuery;
import io.helidon.dbclient.DbStatementContext;

/**
 * Implementation of query.
 */
class JdbcStatementQuery extends JdbcStatement<DbStatementQuery, Stream<DbRow>> implements DbStatementQuery {

    /**
     * Local logger instance.
     */
    private static final System.Logger LOGGER = System.getLogger(JdbcStatementQuery.class.getName());

    JdbcStatementQuery(JdbcExecuteContext executeContext,
                       DbStatementContext statementContext) {
        super(executeContext, statementContext);
    }

    @Override
    protected Stream<DbRow> doExecute(DbClientServiceContext dbContext) {
        Connection conn = connection();
        PreparedStatement statement;
        try {
            // first try block is to create a statement
            statement = super.build(conn, dbContext);
        } catch (Exception e) {
            throw new DbClientException(e.getMessage(), e);
        }

        try {
            ResultSet rs = statement.executeQuery();
            // TODO statementCompletionListener

            Map<Long, DbColumn> metadata = createMetadata(rs);

            DbMapperManager dbMapperManager = dbMapperManager();
            MapperManager mapperManager = mapperManager();

            Stream<DbRow> stream = StreamSupport.stream(new AbstractSpliterator<DbRow>(Long.MAX_VALUE, Spliterator.ORDERED) {
                @Override
                public boolean tryAdvance(Consumer<? super DbRow> action) {
                    try {
                        if (!rs.next()) {
                            return false;
                        }
                        DbRow dbRow = createDbRow(rs, metadata, dbMapperManager, mapperManager);
                        action.accept(dbRow);
                        return true;
                    } catch (SQLException ex) {
                        throw new DbClientException(ex.getMessage(), ex);
                    }
                }
            }, false).onClose(() -> {
                try {
                    rs.close();
                } catch (SQLException e) {
                    throw new DbClientException("Failed to close result-set or connection", e);
                }
            });
            executeContext().onClose(stream::close);
            return stream;
        } catch (SQLException e) {
            LOGGER.log(Level.TRACE,
                    String.format("Failed to execute query %s: %s", statement, e.getMessage()),
                    e);
            try {
                conn.close();
            } catch (SQLException sqlEx) {
                String msg = "Failed to close connection";
                LOGGER.log(Level.TRACE, msg, sqlEx);
                e.addSuppressed(sqlEx);
            }
            throw new DbClientException(e.getMessage(), e);
        }
    }

    @Override
    public Stream<DbRow> execute() {
        return super.execute0();
    }

    static Map<Long, DbColumn> createMetadata(ResultSet rs) throws SQLException {
        ResultSetMetaData metaData = rs.getMetaData();
        int columnCount = metaData.getColumnCount();

        Map<Long, DbColumn> byNumbers = new HashMap<>();

        for (int i = 1; i <= columnCount; i++) {
            String name = metaData.getColumnLabel(i);
            String sqlType = metaData.getColumnTypeName(i);
            Class<?> javaClass = classByName(metaData.getColumnClassName(i));
            DbColumn column = new DbColumn() {
                @Override
                public <T> T as(Class<T> type) {
                    return null;
                }

                @Override
                public <T> T as(GenericType<T> type) {
                    return null;
                }

                @Override
                public Class<?> javaType() {
                    return javaClass;
                }

                @Override
                public String dbType() {
                    return sqlType;
                }

                @Override
                public String name() {
                    return name;
                }
            };
            byNumbers.put((long) i, column);
        }
        return byNumbers;
    }

    private DbRow createDbRow(ResultSet rs,
                              Map<Long, DbColumn> metadata,
                              DbMapperManager dbMapperManager,
                              MapperManager mapperManager) throws SQLException {
        // read whole row
        // for each column
        Map<String, DbColumn> byStringsWithValues = new HashMap<>();
        Map<Integer, DbColumn> byNumbersWithValues = new HashMap<>();

        for (int i = 1; i <= metadata.size(); i++) {
            DbColumn meta = metadata.get((long) i);
            Object value = rs.getObject(i);
            DbColumn withValue = new DbColumn() {
                @Override
                public <T> T as(Class<T> type) {
                    if (null == value) {
                        return null;
                    }
                    if (type.isAssignableFrom(value.getClass())) {
                        return type.cast(value);
                    }
                    return map(value, type);
                }

                @SuppressWarnings("unchecked")
                <SRC, T> T map(SRC value, Class<T> type) {
                    Class<SRC> theClass = (Class<SRC>) value.getClass();

                    try {
                        return mapperManager.map(value, theClass, type, DbClient.MAPPING_QUALIFIER);
                    } catch (MapperException e) {
                        if (type.equals(String.class)) {
                            return (T) String.valueOf(value);
                        }
                        throw e;
                    }
                }

                @SuppressWarnings("unchecked")
                <SRC, T> T map(SRC value, GenericType<T> type) {
                    Class<SRC> theClass = (Class<SRC>) value.getClass();
                    return mapperManager.map(value, GenericType.create(theClass), type, DbClient.MAPPING_QUALIFIER);
                }

                @Override
                public <T> T as(GenericType<T> type) {
                    if (null == value) {
                        return null;
                    }
                    if (type.isClass()) {
                        Class<?> theClass = type.rawType();
                        if (theClass.isAssignableFrom(value.getClass())) {
                            return type.cast(value);
                        }
                    }
                    return map(value, type);
                }

                @Override
                public Class<?> javaType() {
                    if (null == meta.javaType()) {
                        if (null == value) {
                            return null;
                        }
                        return value.getClass();
                    } else {
                        return meta.javaType();
                    }
                }

                @Override
                public String dbType() {
                    return meta.dbType();
                }

                @Override
                public String name() {
                    return meta.name();
                }
            };
            byStringsWithValues.put(meta.name(), withValue);
            byNumbersWithValues.put(i, withValue);
        }

        return new DbRow() {
            @Override
            public DbColumn column(String name) {
                return byStringsWithValues.get(name);
            }

            @Override
            public DbColumn column(int index) {
                return byNumbersWithValues.get(index);
            }

            @Override
            public void forEach(Consumer<? super DbColumn> columnAction) {
                byStringsWithValues.values()
                                   .forEach(columnAction);
            }

            @Override
            public <T> T as(Class<T> type) {
                return dbMapperManager.read(this, type);
            }

            @Override
            public <T> T as(GenericType<T> type) {
                return dbMapperManager.read(this, type);
            }

            @Override
            public <T> T as(Function<DbRow, T> mapper) {
                return mapper.apply(this);
            }

            @Override
            public String toString() {
                StringBuilder sb = new StringBuilder();
                boolean first = true;
                sb.append('{');
                for (DbColumn col : byStringsWithValues.values()) {
                    if (first) {
                        first = false;
                    } else {
                        sb.append(',');
                    }
                    sb.append(col.name());
                    sb.append(':');
                    sb.append(col.value().toString());
                }
                sb.append('}');
                return sb.toString();
            }
        };
    }

    private static Class<?> classByName(String columnClassName) {
        if (columnClassName == null) {
            return null;
        }
        try {
            return Class.forName(columnClassName);
        } catch (ClassNotFoundException e) {
            return null;
        }
    }
}
