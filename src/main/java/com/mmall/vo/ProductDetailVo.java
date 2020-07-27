package com.mmall.vo;

import java.math.BigDecimal;

/**
 * 商品详情的vo，用于封装 product 数据，展示商品详情
 */
public class ProductDetailVo
{
    //首先是 Product 原有的属性
    private Integer  id;
    private Integer categoryId;
    private String name;
    private String subtitle;
    private String mainImage;
    private String subImages;
    private String detail;
    private BigDecimal price; //价格是 BigDecimal类型，数据库也是 decimal 类型
    private Integer stock;
    private Integer status;
    private String createTime;
    private String updateTime;

    //图片服务器的 url，imageHost再拼上图片在图片服务器内的地址（mainImage就是地址），这样就可以获取图片的真实地址
    private String imageHost;
    //父分类
    private Integer parentCategoryId;

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

    public String getSubImages()
    {
        return subImages;
    }

    public void setSubImages(String subImages)
    {
        this.subImages = subImages;
    }

    public String getDetail()
    {
        return detail;
    }

    public void setDetail(String detail)
    {
        this.detail = detail;
    }

    public BigDecimal getPrice()
    {
        return price;
    }

    public void setPrice(BigDecimal price)
    {
        this.price = price;
    }

    public Integer getStock()
    {
        return stock;
    }

    public void setStock(Integer stock)
    {
        this.stock = stock;
    }

    public Integer getStatus()
    {
        return status;
    }

    public void setStatus(Integer status)
    {
        this.status = status;
    }

    public String getCreateTime()
    {
        return createTime;
    }

    public void setCreateTime(String createTime)
    {
        this.createTime = createTime;
    }

    public String getUpdateTime()
    {
        return updateTime;
    }

    public void setUpdateTime(String updateTime)
    {
        this.updateTime = updateTime;
    }

    public String getImageHost()
    {
        return imageHost;
    }

    public void setImageHost(String imageHost)
    {
        this.imageHost = imageHost;
    }

    public Integer getParentCategoryId()
    {
        return parentCategoryId;
    }

    public void setParentCategoryId(Integer parentCategoryId)
    {
        this.parentCategoryId = parentCategoryId;
    }
}
