package com.mmall.controller.portal;

import com.alipay.api.AlipayApiException;
import com.alipay.api.internal.util.AlipaySignature;
import com.alipay.demo.trade.config.Configs;
import com.google.common.collect.Maps;
import com.mmall.common.Const;
import com.mmall.common.ResponseCode;
import com.mmall.common.ServerResponse;
import com.mmall.pojo.User;
import com.mmall.service.IOrderService;
import com.mmall.util.CookieUtil;
import com.mmall.util.JsonUtil;
import com.mmall.util.RedisPoolUtil;
import com.mmall.util.RedisShardedPoolUtil;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

/**
 * 我们将字符的功能集成到订单这里，因为支付与订单是紧密相关的！
 */
@Controller
@RequestMapping("/order/")
public class OrderController
{
    private static  final Logger logger = LoggerFactory.getLogger(OrderController.class);
    @Autowired
    private IOrderService iOrderService;


//--------------------------------------------------------订单前台方法
    /**
     * 创建订单：我们只需要传递一个地址id过来，订单的其他信息，我们查询购物车中勾选的产品信息便可知。
     * 创建流程：
     * @param session
     * @param shippingId
     * @return
     */
    @RequestMapping("create.do")
    @ResponseBody
    public ServerResponse create( Integer shippingId ,HttpServletRequest httpServletRequest)
    {
        String loginToken = CookieUtil.readLoginToken(httpServletRequest);
        if(StringUtils.isEmpty(loginToken))
        {
            return ServerResponse.createByErrorMessage("用户未登录，无法获取当前用户的信息");
        }
        String userJsonStr = RedisShardedPoolUtil.get(loginToken);
        User user = JsonUtil.string2Obj(userJsonStr , User.class);
        if(user ==null){
            return ServerResponse.createByErrorCodeMessage(ResponseCode.NEED_LOGIN.getCode(),ResponseCode.NEED_LOGIN.getDesc());
        }

        return iOrderService.createOrder(user.getId() , shippingId);
    }

    /**
     * 根据userId与订单号取消订单的方法
     * @param session
     * @param orderNo
     * @return
     */
    @RequestMapping("cancel.do")
    @ResponseBody
    public ServerResponse cancel(HttpServletRequest httpServletRequest, Long orderNo){
        String loginToken = CookieUtil.readLoginToken(httpServletRequest);
        if(StringUtils.isEmpty(loginToken))
        {
            return ServerResponse.createByErrorMessage("用户未登录，无法获取当前用户的信息");
        }
        String userJsonStr = RedisShardedPoolUtil.get(loginToken);
        User user = JsonUtil.string2Obj(userJsonStr , User.class);
        if(user ==null){
            return ServerResponse.createByErrorCodeMessage(ResponseCode.NEED_LOGIN.getCode(),ResponseCode.NEED_LOGIN.getDesc());
        }
        return iOrderService.cancel(user.getId(),orderNo);
    }

    /**
     * 从购物车中获取购物车已经选中的所有商品的详情，用于在提交订单之前的展示。
     * 那些没有选中的，在准备提交的时候不会显示。
     * @param session
     * @return
     */
    @RequestMapping("get_order_cart_product.do")
    @ResponseBody
    public ServerResponse getOrderCartProduct(HttpServletRequest httpServletRequest){
        String loginToken = CookieUtil.readLoginToken(httpServletRequest);
        if(StringUtils.isEmpty(loginToken))
        {
            return ServerResponse.createByErrorMessage("用户未登录，无法获取当前用户的信息");
        }
        String userJsonStr = RedisShardedPoolUtil.get(loginToken);
        User user = JsonUtil.string2Obj(userJsonStr , User.class);
        if(user ==null){
            return ServerResponse.createByErrorCodeMessage(ResponseCode.NEED_LOGIN.getCode(),ResponseCode.NEED_LOGIN.getDesc());
        }
        return iOrderService.getOrderCartProduct(user.getId());
    }

    /**
     * 前台的查询订单详情的方法
     * 根据用户id与订单号查询，只能查询这个用户的订单
     * @param session
     * @param orderNo
     * @return
     */
    @RequestMapping("detail.do")
    @ResponseBody
    public ServerResponse detail(HttpServletRequest httpServletRequest,Long orderNo){
        String loginToken = CookieUtil.readLoginToken(httpServletRequest);
        if(StringUtils.isEmpty(loginToken))
        {
            return ServerResponse.createByErrorMessage("用户未登录，无法获取当前用户的信息");
        }
        String userJsonStr = RedisShardedPoolUtil.get(loginToken);
        User user = JsonUtil.string2Obj(userJsonStr , User.class);
        if(user ==null){
            return ServerResponse.createByErrorCodeMessage(ResponseCode.NEED_LOGIN.getCode(),ResponseCode.NEED_LOGIN.getDesc());
        }
        return iOrderService.getOrderDetail(user.getId(),orderNo);
    }

    /**
     * 前台的查询订单列表的方法
     * 根据用户id查询，只能查询这个用户的订单
     * @param session
     * @param pageNum
     * @param pageSize
     * @return
     */
    @RequestMapping("list.do")
    @ResponseBody
    public ServerResponse list(HttpServletRequest httpServletRequest, @RequestParam(value = "pageNum",defaultValue = "1") int pageNum, @RequestParam(value = "pageSize",defaultValue = "10") int pageSize){
        String loginToken = CookieUtil.readLoginToken(httpServletRequest);
        if(StringUtils.isEmpty(loginToken))
        {
            return ServerResponse.createByErrorMessage("用户未登录，无法获取当前用户的信息");
        }
        String userJsonStr = RedisShardedPoolUtil.get(loginToken);
        User user = JsonUtil.string2Obj(userJsonStr , User.class);
        if(user ==null){
            return ServerResponse.createByErrorCodeMessage(ResponseCode.NEED_LOGIN.getCode(),ResponseCode.NEED_LOGIN.getDesc());
        }
        return iOrderService.getOrderList(user.getId(),pageNum,pageSize);
    }


//---------------------------------------------------------------支付
    /**
     * 订单支付方法
     * 我们在前端点击支付，会发送信息到支付宝的服务器，随后支付宝服务器返回相应的付款二维码信息，
     * 我们在代码中生成订单号与二维码信息的访问url，发送回给前端展示
     * @param session
     * @param orderNo
     * @param request
     * @return
     */
    @RequestMapping("pay.do")
    @ResponseBody
    public ServerResponse pay(HttpServletRequest httpServletRequest , Long orderNo , HttpServletRequest request)
    {
        String loginToken = CookieUtil.readLoginToken(httpServletRequest);
        if(StringUtils.isEmpty(loginToken))
        {
            return ServerResponse.createByErrorMessage("用户未登录，无法获取当前用户的信息");
        }
        String userJsonStr = RedisShardedPoolUtil.get(loginToken);
        User user = JsonUtil.string2Obj(userJsonStr , User.class);
        if(user == null)
            return ServerResponse.createByErrorCodeMessage(ResponseCode.NEED_LOGIN.getCode() , ResponseCode.NEED_LOGIN.getDesc());

        String path = request.getSession().getServletContext().getRealPath("upload");
        return iOrderService.pay(orderNo,user.getId(),path);
    }


    /**
     * 支付宝回调
     * 用户扫描付款二维码支付后，支付宝会通过Request返回回调信息，我们需要对回调信息进行验证和处理，
     * 如果回调信息是正常的，那么我们需要返回一个 “success”，告诉支付宝订单支付验证通过，
     * 这时支付宝就会知道我们正常收到回调信息并处理，就不会继续回调；如果支付宝收到fail，则会一直回调。
     * 并且，我们在验证通过回调信息后，也需要更新订单信息和支付信息。
     * @param request
     * @return
     */
    @RequestMapping("alipay_callback.do")
    @ResponseBody
    public Object alipayCallback(HttpServletRequest request)
    {
        Map<String, String> params = Maps.newHashMap();

        //获取 Request 中的参数，支付宝通过Request返回回调信息。注意，返回的value是String[]类型，我们将其转换为String类型，存储到自己的Map<String,String>
        Map<String, String[]> requestParams = request.getParameterMap();

        //下面遍历参数的Map集合：先取出迭代器对象，用于找出Map的key，key=iter.next，当还有key的时候，持续遍历
        for (Iterator iter = requestParams.keySet().iterator() ; iter.hasNext();)
        {
            String key = (String)iter.next();//先取出Map的每一个key
            String[] values = requestParams.get(key);//取出key对应的values数组对象
            String valueStr = "";
            //遍历values
            for (int i = 0; i < values.length; i++)
            {
                valueStr = (i == values.length-1) ? valueStr+values[i] : valueStr+values[i]+",";
            }
            params.put(key , valueStr);
        }
        //这里我们就将返回的数据封装到 Map<String,String>
        /*
        打印日志，sign（签名）：异步返回结果的验签（如果开发者手动验签，不使用 SDK 验签，可以不传此参数）,
        验证签名，即验证信息是不是支付宝发送过来的，很重要！！！用公钥私钥的数字签名方式。
         */
        //
        logger.info("支付宝回调,sign:{},trade_status:{},参数:{}" , params.get("sign") , params.get("trade_status") , params.toString());

        /** 非常重要,验证回调的正确性,是不是支付宝发的.并且呢还要避免重复通知. */
        //1、我们需要将 sign_type ，即使用的签名类型（我们用RSA2）移除，sign与sign_type均不是验签参数，sign已经被 rsaCheckV2() 方法移除
        params.remove("sign_type");

        /**
        1）我们在  OrderServiceImpl 通过 Configs.init("zfbinfo.properties") 加载了支付宝的配置文件，我们这里就可以通过 Configs的方法来取得支付宝的公钥
        2）字符集使用 “utf-8”，小写，与生成订单的 tradeService 方法所设置的字符集保持一致；
        3）sign_type 也可以通过 Configs获取
         */
        try
        {
            boolean alipayRSACheckedV2 = AlipaySignature.rsaCheckV2(params , Configs.getAlipayPublicKey() , "utf-8" , Configs.getSignType());
            if(!alipayRSACheckedV2)
                return ServerResponse.createByErrorMessage("非法请求,验证不通过,再恶意请求我就报警找网警了");
        }
        catch (AlipayApiException e)
        {
            logger.error("支付宝验证回调异常",e);
        }

        //todo 验证各种数据 （可以在service里面验证），参考：https://opendocs.alipay.com/open/194/103296
        //todo 验证：out_trade_no 是否为商户系统中创建的订单号、total_amount 是否确实为该订单的实际金额、 seller_id（或者seller_email) 是否为 out_trade_no 这笔单据的对应的操作方


        //对回调信息进行处理
        ServerResponse serverResponse = iOrderService.aliCallback(params);
        if(serverResponse.isSuccess())
            return Const.AlipayCallback.RESPONSE_SUCCESS;//成功处理，返回“success”给支付宝
        return Const.AlipayCallback.RESPONSE_FAILED;
    }


    /**
     * 根据订单号查询支付状态。
     * 在二维码扫码页面，用户扫码付款成功之后，前台会调用这个接口，来查询订单是否付款成功，
     * 如果付款成功，前台会进行付款后其他页面的跳转
     * @param session
     * @param orderNo
     * @return
     */
    @RequestMapping("query_order_pay_status.do")
    @ResponseBody
    public ServerResponse<Boolean> queryOrderPayStatus(HttpServletRequest httpServletRequest, Long orderNo)
    {
        //先判断用户是否登录，登录才能查询，没有登录则强制登录
        String loginToken = CookieUtil.readLoginToken(httpServletRequest);
        if(StringUtils.isEmpty(loginToken))
        {
            return ServerResponse.createByErrorMessage("用户未登录，无法获取当前用户的信息");
        }
        String userJsonStr = RedisShardedPoolUtil.get(loginToken);
        User user = JsonUtil.string2Obj(userJsonStr , User.class);
        if(user ==null){
            return ServerResponse.createByErrorCodeMessage(ResponseCode.NEED_LOGIN.getCode(),ResponseCode.NEED_LOGIN.getDesc());
        }

        ServerResponse serverResponse = iOrderService.queryOrderPayStatus(user.getId(), orderNo);
        //注意，我们与前端的约定是：查询到返回true，否则发回false，我们这里不能直接返回 ServerResponse.createByErrorMessage
        // 或者 ServerResponse.createBySuccess()，我们用 ServerResponse.createBySuccess(true/false)封装返回的状态即可
        // 因为这里只是查询是否支付成功，不需要返回错误，返回 是否 即可
        if(serverResponse.isSuccess())
            return ServerResponse.createBySuccess(true);
        return ServerResponse.createBySuccess(false);
    }

}
