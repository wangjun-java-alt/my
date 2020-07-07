package com.newsee.database.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @Description: 本地线程全局变量 存放数据源实例
 * @author: 胡乾亮
 * @date: 2017年8月14日上午9:39:31
 */
public class DataSourceTypeContextHolder {
	
	private static Logger log = LoggerFactory.getLogger(DataSourceTypeContextHolder.class);

	//线程本地环境
//	private static final ThreadLocal<String> local = new ThreadLocal<String>();
	  
//    public static ThreadLocal<String> getLocal() {
//        return local;
//    }
  
    /** 
     * 读库 
     */  
    public static void setRead() {  
//        local.set(DataSourceTypeEnum.read.getType());
//        log.info("数据库读写类型切换到读库...");
    }  
  
    /** 
     * 写库 	
     */  
    public static void setWrite() {
        DataSourceContextHolder.getInstance().setMasterOnly();
//        local.set(DataSourceTypeEnum.write.getType());
        log.info("数据库读写类型切换到写库...");
    }  
    
    public static String getReadOrWrite() {  
        //return local.get();
        return null;
    }  
    
    public static void clear(){  
        //local.remove();
    }  
}
