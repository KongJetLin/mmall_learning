package com.mmall.controller.portal;

import com.mmall.common.Const;
import com.mmall.common.RedisPool;
import com.mmall.common.ResponseCode;
import com.mmall.common.ServerResponse;
import com.mmall.pojo.User;
import com.mmall.service.IUserService;
import com.mmall.util.CookieUtil;
import com.mmall.util.JsonUtil;
import com.mmall.util.RedisPoolUtil;
import com.mmall.util.RedisShardedPoolUtil;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
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
    @RequestMapping(value = "login.do",method = RequestMethod.GET)
    @ResponseBody //将返回的数据序列化为JSON
    public ServerResponse<User> login(String username , String password , HttpSession session , HttpServletResponse httpServletResponse)
    {
//        //测试全局异常
//        int i = 0;
//        int j = 666/i;

        ServerResponse<User> response = iUserService.login(username, password);

        //如果登录成功，将用户设置到Session中
        if(response.isSuccess())
        {
//            session.setAttribute(Const.CURRENT_USER , response.getData());//将ServerResponse的User对象存储到Session
            /**
            如果登录成功，我们不再将用户信息User对象存储到Session，
             1）而是将User对象序列化为字符串，随后按键值对的形式：session的id ： User序列化后的字符串 的形式，将其存储到redis，并设置过期时间。
            2）同时，将session对应的id：JSESSIONID 通过 response 写回客户端浏览器，并将这个cookie命名为："mmall_login_token"

             2步：将 sessionid 写入redis缓存、将保存sessionid 的cookie通过response返回给客户端浏览器！
             */
            CookieUtil.writeLoginToken(httpServletResponse , session.getId());//将保存有JSESSIONID的cookie写回浏览器
            RedisShardedPoolUtil.setEx(session.getId() , JsonUtil.obj2String(response.getData()) , Const.RedisCacheExTime.REDIS_SESSION_EXTIME);
        }
        //不管有没有登录成功，我们都将ServerResponse 对象 response 返回，这样前端会根据接收的信息展示不同的提示
        return response;
    }

    /**
     * 用户登出
     * @param httpServletRequest
     * @param httpServletResponse
     * @return
     */
    @RequestMapping(value = "logout.do",method = RequestMethod.POST)
    @ResponseBody
    public ServerResponse<String> logout(HttpServletRequest httpServletRequest , HttpServletResponse httpServletResponse)
    {
        /**
        在登出的时候，先通过 delLoginToken() 方法，通过响应将客户端的保存 sessionid 的cookie删除
         我们同样读取请求中的"mmall_login_token"cookie的值（必须在删除前获取），将redis中存储的用户信息也删除！
         删除：1）存储sessionid的cookie；2）通过 sessionid:User 键值对存储在redis的用户信息
         */
        String loginToken = CookieUtil.readLoginToken(httpServletRequest);//先获取sessionid
        CookieUtil.delLoginToken(httpServletRequest , httpServletResponse);//将存储sessionid的cookie设置为过期
        RedisShardedPoolUtil.del(loginToken);//根据sessionid，将redis中的用户信息也删除

//        //将Session中的用户对象删除
//        session.removeAttribute(Const.CURRENT_USER);

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
     * @param httpServletRequest
     * @return
     */
    @RequestMapping(value = "get_user_info.do",method = RequestMethod.POST)
    @ResponseBody
    public ServerResponse<User> getUserInfo(HttpServletRequest httpServletRequest)
    {
//        User user = (User) session.getAttribute(Const.CURRENT_USER);
        /**
        我们不再从session中获取user对象，而是从请求中获取名为 "mmall_login_token" 的cookie的值 sessionid，即登录时的sessionid，
         这里将其称之为loginToken。
         */
        String loginToken = CookieUtil.readLoginToken(httpServletRequest);
        if(StringUtils.isEmpty(loginToken))
        {
            return ServerResponse.createByErrorMessage("用户未登录，无法获取当前用户的信息");
        }

        //如果获取到sessionid，从redis将User对象字符串取出，并反序列化
        String userJsonStr = RedisShardedPoolUtil.get(loginToken);//先通过jedis将相应的User对象字符串获取到
        User user = JsonUtil.string2Obj(userJsonStr , User.class);//再将User字符反序列化为User对象
        if(user != null)
        {
            return ServerResponse.createBySuccess(user);
        }
        //也有可能因为 redis 中User字符串过期，这里无法获取到，那么也显示未登录
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
    public ServerResponse<User> update_information(HttpServletRequest httpServletRequest,User user)
    {
        //先检查用户是否登录
        String loginToken = CookieUtil.readLoginToken(httpServletRequest);
        if(StringUtils.isEmpty(loginToken))
        {
            return ServerResponse.createByErrorMessage("用户未登录，无法获取当前用户的信息");
        }
        String userJsonStr = RedisShardedPoolUtil.get(loginToken);
        User currentUser = JsonUtil.string2Obj(userJsonStr , User.class);
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
//            session.setAttribute(Const.CURRENT_USER , response.getData());

            RedisShardedPoolUtil.setEx(loginToken , JsonUtil.obj2String(response.getData()) , Const.RedisCacheExTime.REDIS_SESSION_EXTIME);
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
    public ServerResponse<User> get_information(HttpServletRequest httpServletRequest)
    {
        String loginToken = CookieUtil.readLoginToken(httpServletRequest);
        if(StringUtils.isEmpty(loginToken))
        {
            return ServerResponse.createByErrorMessage("用户未登录，无法获取当前用户的信息");
        }
        String userJsonStr = RedisShardedPoolUtil.get(loginToken);
        User currentUser = JsonUtil.string2Obj(userJsonStr , User.class);
        if(currentUser == null)
        {
            //获取用户信息的时候，如果用户没有登录，需要强制登录
            return ServerResponse.createByErrorCodeMessage(ResponseCode.NEED_LOGIN.getCode() , "未登录,需要强制登录status=10");
        }

        return iUserService.getInformation(currentUser.getId());
    }
}
