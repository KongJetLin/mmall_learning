package com.mmall.service;

import org.springframework.web.multipart.MultipartFile;

/**
 * 文件处理的接口
 */
public interface IFileService
{
    String upload(MultipartFile file , String path);
}
