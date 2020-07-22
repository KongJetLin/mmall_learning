package com.mmall.dao;

import com.mmall.pojo.OrderItem;
import org.apache.ibatis.annotations.Param;

import java.util.List;

public interface OrderItemMapper {
    int deleteByPrimaryKey(Integer id);

    int insert(OrderItem record);

    int insertSelective(OrderItem record);

    OrderItem selectByPrimaryKey(Integer id);

    int updateByPrimaryKeySelective(OrderItem record);

    int updateByPrimaryKey(OrderItem record);

    //-------------------------------------

    //根据用户id与订单号，查询这个用户的这个订单中，有多少个订单项
//    List<OrderItem> getOrderByOrderNoUserId(@Param("orderNo")Long orderNo , @Param("userId")Integer userId);


    //批量插入 OrderItem对象
    void batchInsert(@Param("orderItemList") List<OrderItem> orderItemList);

    //根据订单号，查询出所有的 OrderItem
    List<OrderItem> getByOrderNo(@Param("orderNo")Long orderNo);

    //根据订单号和用户id，查询出所有的 OrderItem
    List<OrderItem> getByOrderNoUserId(@Param("orderNo")Long orderNo ,  @Param("userId")Integer userId);
}