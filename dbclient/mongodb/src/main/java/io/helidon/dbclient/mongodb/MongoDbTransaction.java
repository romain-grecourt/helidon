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
package io.helidon.dbclient.mongodb;

import java.lang.System.Logger.Level;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import io.helidon.dbclient.DbStatementDml;
import io.helidon.dbclient.DbStatementGet;
import io.helidon.dbclient.DbStatementQuery;
import io.helidon.dbclient.DbTransaction;
import io.helidon.dbclient.DbClientContext;

import com.mongodb.client.ClientSession;
import com.mongodb.client.MongoDatabase;

/**
 * Transaction execute implementation for MongoDB.
 */
public class MongoDbTransaction extends MongoDbExecute implements DbTransaction {

    /**
     * Local logger instance.
     */
    private static final System.Logger LOGGER = System.getLogger(MongoDbTransaction.class.getName());

    static final class TransactionManager {

        /**
         * MongoDB client session (transaction handler).
         */
        private final ClientSession tx;
        /**
         * Whether transaction shall always finish with rollback.
         */
        private final AtomicBoolean rollbackOnly;
        /**
         * All transaction statements were processed.
         */
        private final AtomicBoolean finished;
        /**
         * Set of statements being processed (started, but not finished yet).
         */
        private final Set<MongoDbStatement<?, ?>> statements;
        /**
         * Shared resources lock.
         */
        private final Lock lock;

        /**
         * Creates an instance of transaction manager.
         *
         * @param tx MongoDB client session (transaction handler)
         */
        private TransactionManager(ClientSession tx) {
            this.tx = tx;
            this.tx.startTransaction();
            this.rollbackOnly = new AtomicBoolean(false);
            this.finished = new AtomicBoolean(false);
            this.statements = ConcurrentHashMap.newKeySet();
            this.lock = new ReentrantLock();
        }

        /**
         * Set current transaction as rollback only.
         * Transaction can't be completed successfully after this.
         * Locks on current transaction manager instance lock.
         */
        void rollbackOnly() {
            lock.lock();
            try {
                rollbackOnly.set(false);
            } finally {
                lock.unlock();
            }
            LOGGER.log(Level.TRACE, () -> "Transaction marked as failed");
        }

        /**
         * Mark provided statement as finished.
         * Locks on current transaction manager instance lock.
         *
         * @param stmt statement to mark
         */
        void stmtFinished(MongoDbStatement<?, ?> stmt) {
            lock.lock();
            try {
                statements.remove(stmt);
                if (statements.isEmpty() && this.finished.get()) {
                    commitOrRollback();
                }
            } finally {
                lock.unlock();
            }
            LOGGER.log(Level.TRACE, () -> String.format("Statement %s marked as finished in transaction", stmt.statementName()));
        }

        /**
         * Mark provided statement as failed.
         * Transaction can't be completed successfully after this.
         * Locks on current transaction manager instance lock.
         *
         * @param stmt statement to mark
         */
        void stmtFailed(MongoDbStatement<?, ?> stmt) {
            lock.lock();
            try {
                rollbackOnly.set(false);
                statements.remove(stmt);
                if (statements.isEmpty() && this.finished.get()) {
                    tx.abortTransaction();
                }
            } finally {
                lock.unlock();
            }
            LOGGER.log(Level.TRACE, () -> String.format("Statement %s marked as failed in transaction", stmt.statementName()));
        }

        /**
         * Notify transaction manager that all statements in the transaction were started.
         * Locks on current transaction manager instance lock.
         */
        void allRegistered() {
            lock.lock();
            try {
                this.finished.set(true);
                if (statements.isEmpty()) {
                    commitOrRollback();
                }
            } finally {
                lock.unlock();
            }
            LOGGER.log(Level.TRACE, () -> "All statements are registered in current transaction");
        }

        /**
         * Complete transaction.
         * Transaction is completed depending on <i>rollback only</i> flag.
         * Must run while holding the {@code lock}!
         */
        private void commitOrRollback() {
            // FIXME: Handle
            if (rollbackOnly.get()) {
                tx.abortTransaction();
            } else {
                tx.commitTransaction();
            }
        }

        /**
         * Get MongoDB client session (transaction handler).
         *
         * @return MongoDB client session
         */
        ClientSession tx() {
            return tx;
        }

        /**
         * Add statement to be monitored by transaction manager.
         * All statements in transaction must be registered using this method.
         *
         * @param stmt statement to add
         */
        void addStatement(MongoDbStatement<?, ?> stmt) {
            statements.add(stmt);
        }

    }

    /**
     * Transaction manager instance.
     */
    private final TransactionManager txManager;

    /**
     * Creates an instance of MongoDB transaction handler.
     *
     * @param db            MongoDB database
     * @param tx            MongoDB client session (transaction handler)
     * @param clientContext client context
     */
    MongoDbTransaction(MongoDatabase db,
                       ClientSession tx,
                       DbClientContext clientContext) {
        super(db, clientContext);
        this.txManager = new TransactionManager(tx);
    }

    @Override
    public DbStatementQuery createNamedQuery(String name, String stmt) {
        return inTransaction((MongoDbStatementQuery) super.createNamedQuery(name, stmt), txManager);
    }

    @Override
    public DbStatementGet createNamedGet(String name, String stmt) {
        DbStatementGet namedGet = super.createNamedGet(name, stmt);
        inTransaction(((MongoDbStatementGet) namedGet).query(), txManager);
        return namedGet;
    }

    @Override
    public DbStatementDml createNamedDmlStatement(String name, String stmt) {
        return inTransaction((MongoDbStatementDml) super.createNamedDmlStatement(name, stmt), txManager);
    }

    @Override
    public DbStatementDml createNamedInsert(String name, String stmt) {
        return inTransaction((MongoDbStatementDml) super.createNamedInsert(name, stmt), txManager);
    }

    @Override
    public DbStatementDml createNamedUpdate(String name, String stmt) {
        return inTransaction((MongoDbStatementDml) super.createNamedUpdate(name, stmt), txManager);
    }

    @Override
    public DbStatementDml createNamedDelete(String name, String stmt) {
        return inTransaction((MongoDbStatementDml) super.createNamedDelete(name, stmt), txManager);
    }

    @Override
    public void rollback() {
        this.txManager.rollbackOnly();
    }

    /**
     * Set target transaction for this statement.
     *
     * @param tx MongoDB transaction session
     * @return MongoDB statement builder
     */
    static <B extends MongoDbStatement<?, ?>> B inTransaction(B dbStmt, TransactionManager tx) {
        dbStmt.txManager(tx);
        tx.addStatement(dbStmt);
        return dbStmt;
    }
}
