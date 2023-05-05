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
package io.helidon.dbclient;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletionStage;

import io.helidon.common.context.Context;
import io.helidon.dbclient.spi.DbClientProvider;

/**
 * Interceptor context to get (and possibly manipulate) database operations.
 * <p>
 * This is a mutable object - acts as a builder during the invocation of {@link DbClientService}.
 * The interceptors are executed sequentially, so there is no need for synchronization.
 */
public final class DbClientServiceContext {

    private final String dbType;
    private final DbStatementType statementType;
    private final Context context;
    private final String statementName;
    private final String statement;
    private final List<Object> indexedParams;
    private final Map<String, Object> namedParams;
    private final boolean indexed;

    private DbClientServiceContext(Builder builder) {
        this.dbType = builder.dbType;
        this.statementType = builder.statementType;
        this.context = builder.context;
        this.statementName = builder.statementName;
        this.statement = builder.statement;
        this.indexedParams = builder.indexedParams;
        this.namedParams = builder.namedParams;
        this.indexed = builder.indexed;
    }

    /**
     * Create a new builder.
     *
     * @return Builder
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Type of this database (usually the same string used by the {@link DbClientProvider#name()}).
     *
     * @return type of database
     */
    public String dbType() {
        return dbType;
    }

    /**
     * Context with parameters passed from the caller, such as {@code SpanContext} for tracing.
     *
     * @return context associated with this request
     */
    public Context context() {
        return context;
    }

    /**
     * Name of a statement to be executed.
     * Ad hoc statements have names generated.
     *
     * @return name of the statement
     */
    public String statementName() {
        return statementName;
    }

    /**
     * Text of the statement to be executed.
     *
     * @return statement text
     */
    public String statement() {
        return statement;
    }

    /**
     * A stage that is completed once the statement finishes execution.
     *
     * @return statement future
     */
    public CompletionStage<Void> statementFuture() {
        throw new UnsupportedOperationException();
    }

    /**
     * A stage that is completed once the results were fully read. The number returns either the number of modified
     * records or the number of records actually read.
     *
     * @return stage that completes once all query results were processed.
     */
    public CompletionStage<Long> resultFuture() {
        throw new UnsupportedOperationException();
    }

    /**
     * Indexed parameters (if used).
     *
     * @return indexed parameters (empty if this statement parameters are not indexed)
     */
    public Optional<List<Object>> indexedParameters() {
        return Optional.ofNullable(indexedParams);
    }

    /**
     * Named parameters (if used).
     *
     * @return named parameters (empty if this statement parameters are not named)
     */
    public Optional<Map<String, Object>> namedParameters() {
        return Optional.ofNullable(namedParams);
    }

    /**
     * Whether this is a statement with indexed parameters.
     *
     * @return Whether this statement has indexed parameters ({@code true}) or named parameters {@code false}.
     */
    public boolean isIndexed() {
        return indexed;
    }

    /**
     * Whether this is a statement with named parameters.
     *
     * @return Whether this statement has named parameters ({@code true}) or indexed parameters {@code false}.
     */
    public boolean isNamed() {
        return !indexed;
    }

    /**
     * Type of the statement being executed.
     *
     * @return statement type
     */
    public DbStatementType statementType() {
        return statementType;
    }

    /**
     * {@link DbClientServiceContext} builder.
     */
    public static final class Builder implements io.helidon.common.Builder<Builder, DbClientServiceContext> {

        private String dbType;
        private String statement;
        private DbStatementType statementType;
        private Context context;
        private List<Object> indexedParams;
        private Map<String, Object> namedParams;
        private boolean indexed;
        private String statementName;

        /**
         * Set the database type.
         *
         * @param dbType a short name of the db type (such as jdbc:mysql)
         * @return this builder
         * @see #dbType()
         */
        public Builder dbType(String dbType) {
            this.dbType = dbType;
            return this;
        }

        /**
         * Set the statement.
         *
         * @param statement statement
         * @return this builder
         * @see #statement()
         */
        public Builder statement(String statement) {
            this.statement = statement;
            return this;
        }

        /**
         * Set the statement.
         *
         * @param statement     statement
         * @param indexedParams indexed parameters
         * @return this builder
         * @see #statement()
         */
        public Builder statement(String statement, List<Object> indexedParams) {
            this.statement = statement;
            this.indexedParams = indexedParams;
            this.indexed = true;
            return this;
        }

        /**
         * Set the statement.
         *
         * @param statement   statement
         * @param namedParams named parameters
         * @return this builder
         * @see #statement()
         */
        public Builder statement(String statement, Map<String, Object> namedParams) {
            this.statement = statement;
            this.namedParams = namedParams;
            this.indexed = false;
            return this;
        }

        /**
         * Set the statement type.
         *
         * @param statementType statement type
         * @return this builder
         * @see #statementType()
         */
        public Builder statementType(DbStatementType statementType) {
            this.statementType = statementType;
            return this;
        }

        /**
         * Set the context.
         *
         * @param context context
         * @return this builder
         * @see #context()
         */
        public Builder context(Context context) {
            this.context = context;
            return this;
        }

        /**
         * Set the statement name.
         *
         * @param statementName statement name
         * @return this builder
         * @see #statementName()
         */
        public Builder statementName(String statementName) {
            this.statementName = statementName;
            return this;
        }

        @Override
        public DbClientServiceContext build() {
            return new DbClientServiceContext(this);
        }
    }
}
