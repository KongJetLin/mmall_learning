package com.mmall.controller.portal;

import com.github.pagehelper.PageInfo;
import com.mmall.common.ServerResponse;
import com.mmall.service.IProductService;
import com.mmall.vo.ProductDetailVo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

/**
 * 前台产品的相关方法
 */
@Controller
@RequestMapping("/product/")
public class ProductController
{
    @Autowired
    private IProductService iProductService;


    /**
     * 前台获取商品详情的方法
     * @param productId
     * @return
     */
    @RequestMapping("detail.do")
    @ResponseBody
    public ServerResponse<ProductDetailVo> detail(Integer productId)
    {
        return iProductService.getProductDetail(productId);
    }


    /**
     * 根据 keyword 关键字（非必须），分类id（非必须）进行分页查询，将结果集封装到 PageInfo
     * 搜索的时候要么根据keyword，要么根据分类，不能能都没有，都没有则认为是参数错误
     * @param keyword
     * @param categoryId
     * @param pageNum
     * @param pageSize
     * @return
     */
    @RequestMapping("list.do")
    @ResponseBody
    public ServerResponse<PageInfo> list(@RequestParam(value="keyword",required = false) String keyword,
                                         @RequestParam(value="categoryId",required = false) Integer categoryId,
                                         @RequestParam(value="pageNum",defaultValue = "1")int pageNum,
                                         @RequestParam(value="pageSize",defaultValue = "10")int pageSize,
                                         @RequestParam(value="orderBy",defaultValue = "")String orderBy)
    {
        return iProductService.getProductByKeywordCategory(keyword , categoryId , pageNum , pageSize , orderBy);
    }
}
