package com.battlelab;

import redis.clients.jedis.HostAndPort;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.Protocol;
import redis.clients.jedis.exceptions.JedisException;

import java.util.Map;

public class JedisHelper {
    private static final String KEY_REDIS_HOST = "builderRedisHost";
    private static final String KEY_REDIS_PORT = "builderRedisPort";
    private static final String KEY_REDIS_PASSWORD = "builderRedisPassword";
    private static final String KEY_REDIS_DATABASE = "builderRedisDatabase";
    private static final int DEFAULT_REDIS_CONNECTION_TIMEOUT = 30 * 1000;
    private static final int DEFAULT_REDIS_SO_TIMEOUT = 30 * 1000;

    public static Jedis buildJedis() {
        Map<String, String> envs = System.getenv();
        String host = envs.get(KEY_REDIS_HOST);
        if (host == null) {
            throw new RuntimeException("必须配置一个有效的redis host");
        }
        String portString = envs.get(KEY_REDIS_PORT);
        if (portString == null) {
            throw new RuntimeException("必须配置一个有效的redis port");
        }
        int port = Integer.parseInt(portString);

        String dbString = envs.get(KEY_REDIS_DATABASE);
        if (dbString == null) {
            throw new RuntimeException("必须配置一个有效的redis db");
        }
        int db = Integer.parseInt(dbString);

        String password = envs.get(KEY_REDIS_PASSWORD);

        HostAndPort hostAndPort = new HostAndPort(host,port);

        Jedis jedis = new Jedis(hostAndPort.getHost(),
                hostAndPort.getPort(),
                Protocol.DEFAULT_TIMEOUT,
                Protocol.DEFAULT_TIMEOUT,
                false);
        try {
            jedis.connect();
            if (password != null) {
                jedis.auth(password);
            }
            if (db != 0) {
                jedis.select(db);
            }
        } catch (JedisException je) {
            jedis.close();
            throw new RuntimeException("redis 链接失败", je);
        }
        return jedis;
    }

    public static void destoryJedis(Jedis jedis) {
        if (jedis.isConnected()) {
            try {
                try {
                    jedis.quit();
                } catch (Exception e) {
                }
                jedis.disconnect();
            } catch (Exception e) {
            }
        }
    }
}
