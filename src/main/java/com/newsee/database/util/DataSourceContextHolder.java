package com.newsee.database.util;


import com.newsee.database.sharding.NSDbShardingUtils;
import com.newsee.database.sharding.NSShardConfig;
import org.apache.shardingsphere.api.hint.HintManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.CollectionUtils;

import java.util.*;

public class DataSourceContextHolder implements AutoCloseable {
    private static final Logger log = LoggerFactory.getLogger(DataSourceContextHolder.class);
    //线程本地环境
    private static final ThreadLocal<DataSourceContextHolder> DATA_SOURCE_CONTEXT_HOLDER = new ThreadLocal<DataSourceContextHolder>();

    public static DataSourceContextHolder getInstance() {
        DataSourceContextHolder instance = DATA_SOURCE_CONTEXT_HOLDER.get();
        if (null == instance) {
            instance = new DataSourceContextHolder();
            DATA_SOURCE_CONTEXT_HOLDER.set(instance);
        }
        return instance;
    }

    public static DataSourceContextHolder getLocal() {
        return getInstance();
    }

    private final Set<String> shardingDatasources = new HashSet();
    private final HintManager hintManager = HintManager.getInstance();

    private void setShardingDatabaseHint() {
        hintManager.setDatabaseShardingValue(String.join(",", shardingDatasources));
    }

    public void addDatasource(final String targetDatasource) {
        if (Objects.nonNull(targetDatasource)) {
            shardingDatasources.addAll(Arrays.asList(targetDatasource.split(",")));
            setShardingDatabaseHint();
        }
    }

    public void addDatasource(final List<String> targetDatasources) {
        if (!CollectionUtils.isEmpty(targetDatasources)) {
            shardingDatasources.addAll(targetDatasources);
            setShardingDatabaseHint();
        }
    }

    public void setDatasource(final String targetDatasource) {
        shardingDatasources.clear();
        addDatasource(targetDatasource);
    }

    public void setDatasource(final List<String> targetDatasource) {
        shardingDatasources.clear();
        addDatasource(targetDatasource);
    }

    public void setMasterOnly() {
        hintManager.setMasterRouteOnly();
    }

//    public static void setDB00() {
//    	localDataSource.set(DataSourceEnum.DB00.getValue());
//        log.info("数据库切换到DB00...");
//    }

    public static String getDataSource() {
        return null == DATA_SOURCE_CONTEXT_HOLDER.get() ? null : String.join(",", DATA_SOURCE_CONTEXT_HOLDER.get().shardingDatasources);
    }

    public static void clear() {
        DataSourceContextHolder instance = DATA_SOURCE_CONTEXT_HOLDER.get();
        if (null != instance) {
            instance.shardingDatasources.clear();
            instance.hintManager.close();
            DATA_SOURCE_CONTEXT_HOLDER.remove();
        }
    }

    @Override
    public void close() {
        DataSourceContextHolder.clear();
    }

    public void setDatasourceByGroupAndPrecinctId(final String rangeGroup, final List<Long> precinctIdList) {
        setDatasource(
                NSDbShardingUtils.getShardingDBByPrecinctIdList(
                        NSShardConfig.getContext().getBean(NSShardConfig.class),
                        new HashSet(precinctIdList),
                        rangeGroup)
        );
    }

    public void setDatasourceByGroup(final String rangeGroup) {
        setDatasource(
                NSDbShardingUtils.getAllShardingDB(
                        NSShardConfig.getContext().getBean(NSShardConfig.class),
                        rangeGroup)
        );
    }

}
