package com.mmall.util;

import com.mmall.common.RedisPool;
import lombok.extern.slf4j.Slf4j;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;

/**
这个类用于封装jedis的各种API，
 即我们从RedisPool获取jedis，用于连接redis服务器，在这里提供jedis，封装在redis中的各类操作方法
 （redis原先的方法不够用，我们需要封装自己的方法）
 */
@Slf4j
public class RedisPoolUtil
{
    /**
     * redis的set方法，这里封装这个方法，使其可以处理异常
     * @param key
     * @param value
     * @return
     */
    public static String set(String key , String value)
    {
        Jedis jedis = null;
        String result = null;

        //从Jedis连接池获取一个jedis对象，用于与redis服务器交互
        try
        {
            jedis = RedisPool.getJedis();
            result = jedis.set(key , value);
        }
        catch (Exception e)
        {//如果在向redis添加键值对的时候出现异常，说明jedis有问题
            log.error("set key:{} value:{} error",key,value,e);//打印日志
            RedisPool.returnBrokenResource(jedis);//将有问题的jedis放入 Broken
            return result;
        }
        //如果没问题，将jedis归还jedis连接池
        RedisPool.returnResource(jedis);
        return result;
    }

    /**
     * 封装redis的get方法
     * @param key
     * @return
     */
    public static String get(String key)
    {
        Jedis jedis = null;
        String result = null;
        try
        {
            jedis = RedisPool.getJedis();
            result = jedis.get(key);
        } catch (Exception e)
        {
            log.error("get key:{} error",key,e);//同样，出现异常者打印日志以及相应的key值
            RedisPool.returnBrokenResource(jedis);
            return result;
        }
        RedisPool.returnResource(jedis);
        return result;
    }

    /**
     * redis的删除方法的封装，注意删除方法返回值是Long
     * @param key
     * @return
     */
    public static Long del(String key)
    {
        Jedis jedis = null;
        Long result = null;
        try
        {
            jedis = RedisPool.getJedis();
            result = jedis.del(key);
        } catch (Exception e)
        {
            log.error("del key:{} error",key,e);//同样，出现异常者打印日志以及相应的key值
            RedisPool.returnBrokenResource(jedis);
            return result;
        }
        RedisPool.returnResource(jedis);
        return result;
    }

    /**
     * 设置一个键值对的同时，设置其过期时间，exTime的单位是秒
     * @param key
     * @param value
     * @param exTime
     * @return
     */
    public static String setEx(String key , String value , int exTime)
    {
        Jedis jedis = null;
        String result = null;

        try
        {
            jedis = RedisPool.getJedis();
            result = jedis.setex(key , exTime , value);
        }
        catch (Exception e)
        {
            log.error("setex key:{} value:{} exTime:{} error",key,value,exTime,e);
            RedisPool.returnBrokenResource(jedis);
            return result;
        }
        RedisPool.returnResource(jedis);
        return result;
    }

    /**
     * 设置key的有效期，单位是秒
     * @param key
     * @param exTime
     * @return
     */
    public static Long expire(String key , int exTime)
    {
        Jedis jedis = null;
        Long result = null;

        try
        {
            jedis = RedisPool.getJedis();
            jedis.expire(key , exTime);//设置某个键的过期时间
        }
        catch (Exception e)
        {
            log.error("expire key:{} exTime:{} error" ,key,exTime,e);
            RedisPool.returnBrokenResource(jedis);
            return result;
        }
        RedisPool.returnResource(jedis);
        return result;
    }

    //测试
    public static void main(String[] args)
    {
        Jedis jedis = RedisPool.getJedis();

        RedisPoolUtil.set("keyTest","value");

        String value = RedisPoolUtil.get("keyTest");

        RedisPoolUtil.setEx("keyex","valueex",60*10);

        RedisPoolUtil.expire("keyTest",60*20);

        RedisPoolUtil.del("keyTest");


        String aaa = RedisPoolUtil.get(null);
        System.out.println(aaa);

        System.out.println("end");


    }
}
