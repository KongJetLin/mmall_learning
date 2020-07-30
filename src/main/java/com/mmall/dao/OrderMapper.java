package com.mmall.dao;

import com.mmall.pojo.Order;
import org.apache.ibatis.annotations.Param;

import java.util.List;

public interface OrderMapper {
    int deleteByPrimaryKey(Integer id);

    int insert(Order record);

    int insertSelective(Order record);

    Order selectByPrimaryKey(Integer id);

    int updateByPrimaryKeySelective(Order record);

    int updateByPrimaryKey(Order record);

    //-----------------------------------

    Order selectByUserIdAndOrderNo(@Param("userId")Integer userId, @Param("orderNo")Long orderNo);

    Order selectByOrderNo(Long orderNo);

    List<Order> selectByUserId(Integer userId);

    List<Order> selectAllOrder();

    //二期新增定时关单
    //根据订单状态和订单创建时间查找相应的订单，即查询创建时间在 date 时间之前，且订单状态为未付款的所有订单
    List<Order> selectOrderStatusByCreateTime(@Param("status") Integer status , @Param("date") String date);

    //根据订单id，关闭这个订单，即将订单状态设置为0（订单关闭）
    int closeOrderByOrderId(Integer id);

}