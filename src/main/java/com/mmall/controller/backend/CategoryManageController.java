package com.mmall.controller.backend;

import com.mmall.common.Const;
import com.mmall.common.ResponseCode;
import com.mmall.common.ServerResponse;
import com.mmall.pojo.Category;
import com.mmall.pojo.User;
import com.mmall.service.ICategoryService;
import com.mmall.service.IUserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import javax.servlet.http.HttpSession;
import java.util.List;

/**
 * 后台分类管理的类
 */
@Controller
@RequestMapping("/manage/category")
public class CategoryManageController
{
    @Autowired
    private IUserService iUserService;

    @Autowired
    private ICategoryService iCategoryService;


    /**
     * 添加新的分类结点的方法
     * 这里使用 @RequestParam 设置 parentId 的默认值为0，即前端没有传递parentId的值的话，默认为0，即其默认为根结点
     * @return
     */
    @RequestMapping("add_category.do")
    @ResponseBody
    public ServerResponse addCategory(HttpSession session , String categoryName , @RequestParam(value = "parentId" , defaultValue = "0") int parentId)
    {
        //先判断用户有没有登录
        User user = (User)session.getAttribute(Const.CURRENT_USER);
        if(user == null)
        {//没有登录要强制登录
            return ServerResponse.createByErrorCodeMessage(ResponseCode.NEED_LOGIN.getCode() , "用户未登录,请登录");
        }

        //校验一下是否是管理员
        if(iUserService.checkAdminRole(user).isSuccess())
        {
            //如果是管理员，则可以进行添加
            return iCategoryService.addCategory(categoryName , parentId);
        }
        else
        {
            return ServerResponse.createBySuccessMessage("无权限操作,需要管理员权限");
        }
    }

    /**
     * 修改品类名称
     * @param session
     * @param categoryId
     * @param categoryName
     * @return
     */
    @RequestMapping("set_category_name.do")
    @ResponseBody
    public ServerResponse setCategoryName(HttpSession session,Integer categoryId,String categoryName)
    {
        //先判断用户有没有登录
        User user = (User)session.getAttribute(Const.CURRENT_USER);
        if(user == null)
        {//没有登录要强制登录
            return ServerResponse.createByErrorCodeMessage(ResponseCode.NEED_LOGIN.getCode() , "用户未登录,请登录");
        }

        //校验一下是否是管理员
        if(iUserService.checkAdminRole(user).isSuccess())
        {
            //如果是管理员，则可以进行更新
            return iCategoryService.updateCategoryName(categoryId , categoryName);
        }
        else
        {
            return ServerResponse.createBySuccessMessage("无权限操作,需要管理员权限");
        }
    }


    /**
     * 获取品类子节点(平级)，即获取某一个品类的所有子品类的详细信息，但是不获取子类的子类，即不递归获取
     * 设置默认categoryId=0，即父节点为0，则说明当前结点没有父亲结点
     * @param categoryId
     * @return
     */
    @RequestMapping("get_category.do")
    @ResponseBody
    public ServerResponse<List<Category>> getChildrenParallelCategory(HttpSession session , @RequestParam(value = "categoryId" , defaultValue = "0") Integer categoryId)
    {
        //先判断用户有没有登录
        User user = (User)session.getAttribute(Const.CURRENT_USER);
        if(user == null)
        {//没有登录要强制登录
            return ServerResponse.createByErrorCodeMessage(ResponseCode.NEED_LOGIN.getCode() , "用户未登录,请登录");
        }

        //校验一下是否是管理员
        if(iUserService.checkAdminRole(user).isSuccess())
        {
            //如果是管理员，则可以进行子类详细信息查询
            return iCategoryService.getChildrenParallelCategory(categoryId);
        }
        else
        {
            return ServerResponse.createBySuccessMessage("无权限操作,需要管理员权限");
        }
    }


    /**
     * 查询当前分类及其递归所有子分类的id值，将多有的ID值封装到List<Integer>
     * @param session
     * @param categoryId
     * @return
     */
    @RequestMapping("get_deep_category.do")
    @ResponseBody
    public ServerResponse<List<Integer>> getCategoryAndDeepChildrenCategory(HttpSession session , @RequestParam(value = "categoryId" , defaultValue = "0") Integer categoryId)
    {
        //先判断用户有没有登录
        User user = (User)session.getAttribute(Const.CURRENT_USER);
        if(user == null)
        {//没有登录要强制登录
            return ServerResponse.createByErrorCodeMessage(ResponseCode.NEED_LOGIN.getCode() , "用户未登录,请登录");
        }

        //校验一下是否是管理员
        if(iUserService.checkAdminRole(user).isSuccess())
        {
            //如果是管理员，则可以进行查询
            return iCategoryService.selectCategoryAndChildrenById(categoryId);
        }
        else
        {
            return ServerResponse.createBySuccessMessage("无权限操作,需要管理员权限");
        }
    }

}
