package com.mmall.service.impl;

import com.google.common.collect.Lists;
import com.mmall.service.IFileService;
import com.mmall.util.FTPUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.util.UUID;

@Service("iFileService")
public class FileServiceImpl implements IFileService
{
    //日志
    private Logger logger = LoggerFactory.getLogger(FileServiceImpl.class);

    //上传文件的方法
    @Override
    public String upload(MultipartFile file , String path)
    {
        /**1、根据源文件的扩展名，创建新文件名*/
        //先获取文件的原始文件名 ，如：abc.jpg
        String fileName = file.getOriginalFilename();
        //获取文件的扩展名：从fileName的最后一个“.”开始获取后面的扩展名,+1后移一位将“.”去除
        String fileExtensionName = fileName.substring(fileName.lastIndexOf(".")+1);
        //为了避免不同的管理员上传同名的图片，如2人上传：abc.jpg，这样后面的就会覆盖前面的，
        //因此我们将新的文件名用UUID来生成
        String uploadFileName = UUID.randomUUID().toString()+"."+fileExtensionName;

        //打印日志
        logger.info("开始上传文件，上传文件的文件名:{},上传的路径:{},新文件名:{}",fileName , path , uploadFileName);

        /** 2、下面开始上传文件 */
        //首先创建文件夹 path，因为我们的文件要放到 path的文件夹链下，因此必须先创建文件夹
        File fileDir = new File(path);
        if(!fileDir.exists())
        {
            fileDir.setWritable(true);//设置文件夹可写
            fileDir.mkdirs();
        }
        //创建要上传文件的完整名：即路径加文件上传名
        File targetFile = new File(path , uploadFileName);


        try
        {//使用SpringMVC的 MultipartFile 上传文件
            file.transferTo(targetFile);
            //文件已经上传成功了，已经上传到upload文件夹下

            //下面，我们需要将文件上传到FTP服务器的“img”目录下
            FTPUtil.uploadFile(Lists.newArrayList(targetFile));
            //已经上传到ftp服务器上

            //既然将文件上传到 FTP服务器，本地的upload下的文件就可以删除，upload空文件夹可以保留
            targetFile.delete();
        }
        catch (IOException e)
        {
            logger.error("上传文件异常",e);
            return null;
        }
//        System.out.println("------------------------------------------------------");
//        System.out.println("---------------"+targetFile.getName()+"----------------");
//        System.out.println("------------------------------------------------------");
        //将上传文件的文件名返回
        return targetFile.getName();
    }
}
