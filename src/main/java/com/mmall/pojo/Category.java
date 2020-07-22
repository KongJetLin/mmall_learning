package com.mmall.pojo;

import java.util.Date;
import java.util.Objects;

public class Category {
    private Integer id;

    private Integer parentId;

    private String name;

    private Boolean status;

    private Integer sortOrder;

    private Date createTime;

    private Date updateTime;

    public Category(Integer id, Integer parentId, String name, Boolean status, Integer sortOrder, Date createTime, Date updateTime) {
        this.id = id;
        this.parentId = parentId;
        this.name = name;
        this.status = status;
        this.sortOrder = sortOrder;
        this.createTime = createTime;
        this.updateTime = updateTime;
    }

    public Category() {
        super();
    }

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public Integer getParentId() {
        return parentId;
    }

    public void setParentId(Integer parentId) {
        this.parentId = parentId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name == null ? null : name.trim();
    }

    public Boolean getStatus() {
        return status;
    }

    public void setStatus(Boolean status) {
        this.status = status;
    }

    public Integer getSortOrder() {
        return sortOrder;
    }

    public void setSortOrder(Integer sortOrder) {
        this.sortOrder = sortOrder;
    }

    public Date getCreateTime() {
        return createTime;
    }

    public void setCreateTime(Date createTime) {
        this.createTime = createTime;
    }

    public Date getUpdateTime() {
        return updateTime;
    }

    public void setUpdateTime(Date updateTime) {
        this.updateTime = updateTime;
    }

    //-------------------------------重写 hashCode() 与 equals() ，只要id相同，我们就认为2个Category对象相同
    //快捷键：ALT+INSERT
    @Override
    public boolean equals(Object o)
    {
        if (this == o) return true;//当前对象与传入的对象是同一个对象（地址相同），返回true
        if (o == null || getClass() != o.getClass()) return false;//obj为null，或者obj与this通过2个不停的类对象创建，直接返回false
        Category category = (Category) o;
        return !(id != null ? !id.equals(category.id) : category.id != null);
    }

    @Override
    public int hashCode()
    {
        //我们比较Category对象的id，即hash值为该对象的id，如果id为空，则返回0（用id来区分Category对象是否相等）
        return id != null ? id : 0;
    }
}