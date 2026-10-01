package org.example;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.concurrent.TimeUnit;

import io.github.cdimascio.dotenv.Dotenv;

public class Main {

    public static void main(String[] args) throws Exception {

        Dotenv env = Dotenv.load();
        String url = env.get("DB_URL");
        String username = env.get("DB_USERNAME");
        String password = env.get("DB_PASSWORD");

        // 1. Create pool configuration
        PoolConfig config = new PoolConfig(
            url,
            username,
            password,
            10
        );

        // 2. Create connection pool
        ConnectionPool pool = new ConnectionPool(config);

        // 3. Borrow a connection from the pool
        Connection connection = pool.borrowConnection(5, TimeUnit.SECONDS);

        try {
            // 4. Create SQL statement
            Statement statement = connection.createStatement();

            // 5. Execute query
            ResultSet resultSet =
                statement.executeQuery("SELECT * FROM users");

            // 6. Read results
            while (resultSet.next()) {
                System.out.println(
                    resultSet.getInt("id") + " "
                    + resultSet.getString("name")
                );
            }

            // Close JDBC resources
            resultSet.close();
            statement.close();

        } finally {
            // 7. Return connection to the pool
            pool.returnConnection(connection);
        }

        // 8. Shut down pool
        pool.shutdown();
    }
}
