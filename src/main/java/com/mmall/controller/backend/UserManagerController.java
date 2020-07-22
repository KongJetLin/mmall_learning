package com.mmall.controller.backend;

import com.mmall.common.Const;
import com.mmall.common.ServerResponse;
import com.mmall.pojo.User;
import com.mmall.service.IUserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;

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

    @RequestMapping(value="login.do",method = RequestMethod.POST)
    @ResponseBody
    public ServerResponse<User> login(String username, String password, HttpSession session)
    {
        ServerResponse<User> response = iUserService.login(username, password);
        if(response.isSuccess())
        {
            User user = response.getData();
            //如果这是一个管理员用户，则可以直接登录，返回登录的用户的信息
            if(user.getRole().equals(Const.Role.ROLE_ADMIN))
            {
                session.setAttribute(Const.CURRENT_USER , user);
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
