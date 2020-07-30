package com.mmall.controller.backend;

import com.mmall.common.Const;
import com.mmall.common.ServerResponse;
import com.mmall.pojo.User;
import com.mmall.service.IUserService;
import com.mmall.util.CookieUtil;
import com.mmall.util.JsonUtil;
import com.mmall.util.RedisShardedPoolUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

/**
 * 这个类是后台用户相关的类
 * 其实只有一个后台管理员登录的方法
 */
@Controller
@RequestMapping("/manage/user")
public class UserManagerController
{
    @Autowired
    private IUserService iUserService;

    /**
     * 拦截器并不会拦截后台登录的方法，因此后台登录的方法还需进行User验证
     * @param username
     * @param password
     * @param httpServletResponse
     * @param session
     * @return
     */
    @RequestMapping(value="login.do",method = RequestMethod.GET)
    @ResponseBody
    public ServerResponse<User> login(String username, String password, HttpServletResponse httpServletResponse, HttpSession session)
    {
        ServerResponse<User> response = iUserService.login(username, password);
        if(response.isSuccess())
        {
            User user = response.getData();
            //如果这是一个管理员用户，则可以直接登录，返回登录的用户的信息
            if(user.getRole().equals(Const.Role.ROLE_ADMIN))
            {
//                session.setAttribute(Const.CURRENT_USER , user);

                //新增redis共享cookie，session的方式（分布式redis）
                CookieUtil.writeLoginToken(httpServletResponse,session.getId());
                RedisShardedPoolUtil.setEx(session.getId(), JsonUtil.obj2String(response.getData()),Const.RedisCacheExTime.REDIS_SESSION_EXTIME);
                return response;
            }
            else
            {
                return ServerResponse.createByErrorMessage("不是管理员,无法登录");
            }
        }
        return response;
    }
}
