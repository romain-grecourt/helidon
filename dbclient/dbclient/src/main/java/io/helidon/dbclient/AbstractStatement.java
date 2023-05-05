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
import java.util.Objects;

import io.helidon.common.context.Context;
import io.helidon.common.context.Contexts;
import io.helidon.common.mapper.MapperManager;

/**
 * Common statement methods and fields.
 *
 * @param <S> type of a subclass
 * @param <R> the result type of the statement as returned by {@link #execute()}
 */
public abstract class AbstractStatement<S extends DbStatement<S, R>, R> implements DbStatement<S, R> {

    private final DbClientContext clientContext;
    private final DbStatementContext statementContext;
    private ParamType paramType = ParamType.UNKNOWN;
    private StatementParameters parameters;

    /**
     * Statement that handles parameters.
     *
     * @param statementContext database statement configuration and context
     */
    protected AbstractStatement(DbStatementContext statementContext) {
        this.statementContext = statementContext;
        this.clientContext = statementContext.clientContext();
    }

    @Override
    public R execute() {
        initParameters(ParamType.INDEXED);
        DbClientServiceContext.Builder builder = DbClientServiceContext.builder()
                                                                       .dbType(dbType())
                                                                       .statement(statementContext.statementName());
        if (paramType == ParamType.NAMED) {
            builder.statement(statementContext.statement(), parameters.namedParams());
        } else {
            builder.statement(statementContext.statement(), parameters.indexedParams());
        }
        builder.statementType(statementType());
        builder.context(Contexts.context().orElseGet(Context::create));

        // TODO chain instead (See https://github.com/helidon-io/helidon/pull/6752/files#diff-9b9ca8317ee483071eea6b057b8452dd091e0cf5bb6d978ba0081c3f6f66097a)
        DbClientServiceContext dbContext = builder.build();
        for (DbClientService service : clientContext.clientServices()) {
            dbContext = service.statement(dbContext);
        }
        return doExecute(dbContext);
    }

    /**
     * Type of this statement.
     *
     * @return statement type
     */
    protected DbStatementType statementType() {
        return statementContext.statementType();
    }

    /**
     * Execute the statement against the database.
     *
     * @param dbContext interceptor context
     * @return result of this db statement.
     */
    protected abstract R doExecute(DbClientServiceContext dbContext);

    /**
     * Type of this database to use in interceptor context.
     *
     * @return type of this db
     */
    protected abstract String dbType();

    /**
     * Context of the DB client.
     *
     * @return context with access to client wide configuration and runtime
     */
    public DbClientContext clientContext() {
        return clientContext;
    }

    @Override
    public S params(List<?> parameters) {
        Objects.requireNonNull(parameters, "Parameters cannot be null (may be an empty list)");

        initParameters(ParamType.INDEXED);
        this.parameters.params(parameters);

        return me();
    }

    @Override
    public S params(Map<String, ?> parameters) {
        initParameters(ParamType.NAMED);
        this.parameters.params(parameters);
        return me();
    }

    @Override
    public S namedParam(Object parameters) {
        initParameters(ParamType.NAMED);
        this.parameters.namedParam(parameters);
        return me();
    }

    @Override
    public S indexedParam(Object parameters) {
        initParameters(ParamType.INDEXED);
        this.parameters.indexedParam(parameters);
        return me();
    }

    @Override
    public S addParam(Object parameter) {
        initParameters(ParamType.INDEXED);
        this.parameters.addParam(parameter);
        return me();
    }

    @Override
    public S addParam(String name, Object parameter) {
        initParameters(ParamType.NAMED);
        this.parameters.addParam(name, parameter);
        return me();
    }

    /**
     * Type of parameters of this statement.
     *
     * @return indexed or named, or unknown in case it could not be yet defined
     */
    protected ParamType paramType() {
        return paramType;
    }

    /**
     * Db mapper manager.
     *
     * @return mapper manager for DB types
     */
    protected DbMapperManager dbMapperManager() {
        return clientContext.dbMapperManager();
    }

    /**
     * Mapper manager.
     *
     * @return generic mapper manager
     */
    protected MapperManager mapperManager() {
        return clientContext.mapperManager();
    }

    /**
     * Get the named parameters of this statement.
     *
     * @return name parameter map
     * @throws java.lang.IllegalStateException in case this statement is using indexed parameters
     */
    protected Map<String, Object> namedParams() {
        initParameters(ParamType.NAMED);
        return parameters.namedParams();
    }

    /**
     * Get the indexed parameters of this statement.
     *
     * @return parameter list
     * @throws java.lang.IllegalStateException in case this statement is using named parameters
     */
    protected List<Object> indexedParams() {
        initParameters(ParamType.INDEXED);
        return parameters.indexedParams();
    }

    /**
     * Statement name.
     *
     * @return name of this statement (never null, may be generated)
     */
    protected String statementName() {
        return statementContext.statementName();
    }

    /**
     * Statement text.
     *
     * @return text of this statement
     */
    protected String statement() {
        return statementContext.statement();
    }

    /**
     * Returns this builder cast to the correct type.
     *
     * @return this as type extending this class
     */
    @SuppressWarnings("unchecked")
    protected S me() {
        return (S) this;
    }

    private void initParameters(ParamType type) {
        if (this.paramType != ParamType.UNKNOWN) {
            // already initialized
            return;
        }
        if (Objects.requireNonNull(type) == ParamType.NAMED) {
            this.paramType = ParamType.NAMED;
            this.parameters = new NamedStatementParameters(clientContext.dbMapperManager());
        } else {
            this.paramType = ParamType.INDEXED;
            this.parameters = new IndexedStatementParameters(clientContext.dbMapperManager());
        }
    }
}
