package com.newsee.database.aop;

import com.newsee.database.util.DataSourceTypeContextHolder;
import org.apache.shardingsphere.api.hint.HintManager;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

/**
 * @Description: service 层 AOP拦截设置切换数据源
 * 必须在事务AOP之前执行，所以实现Ordered,order的值越小，越先执行
 * 如果一旦开始切换到写库，则之后的读都会走写库 
 * @author: 胡乾亮
 * @date: 2017年8月14日上午10:12:36
 */

@Aspect
@Component
@Order(0)
public class DataSourceTypeAopInService {

     @Before("execution(* com.newsee.*.service.impl..*.*(..)) && (@annotation(com.newsee.database.annotation.ReadDataSource) || @annotation(com.newsee.common.annotation.ReadDataSource))")
     public void setReadDataSourceType() {  
         // sharding-jdbc会根据SQL类型自动选择读写库
     }  
       
     @Before("execution(* com.newsee.*.service.impl..*.*(..)) && (@annotation(com.newsee.database.annotation.WriteDataSource) || @annotation(com.newsee.common.annotation.WriteDataSource))")
     public void setWriteDataSourceType() {
          DataSourceTypeContextHolder.setWrite();
     }  
}
