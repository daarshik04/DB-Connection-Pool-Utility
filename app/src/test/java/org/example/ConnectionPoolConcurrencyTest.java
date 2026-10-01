package org.example;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.fail;

import java.sql.Connection;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.Test;

import io.github.cdimascio.dotenv.Dotenv;

public class ConnectionPoolConcurrencyTest {

    Dotenv env = Dotenv.load();

    private PoolConfig createConfig(int poolSize){
        return new PoolConfig(env.get("DB_URL"),
            env.get("DB_USERNAME"),
            env.get("DB_PASSWORD"),
            poolSize);
    }

    @Test 
    void multipleThreadsShouldBorrowAndReturnConnections() throws Exception{

        int poolSize = 3;
        int threadCount = 10;

        ConnectionPool pool = new ConnectionPool(createConfig(poolSize));

        CountDownLatch startSignal = new CountDownLatch(1);
        CountDownLatch finishSignal = new CountDownLatch(threadCount);

        List<Connection> borrowedConnections = new ArrayList<>();

        for(int i=0; i<threadCount; i++){

            Thread thread =new Thread(() ->{
                try{
                    startSignal.await();
                    Connection connection = pool.borrowConnection(5, TimeUnit.SECONDS);

                    synchronized(borrowedConnections){
                        borrowedConnections.add(connection);
                    }
                    Thread.sleep(100);
                    pool.returnConnection(connection);
                } catch (Exception e) {
                    fail(e);
                }finally{
                    finishSignal.countDown();
                }
            });
            thread.start();
        }
        startSignal.countDown();

        finishSignal.await();

        assertEquals(threadCount, borrowedConnections.size());

        pool.shutdown();
        
    }
    
}
