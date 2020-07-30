package com.mmall.controller.portal;

import com.mmall.common.Const;
import com.mmall.common.ResponseCode;
import com.mmall.common.ServerResponse;
import com.mmall.pojo.User;
import com.mmall.service.IUserService;
import com.mmall.util.CookieUtil;
import com.mmall.util.JsonUtil;
import com.mmall.util.RedisShardedPoolUtil;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

/**
 * 这个类是前台用户相关的类,
 * 我们这里使用 SpringSession 的方式来实现单点登录
 */
@Controller
@RequestMapping("/user/springsession/")
public class UserSpringSessionController
{
    @Autowired
    private IUserService iUserService;

    /**
     * 用户登录
     * @param username
     * @param password
     * @param session
     * @param httpServletResponse
     * @return
     */
    @RequestMapping(value = "login.do",method = RequestMethod.GET)
    @ResponseBody
    public ServerResponse<User> login(String username , String password , HttpSession session , HttpServletResponse httpServletResponse)
    {
        ServerResponse<User> response = iUserService.login(username, password);

        if(response.isSuccess())
        {
            //这里使用最开始session存储用户信息的方式
            session.setAttribute(Const.CURRENT_USER , response.getData());

//            CookieUtil.writeLoginToken(httpServletResponse , session.getId());//将保存有JSESSIONID的cookie写回浏览器
//            RedisShardedPoolUtil.setEx(session.getId() , JsonUtil.obj2String(response.getData()) , Const.RedisCacheExTime.REDIS_SESSION_EXTIME);
        }
        return response;
    }

    /**
     * 用户登出
     * @param httpServletRequest
     * @param httpServletResponse
     * @return
     */
    @RequestMapping(value = "logout.do",method = RequestMethod.GET)
    @ResponseBody
    public ServerResponse<String> logout(HttpServletRequest httpServletRequest , HttpServletResponse httpServletResponse , HttpSession session)
    {
//        String loginToken = CookieUtil.readLoginToken(httpServletRequest);
//        CookieUtil.delLoginToken(httpServletRequest , httpServletResponse);
//        RedisShardedPoolUtil.del(loginToken);
        //使用最开始从session获取用户信息的方式
        session.removeAttribute(Const.CURRENT_USER);

        return ServerResponse.createBySuccessMessage("退出成功");
    }

    /**
     * 登录成功后，获取登录用户信息
     * @param httpServletRequest
     * @return
     */
    @RequestMapping(value = "get_user_info.do",method = RequestMethod.GET)
    @ResponseBody
    public ServerResponse<User> getUserInfo(HttpServletRequest httpServletRequest , HttpSession session)
    {
//        String loginToken = CookieUtil.readLoginToken(httpServletRequest);
//        if(StringUtils.isEmpty(loginToken))
//        {
//            return ServerResponse.createByErrorMessage("用户未登录，无法获取当前用户的信息");
//        }
//        String userJsonStr = RedisShardedPoolUtil.get(loginToken);
//        User user = JsonUtil.string2Obj(userJsonStr , User.class);

        User user = (User)session.getAttribute(Const.CURRENT_USER);
        if(user != null)
        {
            return ServerResponse.createBySuccess(user);
        }
        return ServerResponse.createByErrorMessage("用户未登录，无法获取当前用户的信息");
    }

}
