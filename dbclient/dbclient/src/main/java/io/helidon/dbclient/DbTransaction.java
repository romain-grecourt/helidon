package io.helidon.dbclient;

public interface DbTransaction extends DbExecute {

    void commit();
    void rollback();
}
