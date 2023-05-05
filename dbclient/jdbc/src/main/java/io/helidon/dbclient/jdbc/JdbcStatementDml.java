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

import java.lang.System.Logger.Level;
import java.sql.PreparedStatement;
import java.sql.SQLException;

import io.helidon.dbclient.DbClient;
import io.helidon.dbclient.DbClientServiceContext;
import io.helidon.dbclient.DbStatementDml;
import io.helidon.dbclient.DbStatementContext;

class JdbcStatementDml extends JdbcStatement<DbStatementDml, Long> implements DbStatementDml {

    private static final System.Logger LOGGER = System.getLogger(DbClient.class.getName());

    JdbcStatementDml(JdbcExecuteContext executeContext,
                     DbStatementContext statementContext) {
        super(executeContext, statementContext);
    }

    @Override
    protected Long doExecute(DbClientServiceContext dbContext) {
        long count = 0;
        try {
            PreparedStatement preparedStatement = build(connection(), dbContext);
            count = preparedStatement.executeLargeUpdate();
            preparedStatement.close();
            return count;
        } catch (SQLException e) {
            LOGGER.log(Level.WARNING, String.format("Could not close connection: %s", e.getMessage()), e);
            return count;
        }
    }
}
