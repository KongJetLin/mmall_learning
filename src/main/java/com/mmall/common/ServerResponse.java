package com.mmall.common;

import org.codehaus.jackson.annotate.JsonIgnore;
import org.codehaus.jackson.map.annotate.JsonSerialize;

import java.io.Serializable;

/**
 * 通用的数据端的响应对象
 * @param <T>
 */
/*
当我们返回的数据没有 status、msg、data中的某几个的时候，如果我们不加以限制，这些没有的字段会以空的格式构造成为JSON，
我们想使得某个对象不存在的时候，返回的JSON数据也没有这个字段.
保证序列化json的时候,如果是null的对象,key也会消失
 */
@JsonSerialize(include = JsonSerialize.Inclusion.NON_NULL)
public class ServerResponse<T> implements Serializable
{
    private int status;//状态码
    private String msg;//提示信息
    private T data;//要返回的信息

    //下面是构造方法，我们将所有的构造方法设置为 私有类型，这样外部便无法new 这个类
    private ServerResponse(int status){
        this.status = status;
    }

    private ServerResponse(int status,String msg,T data){
        this.status = status;
        this.msg = msg;
        this.data = data;
    }
    /*
    如果我们传递进来一个 int和一个String，会调用 ServerResponse(int status,String msg)，而不是ServerResponse(int status,T data)，
    当我们 传递 int 和其他类型的时候，会调用 ServerResponse(int status,T data)。
     */
    private ServerResponse(int status,T data){
        this.status = status;
        this.data = data;
    }

    private ServerResponse(int status,String msg){
        this.status = status;
        this.msg = msg;
    }

//----------------------------------------------------------
    //判断是否响应成功的方法，响应成功会返回 status=0
    @JsonIgnore //这个方法序列化后不会显示在JSON中
    public boolean isSuccess()
    {
        //这里使用枚举类来获取相应的响应状态数字，这样做比较规范
        return this.status == ResponseCode.SUCCESS.getCode();
    }

    //3个参数的get方法
    public int getStatus(){
        return status;
    }
    public T getData(){
        return data;
    }
    public String getMsg(){
        return msg;
    }

 //----------------------------------------------------------------响应成功相关方法

    //当响应成功的时候，返回一个 ServerResponse对象，里面包含状态码
    public static <T> ServerResponse<T> createBySuccess()
    {
        return new ServerResponse<T>(ResponseCode.SUCCESS.getCode());
    }

    //当响应成功的时候，返回一个 ServerResponse对象，里面包含状态码以及一个文本公前端提示使用
    public static <T> ServerResponse<T> createBySuccessMessage(String msg){
        return new ServerResponse<T>(ResponseCode.SUCCESS.getCode(),msg);
    }

    //当响应成功的时候，返回一个 ServerResponse对象，里面包含状态码以及相应的数据
    public static <T> ServerResponse<T> createBySuccess(T data){
        return new ServerResponse<T>(ResponseCode.SUCCESS.getCode(),data);
    }

    //当响应成功的时候，返回一个 ServerResponse对象，里面包含状态码以及相应的数据以及提示文本
    public static <T> ServerResponse<T> createBySuccess(String msg,T data){
        return new ServerResponse<T>(ResponseCode.SUCCESS.getCode(),msg,data);
    }

//--------------------------------------------------------------------响应失败相关方法
    //响应失败，返回失败的状态码以及描述（这里的描述是公共类型错误的描述）
    public static <T> ServerResponse<T> createByError(){
        return new ServerResponse<T>(ResponseCode.ERROR.getCode(),ResponseCode.ERROR.getDesc());
    }

    //响应失败，返回失败的状态码以及信息（具体的错误可能要返回具体的信息）
    public static <T> ServerResponse<T> createByErrorMessage(String errorMessage){
        return new ServerResponse<T>(ResponseCode.ERROR.getCode(),errorMessage);
    }

    //响应失败，返回失败的错误码以及描述（我们此处需要对状态码封装为一个变量，）
    public static <T> ServerResponse<T> createByErrorCodeMessage(int errorCode,String errorMessage){
        return new ServerResponse<T>(errorCode,errorMessage);
    }

}
