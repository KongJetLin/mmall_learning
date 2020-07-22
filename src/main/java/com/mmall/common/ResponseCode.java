package com.mmall.common;

/**
 * 用于设置状态码以及描述的枚举类
 */
public enum ResponseCode
{
    //先定义各类枚举 的响应数字 及其 状态描述
    SUCCESS(0 , "SUCCESS"),
    ERROR(1 , "ERROR"),
    NEED_LOGIN(10 , "NEED_LOGIN"),
    ILLEGAL_ARGUMENT(2 , "ILLEGAL_ARGUMENT");

    private final int code;//响应数字
    private final String desc;//状态描述

    ResponseCode(int code,String desc){
        this.code = code;
        this.desc = desc;
    }

    public int getCode(){
        return code;
    }
    public String getDesc(){
        return desc;
    }
}
