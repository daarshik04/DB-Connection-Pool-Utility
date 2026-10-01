package org.example;

import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.sql.Connection;

import org.junit.jupiter.api.Test;

public class ConnectionPoolTest {

    @Test 
    void shouldBorrowConfiguredConnection() throws Exception{

        PoolConfig config = new PoolConfig(
            "jdbc:postgresql://localhost:5432/pool_demo",
            "postgres",
            "pgadmin",
            3
        );

        ConnectionPool pool = new ConnectionPool(config);

        Connection conn1 = pool.borrowConnection();
        Connection  conn2 = pool.borrowConnection();
        Connection conn3 = pool.borrowConnection();

        assertNotNull(conn1);
        assertNotNull(conn2);
        assertNotNull(conn3);

        pool.returnConnection(conn1);
        pool.returnConnection(conn3);
        pool.returnConnection(conn2);

        pool.shutdown();

    }
    
}
