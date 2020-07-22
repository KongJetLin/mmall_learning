package com.mmall.controller.portal;

import com.mmall.common.Const;
import com.mmall.common.ResponseCode;
import com.mmall.common.ServerResponse;
import com.mmall.pojo.User;
import com.mmall.service.IUserService;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;

import javax.servlet.http.HttpSession;

/**
 * 这个类是前台用户相关的类
 */
@Controller
@RequestMapping("/user/")
public class UserController
{
    @Autowired
    private IUserService iUserService;

    /**
     * 用户登录
     * @param username
     * @param password
     * @param session
     * @return
     */
    @RequestMapping(value = "login.do",method = RequestMethod.POST)
    @ResponseBody //将返回的数据序列化为JSON
    public ServerResponse<User> login(String username , String password , HttpSession session)
    {
        ServerResponse<User> response = iUserService.login(username, password);

        //如果登录成功，将用户设置到Session中
        if(response.isSuccess())
        {
            session.setAttribute(Const.CURRENT_USER , response.getData());//将ServerResponse的User对象存储到Session
        }
        //不管有没有登录成功，我们都将ServerResponse 对象 response 返回，这样前端会根据接收的信息展示不同的提示
        return response;
    }

    /**
     * 用户登出
     * @param session
     * @return
     */
    @RequestMapping(value = "logout.do",method = RequestMethod.POST)
    @ResponseBody
    public ServerResponse<String> logout(HttpSession session)
    {
        //将Session中的用户对象删除
        session.removeAttribute(Const.CURRENT_USER);
        //返回一个只有状态码值 “0” （表示登出成功）的 ServerResponse<String> 对象
        return ServerResponse.createBySuccessMessage("退出成功");
    }

    /**
     * 用户注册
     * @param user
     * @return
     */
    @RequestMapping(value = "register.do",method = RequestMethod.POST)
    @ResponseBody
    public ServerResponse<String> register(User user)
    {
        return iUserService.register(user);
    }

    /**
     * 在注册界面，实时监测用户名与密码是否存在（虽然我们在注册的时候有校验，但是这里也需要实时校验）
     * @param str
     * @param type 用于设置是用户名还是密码
     * @return
     */
    @RequestMapping(value = "check_valid.do",method = RequestMethod.POST)
    @ResponseBody
    public ServerResponse<String> checkValid(String str , String type)
    {
        return iUserService.checkValid(str , type);
    }


    /**
     * 登录成功后，获取登录用户信息
     * @param session
     * @return
     */
    @RequestMapping(value = "get_user_info.do",method = RequestMethod.POST)
    @ResponseBody
    public ServerResponse<User> getUserInfo(HttpSession session)
    {
        User user = (User) session.getAttribute(Const.CURRENT_USER);
        if(user != null)
        {
            return ServerResponse.createBySuccess(user);
        }

        return ServerResponse.createByErrorMessage("用户未登录，无法获取当前用户的信息");
    }


    /**
     * 在未登录的状态下忘记密码，点击忘记密码，返回用户的密码提示问题
     * @return
     */
    @RequestMapping(value = "forget_get_question.do",method = RequestMethod.POST)
    @ResponseBody
    public ServerResponse<String> forgetGetQuestion(String username)
    {
        return iUserService.selectQuestion(username);
    }


    /**
     * 提交密码提示问题的答案，yueqian如果密码提示问题正确，会跳转到提交新的密码的页面，
     * 我们需要返回一个token，这个token有时限，防止后面其他用户横向越权，任意修改当前用户密码。（其他用户无法获取token）
     * @param username
     * @param question
     * @param answer
     * @return
     */
    @RequestMapping(value = "forget_check_answer.do",method = RequestMethod.POST)
    @ResponseBody
    public ServerResponse<String> forgetCheckAnswer(String username , String question , String answer)
    {
        return iUserService.checkAnswer(username , question , answer);//检查 问题、问题答案，用户名是否正确
    }

    /**
     * 在未登录的状态下忘记密码，重新提交新的密码（包含token），更新数据库中的密码
     * @param username
     * @param passwordNew
     * @param forgetToken
     * @return
     */
    @RequestMapping(value = "forget_reset_password.do",method = RequestMethod.POST)
    @ResponseBody
    public ServerResponse<String> forgetResetPassword(String username , String passwordNew , String forgetToken)
    {
        return iUserService.forgetResetPassword(username , passwordNew , forgetToken);
    }

    /**
     * 登录状态下修改密码
     * @param session
     * @param passwordOld
     * @param passwordNew
     * @return
     */
    @RequestMapping(value = "reset_password.do",method = RequestMethod.POST)
    @ResponseBody
    public ServerResponse<String> resetPassword(HttpSession session,String passwordOld,String passwordNew)
    {
        User user = (User)session.getAttribute(Const.CURRENT_USER);
        if(user == null)
            return ServerResponse.createByErrorMessage("用户未登录");
        System.out.println("=============="+passwordNew);
        System.out.println("=============="+passwordOld);
        return iUserService.resetPassword(passwordOld , passwordNew ,user);
    }


    /**
     * 更新用户信息的方法
     * @param session
     * @param user
     * @return
     */
    @RequestMapping(value = "update_information.do",method = RequestMethod.POST)
    @ResponseBody
    public ServerResponse<User> update_information(HttpSession session,User user)
    {
        //先检查用户是否登录
        User currentUser = (User)session.getAttribute(Const.CURRENT_USER);
        if(currentUser == null)
            return ServerResponse.createByErrorMessage("用户未登录");

        //我们传递过来的用户没有id和username，需要获取session中的id与username的值填充到传递过来的User对象
        //这种做法也可以避免越权问题，就是其他用户用其他id来更新当前用户信息。
        user.setId(currentUser.getId());
        user.setUsername(currentUser.getUsername());

        ServerResponse<User> response = iUserService.updateInformation(user);
        if(response.isSuccess())
        {
            //注意这个 ServerResponse 内的User对象是没有username的，我们将其存储到Session之前，需要设置username
            response.getData().setUsername(currentUser.getUsername());
            session.setAttribute(Const.CURRENT_USER , response.getData());
        }

        //不管有没有更新成功，都要返回response
        return response;
    }

    /**
     * 获取用户详细信息
     * @param session
     * @return
     */
    @RequestMapping(value = "get_information.do",method = RequestMethod.POST)
    @ResponseBody
    public ServerResponse<User> get_information(HttpSession session)
    {
        User currentUser = (User)session.getAttribute(Const.CURRENT_USER);
        if(currentUser == null)
        {
            //获取用户信息的时候，如果用户没有登录，需要强制登录
            return ServerResponse.createByErrorCodeMessage(ResponseCode.NEED_LOGIN.getCode() , "未登录,需要强制登录status=10");
        }

        return iUserService.getInformation(currentUser.getId());
    }
}
