package com.newsee.database.sharding;

import com.newsee.database.util.DataSourceContextHolder;
import org.apache.shardingsphere.api.sharding.hint.HintShardingAlgorithm;
import org.apache.shardingsphere.api.sharding.hint.HintShardingValue;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

public class NSDbHintShardingAlgorithm implements HintShardingAlgorithm {
    Logger logger = LoggerFactory.getLogger(NSDbHintShardingAlgorithm.class);

    @Override
    public Collection<String> doSharding(Collection availableTargetNames, HintShardingValue hintShardingValue) {

        // 控制层hint或者数据源上下文强制路由，直接跳转到对应的分库
        String targetDatasources = DataSourceContextHolder.getDataSource();
        if (Objects.nonNull(targetDatasources)) {
            logger.debug("Matched Datasource[{}] by DataSourceContextHolder.", targetDatasources);
            return Arrays.asList(targetDatasources.split(","));
        }

        return availableTargetNames;
    }
}