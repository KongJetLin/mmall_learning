package com.mmall.common;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerExceptionResolver;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.view.json.MappingJacksonJsonView;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 异常处理器，用于处理SpringMVC全局异常。
 */
@Slf4j
@Component //注意将ExceptionResolver全局异常处理类的对象注入Spring容器
public class ExceptionResolver implements HandlerExceptionResolver
{
    @Override
    public ModelAndView resolveException(HttpServletRequest httpServletRequest, HttpServletResponse httpServletResponse, Object o, Exception e)
    {
        //1、打印异常以及发生异常的URI
        log.error("{} Exception",httpServletRequest.getRequestURI(),e);

        /*
        2、我们的项目是前后端分离的，我们不需要像之前一样，将异常信息写入ModelAndView，并通过 ModelAndView 并跳转到相应错误页面展示。
        我们这里将 ModelAndView 转为JSON，然后直接将异常信息写入 ModelAndView，直接将JSON化的 ModelAndView 返回给前端即可
         */
        ModelAndView modelAndView = new ModelAndView(new MappingJacksonJsonView());//将ModelAndView转换为JSONVIEW

        //当使用是jackson2.x的时候使用MappingJackson2JsonView，课程中使用的是1.9。
        //3、这里将异常信息码、相应的异常类型、异常提示写入ModelAndView，通过ModelAndView序列化为JSON，然后通过Request返回给浏览器
        modelAndView.addObject("status",ResponseCode.ERROR.getCode());
        modelAndView.addObject("msg","接口异常,详情请查看服务端日志的异常信息");
        modelAndView.addObject("data",e.toString());//简单的异常
        return modelAndView;
    }
}
