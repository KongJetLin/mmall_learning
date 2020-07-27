package com.mmall.util;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.codehaus.jackson.map.DeserializationConfig;
import org.codehaus.jackson.map.ObjectMapper;
import org.codehaus.jackson.map.SerializationConfig;
import org.codehaus.jackson.map.annotate.JsonSerialize.Inclusion;
import org.codehaus.jackson.type.JavaType;

import java.text.SimpleDateFormat;


/** JSON序列化与反序列化方法（使用到JSON的java工具包 Jackson）
我们之前登录的时候，是获取User对象，并将User对象放入Session，这次我们要将User对象放入Redis缓存，以在多个服务器之间共享。
 由于在向redis中设置键值对的时候，键是String而不是User，因此我们需要将User序列化为字符串；
 同时，在从redis中取出字符串的User的时候，需要将其反序列化为User对象
 */
@Slf4j
public class JsonUtil
{
    //使用 jackson 提供的 ObjectMapper 进行对象的序列化
    private static ObjectMapper objectMapper = new ObjectMapper();
    //用静态块初始化，初始化 ObjectMapper 的序列化参数
    static {
        //将对象的所有字段都列入序列化
        objectMapper.setSerializationInclusion(Inclusion.ALWAYS);

        //取消默认转换timestamps形式（这种转换带时间戳），如果不取消，有没有设置时间格式，会显示1970.01.01到当前时间的毫秒值
        objectMapper.configure(SerializationConfig.Feature.WRITE_DATES_AS_TIMESTAMPS , false);

        //忽略空Bean转json的错误
        objectMapper.configure(SerializationConfig.Feature.FAIL_ON_EMPTY_BEANS , false);

        //所有的日期格式都统一为以下的样式，即yyyy-MM-dd HH:mm:ss
        objectMapper.setDateFormat(new SimpleDateFormat(DateTimeUtil.STANDARD_FORMAT));

        //忽略 在json字符串中存在，但是在java对象中不存在对应属性的情况。防止错误（这时应用与反序列化的过程）
        objectMapper.configure(DeserializationConfig.Feature.FAIL_ON_UNKNOWN_PROPERTIES , false);
    }

    /**
     * 将对象序列化为String
     * @param obj
     * @param <T>
     * @return
     */
    public static <T> String obj2String(T obj)
    {
        if(obj == null)
            return null;

        try
        {
            return obj instanceof String ? (String)obj : objectMapper.writeValueAsString(obj);
        }
        catch (Exception e)
        {
            log.warn("Parse Object to String error",e);
            return null;
        }
    }

    /**
     * 将obj序列化，并格式化！
     * @param obj
     * @param <T>
     * @return
     */
    public static <T> String obj2StringPretty(T obj)
    {
        if(obj == null)
            return null;

        try
        {
            return obj instanceof String ? (String)obj : objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(obj);
        }
        catch (Exception e)
        {
            log.warn("Parse Object to String error",e);
            return null;
        }
    }
//-------------------------------------------------------------反序列化方法
    /**
     * 将字符串 反序列化为指定类型的对象
     * @param str
     * @param clazz
     * @param <T>
     * @return
     */
    public static <T> T string2Obj(String str , Class<T> clazz)
    {
        if(StringUtils.isEmpty(str) || clazz == null)
            return null;
        try
        {//若需要反序列化的结果类型是String，str本身就是String，不需要反序列化，否则，将其反序列化
            return clazz.equals(String.class) ? (T)str : objectMapper.readValue(str , clazz);
        }
        catch (Exception e)
        {
            log.warn("Parse String to Object error",e);
            return null;
        }
    }

    /**
    * 反序列化的时候，如果是List<其他对象>，那么string2Obj(String str , Class<T> clazz)中clazz选List.class或者对象.class都是不对的，
     * 这种现象对于List、Map 等集合都存在，即上面的方法不够用！
     */

    /**
     * 将参数改为 TypeReference，使用：JsonUtil.string2Obj(userListStr, new org.codehaus.jackson.type.TypeReference<List<User>>() {});
     * 这种就可以处理List与Map！
     * @param str
     * @param typeReference
     * @param <T>
     * @return
     */
    public static <T> T string2Obj(String str , org.codehaus.jackson.type.TypeReference<T> typeReference)
    {
        if(StringUtils.isEmpty(str) || typeReference == null)
            return null;
        try
        {
            return (T)(typeReference.getType().equals(String.class) ? (T)str : objectMapper.readValue(str , typeReference));
        }
        catch (Exception e)
        {
            log.warn("Parse String to Object error",e);
            return null;
        }
    }

    /**
     * 这个方法也可以用于反序列化 ，使用：sonUtil.string2Obj(userListStr , List.class , User.class);
     * @param str
     * @param collectionClass ：集合的类型
     * @param elementClasses ：集合内对象的类型，可以放多个对象（数组）
     * @param <T>
     * @return
     */
    public static <T> T string2Obj(String str , Class<?> collectionClass , Class<?> ...elementClasses)
    {
        JavaType javaType = objectMapper.getTypeFactory().constructParametricType(collectionClass , elementClasses);
        try
        {
            return objectMapper.readValue(str , javaType);
        }
        catch (Exception e)
        {
            log.warn("Parse String to Object error",e);
            return null;
        }
    }


 //----------------------------------------------------测试
    public static void main(String[] args)
    {
//        TestPojo testPojo = new TestPojo();
//        testPojo.setName("Geely");
//        testPojo.setId(666);
//
//        String json = "{\"name\":\"Geely\",\"color\":\"blue\",\"id\":666}";
//        TestPojo testPojoObject = JsonUtil.string2Obj(json,TestPojo.class);

//        String test1Json = JsonUtil.obj2String(testPojo);
//        //{"name":"Geely","id":666}
//        log.info("test1Json:{}",test1Json);

//        User u1 = new User();
//        u1 = null;
//        u1.setId(1);
//        u1.setCreateTime(new Date());
//        u1.setEmail("geely@happymmall.com");
//        String user1JsonPretty = JsonUtil.obj2StringPretty(u1);
//        log.info("userJsonPretty:{}",user1JsonPretty);

//        User u2 = new User();
////        u2.setId(2);
////        u2.setEmail("geelyu2@happymmall.com");
////
////        String user1Json = JsonUtil.obj2String(u1);
////        String user1JsonPretty = JsonUtil.obj2StringPretty(u1);
////
////        log.info("userJson:{}" ,user1Json);
////        log.info("userJsonPretty:{}",user1JsonPretty);
////
////        User user = JsonUtil.string2Obj(user1Json , User.class);
////
////        List<User> userList = Lists.newArrayList();
////        userList.add(u1);
////        userList.add(u2);
////
////        String userListStr = JsonUtil.obj2StringPretty(userList);
////        log.info("============");
////        log.info(userListStr);
////
////        /*
////        这里不能单单放User.class，如果放List.class,则被默认转换为 LinkedHashMap
////         */
//////        List<User> userListObj = JsonUtil.string2Obj(userListStr , List.class);
////        List<User> userListObj = JsonUtil.string2Obj(userListStr, new org.codehaus.jackson.type.TypeReference<List<User>>() {});
////
////
////        List<User> userListObj1 = JsonUtil.string2Obj(userListStr , List.class , User.class);

        System.out.println("end");
    }

}
