package org.example;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;

public class ConnectionPool {
    // Responsibilities:

// 1. Create connections
// 2. Store available connections
// 3. Give connection to caller
// 4. Receive returned connection
// 5. Close connections

    private final BlockingQueue<Connection> availableConnections;

    public ConnectionPool(PoolConfig config) throws SQLException{
         this.availableConnections = new ArrayBlockingQueue<>(config.getPoolSize());

         for (int i =0; i<config.getPoolSize(); i++){

            Connection connection = DriverManager.getConnection(
                config.getJdbcUrl(),
                config.getUsername(),
                config.getPassword()
            );

            availableConnections.add(connection);
         }
    }

    public Connection borrowConnection(long timeout, TimeUnit unit) throws InterruptedException{
        return availableConnections.poll(timeout, unit);
    }

    public void returnConnection(Connection connection)
        throws InterruptedException{

            availableConnections.put(connection);
        
    }

    public void shutdown() throws SQLException{
        for(Connection connection : availableConnections){
            connection.close();
        }
    }


}
