package com.mmall.service.impl;

import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import com.google.common.collect.Maps;
import com.mmall.common.ServerResponse;
import com.mmall.dao.ShippingMapper;
import com.mmall.pojo.Shipping;
import com.mmall.service.IShippingService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

@Service("iShippingService")
public class ShippingServiceImpl implements IShippingService
{
    @Autowired
    private ShippingMapper shippingMapper;


    //根据用户id以及地址对象添加新的地址的方法
    @Override
    public ServerResponse add(Integer userId, Shipping shipping)
    {
        shipping.setUserId(userId);
        int rowCount = shippingMapper.insert(shipping);//将这个对象插入数据库
        if(rowCount > 0)
        {
            //插入成功，我们需要将该地址对象的id存储到HaspMap集合返回给前端
            Map result = Maps.newHashMap();
            //在数据库 insert 方法添加 useGeneratedKeys="true" keyProperty="id"，这样我们插入数据库后生成的id就会插入到shipping对象
            result.put("shippingId" , shipping.getId());
            return ServerResponse.createBySuccess("新建地址成功" , result);
        }
        return ServerResponse.createBySuccessMessage("新建地址失败");
    }

    @Override
    public ServerResponse<String> del(Integer userId, Integer shippingId)
    {
        int rowCount = shippingMapper.deleteByShippingIdUserId(userId , shippingId);
        if(rowCount > 0){
            return ServerResponse.createBySuccess("删除地址成功");
        }
        return ServerResponse.createByErrorMessage("删除地址失败");
    }

    @Override
    public ServerResponse update(Integer userId, Shipping shipping)
    {
        shipping.setUserId(userId);//注意原来的shipping对象并没有传递用户id过来，我们为其添加
        int rowCount = shippingMapper.updateByShipping(shipping);
        if(rowCount > 0){
            return ServerResponse.createBySuccess("更新地址成功");
        }
        return ServerResponse.createByErrorMessage("更新地址失败");
    }

    //根据地址id与用户id，查询某个地址信息
    @Override
    public ServerResponse<Shipping> select(Integer userId, Integer shippingId)
    {
        //查询的时候同样要根据地址与用户id，避免横向越权
        Shipping shipping = shippingMapper.selectByShippingIdUserId(userId, shippingId);
        if(shipping == null)
            return ServerResponse.createBySuccessMessage("无法查询到该地址");
        return ServerResponse.createBySuccess("查询地址成功" , shipping);
    }

    //查询某个用户的所有地址详情地址
    @Override
    public ServerResponse<PageInfo> list(Integer userId, int pageNum, int pageSize)
    {
        PageHelper.startPage(pageNum , pageSize);
        List<Shipping> shippingList = shippingMapper.selectByUserId(userId);
        PageInfo pageInfo = new PageInfo(shippingList);
        return ServerResponse.createBySuccess(pageInfo);
    }


}
