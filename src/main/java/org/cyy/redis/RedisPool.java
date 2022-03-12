package org.cyy.redis;

import org.cyy.config.MyPluginConfig;
import org.cyy.utils.YmlAndPropUtil;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.JedisPoolConfig;

import java.util.Properties;

/**
 * redis连接池配置
 */
public class RedisPool {

    private static JedisPool pool;//jedis连接池
    private static Integer maxTotal;
    private static Integer maxIdle;//在jedispool中最大的idle状态(空闲的)的jedis实例的个数
    private static Integer minIdle;//在jedispool中最小的idle状态(空闲的)的jedis实例的个数


    private static String redisIp;
    private static Integer redisPort;
    private static String username;
    private static String password;

    private static void initPool(){
        JedisPoolConfig config = new JedisPoolConfig();
        config.setMaxTotal(maxTotal);
        config.setMaxIdle(maxIdle);
        config.setMinIdle(minIdle);
        pool = new JedisPool(config,redisIp,redisPort,username,password);
    }

    public static void load(){
        Properties properties = YmlAndPropUtil.loadProperties(MyPluginConfig.redisConfigPath);
        maxTotal = Integer.parseInt(YmlAndPropUtil.getValue(properties,"redis.max.total","5"));
        maxIdle = Integer.parseInt(YmlAndPropUtil.getValue(properties,"redis.max.idle","3"));
        minIdle = Integer.parseInt(YmlAndPropUtil.getValue(properties,"redis.min.idle","2"));
        redisIp = YmlAndPropUtil.getValue(properties,"redis.ip","127.0.0.1");
        redisPort = Integer.parseInt(YmlAndPropUtil.getValue(properties,"redis.port","6379"));
        username = YmlAndPropUtil.getValue(properties,"redis.username");
        password = YmlAndPropUtil.getValue(properties,"redis.password");
        initPool();
    }
    public static Jedis getJedis(){
        return pool.getResource();
    }

    public static void closeRedis(Jedis jedis){
        if(null != jedis){
            jedis.close();
        }
    }

}
