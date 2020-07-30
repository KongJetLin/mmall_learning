package com.mmall.controller.common.interceptor;

import com.google.common.collect.Maps;
import com.mmall.common.Const;
import com.mmall.common.ServerResponse;
import com.mmall.pojo.User;
import com.mmall.util.CookieUtil;
import com.mmall.util.JsonUtil;
import com.mmall.util.RedisShardedPoolUtil;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.ModelAndView;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.PrintWriter;
import java.util.Arrays;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

/**
 * 权限拦截器类
 */
@Slf4j
public class AuthorityInterceptor implements HandlerInterceptor
{
    /**
     * 预处理，controller方法执行前
     * return true 放行，执行下一个拦截器，如果没有，执行controller中的方法
     * return false不放行.
     */
    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception
    {
        log.info("preHandle");
        //请求中Controller中的方法名（将 handle转为 HandlerMethod）
        HandlerMethod handlerMethod = (HandlerMethod)handler;

        //解析HandlerMethod
        String methodName = handlerMethod.getMethod().getName();//请求的方法名
        String className = handlerMethod.getBean().getClass().getSimpleName();//请求的类名（不带包）

        //解析参数,具体的参数key以及value是什么，我们打印日志
        StringBuffer requestParamBuffer = new StringBuffer();

        //获取请求参数的Map集合
        Map<String, String[]> parameterMap = request.getParameterMap();
        //下面我们遍历Map集合，找到其中的键值对
        Set<Map.Entry<String, String[]>> entries = parameterMap.entrySet();
        Iterator<Map.Entry<String, String[]>> iterator = entries.iterator();
        while (iterator.hasNext())
        {
            Map.Entry<String, String[]> entry = (Map.Entry)iterator.next();
            String mapKey = (String)entry.getKey();

            //先初始化mapValue
            String mapValue = StringUtils.EMPTY;
            Object obj = entry.getValue();//获取value对象
            if(obj instanceof String[])
            {
                String[] strs = (String[])obj;
                mapValue = Arrays.toString(strs);//将字符串数组（保存值）转换为 字符串
            }
            //将键值对存储到StringBuffer
            requestParamBuffer.append(mapKey).append("=").append(mapValue);
        }


//-----------------------------------------------------------上面的获取操作都是为了打印后台日志，下面才是真正的进行过滤处理
        //我们也可以通过代码的方式，决定放过哪一个请求
        //这里提供代码，放过登录请求，不经过下面的判断，直接return true
        if(StringUtils.equals(className , "UserManagerController") && StringUtils.equals(methodName , "login"))
        {
            log.info("权限拦截器拦截到请求,className:{},methodName:{}",className,methodName);
            //如果是拦截到登录请求，不打印参数，因为参数里面有密码，全部会打印到日志中，防止日志泄露。这里仅打印类名与方法名
            return true;
        }
        //用户不是登录请求，也打印日志，这里打印请求的详细信息
        log.info("权限拦截器拦截到请求,className:{},methodName:{},param:{}",className,methodName,requestParamBuffer.toString());


        //下面，对于用户是否是管理员以及用户是否登录进行判断，同样从redis中获取用户信息
        User user = null;
        String loginToken = CookieUtil.readLoginToken(request);
        if(StringUtils.isNotEmpty(loginToken))
        {
            String userJsonStr = RedisShardedPoolUtil.get(loginToken);
            user = JsonUtil.string2Obj(userJsonStr , User.class);
        }

        if(user == null || (user.getRole().intValue() != Const.Role.ROLE_ADMIN))
        {
            /**用户未登录或者不善管理员，返回false.即不会调用controller里的方法*/

            //preHandle方法返回boolean，我们无法通过给调用者返回相应的提示信息，这里需要使用response的PrintWriter来给调用者返回相应的提示信息
            //1、对response进行设置
            response.reset();//这里要添加reset，否则报异常 getWriter() has already been called for this response.
            response.setCharacterEncoding("UTF-8");//这里要设置编码，否则会乱码
            response.setContentType("application/json;charset=UTF-8");//这里要设置返回值的类型，因为全部是json接口。

            //2、获取PrintWriter对象进行写回
            PrintWriter out = response.getWriter();
            //3、下面按类型进行返回
            if(user == null)
            {//用户未登录的情况
                /** 我们在上传富文本的时候，富文本对不同的user的情况返回不同的内容，我们在拦截器对user的不同情况进行处理，
                    但是，富文本使用的 simditor ，它与普通方法的返回不同，所以按照simditor的要求进行返回，那么我们对富文本的方法就需要进行特殊处理！
                 上传由于富文本的控件要求，要特殊处理返回值，这里面区分是否登录以及是否有权限
                 */
                if(StringUtils.equals(className , "ProductManageController") && StringUtils.equals(methodName , "richtextImgUpload"))
                {
                    //富文本使用的是simditor所以按照simditor的要求进行返回
                    Map resultMap = Maps.newHashMap();
                    resultMap.put("success" , false);
                    resultMap.put("msg" , "请登录管理员");
                    //需要通过response的PrintWriter返回，因为这里返回Boolean
                    out.print(JsonUtil.obj2String(resultMap));
                }else{
                    //如果不是富文本的方法，直接返回 ServerResponse
                    //将ServerResponse对象转换为JSON返回，与之前的使用@ResponseBody返回一样
                    out.print(JsonUtil.obj2String(ServerResponse.createByErrorMessage("拦截器拦截,用户未登录")));
                }
            }
            else
            {//用户登录但不是管理员
                if(StringUtils.equals(className,"ProductManageController") && StringUtils.equals(methodName,"richtextImgUpload"))
                {
                    Map resultMap = Maps.newHashMap();
                    resultMap.put("success",false);
                    resultMap.put("msg","无权限操作");
                    out.print(JsonUtil.obj2String(resultMap));
                }else{
                    out.print(JsonUtil.obj2String(ServerResponse.createByErrorMessage("拦截器拦截,用户无权限操作")));
                }
            }

            out.flush();
            out.close();//这里要关闭
            return false;
        }
        return true;
    }


    /**
     * 后处理方法，controller方法执行后，success.jsp执行之前
     * 如果我们的项目是前后端结合的项目的话，我们在访问Controller成功后悔跳转到 success.jsp，这个方法在跳转到 success.jsp 之前会执行；
     * 我们的项目是前后端分离的，我们后端不会进行跳转，只会给前端提供接口，并不做服务器转发。
     */
    @Override
    public void postHandle(HttpServletRequest request, HttpServletResponse response, Object handler, ModelAndView modelAndView) throws Exception
    {
        log.info("postHandle");
    }

    /**
     * success.jsp页面执行后，该方法会执行
     */
    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) throws Exception
    {
        log.info("afterCompletion");
    }
}
