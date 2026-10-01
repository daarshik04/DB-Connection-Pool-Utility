package org.example;

public class PoolConfig {
    private final String jdbcUrl;
    private final String username;
    private final String password;
    private final int poolSize;

    public PoolConfig(
        String jdbcUrl,
        String username,
        String password,
        int poolSize
    ){
        this.jdbcUrl = jdbcUrl;
        this.username = username;
        this.password = password;
        this.poolSize = poolSize;
    };

    public String getJdbcUrl(){
        return jdbcUrl;
    }

    public String getUsername(){
        return username;
    }

    public String getPassword(){
        return password;
    }

    public int getPoolSize(){
        return poolSize;
    }


}
