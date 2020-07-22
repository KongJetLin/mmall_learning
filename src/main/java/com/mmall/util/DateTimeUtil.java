package com.mmall.util;

import org.apache.commons.lang3.StringUtils;
import org.joda.time.DateTime;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;

import java.util.Date;

public class DateTimeUtil
{
    //joda-time

    public static final String STANDARD_FORMAT = "yyyy-MM-dd HH:mm:ss";//默认格式


    //str - Date
    public static Date strToDate(String dateTimeStr , String formatStr)
    {
        DateTimeFormatter dateTimeFormatter = DateTimeFormat.forPattern(formatStr);//获取转换对象
        DateTime dateTime = dateTimeFormatter.parseDateTime(dateTimeStr);//将时间字符串转换为 DateTime 类型
        return dateTime.toDate();//将DateTime类型转换为 Date
    }

    //Date - str
    public static String dateToStr(Date date , String formatStr)
    {
        if(date == null)
            return StringUtils.EMPTY;//日期为空，我们也返回空
        DateTime dateTime = new DateTime(date);//先将Date转换为DateTime
        return dateTime.toString(formatStr);
    }

    //下面使用标准的格式进行转换
    public static Date strToDate(String dateTimeStr)
    {
        DateTimeFormatter dateTimeFormatter = DateTimeFormat.forPattern(STANDARD_FORMAT);//获取转换对象
        DateTime dateTime = dateTimeFormatter.parseDateTime(dateTimeStr);//将时间字符串转换为 DateTime 类型
        return dateTime.toDate();//将DateTime类型转换为 Date
    }

    public static String dateToStr(Date date)
    {
        if(date == null)
            return StringUtils.EMPTY;//日期为空，我们也返回空
        DateTime dateTime = new DateTime(date);//先将Date转换为DateTime
        return dateTime.toString(STANDARD_FORMAT);
    }
}
