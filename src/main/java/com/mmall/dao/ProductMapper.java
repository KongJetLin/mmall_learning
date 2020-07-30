package com.mmall.dao;

import com.mmall.pojo.Category;
import com.mmall.pojo.Product;
import org.apache.ibatis.annotations.Param;

import java.util.List;

public interface ProductMapper {
    int deleteByPrimaryKey(Integer id);

    int insert(Product record);

    int insertSelective(Product record);

    Product selectByPrimaryKey(Integer id);

    int updateByPrimaryKeySelective(Product record);

    int updateByPrimaryKey(Product record);

    //----------------------------------

    List<Product> selectList();

    List<Product> selectByNameAndProductId(@Param("productId")Integer productId , @Param("productName")String productName);

    List<Product> selectByNameAndCategoryIds(@Param("productName")String productName , @Param("categoryIdList")List<Integer> categoryIdList);

    //根据产品id查询出产品的库存，这里一定要用Integer，因为int无法为NULL，考虑到很多商品已经删除的情况。
    //如果我们商品被删除，数据库查不到库存，返回的结果是null，int无法接收，需要使用Integer
    Integer selectStockByProductId(Integer id);
}