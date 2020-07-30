package com.mmall.service.impl;

import com.alipay.api.AlipayResponse;
import com.alipay.api.response.AlipayTradePrecreateResponse;
import com.alipay.demo.trade.config.Configs;
import com.alipay.demo.trade.model.ExtendParams;
import com.alipay.demo.trade.model.GoodsDetail;
import com.alipay.demo.trade.model.builder.AlipayTradePrecreateRequestBuilder;
import com.alipay.demo.trade.model.result.AlipayF2FPrecreateResult;
import com.alipay.demo.trade.service.AlipayTradeService;
import com.alipay.demo.trade.service.impl.AlipayTradeServiceImpl;
import com.alipay.demo.trade.utils.ZxingUtils;
import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.mmall.common.Const;
import com.mmall.common.ServerResponse;
import com.mmall.dao.*;
import com.mmall.pojo.*;
import com.mmall.service.IOrderService;
import com.mmall.util.BigDecimalUtil;
import com.mmall.util.DateTimeUtil;
import com.mmall.util.FTPUtil;
import com.mmall.util.PropertiesUtil;
import com.mmall.vo.OrderItemVo;
import com.mmall.vo.OrderProductVo;
import com.mmall.vo.OrderVo;
import com.mmall.vo.ShippingVo;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.time.DateUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.io.File;
import java.io.IOException;
import java.math.BigDecimal;
import java.util.*;

@Service("iOrderService")
@Slf4j
public class OrderServiceImpl implements IOrderService
{
    private static  AlipayTradeService tradeService;
    static {

        /** 一定要在创建AlipayTradeService之前调用Configs.init()设置默认参数
         *  Configs会读取classpath下的zfbinfo.properties文件配置信息，如果找不到该文件则确认该文件是否在classpath目录
         */
        Configs.init("zfbinfo.properties");

        /** 使用Configs提供的默认参数
         *  AlipayTradeService可以使用单例或者为静态成员对象，不需要反复new
         */
        tradeService = new AlipayTradeServiceImpl.ClientBuilder().build();
    }

//    private static final Logger logger = LoggerFactory.getLogger(OrderServiceImpl.class);

    @Autowired
    private OrderMapper orderMapper;
    @Autowired
    private OrderItemMapper orderItemMapper;
    @Autowired
    private PayInfoMapper payInfoMapper;
    @Autowired
    private CartMapper cartMapper;
    @Autowired
    private ProductMapper productMapper;
    @Autowired
    private ShippingMapper shippingMapper;

//----------------------------------------------------------------------------订单功能（前端）

    /**
     * 根据用户id与地址id创建订单，具体逻辑见代码
     * @param userId
     * @param shippingId
     * @return
     */
    @Override
    public ServerResponse createOrder(Integer userId , Integer shippingId)
    {
        //1、从根据用户id，查询购物车表，将这个用户被选中的购物车对象取出，封装到List
        List<Cart> cartList = cartMapper.selectCheckedCartByUserId(userId);

        //2、将用户（userId）购物车内的数据封装为 OrderItem 对象
        ServerResponse serverResponse = this.getCartOrderItem(userId, cartList);
        //如果创建订单项失败，下面不需要执行，直接返回
        if(!serverResponse.isSuccess())
            return serverResponse;

        //取出订单项对象的集合
        List<OrderItem> orderItemList = (List<OrderItem>)serverResponse.getData();
        if(CollectionUtils.isEmpty(orderItemList)){
            return ServerResponse.createByErrorMessage("购物车为空");
        }

        //3、计算订单项集合的总价，即该订单的总价
        BigDecimal payment = this.getOrderTotalPrice(orderItemList);

        //4、生成订单（根据userId、地址id、总价）
        Order order = this.assembleOrder(userId, shippingId, payment);
        if(order == null)
            return ServerResponse.createByErrorMessage("生成订单错误");

        //5、订单与订单项都存在，此时为每一个订单项对象添加orderNo，将 Order 与 OrderItem 联系起来（此时订单已经生成，有订单号）
        //注意，订单插入数据库，在生成订单的时候已经完成
        for (OrderItem orderItem : orderItemList)
        {
            orderItem.setOrderNo(order.getOrderNo());
        }

        //6、将生成的 OrderItem 对象的集合全部插入数据库，使用mybatis 批量插入
        orderItemMapper.batchInsert(orderItemList);

        //7、生成成功订单成功，我们要减少我们产品的库存，避免其他用户购买
        this.reduceProductStock(orderItemList);

        //8、生成订单成功，需要清空一下购物车
        this.cleanCart(cartList);

        //9、封装订单vo对象，返回给前端（原来的订单对象的不能封装全部数据）
        OrderVo orderVo = this.assembleOrderVo(order, orderItemList);

        return ServerResponse.createBySuccess(orderVo);
    }

    /**
     * 取消订单
     * @param userId
     * @param orderNo
     * @return
     */
    public ServerResponse<String> cancel(Integer userId,Long orderNo)
    {
        Order order = orderMapper.selectByUserIdAndOrderNo(userId, orderNo);
        //首先判断订单是否存在
        if(order == null)
        {
            return ServerResponse.createByErrorMessage("该用户此订单不存在");
        }

        //判断订单状态是否是未付款，如果不是未付款，说明已经取消或者付款，不能再次取消
        if(order.getStatus() != Const.OrderStatusEnum.NO_PAY.getCode())
        {
            return ServerResponse.createByErrorMessage("已付款,无法取消订单");
        }

        //更新订单信息
        Order updateOrder = new Order();
        updateOrder.setId(order.getId());//需要根据id俩更新数据库
        updateOrder.setStatus(Const.OrderStatusEnum.CANCELED.getCode());

        //更新到数据库
        int rowCount = orderMapper.updateByPrimaryKeySelective(updateOrder);
        if(rowCount > 0){
            return ServerResponse.createBySuccess();
        }
        return ServerResponse.createByError();

    }

    /**
     * 根据用户id，查询该用户购物车中所有的商品信息并展示
     * @param userId
     * @return
     */
    public ServerResponse getOrderCartProduct(Integer userId)
    {
        //需要显示订单与订单下产品的信息，封装一个OrderProduct对象
        OrderProductVo orderProductVo = new OrderProductVo();

        //1、获取该用户的所有被选中的购物车对象
        List<Cart> cartList = cartMapper.selectCheckedCartByUserId(userId);
        //2、将 List<Cart> 封装为 List<OrderItem>
        ServerResponse serverResponse = this.getCartOrderItem(userId, cartList);
        if(!serverResponse.isSuccess())
            return serverResponse;
        //取出订单项对象的集合
        List<OrderItem> orderItemList = (List<OrderItem>)serverResponse.getData();

        List<OrderItemVo> orderItemVoList = Lists.newArrayList();

        BigDecimal payment = new BigDecimal("0");
        //遍历所有的订单项对象，用于计算所有订单项的总价以及构建 OrderItemVo对象
        for (OrderItem orderItem : orderItemList)
        {
            payment = BigDecimalUtil.add(payment.doubleValue() , orderItem.getTotalPrice().doubleValue());
            orderItemVoList.add(assembleOrderItemVo(orderItem));
        }
        //为 OrderProductVo 的属性注入值
        orderProductVo.setImageHost(PropertiesUtil.getProperty("ftp.server.http.prefix"));
        orderProductVo.setOrderItemVoList(orderItemVoList);
        orderProductVo.setProductTotalPrice(payment);

        return ServerResponse.createBySuccess(orderProductVo);
    }


    //根据用户id与订单号查询订单，只能查询这个用户的订单
    public ServerResponse<OrderVo> getOrderDetail(Integer userId, Long orderNo)
    {
        //1、先查找到订单对象
        Order order = orderMapper.selectByUserIdAndOrderNo(userId, orderNo);
        if(order != null)
        {
            //查询出这个用户下的订单号为 orderNo 的订单项集合
            List<OrderItem> orderItemList = orderItemMapper.getByOrderNoUserId(orderNo, userId);
            OrderVo orderVo = assembleOrderVo(order, orderItemList);
            return ServerResponse.createBySuccess(orderVo);
        }
        return  ServerResponse.createByErrorMessage("没有找到该订单");
    }

    //根据用户id查询，只能查询这个用户的订单
    public ServerResponse<PageInfo> getOrderList(Integer userId,int pageNum,int pageSize)
    {
        PageHelper.startPage(pageNum , pageSize);
        //首先查询这个用户下的所有订单
        List<Order> orderList = orderMapper.selectByUserId(userId);
        //组装 OrderVoList
        List<OrderVo> orderVoList = assembleOrderVoList(orderList, userId);//非管理员，需要传递用户id过去
        PageInfo pageInfo = new PageInfo(orderList);
        pageInfo.setList(orderVoList);
        return ServerResponse.createBySuccess(pageInfo);
    }


//------------------------------------------------------------------------订单功能（后端）

    /**
     * 查询所有订单信息
     * @param pageNum
     * @param pageSize
     * @return
     */
    public ServerResponse<PageInfo> manageList(int pageNum,int pageSize)
    {
        //1、开始分页
        PageHelper.startPage(pageNum , pageSize);
        //2、查询出所有的订单
        List<Order> orderList = orderMapper.selectAllOrder();
        //3、将 List<Order> 封装为 List<OrderVo>
        List<OrderVo> orderVoList = assembleOrderVoList(orderList , null);//管理员，不需要根据用户id
        //4、分页组装
        PageInfo pageInfo = new PageInfo(orderList);
        pageInfo.setList(orderVoList);
        return ServerResponse.createBySuccess(pageInfo);
    }

    /**
     * 查询订单详情：需要返回 OrderVo 对象
     * @param orderNo
     * @return
     */
    public ServerResponse<OrderVo> manageDetail(Long orderNo)
    {
        //1、先查询出订单号对应的订单
        Order order = orderMapper.selectByOrderNo(orderNo);
        if(order != null)
        {
            //2、想要组装 OrderVo，需要调用assembleOrderVo，需要List<OrderItem>
            List<OrderItem> orderItemList = orderItemMapper.getByOrderNo(orderNo);//根据订单号，查询出订单下的所有订单项对象
            OrderVo orderVo = assembleOrderVo(order, orderItemList);//生成OrderVo对象
            return ServerResponse.createBySuccess(orderVo);
        }
        return ServerResponse.createByErrorMessage("订单不存在");
    }

    //同样需要 OrderVo 对象
    public ServerResponse<PageInfo> manageSearch(Long orderNo,int pageNum,int pageSize)
    {
        PageHelper.startPage(pageNum , pageSize);
        //查询订单
        Order order = orderMapper.selectByOrderNo(orderNo);
        if(order != null)
        {
            //根据订单号查询 List<OrderItem>
            List<OrderItem> orderItemList = orderItemMapper.getByOrderNo(orderNo);
            OrderVo orderVo = assembleOrderVo(order, orderItemList);

            //这里需要将order与orderVo转换为List，这样 PageInfo 才能接收
            PageInfo pageInfo = new PageInfo(Lists.newArrayList(order));
            pageInfo.setList(Lists.newArrayList(orderVo));
            return ServerResponse.createBySuccess(pageInfo);
        }
        return ServerResponse.createByErrorMessage("订单不存在");
    }

    //根据订单号发货的方法
    public ServerResponse<String> manageSendGoods(Long orderNo)
    {
        //查询订单
        Order order = orderMapper.selectByOrderNo(orderNo);
        if(order != null)
        {
            //查看订单状态是不是已付款，已付款才能发货
            if(order.getStatus() == Const.OrderStatusEnum.PAID.getCode())
            {
                order.setStatus(Const.OrderStatusEnum.SHIPPED.getCode());
                order.setSendTime(new Date());//注意为发货时间赋值
                orderMapper.updateByPrimaryKeySelective(order);//更新到数据库
                return ServerResponse.createBySuccess("发货成功");
            }
        }
        return ServerResponse.createByErrorMessage("订单不存在");
    }


//-----------------------------------------------------------------------------------------辅助方法

    /**
     * 根据 List<Order> orderList 以及 用户id，将List<Order> orderList 组装为 List<OrderVo>
     *  实际上调用了下面的 assembleOrderVo(Order order, List<OrderItem> orderItemList)方法
     */
    private List<OrderVo> assembleOrderVoList(List<Order> orderList , Integer userId)
    {
        List<OrderVo> orderVoList = Lists.newArrayList();
        //下面遍历 order的集合，查询出 order 所有的OrderItem，组装成为 List<OrderItem>,
        // 这样就可以根据 order与其List<OrderItem>，调用assembleOrderVo() 方法，组装成为这个order对象的 OrderVo
        for (Order order : orderList)
        {
            List<OrderItem> orderItemList = Lists.newArrayList();
            if(userId == null)
            {
                //管理员查询的时候 不需要传userId
                //根据订单号，查询出所有的 OrderItem
                orderItemList = orderItemMapper.getByOrderNo(order.getOrderNo());
            }
            else
            {
                //非管理员，需要根据userId查询出它对应的订单号
                orderItemList = orderItemMapper.getByOrderNoUserId(order.getOrderNo() , userId);
            }
            //根据 order和orderItemList，组装OrderVo
            OrderVo orderVo = assembleOrderVo(order, orderItemList);
            orderVoList.add(orderVo);
        }
        return orderVoList;
    }


    /**
     * 根据 Order对象以及相应的 orderItemList（订单项对象集合），组装OrderVo对象
     */
    private OrderVo assembleOrderVo(Order order, List<OrderItem> orderItemList)
    {
        OrderVo orderVo = new OrderVo();
        orderVo.setOrderNo(order.getOrderNo());
        orderVo.setPayment(order.getPayment());
        orderVo.setPaymentType(order.getPaymentType());
        //支付类型描述：根据字符类型，找到相应的枚举对象，再找到该枚举对象的描述
        orderVo.setPaymentTypeDesc(Const.PaymentTypeEnum.codeOf(order.getPaymentType()).getValue());

        orderVo.setPostage(order.getPostage());
        orderVo.setStatus(order.getStatus());
        //设置支付状态的描述，用同样的方法从枚举中获取
        orderVo.setStatusDesc(Const.OrderStatusEnum.codeOf(order.getStatus()).getValue());
        orderVo.setShippingId(order.getShippingId());

        //设置 ShippingVo对象，并从 Shipping 找出收货人的姓名
        Shipping shipping = shippingMapper.selectByPrimaryKey(order.getShippingId());
        if(shipping != null)
        {
            orderVo.setReceiverName(shipping.getReceiverName());
            //封装shippingVo对象，并注入OrderVo的ShippingVo对象
            orderVo.setShippingVo(this.assembleShippingVo(shipping));
        }

        //设置几个时间，注意转换为String
        orderVo.setPaymentTime(DateTimeUtil.dateToStr(order.getPaymentTime()));
        orderVo.setSendTime(DateTimeUtil.dateToStr(order.getSendTime()));
        orderVo.setEndTime(DateTimeUtil.dateToStr(order.getEndTime()));
        orderVo.setCreateTime(DateTimeUtil.dateToStr(order.getCreateTime()));
        orderVo.setCloseTime(DateTimeUtil.dateToStr(order.getCloseTime()));

        //设置FTP服务器存储图片地址的访问地址
        orderVo.setImageHost(PropertiesUtil.getProperty("ftp.server.http.prefix"));

        //封装 List<OrderItemVo>
        List<OrderItemVo> orderItemVoList = Lists.newArrayList();
        for (OrderItem orderItem : orderItemList)
        {
            OrderItemVo orderItemVo = this.assembleOrderItemVo(orderItem);
            orderItemVoList.add(orderItemVo);
        }
        orderVo.setOrderItemVoList(orderItemVoList);

        return orderVo;
    }

    /**
     * 封装 OrderItemVo
     */
    private OrderItemVo assembleOrderItemVo(OrderItem orderItem)
    {
        OrderItemVo orderItemVo = new OrderItemVo();
        orderItemVo.setOrderNo(orderItem.getOrderNo());
        orderItemVo.setProductId(orderItem.getProductId());
        orderItemVo.setProductName(orderItem.getProductName());
        orderItemVo.setProductImage(orderItem.getProductImage());
        orderItemVo.setCurrentUnitPrice(orderItem.getCurrentUnitPrice());
        orderItemVo.setQuantity(orderItem.getQuantity());
        orderItemVo.setTotalPrice(orderItem.getTotalPrice());

        orderItemVo.setCreateTime(DateTimeUtil.dateToStr(orderItem.getCreateTime()));

        return orderItemVo;
    }


    /**
     * 封装 shippingVo
     */
    private ShippingVo assembleShippingVo(Shipping shipping){
        ShippingVo shippingVo = new ShippingVo();
        shippingVo.setReceiverName(shipping.getReceiverName());
        shippingVo.setReceiverAddress(shipping.getReceiverAddress());
        shippingVo.setReceiverProvince(shipping.getReceiverProvince());
        shippingVo.setReceiverCity(shipping.getReceiverCity());
        shippingVo.setReceiverDistrict(shipping.getReceiverDistrict());
        shippingVo.setReceiverMobile(shipping.getReceiverMobile());
        shippingVo.setReceiverZip(shipping.getReceiverZip());
        shippingVo.setReceiverPhone(shippingVo.getReceiverPhone());
        return shippingVo;
    }


    /**
    清空购物车:根据购物车id删除相应的购物车对象
     */
    private void cleanCart(List<Cart> cartList)
    {
        for (Cart cart : cartList)
        {
            cartMapper.deleteByPrimaryKey(cart.getId());
        }

    }

    /**
    根据订单项对象的集合，我们找到每一个订单项对应的产品以及订单项内购买该产品的数量，
     减少该产品的库存（在生成订单后才减少库存，避免其他用户购买，如果订单取消，则会重新增加库存）
     */
    private void reduceProductStock(List<OrderItem> orderItemList)
    {
        for (OrderItem orderItem : orderItemList)
        {
            //先找出产品对象
            Product product = productMapper.selectByPrimaryKey(orderItem.getProductId());
            //设置product的库存
            product.setStock(product.getStock() - orderItem.getQuantity());
            //更新到数据库
            productMapper.updateByPrimaryKeySelective(product);
        }
    }


    /**
     根据userId、地址id、总价 生成订单
     */
    private Order assembleOrder(Integer userId , Integer shippingId , BigDecimal payment)
    {
        Order order = new Order();
        long orderNo = this.generateOrderNo();//生成订单号
        order.setOrderNo(orderNo);
        order.setUserId(userId);
        order.setPayment(payment);
        order.setShippingId(shippingId);
        order.setPostage(0);//运费
        //设置订单状态
        order.setStatus(Const.OrderStatusEnum.NO_PAY.getCode());
        //设置支付类型
        order.setPaymentType(Const.PaymentTypeEnum.ONLINE_PAY.getCode());

        //发货时间在后台发货时添加、支付时间在支付 pay 的时候添加
        int rowCount = orderMapper.insert(order);//将订单插入数据库
        if(rowCount > 0){
            return order;
        }
        return null;
    }


    /**
    根据当前时间生成订单号
     */
    private long generateOrderNo()
    {
        long currentTime = System.currentTimeMillis();
        return currentTime + new Random().nextInt(100);
    }


    /**
    根据订单项对象的集合，计算订单总价
     */
    private BigDecimal getOrderTotalPrice(List<OrderItem> orderItemList)
    {
        BigDecimal payment = new BigDecimal("0");//注意这里使用字符串的“0”
        for (OrderItem orderItem : orderItemList)
        {
            payment = BigDecimalUtil.add(payment.doubleValue() , orderItem.getTotalPrice().doubleValue());
        }
        return payment;
    }


    /**
    一个Cart对象对应一个产品，一个OrderItem对象也对应一个产品，我们将取出的Cart对象转换为OrderItem对象
     */
    private ServerResponse<List<OrderItem>> getCartOrderItem(Integer userId , List<Cart> cartList)
    {
        List<OrderItem> orderItemList = Lists.newArrayList();
        if(CollectionUtils.isEmpty(cartList))
        {
            //如果查询到的购物车对象为空，返回错误提示信息
            return ServerResponse.createByErrorMessage("购物车为空");
        }

        //遍历购物车对象，校验购物车对象的数据,包括产品的状态和数量
        for (Cart cartItem : cartList)
        {
            OrderItem orderItem = new OrderItem();
            //先购物这个购物车对象的 productId 查询出对应的产品
            Product product = productMapper.selectByPrimaryKey(cartItem.getProductId());
            //判断这个产品是否下架
            if(product.getStatus() != Const.ProductStatusEnum.ON_SALE.getCode())
            {
                //该产品状态不是在售
                return ServerResponse.createByErrorMessage("产品"+product.getName()+"不是在线售卖状态");
            }
            //判断购买数量有没有超过限制
            // 其实在添加到购物车的时候我们已经对购物车中产品的数量加了限制，不会超过库存，
            // 这里再次判断，是为了避免在购买的过程中，其他用户在中间购买，导致产品数量发生变化，因此再次判断
            if(cartItem.getQuantity() > product.getStock())
            {
                return ServerResponse.createByErrorMessage("产品"+product.getName()+"库存不足");
            }

            //如果校验通过，将相应的信息填充到 OrderItem （此时订单还没有生成，没有订单号）
            orderItem.setUserId(userId);
            orderItem.setProductId(product.getId());
            orderItem.setProductName(product.getName());
            orderItem.setProductImage(product.getMainImage());
            orderItem.setCurrentUnitPrice(product.getPrice());//当前购买的单价，不能变化
            orderItem.setQuantity(cartItem.getQuantity());
            //订单总价
            orderItem.setTotalPrice(BigDecimalUtil.mul(product.getPrice().doubleValue() , cartItem.getQuantity()));

            //将封装好的订单对象添加到 List
            orderItemList.add(orderItem);
        }
        return ServerResponse.createBySuccess(orderItemList);
    }


 //----------------------------------------------------------------------------支付功能
    //根据 订单号、用户id以及path进行支付的方法
    //根据订单号与用户id，就可以知道那一个订单需要支付，path指的是生成的支付二维码要传递到哪里的路径
    @Override
    public ServerResponse pay(Long orderNo, Integer userId, String path)
    {
        //我们与前端的约定是：把订单号与二维码的url返回给前端，用Map保存
        Map<String, String> resultMap = Maps.newHashMap();

        //根据userId与orderNo查询出订单是否存在
        Order order = orderMapper.selectByUserIdAndOrderNo(userId, orderNo);
        if (order == null) return ServerResponse.createByErrorMessage("用户没有该订单");

        //将订单号添加到 resultMap中
        resultMap.put("orderNo", String.valueOf(order.getOrderNo()));


        /** 下面组装生成支付宝订单的各种参数 */
        // (必填) 商户网站订单系统中唯一订单号，64个字符以内，只能包含字母、数字、下划线，
        // 需保证商户系统端不能重复，建议通过数据库sequence生成，
        String outTradeNo = order.getOrderNo().toString();

        // (必填) 订单标题，粗略描述用户的支付目的。如“xxx品牌xxx门店当面付扫码消费”
        String subject = new StringBuilder().append("happymmall扫码支付，订单号：").append(outTradeNo).toString();

        // (必填) 订单总金额，单位为元，不能超过1亿元
        // 如果同时传入了【打折金额】,【不可打折金额】,【订单总金额】三者,则必须满足如下条件:【订单总金额】=【打折金额】+【不可打折金额】
        String totalAmount = order.getPayment().toString();

        // (可选) 订单不可打折金额，可以配合商家平台配置折扣活动，如果酒水不参与打折，则将对应金额填写至此字段
        // 如果该值未传入,但传入了【订单总金额】,【打折金额】,则该值默认为【订单总金额】-【打折金额】
        String undiscountableAmount = "0";

        // 卖家支付宝账号ID，用于支持一个签约账号下支持打款到不同的收款账号，(打款到sellerId对应的支付宝账号)
        // 如果该字段为空，则默认为与支付宝签约的商户的PID，也就是appid对应的PID
        String sellerId = "";

        // 订单描述，可以对交易或商品进行一个详细地描述，比如填写"购买商品2件共15.00元"
        String body = new StringBuilder().append("订单").append(outTradeNo).append("购买商品共").append(totalAmount).append("元").toString();

        // 商户操作员编号，添加此参数可以为商户操作员做销售统计
        String operatorId = "test_operator_id";

        // (必填) 商户门店编号，通过门店号和商家后台可以配置精准到门店的折扣信息，详询支付宝技术支持
        String storeId = "test_store_id";

        // 业务扩展参数，目前可添加由支付宝分配的系统商编号(通过setSysServiceProviderId方法)，详情请咨询支付宝技术支持
        ExtendParams extendParams = new ExtendParams();
        extendParams.setSysServiceProviderId("2088100200300400500");

        // 支付超时，定义为120分钟
        String timeoutExpress = "120m";

        // 商品明细列表，需填写购买商品详细信息，
        List<GoodsDetail> goodsDetailList = new ArrayList<GoodsDetail>();

        /**
         一个订单对象下面可能有多个订单项对象。一个订单项对象对应某一个产品（可能购买了多个产品）
         某个订单对象可能对应多个产品！每一个产品对应一个订单项对象！
         */
        List<OrderItem> orderItemList = orderItemMapper.getByOrderNoUserId(orderNo, userId);
        for (OrderItem orderItem : orderItemList)
        {
            //注意，这里取出来的价格是“分”，我们需要将其用BigDecimal处理，转换为个位，最后转换为long类型
            GoodsDetail goods = GoodsDetail.newInstance(orderItem.getProductId().toString(), orderItem.getProductName(), BigDecimalUtil.mul(orderItem.getCurrentUnitPrice().doubleValue(), new Double(100).doubleValue()).longValue(), orderItem.getQuantity());
            goodsDetailList.add(goods);
        }

        // 创建扫码支付请求builder，设置请求参数
        AlipayTradePrecreateRequestBuilder builder = new AlipayTradePrecreateRequestBuilder().setSubject(subject).setTotalAmount(totalAmount).setOutTradeNo(outTradeNo).setUndiscountableAmount(undiscountableAmount).setSellerId(sellerId).setBody(body).setOperatorId(operatorId).setStoreId(storeId).setExtendParams(extendParams).setTimeoutExpress(timeoutExpress)
                //这里 http://kongjetlin.natapp1.cc 是我们购买的NATAPP的域名。用于使得阿里服务器可以返回支付信息给我们的本地项目
                .setNotifyUrl(PropertiesUtil.getProperty("alipay.callback.url"))//支付宝服务器主动通知商户服务器里指定的页面http路径,根据需要设置
                .setGoodsDetailList(goodsDetailList);

        /** 这里调用tradePrecreate()方法，将用户订单的信息封装完毕后发送给支付宝，用于生成订单，
         支付宝生成订单成功后，会将相应的返回信息封装到 response（包括支付二维码信息），然后我们下面取出相应的返回信息进行提示，
         并通过 ZxingUtils.getQRCodeImge() 方法生成支付二维码，并上传到二维码图片服务器，以便前端可以展示给用户。
         * */
        AlipayF2FPrecreateResult result = tradeService.tradePrecreate(builder);
        switch (result.getTradeStatus())
        {
            case SUCCESS:
                log.info("支付宝预下单成功: )");

                AlipayTradePrecreateResponse response = result.getResponse();
                dumpResponse(response);


                //生成二维码，并保存到 path 下的 upload 文件夹
                //先保证 path 路径的文件夹都存在
                File folder = new File(path);
                if (!folder.exists())
                {
                    folder.setWritable(true);
                    folder.mkdirs();
                }

                /** 说明：
                 1）这里 path 指的是二维码在本地的存储路径（文件夹），qrPath=path+二维码文件名，即二维码的存储路径（包含图片）
                 qrFileName指的是二维码的文件名；
                 targetFile=new File(path, qrFileName)，这个是二维码的完整路径的File对象，用于上传二维码到图片服务器。
                 最后，我们将二维码上传到图片服务器，qrUrl=域名+二维码名，这样我们就可以通过url访问到图片服务器的图片，并在前端展示。

                 2）流程：先生成二维码保存路径 qrPath，并将二维码生成保存到这个路径（根据支付宝的返回信息生成二维码），
                 随后根据文件名生成 targetFile，将二维码上传到文件服务器，并生成图片服务器的访问url，这样前端就能展示相应信息。
                 */

                // 需要修改为运行机器上的路径。注意这里path后面没有“/”，为了保证路径正确，path后必须加“/”，否则路径就是 xxxx/uploadqr.png
                String qrPath = String.format(path + "/qr-%s.png", response.getOutTradeNo());//订单号会替换到 “%s” 处，这里获取二维码的路径名
                String qrFileName = String.format("qr-%s.png", response.getOutTradeNo());//二维码的文件名
                ZxingUtils.getQRCodeImge(response.getQrCode(), 256, qrPath);//生成二维码到指定路径qrPath

                File targetFile = new File(path, qrFileName);//目标文件（二维码）的访问路径
                try
                {
                    FTPUtil.uploadFile(Lists.newArrayList(targetFile));//将二维码上传到图片服务器，这样前端才能访问到它
                }
                catch (IOException e)
                {
                    log.error("上传二维码异常", e);
                }
                log.info("filePath:" + qrPath);
                //这个二维码图片在文件服务器的访问url: 域名+图片名
                String qrUrl = PropertiesUtil.getProperty("ftp.server.http.prefix") + targetFile.getName();
                resultMap.put("qrUrl", qrUrl);
                return ServerResponse.createBySuccess(resultMap);

            case FAILED:
                log.error("支付宝预下单失败!!!");
                return ServerResponse.createByErrorMessage("支付宝预下单失败!!!");

            case UNKNOWN:
                log.error("系统异常，预下单状态未知!!!");
                return ServerResponse.createByErrorMessage("系统异常，预下单状态未知!!!");

            default:
                log.error("不支持的交易状态，交易返回异常!!!");
                return ServerResponse.createByErrorMessage("不支持的交易状态，交易返回异常!!!");
        }
    }

    // 简单打印应答
    private void dumpResponse(AlipayResponse response)
    {
        if (response != null)
        {
            log.info(String.format("code:%s, msg:%s", response.getCode(), response.getMsg()));
            if (StringUtils.isNotEmpty(response.getSubCode()))
            {
                log.info(String.format("subCode:%s, subMsg:%s", response.getSubCode(), response.getSubMsg()));
            }
            log.info("body:" + response.getBody());
        }


    }

    //对支付后支付宝返回的回调信息进行处理（签名已经验证通过）
    public ServerResponse aliCallback(Map<String , String> params)
    {
        //获取订单号
        Long orderNo = Long.parseLong(params.get("out_trade_no"));
        //获取支付宝的支付流水号
        String tradeNo = params.get("trade_no");
        //获取交易状态
        String tradeStatus = params.get("trade_status");

        //注意区分 订单状态 以及 支付宝交易状态

        //根据订单号，验证该订单是否存在
        Order order = orderMapper.selectByOrderNo(orderNo);
        if(order == null)
            return ServerResponse.createByErrorMessage("非快乐慕商城的订单,回调忽略");

        /** 关于重复回调的说明
        用户还没有支付成功之前，订单状态都是小于20的.当用户支付成功，支付宝就会触发回调，此时订单状态还是小于20，
        然后我们代码接收到回调信息后，更新订单的状态为20。如果支付宝下一次再次回调，此时订单状态为20或者大于20，就说明支付宝重复回调。
        另外，需要注意的是，不仅仅是支付成功会回调，等待买家付款也会回调（买家扫码后还没有支付）。
         */
        if(order.getStatus() >= Const.OrderStatusEnum.PAID.getCode())
        {
            /*
            支付宝正常重复回调，我们这里不能返回失败，而要返回成功，因为我们在OrderController中会对回调是否验证通过进行判断，
            如果这里返回失败，我们Controller就会返回fail给支付宝，这样支付宝就会以为我们还不知道订单支付成功，就会继续重复回调。
            如果返回成功，支付宝就知道我们已经知道订单支付成功，并且我们对订单信息进行修改，支付宝就不会重复回调！
             */
            return ServerResponse.createBySuccess("支付宝重复调用");
        }
        /*
        只有交易通知状态为 TRADE_SUCCESS 或 TRADE_FINISHED 时，支付宝才会认定为买家付款成功。
        我们这里判断支付宝返回的回调信息中的交易状态是不是 TRADE_SUCCESS，如果是，就说明用户付款交易成功，我们就更新订单状态。
         */
        if(Const.AlipayCallback.TRADE_STATUS_TRADE_SUCCESS.equals(tradeStatus))
        {
            //交易成功
            order.setPaymentTime(DateTimeUtil.strToDate(params.get("gmt_payment")));//更新订单的付款时间到数据库，注意转为Date
            order.setStatus(Const.OrderStatusEnum.PAID.getCode());//将订单状态设置为已付款
            orderMapper.updateByPrimaryKeySelective(order);//更新数据库订单状态
        }

        /** 创建支付信息对象
        无论当前的支付状态是："WAIT_BUYER_PAY"、TRADE_CLOSED、TRADE_SUCCESS、TRADE_FINISHED，
         只要支付宝回调信息回来，我们就必须将当前的支付状态封装成为PayInfo对象，并插入到数据库，
         即一个订单在数据库中可能有几个字符状态对象！
         */
        PayInfo payInfo = new PayInfo();
        payInfo.setUserId(order.getUserId());
        payInfo.setOrderNo(order.getOrderNo());
        payInfo.setPlatformStatus(tradeStatus);//支付宝的支付状态
        payInfo.setPlatformNumber(tradeNo);//支付宝支付流水号
        payInfo.setPayPlatform(Const.PayPlatformEnum.ALIPAY.getCode());

        //将字符信息插入到数据库
        payInfoMapper.insert(payInfo);

        return ServerResponse.createBySuccess();//处理成功
    }


    //根据用户id与订单号，查询订单支付状态（加上用户查询是为了避免出现横向越权）
    public ServerResponse queryOrderPayStatus(Integer userId , Long orderNo)
    {
        Order order = orderMapper.selectByUserIdAndOrderNo(userId, orderNo);
        if(order == null)
            return ServerResponse.createByErrorMessage("用户没有该订单");

        //当用户有该订单的时候，查询该订单是否支付
        if(order.getStatus() >= Const.OrderStatusEnum.PAID.getCode())
            return ServerResponse.createBySuccess();
        return ServerResponse.createByError();
    }

//-------------------------------------------------------------------------关闭订单

    /**
     * 这个方法用于设置在添加订单 hour 时间后，如果没有对订单进行付款，会对订单进行关闭
     * @param hour
     */
    @Override
    public void closeOrder(int hour)
    {
        //1、计算出当前时间之前的 hour 时间
        Date closeDateTime = DateUtils.addHours(new Date(), -hour);

        //2、查询出所有 closeDateTime之前，所有没有付款的订单，这些订单都需要进行关闭
        List<Order> orderList = orderMapper.selectOrderStatusByCreateTime(Const.OrderStatusEnum.NO_PAY.getCode(), DateTimeUtil.dateToStr(closeDateTime));

        //3、遍历订单集合，查询出每一个订单集合锁对应的订单项
        for (Order order : orderList)
        {
            //3.1 根据订单号，查询出该订单所有的订单项集合
            List<OrderItem> orderItemList = orderItemMapper.getByOrderNo(order.getOrderNo());
            //3.2 对每一个订单项进行遍历
            for (OrderItem orderItem : orderItemList)
            {
                //3.2.1 查询出该订单项对应的产品的库存
                //一定要用主键where条件（走的索引），才会使用行锁，而不是锁表。同时必须是支持MySQL的InnoDB。
                Integer stock = productMapper.selectStockByProductId(orderItem.getProductId());

                //考虑到已生成的订单里的商品，被删除的情况
                if(stock == 0)
                    continue;//如果产品被删除，关单后不需要更新这个产品的数量
                //更新该产品的库存
                Product product = new Product();
                product.setId(orderItem.getProductId());
                product.setStock(stock+orderItem.getQuantity());//产品库存+订单项内产品的购买数量
                //只有产品执行更新，此时事务提交（MYSQL默认更新自动提交事务），行锁才会解开，其他的查询语句才可以查询这个产品的行
                productMapper.updateByPrimaryKeySelective(product);
            }
            //更新完订单内产品的库存，就可以关闭这个订单
            orderMapper.closeOrderByOrderId(order.getId());
            log.info("关闭订单OrderNo：{}",order.getOrderNo());
        }
    }

}
