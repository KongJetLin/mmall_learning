package com.mmall.vo;

import java.math.BigDecimal;
import java.util.List;

/**
 * 用于封装购物车中相关的属性
 */
public class CartVo
{
    private List<CartProductVo> productVoList;//封装 CartProductVo对象
    private BigDecimal cartTotalPrice;//购物车中所有产品的总价
    private Boolean allChecked;
    private String imageHost;//购物车里面要显示一个图片，这里需要图片的地址

    public List<CartProductVo> getProductVoList()
    {
        return productVoList;
    }

    public void setProductVoList(List<CartProductVo> productVoList)
    {
        this.productVoList = productVoList;
    }

    public BigDecimal getCartTotalPrice()
    {
        return cartTotalPrice;
    }

    public void setCartTotalPrice(BigDecimal cartTotalPrice)
    {
        this.cartTotalPrice = cartTotalPrice;
    }

    public Boolean getAllChecked()
    {
        return allChecked;
    }

    public void setAllChecked(Boolean allChecked)
    {
        this.allChecked = allChecked;
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
