package com.mmall.common;

import com.google.common.collect.Sets;

import java.util.Set;

/**
 * 常量类
 * 说明：如果需要使用到状态码来显示相应的汉字，就使用枚举 enum
 * 如果仅仅是使用常量，我们可以定义接口（多个常量，表示这一组常量属于某个用途）内部的常量，或者是仅仅定义一个常量（单独使用）
 */
public class Const
{
    //定义用户对象 User 名的常量
    public static final String CURRENT_USER = "currentUser";

    //定义 type 的时候使用，判断是用户名还是email
    public static final String EMAIL = "email";
    public static final String USERNAME = "username";

    //定义2个用户类型常量的接口
    public interface Role
    {
        int ROLE_CUSTOMER = 0;//普通用户
        int ROLE_ADMIN = 1;//管理员
    }

    //定义商品状态（是否在线）的枚举
    public enum ProductStatusEnum{
        ON_SALE(1 , "在线");
        private String value;
        private int code;
        ProductStatusEnum(int code,String value){
            this.code = code;
            this.value = value;
        }

        public String getValue() {
            return value;
        }

        public int getCode() {
            return code;
        }
    }

    //用于指定排序参数的接口，前端在查询产品的时候，可能会传递一个排序参数
    public interface productListOrderBy
    {
        Set<String> PRICE_ASC_DESC = Sets.newHashSet("price_desc" , "price_asc");
    }

    //购物车相应的常量
    public interface Cart
    {
        int CHECKED = 1;//即购物车选中状态
        int UN_CHECKED = 0;//购物车中未选中状态

        //用于表示选择的商品数量是否大于商品的库存
        String LIMIT_NUM_FAIL = "LIMIT_NUM_FAIL";
        String LIMIT_NUM_SUCCESS = "LIMIT_NUM_SUCCESS";
    }

    //声明订单状态的枚举
    public enum OrderStatusEnum
    {
        CANCELED(0,"已取消"),
        NO_PAY(10,"未支付"),
        PAID(20,"已付款"),
        SHIPPED(40,"已发货"),
        ORDER_SUCCESS(50,"订单完成"),
        ORDER_CLOSE(60,"订单关闭");

        OrderStatusEnum(int code , String value)
        {
            this.code = code;
            this.value = value;
        }

        private String value;
        private int code;

        public String getValue()
        {
            return value;
        }

        public int getCode()
        {
            return code;
        }

        public static OrderStatusEnum codeOf(int code)
        {
            for (OrderStatusEnum orderStatusEnum : values())
            {
                if(orderStatusEnum.getCode() == code)
                    return orderStatusEnum;
            }
            throw new RuntimeException("么有找到对应的枚举");
        }
    }

    //保存 支付宝交易状态常量 的接口，支付宝的交易状态由回调函数返回给我们的代码
    public interface AlipayCallback
    {
        //等待买家付款
        String TRADE_STATUS_WAIT_BUYER_PAY = "WAIT_BUYER_PAY";
        //交易成功
        String TRADE_STATUS_TRADE_SUCCESS = "TRADE_SUCCESS";

        //返回给回调函数的信息：我们接收到回调信息，验证回调信息提供，并处理成功
        String RESPONSE_SUCCESS = "success";
        //支付宝回调信息有问题，我们没有处理
        String RESPONSE_FAILED = "failed";
    }

    //支付平台
    public enum PayPlatformEnum{
        ALIPAY(1,"支付宝");

        PayPlatformEnum(int code,String value){
            this.code = code;
            this.value = value;
        }
        private String value;
        private int code;

        public String getValue() {
            return value;
        }

        public int getCode() {
            return code;
        }
    }

    //设置支付类型（订单的 payment_type），用枚举是因为后面我们要取出支付方式做汉字展示
    public enum PaymentTypeEnum
    {
        ONLINE_PAY(1,"在线支付");

        PaymentTypeEnum(int code,String value){
            this.code = code;
            this.value = value;
        }
        private String value;
        private int code;

        public String getValue() {
            return value;
        }

        public int getCode() {
            return code;
        }

        //这个方法用于根据code返回相应的枚举类型
        public static PaymentTypeEnum codeOf(int code)
        {
            //对枚举数组 values 进行遍历
            for (PaymentTypeEnum paymentTypeEnum : values())
            {
                if(paymentTypeEnum.getCode() == code)
                    return paymentTypeEnum;
            }
            throw new RuntimeException("么有找到对应的枚举");
        }
    }

//---------------------------------------------------------------二期常量

    //Redis 中各个量的缓存时间
    public interface RedisCacheExTime
    {
        int REDIS_SESSION_EXTIME = 60 * 30;//Session的缓存时间为 30分钟
    }


}
