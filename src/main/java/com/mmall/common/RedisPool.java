package com.mmall.common;

import com.mmall.util.PropertiesUtil;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.JedisPoolConfig;

/**
redis连接池：其实就是封装了jedis连接池并设置其各类属性，从jedis连接池中取出jedis，就可以连接redis服务
 1）jedis就是集成了redis的一些命令操作，封装了redis的java客户端，jedis提供了连接池管理。
    通过redis.clients.jedis.JedisPool来管理，即通过池来管理，通过池对象获取jedis实例，
    然后通过jedis实例直接操作redis服务，剔除了与业务无关的冗余代码。（即java中使用jedis可以直接连接redis服务器，使用jedis服务）

 */
public class RedisPool
{
    private static JedisPool pool;//jedis连接池，存储jedis实例
    //jedis连接池中和redis服务器最大的连接数
    private static Integer maxTotal = Integer.parseInt(PropertiesUtil.getProperty("redis.max.total" , "20"));
    //在jedispool中最大的idle状态(空闲的)的jedis实例的个数
    private static Integer maxIdle = Integer.parseInt(PropertiesUtil.getProperty("redis.max.idle","20"));
    //在jedispool中最小的idle状态(空闲的)的jedis实例的个数
    private static Integer minIdle = Integer.parseInt(PropertiesUtil.getProperty("redis.min.idle","20"));
    //在向jedis连接池borrow一个jedis实例的时候，是否要进行验证操作，如果赋值true，则必须验证获得jedis实例是可用的，才会得到这个实例，因此得到的jedis实例肯定是可以用的。
    private static Boolean testOnBorrow = Boolean.parseBoolean(PropertiesUtil.getProperty("redis.test.borrow","true"));
    //在return一个jedis实例的时候，是否要进行验证操作，如果赋值true。则放回jedispool的jedis实例肯定是可以用的。
    private static Boolean testOnReturn = Boolean.parseBoolean(PropertiesUtil.getProperty("redis.test.return","true"));

    private static String redisIp = PropertiesUtil.getProperty("redis.ip");
    private static Integer redisPort = Integer.parseInt(PropertiesUtil.getProperty("redis.port"));

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

        //初始化jedis连接池：JedisPoolConfig、redis的IP地址，端口，超时时间（毫秒）
        pool = new JedisPool(config,redisIp,redisPort,1000*2);
    }

    //使用静态代码块初始化jedis连接池
    static {
        initPool();
    }

    //将jedis连接池开放给外部使用：从Jedis连接池获取一个jedis实例
    public static Jedis getJedis()
    {
        return pool.getResource();
    }

    //将jedis放回连接池
    public static void returnBrokenResource(Jedis jedis)
    {
        //若jedis失效，将jedis放回 broken处
        pool.returnBrokenResource(jedis);
    }

    public static void returnResource(Jedis jedis)
    {
        //若jedis没有失效，将jedis放回jedis连接池
        pool.returnResource(jedis);
    }

    //测试
//    public static void main(String[] args) {
//        Jedis jedis = pool.getResource();
//        jedis.set("geelykey","geelyvalue");
//        returnResource(jedis);
//
//        pool.destroy();//临时调用，销毁连接池中的所有连接
//        System.out.println("program is end");
//    }
}
