package io.helidon.examples.dbclient.jdbc;

import java.sql.Connection;

import io.helidon.config.Config;
import io.helidon.dbclient.DbClientException;
import io.helidon.dbclient.jdbc.ConnectionPool;
import java.sql.SQLException;

import oracle.ucp.jdbc.PoolDataSource;
import oracle.ucp.jdbc.PoolDataSourceFactory;

/**
 * UCP connection pool.
 */
public class UCPConnectionPool implements ConnectionPool {

    private final PoolDataSource pds;
    private final String user;
    private final String password;
    private final String url;

    UCPConnectionPool(Config config) {
        user = config.get("ucp.username").asString().orElse("user");
        password = config.get("ucp.password").asString().orElse("password");
        url =  config.get("ucp.url").asString().orElse("jdbc:oracle:thin:@localhost:1521/xe");
        pds = PoolDataSourceFactory.getPoolDataSource();
    }

    @Override
    public Connection connection() {
        try {
            pds.setConnectionFactoryClassName("oracle.jdbc.pool.OracleDataSource");
            pds.setURL(url);
            pds.setUser(user);
            pds.setPassword(password);
            pds.setConnectionPoolName("JDBC_UCP_POOL");
            pds.setInitialPoolSize(1);
            pds.setMinPoolSize(1);
            pds.setMaxPoolSize(3);
            pds.setConnectionWaitTimeout(10);
            return pds.getConnection();
        } catch (SQLException ex) {
            throw new DbClientException(
                    String.format("Failed to create a connection to %s", pds.getURL()), ex);
        }
    }

}
