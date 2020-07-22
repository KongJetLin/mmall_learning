package com.mmall.vo;

import java.math.BigDecimal;

/**
 * 结合了产品和购物车的一个抽象对象
 */
public class CartProductVo
{
    //首先是购物车的属性
    private Integer id;//购物车id
    private Integer userId;
    private Integer productId;
    private Integer quantity;//购物车中此商品的数量
    private Integer productChecked;//此商品是否勾选

    //其次是产品的属性
    private String productName;
    private String productSubtitle;
    private String productMainImage;
    private BigDecimal productPrice;
    private Integer productStatus;
    private Integer productStock;

    //新增属性
    private BigDecimal productTotalPrice;//购物车中此产品的总价
    private String limitQuantity;//限制数量的一个返回结果，即判断我们选择的产品数量是否大于产品的库存

    public Integer getId()
    {
        return id;
    }

    public void setId(Integer id)
    {
        this.id = id;
    }

    public Integer getUserId()
    {
        return userId;
    }

    public void setUserId(Integer userId)
    {
        this.userId = userId;
    }

    public Integer getProductId()
    {
        return productId;
    }

    public void setProductId(Integer productId)
    {
        this.productId = productId;
    }

    public Integer getQuantity()
    {
        return quantity;
    }

    public void setQuantity(Integer quantity)
    {
        this.quantity = quantity;
    }

    public Integer getProductChecked()
    {
        return productChecked;
    }

    public void setProductChecked(Integer productChecked)
    {
        this.productChecked = productChecked;
    }

    public String getProductName()
    {
        return productName;
    }

    public void setProductName(String productName)
    {
        this.productName = productName;
    }

    public String getProductSubtitle()
    {
        return productSubtitle;
    }

    public void setProductSubtitle(String productSubtitle)
    {
        this.productSubtitle = productSubtitle;
    }

    public String getProductMainImage()
    {
        return productMainImage;
    }

    public void setProductMainImage(String productMainImage)
    {
        this.productMainImage = productMainImage;
    }

    public BigDecimal getProductPrice()
    {
        return productPrice;
    }

    public void setProductPrice(BigDecimal productPrice)
    {
        this.productPrice = productPrice;
    }

    public Integer getProductStatus()
    {
        return productStatus;
    }

    public void setProductStatus(Integer productStatus)
    {
        this.productStatus = productStatus;
    }

    public Integer getProductStock()
    {
        return productStock;
    }

    public void setProductStock(Integer productStock)
    {
        this.productStock = productStock;
    }

    public BigDecimal getProductTotalPrice()
    {
        return productTotalPrice;
    }

    public void setProductTotalPrice(BigDecimal productTotalPrice)
    {
        this.productTotalPrice = productTotalPrice;
    }

    public String getLimitQuantity()
    {
        return limitQuantity;
    }

    public void setLimitQuantity(String limitQuantity)
    {
        this.limitQuantity = limitQuantity;
    }
}
