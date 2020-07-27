package com.mmall.service.impl;

import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import com.google.common.collect.Lists;
import com.mmall.common.Const;
import com.mmall.common.ResponseCode;
import com.mmall.common.ServerResponse;
import com.mmall.dao.CategoryMapper;
import com.mmall.dao.ProductMapper;
import com.mmall.pojo.Category;
import com.mmall.pojo.Product;
import com.mmall.service.ICategoryService;
import com.mmall.service.IProductService;
import com.mmall.util.DateTimeUtil;
import com.mmall.util.PropertiesUtil;
import com.mmall.vo.ProductDetailVo;
import com.mmall.vo.ProductListVo;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service("iProductService")
public class ProductServiceImpl implements IProductService
{
    @Autowired
    private ProductMapper productMapper;

    @Autowired
    private CategoryMapper categoryMapper;

    @Autowired
    private ICategoryService iCategoryService;

    //保存或更新产品，保存和更新产品是2个操作，我们在代码中可以合并为一个，进行新增或者保存的操作即可！
    @Override
    public ServerResponse<String> saveOrUpdateProduct(Product product)
    {
        if(product != null)
        {
            //首先，不管是更新还是添加，主图肯定是空的，因为主图要从子图中获取，我们先给主图赋值
            if(StringUtils.isNotBlank(product.getSubImages()))
            {
                //先将子图的文本切割开为数组：子图的数据类型是 text文本类型（数据库），即String类型（java）
                String[] subImageArray = product.getSubImages().split(",");
                if(subImageArray.length > 0)
                {//取出子图后，将子图第一个图片文本赋予主图（varchar// ）
                    product.setMainImage(subImageArray[0]);
                }
            }

            //判断是更新还是添加，如果是添加，id必然为null
            if(product.getId() != null)
            {//添加产品
                int rowCount = productMapper.updateByPrimaryKeySelective(product);
                if(rowCount > 0)
                    return ServerResponse.createBySuccess("更新产品成功");
                return ServerResponse.createBySuccess("更新产品失败");
            }
            else
            {//新增产品
                int rowCount = productMapper.insert(product);
                if(rowCount > 0){
                    return ServerResponse.createBySuccess("新增产品成功");
                }
                return ServerResponse.createBySuccess("新增产品失败");
            }

        }
        return ServerResponse.createByErrorMessage("新增或更新产品参数不正确");
    }

    //修改产品销售状态（上下架）
    @Override
    public ServerResponse<String> setSaleStatus(Integer productId,Integer status)
    {
        if(productId == null || status == null)
        {//传递的参数为null，这里需要向前段传递参数非法
            return ServerResponse.createByErrorCodeMessage(ResponseCode.ILLEGAL_ARGUMENT.getCode() , ResponseCode.ILLEGAL_ARGUMENT.getDesc());
        }

        Product product = new Product();
        product.setId(productId);
        product.setStatus(status);

        int rowCount = productMapper.updateByPrimaryKeySelective(product);
        if(rowCount > 0){
            return ServerResponse.createBySuccess("修改产品销售状态成功");
        }
        return ServerResponse.createByErrorMessage("修改产品销售状态失败");
    }

    @Override
    //获取商品详情的方法
    public ServerResponse<ProductDetailVo> manageProductDetail(Integer productId)
    {
        if(productId == null)
        {//后台如果参数错误，要给出参数错误的提示
            return ServerResponse.createByErrorCodeMessage(ResponseCode.ILLEGAL_ARGUMENT.getCode() , ResponseCode.ILLEGAL_ARGUMENT.getDesc());
        }

        Product product = productMapper.selectByPrimaryKey(productId);
        if(product == null)
            return ServerResponse.createByErrorMessage("产品已删除或者下架");

        /**
        下面使用vo对象进行数据的封装（value object）：ProductDetailVo
         因为 product 对象不足以封装下面的数据，如果要修改 product 对象又不合适，那么就创建一个更大的 ProductDetailVo 对象来封装
         */
        ProductDetailVo productDetailVo = assembleProductDetailVo(product);
        return ServerResponse.createBySuccess(productDetailVo);
    }

    //将 product 转换为 ProductDetailVo 的方法
    private ProductDetailVo assembleProductDetailVo(Product product)
    {
        ProductDetailVo productDetailVo = new ProductDetailVo();
        //先将 product 中有的数据填充搭配 productDetailVo
        productDetailVo.setId(product.getId());
        productDetailVo.setSubtitle(product.getSubtitle());
        productDetailVo.setPrice(product.getPrice());
        productDetailVo.setMainImage(product.getMainImage());
        productDetailVo.setSubImages(product.getSubImages());
        productDetailVo.setCategoryId(product.getCategoryId());
        productDetailVo.setDetail(product.getDetail());
        productDetailVo.setName(product.getName());
        productDetailVo.setStatus(product.getStatus());
        productDetailVo.setStock(product.getStock());

        //imageHost：从配置文件获取，方便后面的管理
        //设置图片服务器地址前缀
        productDetailVo.setImageHost(PropertiesUtil.getProperty("ftp.server.http.prefix" , "http://image.upload.com/"));

        //设置 parentCategoryId 的方法（用于设置当前产品分类的父分类的Id）
        Category category = categoryMapper.selectByPrimaryKey(product.getCategoryId());//先查询出当前产品的分类
        if(category == null)
        {
            //若当前产品分类为 null，我们设置当前产品的分类为根结点分类，即设置其 parentId 为0
            productDetailVo.setParentCategoryId(0);
        }
        else
        {
            productDetailVo.setParentCategoryId(category.getParentId());
        }

        //createtime 与 updatetime，由于题目从 product 中取出来是毫秒值，需要对时间进行格式化
        //将时间转换为字符串类型进行展示
        productDetailVo.setCreateTime(DateTimeUtil.dateToStr(product.getCreateTime()));
        productDetailVo.setUpdateTime(DateTimeUtil.dateToStr(product.getUpdateTime()));
        return productDetailVo;
    }


    //根据分页信息：当前页数，每页显示条数，返回产品列表
    @Override
    public ServerResponse<PageInfo> getProductList(int pageNum , int pageSize)
    {
        /** PageHelper 的使用流程
         1）startPage--start
         2）填充自己的sql查询逻辑
         3）pageHelper-收尾

         我们设置了PageHelper的参数后，后面 selectList 的时候，还是查询出全部的产品，
         但是我们用 PageInfo 过滤，就可以只获取当前页的产品集合
         */
        //start
        PageHelper.startPage(pageNum , pageSize);

        //填充自己的SQL逻辑
        List<Product> productList = productMapper.selectList();
        //用于保存封装了product数据的 ProductListVo的集合
        List<ProductListVo> productListVoList = Lists.newArrayList();
        //这里分页查询后，不需要product那么多的信息，因此我们再创建一个 ProductListVo 用于封装返回的数据
        for (Product product : productList)
        {
            ProductListVo productListVo = assembleProductListVo(product);
            productListVoList.add(productListVo);
        }

        //PageHelper收尾
        PageInfo pageResult = new PageInfo(productList);
        /**
        首先将原来SQL查询全部的结果集放入PageInfo对象，
        将原来查询全部的结果放入PageInfo后，会自动进行分页。将设置的分页的信息 productList 取出，并封装到 PageInfo对象中。
        但是我们要的不是 List<Product>，那么我们重置PageInfo对象中的List为 List<ProductListVo>即可
         */
        pageResult.setList(productListVoList);
        //注意这里返回的 ServerResponse不是封装 List<productListVoList>，而是 PageInfo对象，这个对象里面封装了分页的 List<productListVoList>
        return ServerResponse.createBySuccess(pageResult);
    }

    //用于将Product的属性封装到 ProductListVo
    private ProductListVo assembleProductListVo(Product product)
    {
        ProductListVo productListVo = new ProductListVo();
        productListVo.setId(product.getId());
        productListVo.setName(product.getName());
        productListVo.setCategoryId(product.getCategoryId());
        productListVo.setMainImage(product.getMainImage());
        productListVo.setSubtitle(product.getSubtitle());
        productListVo.setPrice(product.getPrice());
        productListVo.setStatus(product.getStatus());
        productListVo.setImageHost(PropertiesUtil.getProperty("ftp.server.http.prefix" , "http://img.happymmall.com/"));
        return productListVo;
    }


    //根据产品的 id 或者 name 搜索产品并分页显示的方法
    @Override
    public ServerResponse<PageInfo> searchProduct(Integer productId , String productName ,  int pageNum , int pageSize)
    {
        //首先，进行分页初始化
        PageHelper.startPage(pageNum , pageSize);

        //进行自己的SQL查询
        if(StringUtils.isNotBlank(productName))
        {
            //为productName填充模糊查询的“%”
            productName = new StringBuilder().append("%").append(productName).append("%").toString();
        }

        //进行 id 或者 name 的查询（有哪个查询哪个）
        List<Product> productList = productMapper.selectByNameAndProductId(productId, productName);

        List<ProductListVo> productListVoList = Lists.newArrayList();
        //同样，product对象无法展示所有信息，需要转换为 ProductListVo
        for (Product product : productList)
        {
            ProductListVo productListVo = assembleProductListVo(product);
            productListVoList.add(productListVo);
        }

        //分页收尾
        PageInfo pageResult = new PageInfo(productList);
        pageResult.setList(productListVoList);
        return ServerResponse.createBySuccess(pageResult);
    }


//----------------------------------------------------------------------------------------------前台商品的方法

    //根据商品id获取商品详情
    @Override
    public ServerResponse<ProductDetailVo> getProductDetail(Integer productId)
    {
        if(productId == null)
        {
            return ServerResponse.createByErrorCodeMessage(ResponseCode.ILLEGAL_ARGUMENT.getCode() , ResponseCode.ILLEGAL_ARGUMENT.getDesc());
        }

        Product product = productMapper.selectByPrimaryKey(productId);
        if(product == null)
        {
            return ServerResponse.createByErrorMessage("产品已删除或者下架");
        }
        //前台与后台不同的一点，前台需要判断商品是否下架，下架则无法获取商品详情
        if(product.getStatus() != Const.ProductStatusEnum.ON_SALE.getCode())
        {
            return ServerResponse.createByErrorMessage("产品已删除或者下架");
        }


        ProductDetailVo productDetailVo = assembleProductDetailVo(product);
        return ServerResponse.createBySuccess(productDetailVo);
    }


    //根据 keyword 关键字（非必须），分类id（非必须）进行分页查询，将结果集封装到 PageInfo
    @Override
    public ServerResponse<PageInfo> getProductByKeywordCategory(String keyword,Integer categoryId,int pageNum,int pageSize,String orderBy)
    {
        //首先，搜索的时候要么根据keyword，要么根据分类，不能能都没有，都没有则认为是参数错误
        if(StringUtils.isBlank(keyword) && categoryId == null)
            return ServerResponse.createByErrorCodeMessage(ResponseCode.ILLEGAL_ARGUMENT.getCode(),ResponseCode.ILLEGAL_ARGUMENT.getDesc());

        //定义一个List保存分类ID，因为如果传递一个大的分类过来，我们需要递归找到其所有子分类，并查询所有子分类的产品
        List<Integer> categoryIdList = new ArrayList<>();

        //如果分类id不为null，进行categoryId条件的设置
        if(categoryId != null)
        {
            Category category = categoryMapper.selectByPrimaryKey(categoryId);


            if(category == null && StringUtils.isBlank(keyword))
            {
                //没有该分类,并且还没有关键字,这个时候返回一个空的结果集,不报错。
                //指针情况是前端查询某个不存在的分类，但是这并不是错误，返回空的结果集即可，同样需要分类
                PageHelper.startPage(pageNum , pageSize);
                List<ProductListVo> productListVoList = Lists.newArrayList();
                PageInfo pageInfo = new PageInfo(productListVoList);
                //结果集没有变化，不需要重新设置List
                return ServerResponse.createBySuccess(pageInfo);
            }
            //调用CategoryService的查询当前分类及其所有子类的方法，查询出所有分类id
            categoryIdList = iCategoryService.selectCategoryAndChildrenById(category.getId()).getData();
        }

        //进行 keyword 的设置
        if(StringUtils.isNotBlank(keyword)){
            keyword = new StringBuilder().append("%").append(keyword).append("%").toString();
        }

        PageHelper.startPage(pageNum,pageSize);

        //排序处理
        if(StringUtils.isNotBlank(orderBy))
        {
            //如果 排序字段包含在外面定义的接口的常量中，对排序的量进行获取
            if(Const.productListOrderBy.PRICE_ASC_DESC.contains(orderBy))
            {
                String[] orderByArray = orderBy.split("_");
                //PageHelper内排序的格式为："price asc"
                PageHelper.orderBy(orderByArray[0]+" "+orderByArray[1]);
                //这样，外面后面根据 keyword 或 List<Integer> categoryList 查询出结果后，PageHelper就会自动对结果进行排序
            }
        }

        /*
        这里如果我们传递一个空的 categoryIdList（前面new了它，它不为null，但是可能没有元素），虽然categoryIdList不为null，但是它里面没有元素，
        这样我们就查询不出结果，这样就会出错。
        我们在 keyword 或 categoryIdList不为null，但是没有元素的时候将其设置为null，这样SQL语句直接不会查询，不至于查询不出结果
         */
        List<Product> productList = productMapper.selectByNameAndCategoryIds(StringUtils.isBlank(keyword)?null:keyword, categoryIdList.size()==0?null:categoryIdList);
        List<ProductListVo> productListVoList = Lists.newArrayList();
        for (Product product : productList)
        {
            ProductListVo productListVo = assembleProductListVo(product);
            productListVoList.add(productListVo);
        }

        PageInfo pageInfo = new PageInfo(productList);
        pageInfo.setList(productListVoList);
        return ServerResponse.createBySuccess(pageInfo);
    }
}
