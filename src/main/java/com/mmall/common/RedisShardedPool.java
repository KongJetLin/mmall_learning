package com.mmall.common;

import com.mmall.util.PropertiesUtil;
import redis.clients.jedis.*;
import redis.clients.util.Hashing;
import redis.clients.util.Sharded;

import java.util.ArrayList;
import java.util.List;

/**
redis 分布式连接池
 */
public class RedisShardedPool
{
    private static ShardedJedisPool pool;//分布式的 jedis 连接池，存储jedis实例

    private static Integer maxTotal = Integer.parseInt(PropertiesUtil.getProperty("redis.max.total" , "20"));

    private static Integer maxIdle = Integer.parseInt(PropertiesUtil.getProperty("redis.max.idle","20"));

    private static Integer minIdle = Integer.parseInt(PropertiesUtil.getProperty("redis.min.idle","20"));

    private static Boolean testOnBorrow = Boolean.parseBoolean(PropertiesUtil.getProperty("redis.test.borrow","true"));

    private static Boolean testOnReturn = Boolean.parseBoolean(PropertiesUtil.getProperty("redis.test.return","true"));

    private static String redis1Ip = PropertiesUtil.getProperty("redis1.ip");
    private static Integer redis1Port = Integer.parseInt(PropertiesUtil.getProperty("redis1.port"));
    private static String redis2Ip = PropertiesUtil.getProperty("redis2.ip");
    private static Integer redis2Port = Integer.parseInt(PropertiesUtil.getProperty("redis2.port"));

    //初始化jedis连接池
    private static void initPool()
    {
        JedisPoolConfig config = new JedisPoolConfig();
        config.setMaxTotal(maxTotal);
        config.setMaxIdle(maxIdle);
        config.setMinIdle(minIdle);
        config.setTestOnBorrow(testOnBorrow);
        config.setTestOnReturn(testOnReturn);

        //连接耗尽的时候，是否阻塞，false一旦连接耗尽就会抛出异常，true阻塞直到超时。默认为true。
        config.setBlockWhenExhausted(true);

        JedisShardInfo info1 = new JedisShardInfo(redis1Ip,redis1Port,1000*2);//第一个分布式jedis的配置信息
        JedisShardInfo info2 = new JedisShardInfo(redis2Ip,redis2Port,1000*2);//第二个分布式jedis的配置信息

        List<JedisShardInfo> jedisShardInfoList = new ArrayList<JedisShardInfo>(2);

        jedisShardInfoList.add(info1);
        jedisShardInfoList.add(info2);

        //初始化分布式jedis连接池：JedisPoolConfig、多个jedis的IP地址，端口，超时时间（毫秒）配置集合、选择一致性哈希算法、
        pool = new ShardedJedisPool(config , jedisShardInfoList , Hashing.MURMUR_HASH, Sharded.DEFAULT_KEY_TAG_PATTERN);
    }

    //使用静态代码块初始化jedis连接池
    static {
        initPool();
    }

    //将jedis连接池开放给外部使用：从Jedis连接池获取一个jedis实例
    public static ShardedJedis getJedis()
    {
        return pool.getResource();
    }

    //将jedis放回连接池
    public static void returnBrokenResource(ShardedJedis jedis)
    {
        //若jedis失效，将jedis放回 broken处
        pool.returnBrokenResource(jedis);
    }

    public static void returnResource(ShardedJedis jedis)
    {
        //若jedis没有失效，将jedis放回jedis连接池
        pool.returnResource(jedis);
    }




    public static void main(String[] args) {
        ShardedJedis jedis = pool.getResource();

        for(int i =0;i<10;i++){
            jedis.set("key"+i,"value"+i);
        }
        returnResource(jedis);

//        pool.destroy();//临时调用，销毁连接池中的所有连接
        System.out.println("program is end");
    }
}
