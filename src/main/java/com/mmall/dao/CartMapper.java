package com.mmall.dao;

import com.mmall.pojo.Cart;
import com.mmall.pojo.User;
import org.apache.ibatis.annotations.Param;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;

public interface CartMapper {
    int deleteByPrimaryKey(Integer id);

    int insert(Cart record);

    int insertSelective(Cart record);

    Cart selectByPrimaryKey(Integer id);

    int updateByPrimaryKeySelective(Cart record);

    int updateByPrimaryKey(Cart record);

    //-----------------------------------------购物车功能
    Cart selectCartByUserIdProductId(@Param("userId") Integer userId, @Param("productId")Integer productId);

    List<Cart> selectCartByUserId(Integer userId);

    int getCartProductCheckedStatusByUserId(Integer userId);

    int deleteByUserIdProductIds(@Param("userId") Integer userId, @Param("productIdList")List<String> productIdList);

    int checkedOrUncheckedProduct(@Param("userId") Integer userId , @Param("productId")Integer productId , @Param("checked") Integer checked);

    int selectCartProductCount(@Param("userId") Integer userId);

    //--------------------------------------------订单功能

    List<Cart> selectCheckedCartByUserId(Integer userId);
}