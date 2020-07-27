package com.mmall.service.impl;

import com.google.common.base.Splitter;
import com.google.common.collect.Lists;
import com.mmall.common.Const;
import com.mmall.common.ResponseCode;
import com.mmall.common.ServerResponse;
import com.mmall.dao.CartMapper;
import com.mmall.dao.ProductMapper;
import com.mmall.pojo.Cart;
import com.mmall.pojo.Product;
import com.mmall.service.ICartService;
import com.mmall.util.BigDecimalUtil;
import com.mmall.util.PropertiesUtil;
import com.mmall.vo.CartProductVo;
import com.mmall.vo.CartVo;
import org.apache.commons.collections.CollectionUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;

@Service("iCartService")
public class CartServiceImpl implements ICartService
{
    @Autowired
    private CartMapper cartMapper;
    @Autowired
    private ProductMapper productMapper;

    //添加产品到购物车
    @Override
    public ServerResponse<CartVo> add(Integer userId , Integer productId , Integer count)
    {
        //我们需要根据 产品id和用户id，查询购物车表，看一下这个用户之前在购物车里面添加了多少个产品，
        //然后我们将新的count数量添加到相应的购物车对象
        if(productId == null || count == null)
            //产品id 与 添加的数量 必须都有，不然参数出错
            return ServerResponse.createByErrorCodeMessage(ResponseCode.ILLEGAL_ARGUMENT.getCode() , ResponseCode.ILLEGAL_ARGUMENT.getDesc());

        System.out.println("--------------------------------------------");
        System.out.println("--------------"+userId+"--------------------");
        System.out.println("--------------------------------------------");

        Cart cart = cartMapper.selectCartByUserIdProductId(userId, productId);
        if(cart == null)
        {
            //这个产品不在这个购物车里,需要新增一个这个产品的记录
            Cart cartItem = new Cart();
            cartItem.setQuantity(count);
            cartItem.setUserId(userId);
            //将该购物车对象设置为选中状态（因为我们添加购买产品的时候，会默认选中）
            cartItem.setChecked(Const.Cart.CHECKED);
            cartItem.setProductId(productId);
            cartMapper.insert(cartItem);
        }
        else
        {
            //这个产品已经在购物车里了.
            //如果产品已存在,数量相加
            count = count + cart.getQuantity();
            cart.setQuantity(count);
            cartMapper.updateByPrimaryKeySelective(cart);
        }
        //将商品添加到购物车后，根据该用户的id，封装其所有购物车数据到CartVo，返回给前端显示 添加后的购物车
        return list(userId);
    }

    //更新购物车
    @Override
    public ServerResponse<CartVo> update(Integer userId,Integer productId,Integer count)
    {
        if(productId == null || count == null)
            return ServerResponse.createByErrorCodeMessage(ResponseCode.ILLEGAL_ARGUMENT.getCode() , ResponseCode.ILLEGAL_ARGUMENT.getDesc());
        //根据用户id和产品id，查询有没有对应的购物车对象，如果有，更新该对象的数量
        Cart cart = cartMapper.selectCartByUserIdProductId(userId, productId);
        if(cart != null)
        {
            cart.setQuantity(count);
        }
        cartMapper.updateByPrimaryKeySelective(cart);
        //更新用户的购物车数据后，同样需要用户的id，封装其所有购物车数据到CartVo，返回给前端显示 更新后的购物车
        return list(userId);
    }

    //根据产品ids的集合，删除购物车中这些产品的方法
    @Override
    public ServerResponse<CartVo> deleteProduct(Integer userId,String productIds)
    {
        //使用 guava 的方法，将保存id的字符串风格开来
        List<String> productList = Splitter.on(",").splitToList(productIds);
        if(CollectionUtils.isEmpty(productList))
            return ServerResponse.createByErrorCodeMessage(ResponseCode.ILLEGAL_ARGUMENT.getCode(),ResponseCode.ILLEGAL_ARGUMENT.getDesc());
        cartMapper.deleteByUserIdProductIds(userId, productList);

        //同样需要封装删除后的数据到CartVo
        return list(userId);
    }

    //查询显示整个购物车信息的接口，返回CartVo对象
    @Override
    public ServerResponse<CartVo> list (Integer userId){
        CartVo cartVo = this.getCartVoLimit(userId);
        return ServerResponse.createBySuccess(cartVo);
    }

    /**
    根据 userId 或者 productId 设置某个或者某些购物车对象的选中状态。
     如果我们单独使用 userId ，就是在使用“全选”或“全不选”功能，将某个用户购物车内的购物车对象全选或者全不选。
     如果使用的是 userId 或者 productId，就是将单个的购物车对象“选中”或者“取消选中”。
     无论是不是选中，最后都要返回 CartVo 对象
     */
    @Override
    public ServerResponse<CartVo> selectOrUnSelect(Integer userId,Integer productId,Integer checked)
    {
        //进行选中状态的设置
        cartMapper.checkedOrUncheckedProduct(userId,productId,checked);
        return this.list(userId);
    }

    //根据用户id查询该用户购物车内产品的数量
    @Override
    public ServerResponse<Integer> getCartProductCount(Integer userId)
    {
        if(userId == null)
            return ServerResponse.createBySuccess(0);
        return ServerResponse.createBySuccess(cartMapper.selectCartProductCount(userId));
    }


//-----------------------------------------------------------------------------------------------------辅助方法
    /**
     * 这个方法根据 Cart 对象与Cart对应 Product 对象，封装 cartProductVo 对象，由于Cart对象可能有多个（用户购买多个产品，一个产品对应一个购物车对象），
     * 而我们要显示的是用户购物车内所有的产品，因此需要将 List<Cart> 封装 为 List<CartProductVo>.
     * 另一方面，List<CartProductVo> 内的数据不完全，我们需要一个 CartVo 对象，封装 List<CartProductVo> 对象内属性集合成的某些数据，
     * 我们添加到购物车后，将 CartVo 返回给前端，才能显示完整的购物车
     * @param userId
     * @return
     */
    private CartVo getCartVoLimit(Integer userId)
    {
        CartVo cartVo = new CartVo();

        //先根据用户id查询出该用户的所有购物车对象
        List<Cart> cartList = cartMapper.selectCartByUserId(userId);
        //生成一个 CartProductVo
        List<CartProductVo> cartProductVoList = Lists.newArrayList();

        //初始化该用户所有购物车中的商品总价，即各个购物车对象内的产品（每个购物车对象只有一个产品，可能该产品购买多个）的价格相加，得到该用户购物车内商品总价
        BigDecimal cartTotalPrice = new BigDecimal("0");

        //如果查询出来该用户的购物车对象不为null，则进行 CartProductVo 对象的封装
        if(CollectionUtils.isNotEmpty(cartList))
        {
            for (Cart cartItem : cartList)
            {
                CartProductVo cartProductVo = new CartProductVo();
                //将购物车的属性注入
                cartProductVo.setId(cartItem.getId());
                cartProductVo.setUserId(cartItem.getUserId());
                cartProductVo.setProductId(cartItem.getProductId());

                //将产品属性注入
                Product product = productMapper.selectByPrimaryKey(cartItem.getProductId());
                if(product != null)
                {
                    cartProductVo.setProductMainImage(product.getMainImage());
                    cartProductVo.setProductName(product.getName());
                    cartProductVo.setProductSubtitle(product.getSubtitle());
                    cartProductVo.setProductStatus(product.getStatus());
                    cartProductVo.setProductPrice(product.getPrice());
                    cartProductVo.setProductStock(product.getStock());

                    //判断库存
                    int buyLimitCount = 0;
                    if(product.getStock() >= cartItem.getQuantity())
                    {
                        buyLimitCount = cartItem.getQuantity();
                        //如果产品库存大于购物车中选择的产品数量，此时购物车中此产品的数量不需要改变
                        cartProductVo.setLimitQuantity(Const.Cart.LIMIT_NUM_SUCCESS);//限制产品数量成功
                    }
                    else
                    {
                        buyLimitCount = product.getStock();
                        cartProductVo.setLimitQuantity(Const.Cart.LIMIT_NUM_FAIL);//限制产品数量失败
                        //此时需要将购物车中的库存设置为有效库存
                        Cart cartForQuantity = new Cart();
                        cartForQuantity.setId(cartItem.getId());
                        cartForQuantity.setQuantity(buyLimitCount);
                        cartMapper.updateByPrimaryKeySelective(cartForQuantity);//更新数据库中此产品的数量，只更新 quantity
                    }

                    //注入购物车属性：数量，该数量经过上面的处理，不会大于商品的库存
                    cartProductVo.setQuantity(buyLimitCount);
                    //计算该购物车对象内产品的总价：该产品的价格 * 购物车内该产品的购买数量
                    cartProductVo.setProductTotalPrice(BigDecimalUtil.mul(cartItem.getQuantity() , product.getPrice().doubleValue()));
                    //是否选中
                    cartProductVo.setProductChecked(cartItem.getChecked());
                }
                //判断这个购物车是否被勾选，如果勾选，将这个购物车对象内产品的总价，添加到整个购物车的总价
                if(cartItem.getChecked() == Const.Cart.CHECKED)
                {
                    //加上次购物车对象中产品的总价
                    cartTotalPrice = BigDecimalUtil.add(cartTotalPrice.doubleValue() , cartProductVo.getProductTotalPrice().doubleValue());
                }
                //最后，将当前 Cart对象封装的 CartProductVo 放入 CartProductVoList
                cartProductVoList.add(cartProductVo);
            }
        }
        //最后，处理这个判断，CartVo 的 List<CartProductVo> productVoList 属性 与BigDecimal cartTotalPrice 属性封装完毕
        cartVo.setProductVoList(cartProductVoList);
        cartVo.setCartTotalPrice(cartTotalPrice);
        cartVo.setAllChecked(getAllCheckedStatus(userId));
        //获取访问FTP服务器存储图片位置的域名前缀，即：http://image.upload.com/
        cartVo.setImageHost(PropertiesUtil.getProperty("ftp.server.http.prefix"));

        return cartVo;
    }

    //判断是否全选的方法：我们用用户id与checked=0，去查询有没有这样的购物车对象，如果有，说明没有全选，否则全选
    private boolean getAllCheckedStatus(Integer userId)
    {
        if(userId == null)
            return false;
        return cartMapper.getCartProductCheckedStatusByUserId(userId) == 0;
    }



//---------------------------------------------------------------------------------------------------------------------




}
