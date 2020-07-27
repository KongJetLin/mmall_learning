package com.mmall.util;

import org.apache.commons.net.ftp.FTPClient;
import org.apache.commons.net.ftp.FTPFile;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.List;

/**
 * 用于将文件上传到 FTP服务器的工具类
 */
public class FTPUtil
{
    //日志
    private static final Logger logger = LoggerFactory.getLogger(FTPUtil.class);

    //获取FTP服务器的 IP地址，用户名以及密码
    private static String ftpIp = PropertiesUtil.getProperty("ftp.server.ip");
    private static String ftpUser = PropertiesUtil.getProperty("ftp.user");
    private static String ftpPass = PropertiesUtil.getProperty("ftp.pass");

    public FTPUtil(String ip, int port, String user, String pwd)
    {
        this.ip = ip;
        this.port = port;
        this.user = user;
        this.pwd = pwd;
    }

//------------------------开放给外部使用的具体方法

    //批量上传文件
    public static boolean uploadFile(List<File> fileList) throws IOException
    {
        //创建FTPUtil的对象，这里指定上传的端口是 21
        FTPUtil ftpUtil = new FTPUtil(ftpIp, 21, ftpUser, ftpPass);
        logger.info("开始连接FTP服务器");
        boolean result = ftpUtil.uploadFile("img", fileList);
        logger.info("断开ftp服务器,结束上传,上传结果:{}" , result);
        return result;
    }

    //上传文件的方法。我们将文件上传到FTP服务器，FTP服务器（Linux系统）在LINUX系统中是文件夹，
    //这个 remotePath 指的就是Linux服务器下FTP文件夹下的某个文件夹，如“img”的路径
    public boolean uploadFile(String remotePath , List<File> fileList) throws IOException
    {
        boolean upload = true;
        FileInputStream fis = null;
        //连接FTP服务器成功就开始上传
        if(connectFTPserver(this.ip , this.port , this.user , this.pwd))
        {
            //将服务器的工作目录更改到 remotePath 下
            try
            {
                ftpClient.changeWorkingDirectory(remotePath);
                //设置FTP服务器的缓冲区、编码
                ftpClient.setBufferSize(1024);
                ftpClient.setControlEncoding("UTF-8");
                //将文件类型设置为二进制文件类型，避免乱码问题
                ftpClient.setFileType(FTPClient.BINARY_FILE_TYPE);
                //打开本地的被动模式
                ftpClient.enterLocalPassiveMode();
                //开始上传
                for (File file : fileList)
                {
                    fis = new FileInputStream(file);
                    //将文件名与FileInputStream流放入
                    ftpClient.storeFile(file.getName() , fis);
                }
            }
            catch (IOException e)
            {
                logger.info("上传文件异常" , e);
                upload = false;
                e.printStackTrace();
            }
            finally
            {
                //将FileInputStream关闭，同时断开FTP的连接
                fis.close();
                ftpClient.disconnect();//不释放连接，时间久了就会出问题
            }
        }
        return upload;//将是否上传成功的结果返回
    }

    //连接FTP服务器
    private boolean connectFTPserver(String ip, int port, String user, String pwd)
    {
        boolean isSuccess = false;
        ftpClient = new FTPClient();//初始化FTP服务器客户端
        try
        {
            ftpClient.connect(ip);//先根据IP连接
            isSuccess = ftpClient.login(user , pwd);//登录，如果成功登录，说明服务器连接成功
        }
        catch (IOException e)
        {
            logger.error("连接FTP服务器异常",e);
        }
        return isSuccess;
    }

//-------------------------

    //下面声明字段：IP地址，端口，用户，密码，FTP客户端
    private String ip;
    private int port;
    private String user;
    private String pwd;
    private FTPClient ftpClient;

    public String getIp()
    {
        return ip;
    }

    public void setIp(String ip)
    {
        this.ip = ip;
    }

    public int getPort()
    {
        return port;
    }

    public void setPort(int port)
    {
        this.port = port;
    }

    public String getUser()
    {
        return user;
    }

    public void setUser(String user)
    {
        this.user = user;
    }

    public String getPwd()
    {
        return pwd;
    }

    public void setPwd(String pwd)
    {
        this.pwd = pwd;
    }

    public FTPClient getFtpClient()
    {
        return ftpClient;
    }

    public void setFtpClient(FTPClient ftpClient)
    {
        this.ftpClient = ftpClient;
    }
}
