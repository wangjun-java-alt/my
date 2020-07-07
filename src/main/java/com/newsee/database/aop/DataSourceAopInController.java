package com.newsee.database.aop;

import java.util.*;

import javax.servlet.http.HttpServletRequest;

import com.newsee.database.sharding.NSDbShardingUtils;
import com.newsee.database.sharding.NSShardConfig;
import com.newsee.database.util.DataSourceContextHolder;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

/**
 * 从请求层直接定位到分片数据库
 * AOP 拦截controller层，通过当前请求上的项目precinctId和租户enterpriseId直接把数据库请求指向对应的分片数据库
 */

@Aspect
@Component
@Order(0)
public class DataSourceAopInController {
	Logger logger = LoggerFactory.getLogger(DataSourceAopInController.class);
	@Autowired
	private NSShardConfig shardConfigs;
	
	@Around("execution(* com.newsee.*.controller..*.*(..)) ")
	public Object setTargetShardingDataSource(ProceedingJoinPoint joinPoint) throws Throwable {
		Object result = null;
		ServletRequestAttributes requestAttributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
		HttpServletRequest request = requestAttributes.getRequest();
		dumpHeaderAndParameter(request);
		Set<String> precinctList = getValueFromHeaderOrParameter(request, "precinctId");
		precinctList.addAll(getValueListFromHeaderOrParameter(request, "precinctIds"));
		List<String> targetShardingDb = NSDbShardingUtils.getShardingDBByPrecinctIdList(shardConfigs, precinctList);
		logger.debug("Got targetShardingDb[{}] by precinct[{}] from request header/parameter.", String.join(",", targetShardingDb), String.join(",", precinctList));

		// 如果根据项目Id找不到分片信息，则开始以租户Id找
		if(CollectionUtils.isEmpty(targetShardingDb)) {
			Set<String> enterpriseList = getValueFromHeaderOrParameter(request, "enterpriseId");
			enterpriseList.addAll(getValueListFromHeaderOrParameter(request, "enterpriseIds"));
			targetShardingDb = NSDbShardingUtils.getShardingDBByEnterpriseIdList(shardConfigs, enterpriseList);
			logger.debug("Got targetShardingDb[{}] by EnterpriseId[{}] from request header/parameter..", String.join(",", targetShardingDb), String.join(",", enterpriseList));
		}
		if(CollectionUtils.isEmpty(targetShardingDb)) {
			Set<String> orderNoList = getValueFromHeaderOrParameter(request, "orderNo");
			orderNoList.addAll(getValueListFromHeaderOrParameter(request, "orderNos"));
			targetShardingDb = NSDbShardingUtils.getShardingDBByOrderNoList(shardConfigs, orderNoList);
			logger.debug("Got targetShardingDb[{}] by orderNo[{}] from request header/parameter..", String.join(",", targetShardingDb), String.join(",", orderNoList));
		}

		try (DataSourceContextHolder dataSourceContextHolder = DataSourceContextHolder.getInstance()) {
			if (!CollectionUtils.isEmpty(targetShardingDb)) {
				dataSourceContextHolder.setDatasource(targetShardingDb);
			} else {
				List<String> allDb = NSDbShardingUtils.getAllShardingDB(shardConfigs);
				logger.debug("Can NOT determine any target sharding datasource, will use ALL datasources[{}].", String.join(",", allDb));
				dataSourceContextHolder.setDatasource(allDb);
			}
			result = joinPoint.proceed();
		} /*catch (Throwable throwable) {
			throwable.printStackTrace();
			throw new RuntimeException(throwable);
		}*/
		return result;
	}

	public Set<String> getValueFromHeaderOrParameter(HttpServletRequest request, String parameterName) {
		Set<String> valueList = new HashSet<>();
		String valueFromHeader = request.getHeader(parameterName);
		if(!StringUtils.isEmpty(valueFromHeader)){ valueList.add(valueFromHeader); };
		String valueFromParam = request.getParameter(parameterName);
		if(!StringUtils.isEmpty(valueFromParam)){ valueList.add(valueFromParam); };
		return valueList;
	}

	public Set<String> getValueListFromHeaderOrParameter(HttpServletRequest request, String parameterName) {
		Set<String> valueList = new HashSet<>();
		String valueFromHeader = request.getHeader(parameterName);
		if(!StringUtils.isEmpty(valueFromHeader)){ valueList.addAll(Arrays.asList(valueFromHeader.split(","))); };
		String valueFromParam = request.getParameter(parameterName);
		if(!StringUtils.isEmpty(valueFromParam)){ valueList.addAll(Arrays.asList(valueFromParam.split(","))); };
		return valueList;
	}

	public void dumpHeaderAndParameter(HttpServletRequest request) {
		Enumeration<String> headerNames = request.getHeaderNames();
		while(headerNames.hasMoreElements()) {
			String headerName = headerNames.nextElement();
			logger.debug("Requst Header Name - {}, Value - {}", headerName, request.getHeader(headerName));
		}
		Enumeration<String> params = request.getParameterNames();
		while(params.hasMoreElements()){
			String paramName = params.nextElement();
			logger.debug("Requst Parameter Name - {}, Value - {}", paramName, request.getParameter(paramName));
		}
	}
}
