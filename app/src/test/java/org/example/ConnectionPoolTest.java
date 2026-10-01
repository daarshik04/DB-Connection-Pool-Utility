package org.example;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.sql.Connection;

import org.junit.jupiter.api.Test;

import io.github.cdimascio.dotenv.Dotenv;

public class ConnectionPoolTest {

    Dotenv env = Dotenv.load();
    String url = env.get("DB_URL");
    String username = env.get("DB_USERNAME");
    String password = env.get("DB_PASSWORD");

    @Test 
    void shouldBorrowConfiguredConnection() throws Exception{

        

        PoolConfig config = new PoolConfig(
            url,
            username,
            password,
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

    @Test
    void returnedConnectionShouldBeReusable() throws Exception {
    
        PoolConfig config = new PoolConfig(
                url,
                username,
                password,
                1
        );
    
        ConnectionPool pool = new ConnectionPool(config);
    
        Connection first = pool.borrowConnection();
    
        pool.returnConnection(first);
    
        Connection second = pool.borrowConnection();
    
        assertSame(first, second);
    
        pool.returnConnection(second);
        pool.shutdown();
    }

    @Test
    void shouldBlockWhenPoolIsExhausted() throws Exception{

        PoolConfig config = new PoolConfig(
            url,
            username,
            password,
            1
        );

        ConnectionPool pool =new ConnectionPool(config);

        Connection conn1 = pool.borrowConnection();

        Thread thread = new Thread(() ->{
            try{
                pool.borrowConnection();
            }catch(InterruptedException e){
                Thread.currentThread().interrupt();
            }
        });

        thread.start();

        Thread.sleep(500);

        assertTrue(thread.isAlive());

        pool.returnConnection(conn1);
        thread.join(1000);

        assertFalse(thread.isAlive());

        pool.shutdown();
            
    }
    
}
