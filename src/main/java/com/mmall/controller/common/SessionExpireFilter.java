package com.mmall.controller.common;

import com.mmall.common.Const;
import com.mmall.pojo.User;
import com.mmall.util.CookieUtil;
import com.mmall.util.JsonUtil;
import com.mmall.util.RedisPoolUtil;
import org.apache.commons.lang3.StringUtils;

import javax.servlet.*;
import javax.servlet.http.HttpServletRequest;
import java.io.IOException;

/**
用于设置redis中 sessionid:String(User) 键值对过期时间的过滤器.我们这里使用 Servlet 的 Filter 过滤器实现

 注意：我们这里放行所有的“.do”结尾的请求，那么退出“logout.do”也会执行过滤器将redis键值对的时间设置为30分钟，
 但是执行完过滤器后，“logout.do”，又会将 redis 中 相应的键值对删除，那么就算前面重新设置其时间也30分钟也没关系，反正后面会被删除。
 */
public class SessionExpireFilter implements Filter
{

    @Override
    public void init(FilterConfig filterConfig) throws ServletException
    { }

    @Override
    public void doFilter(ServletRequest servletRequest, ServletResponse servletResponse, FilterChain filterChain) throws IOException, ServletException
    {
        //1、先将 servletRequest 转换为 HttpServletRequest（我们需要的是 HttpServletRequest）
        HttpServletRequest httpServletRequest = (HttpServletRequest)servletRequest;
        //2、从HttpServletRequest请求中获取 "mmall_login_token" 的 sessionid 值
        String loginToken = CookieUtil.readLoginToken(httpServletRequest);

        //3、判断logintoken是否为空或者""，如果不为空的话，符合条件，继续拿user信息
        if(StringUtils.isNotEmpty(loginToken))
        {
            //3.1 首先，我们需要从redis将用户信息取出，若用户信息不为空，
            // 我们才可以根据 sessionid 更新 redis 中该用户信息对应的键值对 ：sessionid:String(User) 的过期时间重新设置为30分钟
            String userJsonStr = RedisPoolUtil.get(loginToken);
            User user = JsonUtil.string2Obj(userJsonStr , User.class);
            if(user != null)
            {
                RedisPoolUtil.expire(loginToken , Const.RedisCacheExTime.REDIS_SESSION_EXTIME);
            }
        }
        //过滤器放行。
        filterChain.doFilter(servletRequest,servletResponse);
    }

    @Override
    public void destroy()
    { }
}
