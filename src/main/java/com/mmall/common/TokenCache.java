package com.mmall.common;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.TimeUnit;

public class TokenCache
{
    //先声明日志
    private static Logger logger = LoggerFactory.getLogger(TokenCache.class);
    public static final String TOKEN_PREFIX = "token_";

    /**
     * 声明一个静态的内存块（guava），用于缓存 token。
     * 声明缓存初始化容量为1000，设置缓存最大值为10000，超过这个值，会使用LRU算法，LRU算法会将最少使用的缓存数据清除。
     *  声明缓存内数据的有效时间为12小时.
      */
    private static LoadingCache<String , String> localCache = CacheBuilder.newBuilder().initialCapacity(10000).expireAfterAccess(12,TimeUnit.HOURS)
            .build(new CacheLoader<String, String>()
            {
                //默认的数据加载实现,当调用get取值的时候,如果key没有对应的值,就调用这个方法进行加载.
                //如果key没有对应的值，就返回 “null” 字符串
                @Override
                public String load(String key) throws Exception
                {
                    return "null";//写为字符串的 null ，避免后面比较的时候可能出现的空指针异常
                }
            });

    //设置数据到 guava 缓存的方法
    public static void setKey(String key , String value)
    {
        localCache.put(key , value);
    }

    //获取 guava 中某个缓存数据的方法
    public static String getKey(String key)
    {
        String value = null;
        try
        {
            value = localCache.get(key);
            //如果value的值是字符串 "null"（前面设置找不到数据返回字符串的null，即此时 value="null"），返回null
            if(value.equals("null"))
                return null;
            return value;
        }catch (Exception e)
        {//出现异常，在日志打印处理
            logger.error("localCache get an error");
        }
        return null;//如果前面没有返回value值，则这里返回null
    }
}
