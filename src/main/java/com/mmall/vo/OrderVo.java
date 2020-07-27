package com.mmall.vo;

import java.math.BigDecimal;
import java.util.List;

/*
封装创建订单时返回给前端订单数据的对象
 */
public class OrderVo
{
    //订单原有信息
    private Long orderNo;//订单号
    private BigDecimal payment;//订单总金额
    private Integer paymentType;//支付类型
    private Integer postage;//运费
    private Integer status;//订单状态
    private String paymentTime;//支付时间
    private Integer shippingId;//地址对象id
    //将时间全部转换为String
    private String createTime;//订单创建时间
    private String sendTime;//发货时间
    private String endTime;//订单结束时间
    private String closeTime;//订单关闭时间

    //新添加字段：
    private String paymentTypeDesc;//支付类型描述
    private String statusDesc;//状态描述

    //订单的明细：每一个订单的明细封装到 OrderItemVo 中，一个订单中有多个订单明细
    private List<OrderItemVo> orderItemVoList;
    //图片访问地址
    private String imageHost;
    private String receiverName;//收货人姓名
    //我们在查看订单详情的时候，还需具体的收货地址，这里封装收货地址对象
    private ShippingVo shippingVo;



    public Long getOrderNo()
    {
        return orderNo;
    }

    public void setOrderNo(Long orderNo)
    {
        this.orderNo = orderNo;
    }

    public BigDecimal getPayment()
    {
        return payment;
    }

    public void setPayment(BigDecimal payment)
    {
        this.payment = payment;
    }

    public Integer getPaymentType()
    {
        return paymentType;
    }

    public void setPaymentType(Integer paymentType)
    {
        this.paymentType = paymentType;
    }

    public Integer getPostage()
    {
        return postage;
    }

    public void setPostage(Integer postage)
    {
        this.postage = postage;
    }

    public Integer getStatus()
    {
        return status;
    }

    public void setStatus(Integer status)
    {
        this.status = status;
    }

    public String getPaymentTime()
    {
        return paymentTime;
    }

    public void setPaymentTime(String paymentTime)
    {
        this.paymentTime = paymentTime;
    }

    public Integer getShippingId()
    {
        return shippingId;
    }

    public void setShippingId(Integer shippingId)
    {
        this.shippingId = shippingId;
    }

    public String getCreateTime()
    {
        return createTime;
    }

    public void setCreateTime(String createTime)
    {
        this.createTime = createTime;
    }

    public String getSendTime()
    {
        return sendTime;
    }

    public void setSendTime(String sendTime)
    {
        this.sendTime = sendTime;
    }

    public String getEndTime()
    {
        return endTime;
    }

    public void setEndTime(String endTime)
    {
        this.endTime = endTime;
    }

    public String getCloseTime()
    {
        return closeTime;
    }

    public void setCloseTime(String closeTime)
    {
        this.closeTime = closeTime;
    }

    public String getPaymentTypeDesc()
    {
        return paymentTypeDesc;
    }

    public void setPaymentTypeDesc(String paymentTypeDesc)
    {
        this.paymentTypeDesc = paymentTypeDesc;
    }

    public String getStatusDesc()
    {
        return statusDesc;
    }

    public void setStatusDesc(String statusDesc)
    {
        this.statusDesc = statusDesc;
    }

    public List<OrderItemVo> getOrderItemVoList()
    {
        return orderItemVoList;
    }

    public void setOrderItemVoList(List<OrderItemVo> orderItemVoList)
    {
        this.orderItemVoList = orderItemVoList;
    }

    public String getImageHost()
    {
        return imageHost;
    }

    public void setImageHost(String imageHost)
    {
        this.imageHost = imageHost;
    }

    public String getReceiverName()
    {
        return receiverName;
    }

    public void setReceiverName(String receiverName)
    {
        this.receiverName = receiverName;
    }

    public ShippingVo getShippingVo()
    {
        return shippingVo;
    }

    public void setShippingVo(ShippingVo shippingVo)
    {
        this.shippingVo = shippingVo;
    }
}
