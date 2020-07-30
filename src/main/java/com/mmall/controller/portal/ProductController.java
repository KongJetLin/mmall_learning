package com.mmall.controller.portal;

import com.github.pagehelper.PageInfo;
import com.mmall.common.ServerResponse;
import com.mmall.service.IProductService;
import com.mmall.vo.ProductDetailVo;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

/**
 * 前台产品的相关方法
 * 我们将这两个接口改造成为 restful 风格
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
     * 使用restful风格
     * 不使用 restful风格，请求url为：http://www.happymmall.com/product/detail.do?productId=26
     * 使用restful风格，请求url为：http://www.happymmall.com/product/26
     * 这里，26 就代表编号为26的产品资源，restful使用的是资源定位的方式来请求。
     */
    @RequestMapping(value = "/{productId}" , method = RequestMethod.GET)
    @ResponseBody
    public ServerResponse<ProductDetailVo> detailRestful(@PathVariable Integer productId)
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

    /**
     * 使用 restful 的方式请求，
     * 请求格式：http://www.happymmall.com/product/%E7%BE%8E%E7%9A%84/100006/1/10/price_asc
     * 这里所有的资源都不能为空，只要有一个资源为空（假设是分页数为空），就会报404，因为restful的资源定位必须是准确的，不能缺少。
     * 这样也就说明，不是所有的资源都适合使用restful。
     *
     * 其实这里 keyword 与 categoryId 只要有一个不为null即可搜索，但是restful的方式要求这两个必须都要有，否则资源无法占位。
     * 见下面例子的解析
     */
    @RequestMapping(value = "/{keyword}/{categoryId}/{pageNum}/{pageSize}/{orderBy}",method = RequestMethod.GET)
    @ResponseBody
    public ServerResponse<PageInfo> listRESTful(@PathVariable(value = "keyword")String keyword,
                                                @PathVariable(value = "categoryId")Integer categoryId,
                                                @PathVariable(value = "pageNum") Integer pageNum,
                                                @PathVariable(value = "pageSize") Integer pageSize,
                                                @PathVariable(value = "orderBy") String orderBy)
    {
        //这里需要手动设置默认值，因为 @PathVariable 不能设置默认值以及是否必须
        if(pageNum == null){
            pageNum = 1;
        }
        if(pageSize == null){
            pageSize = 10;
        }
        if(StringUtils.isBlank(orderBy)){
            orderBy = "price_asc";
        }

        return iProductService.getProductByKeywordCategory(keyword , categoryId , pageNum , pageSize , orderBy);
    }

    //------------------------------------------------------------------------------------------------------------------------------------------

    /**
     * 这个接口只传递 categoryId，没有 keyword
     * http://www.happymmall.com/product/100006/1/10/price_asc
     * 使用这个方法，请求进来的时候，SpringMVC 并不知道应该将资源分配给 那一个 listRESTfulBadcase 方法，因此会出现异常，
     * 就是2个 listRESTfulBadcase 方法都会匹配。
     */
    @RequestMapping(value = "/{categoryId}/{pageNum}/{pageSize}/{orderBy}",method = RequestMethod.GET)
    @ResponseBody
    public ServerResponse<PageInfo> listRESTfulBadcase(@PathVariable(value = "categoryId")Integer categoryId,
                                                @PathVariable(value = "pageNum") Integer pageNum,
                                                @PathVariable(value = "pageSize") Integer pageSize,
                                                @PathVariable(value = "orderBy") String orderBy)
    {
        //这里需要手动设置默认值，因为 @PathVariable 不能设置默认值以及是否必须
        if(pageNum == null){
            pageNum = 1;
        }
        if(pageSize == null){
            pageSize = 10;
        }
        if(StringUtils.isBlank(orderBy)){
            orderBy = "price_asc";
        }

        return iProductService.getProductByKeywordCategory("" , categoryId , pageNum , pageSize , orderBy);
    }

    /**
     * 这个接口只传递 keyword ，没有categoryId
     */
    @RequestMapping(value = "/{keyword}/{pageNum}/{pageSize}/{orderBy}",method = RequestMethod.GET)
    @ResponseBody
    public ServerResponse<PageInfo> listRESTfulBadcase(@PathVariable(value = "keyword")String keyword,
                                                       @PathVariable(value = "pageNum") Integer pageNum,
                                                       @PathVariable(value = "pageSize") Integer pageSize,
                                                       @PathVariable(value = "orderBy") String orderBy){
        if(pageNum == null){
            pageNum = 1;
        }
        if(pageSize == null){
            pageSize = 10;
        }
        if(StringUtils.isBlank(orderBy)){
            orderBy = "price_asc";
        }

        return iProductService.getProductByKeywordCategory(keyword,null,pageNum,pageSize,orderBy);
    }

    //------------------------------------------------------------------------------------------------------------------------------------------

    /**
     * 如果我们想在只传递 keyword 或者 categoryId 其中一个的时候就可以实现 restful，如下方法
     * 即使用一个常量来区分2个URL，这样服务器就能识别2个URL，如：/keyword/{keyword}、/category/{categoryId}
     */
    //http://www.happymmall.com/product/keyword/手机/1/10/price_asc
    @RequestMapping(value = "/keyword/{keyword}/{pageNum}/{pageSize}/{orderBy}",method = RequestMethod.GET)
    @ResponseBody
    public ServerResponse<PageInfo> listRESTful(@PathVariable(value = "keyword")String keyword,
                                                @PathVariable(value = "pageNum") Integer pageNum,
                                                @PathVariable(value = "pageSize") Integer pageSize,
                                                @PathVariable(value = "orderBy") String orderBy){
        if(pageNum == null){
            pageNum = 1;
        }
        if(pageSize == null){
            pageSize = 10;
        }
        if(StringUtils.isBlank(orderBy)){
            orderBy = "price_asc";
        }

        return iProductService.getProductByKeywordCategory(keyword,null,pageNum,pageSize,orderBy);
    }

    //http://www.happymmall.com/product/category/100012/1/10/price_asc
    @RequestMapping(value = "/category/{categoryId}/{pageNum}/{pageSize}/{orderBy}",method = RequestMethod.GET)
    @ResponseBody
    public ServerResponse<PageInfo> listRESTful(@PathVariable(value = "categoryId")Integer categoryId,
                                                @PathVariable(value = "pageNum") Integer pageNum,
                                                @PathVariable(value = "pageSize") Integer pageSize,
                                                @PathVariable(value = "orderBy") String orderBy){
        if(pageNum == null){
            pageNum = 1;
        }
        if(pageSize == null){
            pageSize = 10;
        }
        if(StringUtils.isBlank(orderBy)){
            orderBy = "price_asc";
        }

        return iProductService.getProductByKeywordCategory("",categoryId,pageNum,pageSize,orderBy);
    }
}
