package com.mmall.service.impl;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import com.mmall.common.ServerResponse;
import com.mmall.dao.CategoryMapper;
import com.mmall.pojo.Category;
import com.mmall.service.ICategoryService;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Service("iCategoryService")
public class CategoryServiceImpl implements ICategoryService
{
    //日志
    private Logger logger = LoggerFactory.getLogger(CategoryServiceImpl.class);

    @Autowired
    private CategoryMapper categoryMapper;

    //用于添加品类的方法
    @Override
    public ServerResponse<Category> addCategory(String categoryName , Integer parentId)
    {
        if(parentId == null || StringUtils.isBlank(categoryName))
            return ServerResponse.createByErrorMessage("添加品类参数错误");

        //将数据注入Category对象
        Category category = new Category();
        category.setName(categoryName);
        category.setParentId(parentId);
        category.setStatus(true);//注意status是Boolean型的，用于判断该品类是否已经废弃

        int rowCount = categoryMapper.insert(category);
        if(rowCount > 0)
            return ServerResponse.createBySuccessMessage("添加品类成功");
        return ServerResponse.createByErrorMessage("添加品类失败");
    }

    //用于修改品类名称的方法
    @Override
    public ServerResponse<String> updateCategoryName(Integer categoryId, String categoryName)
    {
        if(categoryId == null || StringUtils.isBlank(categoryName))
            return ServerResponse.createByErrorMessage("更新品类参数错误");

        //填充Category对象
        Category category = new Category();
        category.setName(categoryName);
        category.setId(categoryId);

        //有选择性地更新，也是通过 id 进行更新的
        int rowCount = categoryMapper.updateByPrimaryKeySelective(category);
        if(rowCount > 0)
            return ServerResponse.createBySuccessMessage("更新品类名称成功");
        return ServerResponse.createByErrorMessage("更新品类名称失败");
    }


    //查询子品类的详细信息
    @Override
    public ServerResponse<List<Category>> getChildrenParallelCategory(Integer categoryId)
    {
        List<Category> categoryList = categoryMapper.selectCategoryChildrenByParentId(categoryId);
        if(CollectionUtils.isEmpty(categoryList))
            //如果查询的子分类是空的，不给前端返回，因为这样返回给前端也没什么用，我们打印日志就可以
            logger.info("未找到当前分类的子分类");

        //找到子品类则将他们封装返回
        return ServerResponse.createBySuccess(categoryList);
    }

    //根据父结点的id，查询所有递归子结点的id
    @Override
    public ServerResponse<List<Integer>> selectCategoryAndChildrenById(Integer categoryId)
    {
        //使用 guava 里面的方法进行初始化 set，其实也可以直接 new HashSet()
        Set<Category> categorySet = Sets.newHashSet();

        //调用递归算法查询所有id
        findChildrenCategory(categorySet , categoryId);

        //同样使用 guava 进行初始化 List
        List<Integer> categoryIdList = Lists.newArrayList();
        //只有 categoryId 不为null的时候，我们查询出来的 Set<Category> 才不为null，才能对其进行遍历
        if(categoryId != null)
        {
            for (Category categoryItem : categorySet)
            {
                categoryIdList.add(categoryItem.getId());
            }
        }
        return ServerResponse.createBySuccess(categoryIdList);
    }

    /** 递归算出子结点
    1）Set里面的参数不可重复，即相同的元素不可重复放入Set，这就要求元素具有比较性，该元素必须重写 hashCode()与equals()方法
     */
    private Set<Category> findChildrenCategory(Set<Category> categorySet , Integer categoryId)
    {
        //根据 categoryId 查询出子当前结点，如果当前结点不为 null ，则将当前结点添加到set
        Category category = categoryMapper.selectByPrimaryKey(categoryId);
        if(category != null)
            categorySet.add(category);//将当前category添加到Set集合

        //根据当前结点的 categoryId，查询出以 categoryId 为父结点的子结点的id
        List<Category> categoryList = categoryMapper.selectCategoryChildrenByParentId(categoryId);
        /*
        注意，即使当前结点的 categoryId 查询出来没有子结点，Mybatis 返回的categoryList 也不会是null，
        其实我们只需要遍历 categoryList 即可，如果它里面没有元素，说明当前结点没有子结点，那么就不会继续递归下去（这就是递归结束条件）
         */
        for (Category categoryItem : categoryList)
        {
            //查询出当前结点的子结点，再递归将子结点添加到 set，并查询子结点的子结点
            findChildrenCategory(categorySet , categoryItem.getId());
        }

        return categorySet;//将添加了结点的Set返回即可
    }

}
