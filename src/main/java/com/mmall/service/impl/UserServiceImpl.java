package com.mmall.service.impl;

import com.mmall.common.Const;
import com.mmall.common.ServerResponse;
import com.mmall.common.TokenCache;
import com.mmall.dao.UserMapper;
import com.mmall.pojo.User;
import com.mmall.service.IUserService;
import com.mmall.util.MD5Util;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service("iUserService")
public class UserServiceImpl implements IUserService
{
    @Autowired
    private UserMapper userMapper;

    @Override
    public ServerResponse<User> login(String username, String password)
    {
        /**这里将用户名与用户名+密码分开查询，是为了分开验证2类状态*/

        //先根据用户名查询用户数量：0-不存在这个用户，1-存在
        int resultCount = userMapper.checkUsername(username);
        if(resultCount == 0)
            return ServerResponse.createByErrorMessage("用户名不存在");

        //我们注册的时候，密码被MD5加密，因此这里password应该比较的是加密后的值
        String md5Password = MD5Util.MD5EncodeUtf8(password);

        //用户存在的时候，检查用户名与密码是否正确
        User user = userMapper.selectLogin(username, md5Password);
        if(user == null)
            return ServerResponse.createByErrorMessage("密码错误");

        //如果用户名与密码都正确，将查询出的用户封装到 ServerResponse并返回
        user.setPassword(org.apache.commons.lang3.StringUtils.EMPTY);//将用户的密码置空
        return ServerResponse.createBySuccess("登录成功" , user);
    }

    @Override
    public ServerResponse<String> register(User user)
    {
        //首先，注册的时候检查 用户名 与 邮箱是否已经存在
        //复用下面的 checkValid() 方法进行用户名与邮箱的校验
        ServerResponse<String> validResponse = this.checkValid(user.getUsername(), Const.USERNAME);
        if(!validResponse.isSuccess())
            return validResponse;//校验失败，直接返回保存失败信息的 ServerResponse 对象

        validResponse = this.checkValid(user.getEmail(), Const.EMAIL);
        if(!validResponse.isSuccess())
            return validResponse;

        //如果用户名和邮箱都不存在，说明可以注册，下面堆User对象进行设置

        //1、注册的时候需要设置用户的角色
        user.setRole(Const.Role.ROLE_CUSTOMER);
        //2、对明文密码进行MD5加密
        user.setPassword(MD5Util.MD5EncodeUtf8(user.getPassword()));

        //将设置好的User对象插入数据库
        int responseCount = userMapper.insert(user);//返回影响的数据的行数
        if(responseCount == 0)
            return ServerResponse.createByErrorMessage("注册失败");//有可能是数据库的原因
        return ServerResponse.createBySuccessMessage("注册成功");
    }

    @Override
    public ServerResponse<String> checkValid(String str , String type)
    {
        //首先校验 type 是否为空，注意StringUtils.isNotEmpty()，对于 " "，判断其不为空
        //而我们需要在传递一个 " "的时候判断其为空，使用 StringUtils.isNotBlank()
        if(org.apache.commons.lang3.StringUtils.isNotBlank(type))
        {
            if(Const.USERNAME.equals(type))
            {
                int responseCount = userMapper.checkUsername(str);
                if(responseCount > 0)
                    return ServerResponse.createByErrorMessage("用户名已存在");
            }

            if(Const.EMAIL.equals(type))
            {
                int responseCount = userMapper.checkEmail(str);
                if(responseCount > 0)
                    return ServerResponse.createByErrorMessage("email已存在");
            }
        }
        else
        {
            return ServerResponse.createByErrorMessage("参数错误");
        }
        //校验都通过，校验成功（用户或email不存在）
        return ServerResponse.createBySuccessMessage("校验成功");
    }


    //返回忘记密码时，用于返回用户的密码提示问题
    @Override
    public ServerResponse<String> selectQuestion(String username)
    {
        ServerResponse<String> validResponse = this.checkValid(username, Const.USERNAME);
        if(validResponse.isSuccess())
        {
            //此时说明用户不存在
            return ServerResponse.createByErrorMessage("用户不存在");
        }

        //查询密码提示问题
        String question = userMapper.selectQuestionByUsername(username);
        if(org.apache.commons.lang3.StringUtils.isNotBlank(question))
            return ServerResponse.createBySuccess(question);

        return ServerResponse.createByErrorMessage("找回密码的问题不存在");
    }

    //检查密码提示答案是否正确，正确返回一个封装 Token 的 ServerResponse对象
    @Override
    public ServerResponse<String> checkAnswer(String username , String question , String answer)
    {
        int resultCount = userMapper.checkAnswer(username, question, answer);
        if(resultCount>0)
        {
            /**
            说明问题及问题答案是这个用户的,并且是正确的，
            此时我们获取 token ，并将Token 放入本地Cache 中，并设置其有效期，并将其发送给登录用户。
             后面用户提交新的密码的时候，也会将 token 带回来，我们需要在本地取缓存取出 token 进行比较。
             */
            String forgetToken = UUID.randomUUID().toString();
            TokenCache.setKey(TokenCache.TOKEN_PREFIX+username , forgetToken);//将 token 设置到guava缓存
            return ServerResponse.createBySuccess(forgetToken);//将 token 返回
        }

        return ServerResponse.createByErrorMessage("问题答案错误");
    }

    @Override
    //检查更新密码的用户与token是否正确，正确更新数据库这个用户的密码
    public ServerResponse<String> forgetResetPassword(String username,String passwordNew,String forgetToken)
    {
        //先检查用户是否存在
        ServerResponse<String> validResponse = this.checkValid(username, Const.USERNAME);
        if(validResponse.isSuccess())
            return ServerResponse.createByErrorMessage("用户不存在");

        //检查token：先检查其token是否存在，再检查本地缓存内token是否过期，再检查传递过来的token与本地token是否相同
        if(org.apache.commons.lang3.StringUtils.isBlank(forgetToken))
            return ServerResponse.createByErrorMessage("参数错误,token需要传递");

        //获取本地 token，检查其是否过期
        String token = TokenCache.getKey(TokenCache.TOKEN_PREFIX + username);
        if(org.apache.commons.lang3.StringUtils.isBlank(token))
            return ServerResponse.createByErrorMessage("token无效或者过期");


        //检查token是否相等
        if(org.apache.commons.lang3.StringUtils.equals(token , forgetToken))
        {
            String md5Password = MD5Util.MD5EncodeUtf8(passwordNew);
            //下面根据用户名更新密码
            int rowCount = userMapper.updatePasswordByUsername(username, md5Password);
            if(rowCount > 0)
                return ServerResponse.createBySuccessMessage("修改密码成功");
        }
        else
        {
            return ServerResponse.createByErrorMessage("token错误,请重新获取重置密码的token");
        }
        return ServerResponse.createByErrorMessage("修改密码失败");
    }

    //登录状态下设置密码
    @Override
    public ServerResponse<String> resetPassword(String passwordOld,String passwordNew,User user)
    {
        /*
        防止横向越权,要校验一下这个用户的旧密码,一定要指定是这个用户.
        因此我们指定是这个用户在登录状态下，用他的旧密码才能修改新密码。
        如果只有密码，其他恶意用户就可以不断模拟旧密码，最后一定可以试出来，就可以修改原先的密码。
         */
        int rowCount = userMapper.checkPassword(MD5Util.MD5EncodeUtf8(passwordOld), user.getId());
        if(rowCount == 0)
            return ServerResponse.createByErrorMessage("旧密码错误");//说明旧的密码是错误的

        user.setPassword(MD5Util.MD5EncodeUtf8(passwordNew));
        int updateCount = userMapper.updateByPrimaryKeySelective(user);
        if(updateCount > 0){
            return ServerResponse.createBySuccessMessage("密码更新成功");
        }
        return ServerResponse.createByErrorMessage("密码更新失败");
    }

    //更新用户信息的方法
    @Override
    public ServerResponse<User> updateInformation(User user)
    {
        /** 注意点
            1）username是不能被更新的，因此我们不能直接使用 user对象进行更新，而要新创建一个 user对象
         2）email也要进行一个校验,校验新的email是不是已经存在,并且存在的email如果相同的话,不能是属于其他用户的.
         */
        int rowCount = userMapper.checkEmailByUserId(user.getEmail(), user.getId());//返回拥有这个email的其他用户的数量
        if(rowCount > 0)
            return ServerResponse.createByErrorMessage("email已存在,请更换email再尝试更新");

        //由于username不能进行更新，创建一个新的User对象进行更新，不设置username
        User updateUser = new User();
        updateUser.setId(user.getId());
        updateUser.setEmail(user.getEmail());
        updateUser.setPhone(user.getPhone());
        updateUser.setQuestion(user.getQuestion());
        updateUser.setAnswer(user.getAnswer());

        int updateCount = userMapper.updateByPrimaryKeySelective(user);
        if(updateCount > 0){
            return ServerResponse.createBySuccess("更新个人信息成功",updateUser);
        }
        return ServerResponse.createByErrorMessage("更新个人信息失败");
    }

    //获取用户的登录信息
    @Override
    public ServerResponse<User> getInformation(Integer userId)
    {
        User user = userMapper.selectByPrimaryKey(userId);
        if(user == null)
        {
            return ServerResponse.createByErrorMessage("找不到当前用户");
        }
        //我们在返回用户信息的时候，不应该将用户的密码也返回，这里将用户密码置空
        user.setPassword(StringUtils.EMPTY);
        return ServerResponse.createBySuccess(user);
    }

    /** backend */
    //校验是不是管理员
    @Override
    public ServerResponse<String> checkAdminRole(User user)
    {
        //user.getRole()返回Integer对象，将其转换为int，才能与 Const.Role.ROLE_ADMIN比较
        if(user != null && user.getRole().intValue() == Const.Role.ROLE_ADMIN)
        {
            return ServerResponse.createBySuccess();//是管理员，返回成功对象
        }
        //不是管理员，返回失败的对象
        return ServerResponse.createByError();
    }

}
