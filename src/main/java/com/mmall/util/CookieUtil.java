package com.mmall.util;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang.StringUtils;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
用于将Cookie信息 COOKIE_NAME 写到某个域名 COOKIE_DOMAIN 上
 */
@Slf4j
public class CookieUtil
{
    //使用以及域名作为 COOKIE_DOMAIN，这样三级域名都能识别到，如 “www.happymmall.com”、“user.happymmall.com”等
    private final static String COOKIE_DOMAIN = "happymmall.com";
    private final static String COOKIE_NAME = "mmall_login_token";


    /** 向 response 写入 包含 JSESSIONID 的cookie
    将保存了 JSESSIONID 的 cookie 写入 响应对象，并设置 cookie 的作用域名
    1）此处 token 指的是 JSESSIONID
     2）在登录的时候，就会写入 cookie 到response
     */
    public static void writeLoginToken(HttpServletResponse response , String token)
    {
        Cookie ck = new Cookie(COOKIE_NAME , token);//创建一个cookie，将JSESSIONID写入
        ck.setDomain(COOKIE_DOMAIN);//设置该COOKIE指向的域名
        //代表设置在根目录，代表所有的页面和代码都可以获取cookie；若设置为某一个目录，只有该目录下的代码或者页面才能获取cookie
        ck.setPath("/");
        ck.setHttpOnly(true);//为了避免脚本攻击带来的风险，这个属性规定，不允许通过脚本访问cookie，避免脚本信息的
        /*
        有效时间，单位是秒。如果是-1，代表永久。
        如果这个maxage不设置的话，cookie就不会写入硬盘，而是写在内存，只在当前页面有效。
        如果写入硬盘，那么浏览器或电脑重启，在Cookie的有效时间内，仍然可以找到Cookie。
         */
        ck.setMaxAge(60 * 60 * 24 * 365);
        log.info("write cookieName:{},cookieValue:{}",ck.getName(),ck.getValue());
        response.addCookie(ck);//将Cookie信息写入相应对象 Response
    }

    /** 从request读取包含 JSESSIONID 的cookie
    我们在继续访问的时候，就需要从 redis 中获取User对象以保持登录状态，就需要读取 cookie，
     拿到登录时存储的 JSESSIONID，才能获取到相应的User信息。
     */
    public static String readLoginToken(HttpServletRequest request){
        Cookie[] cks = request.getCookies();
        if(cks != null)
        {
            for(Cookie ck : cks)
            {
                log.info("read cookieName:{},cookieValue:{}",ck.getName(),ck.getValue());
                if(StringUtils.equals(ck.getName(),COOKIE_NAME))
                {//当请求的 COOKIE 名等于 COOKIE_NAME 的时候，说明获取到保存 JSESSIONID 的cookie，将这个 cookie 值返回
                    log.info("return cookieName:{},cookieValue:{}",ck.getName(),ck.getValue());
                    return ck.getValue();
                }
            }
        }
        return null;
    }

    /** 删除 request 以及 response 中的cookie
    当我们注销登录的时候，就需要将 cookie 清除，使得浏览器的该 保存 JSEESIONID 的cookie不可用（过期），
     那么浏览器再通过这个 cookie的JSESSIONID 访问的时候，该cookie已经过期。
     */
    public static void delLoginToken(HttpServletRequest request,HttpServletResponse response)
    {
        Cookie[] cks = request.getCookies();
        if(cks != null)
        {
            /* 删除cookie的方法
            先通过 request 获取到相应的cookie，将该cookie的时间设置为 0，此时它就失效了，
            随后，我们再通过 response 将cookie 更新的信息写回 浏览器，浏览器更新这个cookie的信息，
            下次浏览器再次访问的时候，携带的cookie信息就是不可用的！
             */
            for(Cookie ck : cks){
                if(StringUtils.equals(ck.getName(),COOKIE_NAME))
                {
                    ck.setDomain(COOKIE_DOMAIN);
                    ck.setPath("/");
                    ck.setMaxAge(0);//设置成0，代表删除此cookie。
                    log.info("del cookieName:{},cookieValue:{}",ck.getName(),ck.getValue());
                    response.addCookie(ck);
                    return;
                }
            }
        }
    }

}
/** 关于 COOKIE_DOMAIN 的访问的说明
 若我们设置的cookie的domain是  X:domain=".happymmall.com" 这个一级域名下的 domain
 那么下面5个域名，都可以拿到相应的 cookie。

 a、b这两个二级域名都无法拿到对方的cookie；c、d 可以共享 a 的cookie，也可以共享 e 的cookie，同样 c、d都拿不到对方的cookie，
 同样 e 也可以拿到 a 的cookie。总结起来就是，越大的域名，就可以拿到越小的域名的cookie

 a:A.happymmall.com            cookie:domain=A.happymmall.com;path="/"
 b:B.happymmall.com            cookie:domain=B.happymmall.com;path="/"
 c:A.happymmall.com/test/cc    cookie:domain=A.happymmall.com;path="/test/cc"
 d:A.happymmall.com/test/dd    cookie:domain=A.happymmall.com;path="/test/dd"
 e:A.happymmall.com/test       cookie:domain=A.happymmall.com;path="/test"

 */
