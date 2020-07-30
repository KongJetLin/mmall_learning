package com.mmall.common;

import com.mmall.util.PropertiesUtil;
import lombok.extern.slf4j.Slf4j;
import org.redisson.Redisson;
import org.redisson.config.Config;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;

/**
 * Redisson 的初始化类
 * 我们使用 Redisson来操作redis，这样更加方便！
 */
@Component //同样需要将 Redisson初始化类的对象放入Spring容器
@Slf4j
public class RedissonManager
{
    private Config config = new Config();

    private Redisson redisson = null;

    //将初始化好的 redisson 开放给外部使用
    public Redisson getRedisson()
    {
        return redisson;
    }

    //获取分布式redis的IP与端口
    private static String redis1Ip = PropertiesUtil.getProperty("redis1.ip");
    private static Integer redis1Port = Integer.parseInt(PropertiesUtil.getProperty("redis1.port"));
    private static String redis2Ip = PropertiesUtil.getProperty("redis2.ip");
    private static Integer redis2Port = Integer.parseInt(PropertiesUtil.getProperty("redis2.port"));

    //初始化Redisson，可以使用静态块，也可以使用 @PostConstruct，即 RedissonManager 在执行完构造器后会执行 init()
    //Redisson不支持一致性算法进行redis的分布式
    @PostConstruct
    private void init()
    {
        try
        {
            //这里需要使用：host:port 的格式，使得 redisson 连接到 redis
            config.useSingleServer().setAddress(new StringBuilder().append(redis1Ip).append(":").append(redis1Port).toString());
            //使用 config 初始化 Redisson
            redisson = (Redisson) Redisson.create(config);
            log.info("初始化Redisson结束");
        }
        catch (Exception e)
        {
            log.error("redisson init error",e);
        }
    }

}
