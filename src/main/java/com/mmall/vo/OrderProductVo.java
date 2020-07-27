package com.mmall.vo;

import java.math.BigDecimal;
import java.util.List;

public class OrderProductVo
{
    //订单项对象封装对象 ： OrderItemVo 的集合
    private List<OrderItemVo> orderItemVoList;
    private BigDecimal productTotalPrice;//所有订单项对象的总价
    private String imageHost;//图片访问地址（需要显示订单产品的一张图片）

    public List<OrderItemVo> getOrderItemVoList()
    {
        return orderItemVoList;
    }

    public void setOrderItemVoList(List<OrderItemVo> orderItemVoList)
    {
        this.orderItemVoList = orderItemVoList;
    }

    public BigDecimal getProductTotalPrice()
    {
        return productTotalPrice;
    }

    public void setProductTotalPrice(BigDecimal productTotalPrice)
    {
        this.productTotalPrice = productTotalPrice;
    }

    public String getImageHost()
    {
        return imageHost;
    }

    public void setImageHost(String imageHost)
    {
        this.imageHost = imageHost;
    }
}
