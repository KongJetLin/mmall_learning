package com.mmall.controller.backend;

import com.github.pagehelper.PageInfo;
import com.google.common.collect.Maps;
import com.mmall.common.Const;
import com.mmall.common.ResponseCode;
import com.mmall.common.ServerResponse;
import com.mmall.pojo.Product;
import com.mmall.pojo.User;
import com.mmall.service.IFileService;
import com.mmall.service.IProductService;
import com.mmall.service.IUserService;
import com.mmall.service.impl.FileServiceImpl;
import com.mmall.util.PropertiesUtil;
import com.mmall.vo.ProductDetailVo;
import com.mmall.vo.ProductListVo;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 后台产品的相关方法
 */
@Controller
@RequestMapping("/manage/product")
public class ProductManageController
{
    @Autowired
    private IUserService iUserService;
    @Autowired
    private IProductService iProductService;
    @Autowired
    private IFileService iFileService;


    /**
     * 后台新增或者更新产品的接口
     * 保存和更新产品是2个操作，我们在代码中可以合并为一个，进行新增或者保存的操作即可！
     * @param session
     * @param product
     * @return
     */
    @RequestMapping("save.do")
    @ResponseBody
    public ServerResponse<String> productSave(HttpSession session, Product product)
    {
        User user = (User)session.getAttribute(Const.CURRENT_USER);
        if(user == null)
        {//在后台，如果没有登录，全部是要强制登录的！
            return ServerResponse.createByErrorCodeMessage(ResponseCode.NEED_LOGIN.getCode() , "用户未登录,请登录管理员");
        }

        if(iUserService.checkAdminRole(user).isSuccess())
        {
            //填充我们增加产品的业务逻辑
            return iProductService.saveOrUpdateProduct(product);
        }
        else
        {
            return ServerResponse.createByErrorMessage("无权限操作");
        }
    }

    /**
     * 根据传递过来的产品id与status，对产品上下架的状态进行更新（1，在售，2下架）
     * @param session
     * @param productId
     * @param status
     * @return
     */
    @RequestMapping("set_sale_status.do")
    @ResponseBody
    public ServerResponse<String> setSaleStatus(HttpSession session , Integer productId , Integer status)
    {
        User user = (User)session.getAttribute(Const.CURRENT_USER);
        if (user == null)
            return ServerResponse.createBySuccessMessage("用户未登录,请登录管理员");

        if(iUserService.checkAdminRole(user).isSuccess())
        {
            return iProductService.setSaleStatus(productId , status);
        }
        else
        {
            return ServerResponse.createByErrorMessage("无权限操作");
        }
    }

    /**
     * 获取商品详情功能
     * @param session
     * @param productId
     * @return
     */
    @RequestMapping("detail.do")
    @ResponseBody
    public ServerResponse<ProductDetailVo> getDetail(HttpSession session, Integer productId)
    {
        User user = (User)session.getAttribute(Const.CURRENT_USER);
        if (user == null)
            return ServerResponse.createBySuccessMessage("用户未登录,请登录管理员");

        if(iUserService.checkAdminRole(user).isSuccess())
        {
            return iProductService.manageProductDetail(productId);
        }
        else
        {
            return ServerResponse.createByErrorMessage("无权限操作");
        }
    }


    /**
     * 查询某一页的产品。并做分页展示的方法
     * @param session
     * @param pageNum  当前页数，默认第一页
     * @param pageSize  每页显示条目数，默认一页显示10条
     * @return
     */
    @RequestMapping("list.do")
    @ResponseBody
    public ServerResponse<PageInfo> getList(HttpSession session , @RequestParam(value = "pageNum" , defaultValue = "1")int pageNum , @RequestParam(value = "pageSize" ,defaultValue = "10")int pageSize)
    {
        User user = (User)session.getAttribute(Const.CURRENT_USER);
        if (user == null)
            return ServerResponse.createBySuccessMessage("用户未登录,请登录管理员");

        if(iUserService.checkAdminRole(user).isSuccess())
        {
            return iProductService.getProductList(pageNum , pageSize);
        }
        else
        {
            return ServerResponse.createByErrorMessage("无权限操作");
        }
    }


    /**
     * 根据产品的 id 或者 name 搜索产品并分页显示的方法
     * @return
     */
    @RequestMapping("search.do")
    @ResponseBody
    public ServerResponse productSearch(HttpSession session , Integer productId , String productName ,  @RequestParam(value = "pageNum" , defaultValue = "1")int pageNum , @RequestParam(value = "pageSize" ,defaultValue = "10")int pageSize)
    {
        User user = (User)session.getAttribute(Const.CURRENT_USER);
        if (user == null)
            return ServerResponse.createBySuccessMessage("用户未登录,请登录管理员");

        if(iUserService.checkAdminRole(user).isSuccess())
        {
            return iProductService.searchProduct(productId , productName , pageNum ,pageSize);
        }
        else
        {
            return ServerResponse.createByErrorMessage("无权限操作");
        }
    }


    /**
     * 在后台的商品编辑中，对商品进行图片的上传，同时返回上传文件到FTP服务器地址的uri与url
     * @param file
     * @param request
     * @return
     */
    @RequestMapping("upload.do")
    @ResponseBody
    public ServerResponse upload(HttpSession session , @RequestParam(value = "upload_file" , required = false) MultipartFile file , HttpServletRequest request)
    {
        //注意，在上传文件之前，必须先判断权限，避免恶意攻击上传
        User user = (User)session.getAttribute(Const.CURRENT_USER);
        if(user == null){
            return ServerResponse.createByErrorCodeMessage(ResponseCode.NEED_LOGIN.getCode(),"用户未登录,请登录管理员");
        }

        if(iUserService.checkAdminRole(user).isSuccess()){
            //在 webapp 目录下面创建一个文件夹 upload，获取这个文件夹的真实路径
            String path = request.getSession().getServletContext().getRealPath("upload");
            //将文件上传到 upload 文件夹后删除，并同时上传到 FTp服务器，返回上传文件的文件名
            String targetFileName = iFileService.upload(file , path);

            /**
             下面，我们拼接访问FTP服务器中相应文件的url
             上面已经将文件上传到 FTP 服务器，并返回FTP服务器处该文件的文件名（即URI），此处，我们获取FTP服务器的访问的HTTP路径，
             将该路径与文件名拼接起来，就可以获取到通过HTTP访问FTP服务器下该文件的路径（URL）。
             */
            String url = PropertiesUtil.getProperty("ftp.server.http.prefix")+targetFileName;
            Map fileMap = Maps.newHashMap();
            fileMap.put("uri" , targetFileName);
            fileMap.put("url" , url);
            return ServerResponse.createBySuccess(fileMap);
        }else{
            return ServerResponse.createByErrorMessage("无权限操作");
        }
    }


    /**
     * 富文本中文件的上传
     * @param session
     * @param file
     * @param request
     * @return
     */
    @RequestMapping("richtext_img_upload.do")
    @ResponseBody
    public Map richtextImgUpload(HttpSession session , @RequestParam(value = "upload_file" , required = false) MultipartFile file , HttpServletRequest request , HttpServletResponse response)
    {
        Map resultMap = Maps.newHashMap();
        User user = (User)session.getAttribute(Const.CURRENT_USER);
        if(user == null){
            resultMap.put("success" , false);
            resultMap.put("msg" , "请登录管理员");
            return resultMap;
        }
        /** 富文本中对于返回值有自己的要求,我们使用是simditor所以按照simditor的要求进行返回（前端使用 simditor 富文本插件，因此我们要按照 simditor 的要求）
         {
         "success": true/false,
         "msg": "error message", # optional
         "file_path": "[real file path]"
         }
         */

        if(iUserService.checkAdminRole(user).isSuccess()){
            //在 webapp 目录下面创建一个文件夹 upload，获取这个文件夹的真实路径
            String path = request.getSession().getServletContext().getRealPath("upload");
            //将文件上传到 upload 文件夹后删除，并同时上传到 FTp服务器，返回上传文件的文件名
            String targetFileName = iFileService.upload(file , path);
            if(StringUtils.isBlank(targetFileName))
            {
                resultMap.put("success" , false);
                resultMap.put("msg" , "上传失败");
                return resultMap;
            }

            String url = PropertiesUtil.getProperty("ftp.server.http.prefix")+targetFileName;
            resultMap.put("success" , true);
            resultMap.put("msg" , "上传成功");
            resultMap.put("file_path" , url);
            //上传成功需要返回一个 “Access-Control-Allow-Headers”，这时我们与前端的约定
            response.addHeader("Access-Control-Allow-Headers" , "X-File-Name");
            return resultMap;
        }else{
            resultMap.put("success" , false);
            resultMap.put("msg" , "无权限操作");
            return resultMap;
        }
    }



}
