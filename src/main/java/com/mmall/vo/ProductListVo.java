package com.mmall.vo;

import java.math.BigDecimal;

/**
 * 分类展示商品后，返回商品信息，商品信息不需要 product那么详细，则用 ProductListVo 封装
 */
public class ProductListVo
{
    //原来的属性，只需要 id,name,categoryId,subtitle,mainImage,price,status
    private Integer id;
    private Integer categoryId;
    private String name;
    private String subtitle;
    private String mainImage;
    private BigDecimal price;
    private Integer status;

    //新的属性
    private String imageHost;

    public Integer getId()
    {
        return id;
    }

    public void setId(Integer id)
    {
        this.id = id;
    }

    public Integer getCategoryId()
    {
        return categoryId;
    }

    public void setCategoryId(Integer categoryId)
    {
        this.categoryId = categoryId;
    }

    public String getName()
    {
        return name;
    }

    public void setName(String name)
    {
        this.name = name;
    }

    public String getSubtitle()
    {
        return subtitle;
    }

    public void setSubtitle(String subtitle)
    {
        this.subtitle = subtitle;
    }

    public String getMainImage()
    {
        return mainImage;
    }

    public void setMainImage(String mainImage)
    {
        this.mainImage = mainImage;
    }

    public BigDecimal getPrice()
    {
        return price;
    }

    public void setPrice(BigDecimal price)
    {
        this.price = price;
    }

    public Integer getStatus()
    {
        return status;
    }

    public void setStatus(Integer status)
    {
        this.status = status;
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
