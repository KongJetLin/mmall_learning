package com.mmall.util;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Properties;

/**
 * 获取配置文件属性值的工具类
 */
public class PropertiesUtil
{
    //日志
    private static Logger logger = LoggerFactory.getLogger(PropertiesUtil.class);

    private static Properties prop;

    //java的ClassLoader加载的时候，首先加载静态块，用于初始化静态代码块
    static {
        String fileName = "mmall.properties";
        prop = new Properties();

        //使得 Properties 对象加载配置文件
        try
        {
            prop.load(new InputStreamReader(PropertiesUtil.class.getClassLoader().getResourceAsStream(fileName) , "UTF-8"));
        }
        catch (IOException e)
        {
            logger.error("配置文件读取异常" , e);
        }
    }

    //获取配置文件某个属性的方法
    public static String getProperty(String key)
    {
        String property = prop.getProperty(key.trim());
        //属性值为 "" 或者 " " 都将其视为null
        if(StringUtils.isBlank(property))
            return null;
        return property.trim();
    }

    //重载，只不过这里多了一个默认值
    public static String getProperty(String key , String defaultValue)
    {
        String property = prop.getProperty(key.trim());//注意将key前后的空格去除
        //属性值为 "" 或者 " " 都将其视为 defaultValue
        if(StringUtils.isBlank(property))
            property = defaultValue;
        return property.trim();
    }
}
