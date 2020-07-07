package com.newsee.database.sharding;

import com.newsee.database.util.DataSourceContextHolder;
import org.apache.shardingsphere.api.sharding.complex.ComplexKeysShardingAlgorithm;
import org.apache.shardingsphere.api.sharding.complex.ComplexKeysShardingValue;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.StringUtils;

import java.util.*;

public class NSDbShardingAlgorithm implements ComplexKeysShardingAlgorithm {
    Logger logger = LoggerFactory.getLogger(NSDbShardingAlgorithm.class);

    public static final Set<String> PRECINCTID_ALIAS = Collections.unmodifiableSet(new HashSet(Arrays.asList("preinctId","PreinctId","precinctId","PrecinctId","precinct_id")));
    public static final Set<String> HOUSEID_ALIAS = Collections.unmodifiableSet(new HashSet(Arrays.asList("houseId", "HouseId", "house_id")));
    public static final Set<String> PAYMENT_ORDERNO_ALIAS = Collections.unmodifiableSet(new HashSet(Arrays.asList("orderNo", "OrderNo")));
    public static final Set<String> AUTO_INCREMENT_ID_ALIAS = Collections.unmodifiableSet(new HashSet(Arrays.asList("id", "Id")));
    public static final Set<String> ENTERPRISEDID_ALIAS = Collections.unmodifiableSet(new HashSet(Arrays.asList("enterpriseId", "EnterpriseId")));

    @Override
    public Collection<String> doSharding(Collection collection, ComplexKeysShardingValue complexKeysShardingValue) {
        logger.debug(complexKeysShardingValue.toString());

        // 控制层hint，直接跳转到对应的分库
        String targetDatasources = DataSourceContextHolder.getDataSource();
        if (Objects.nonNull(targetDatasources)) {
            logger.debug("Matched Datasource {} by DataSourceContextHolder.", targetDatasources);
            return Arrays.asList(targetDatasources.split(","));
        }

        NSShardConfig shardConfigs = NSShardConfig.getContext().getBean(NSShardConfig.class);

        // 分片字段及其精确匹配的值
        Map<String, Collection<Object>> columnNameAndShardingValuesMap = complexKeysShardingValue.getColumnNameAndShardingValuesMap();
        // 分片字段及其范围匹配的值
        //Map<String, Range> columnNameAndRangeValuesMap = complexKeysShardingValue.getColumnNameAndRangeValuesMap();

        List<String> shardingResults = new ArrayList<>();
//        columnNameAndShardingValuesMap.forEach((columnName, shardingValues) -> {
//        });

        // 复合分片键中包含有项目Id
        if (Arrays.stream(PRECINCTID_ALIAS.toArray()).anyMatch(columnNameAndShardingValuesMap::containsKey)) {
            Arrays.stream(PRECINCTID_ALIAS.toArray()).filter(columnNameAndShardingValuesMap::containsKey).forEach(shardingColumnName -> {
                Collection<Object> shardingValues = columnNameAndShardingValuesMap.get(shardingColumnName);
                shardingValues.forEach(value -> {
                    List<String> dbs = NSDbShardingUtils.getShardingDBByPrecinctId(shardConfigs, (Long)value);
                    if (!StringUtils.isEmpty(dbs)) {
                        shardingResults.addAll(dbs);
                        logger.debug("Matched Datasource {} by Precinct {}:{}", dbs, shardingColumnName, value);
                    }
                });
                if (shardingResults.size() > 0) return;
            });
        }

        // 复合分片键中包含有房产Id
        if (Arrays.stream(HOUSEID_ALIAS.toArray()).anyMatch(columnNameAndShardingValuesMap::containsKey)) {
            Arrays.stream(HOUSEID_ALIAS.toArray()).filter(columnNameAndShardingValuesMap::containsKey).forEach(shardingColumnName -> {
                Collection<Object> shardingValues = columnNameAndShardingValuesMap.get(shardingColumnName);
                shardingValues.forEach(value -> {
                    List<String> dbs = NSDbShardingUtils.getShardingDBByHouseId(shardConfigs, (Long)value);
                    if (!StringUtils.isEmpty(dbs)) {
                        shardingResults.addAll(dbs);
                        logger.debug("Matched Datasource {} by House {}:{}", dbs, shardingColumnName, value);
                    }
                });
                if (shardingResults.size() > 0) return;
            });
        }

        // 复合分片键中包含有缴费订单号
        if (Arrays.stream(PAYMENT_ORDERNO_ALIAS.toArray()).anyMatch(columnNameAndShardingValuesMap::containsKey)) {
            Arrays.stream(PAYMENT_ORDERNO_ALIAS.toArray()).filter(columnNameAndShardingValuesMap::containsKey).forEach(shardingColumnName -> {
                Collection<Object> shardingValues = columnNameAndShardingValuesMap.get(shardingColumnName);
                shardingValues.forEach(value -> {
                    List<String> dbs = NSDbShardingUtils.getShardingDBByOrderNo(shardConfigs, (String) value);
                    if (!StringUtils.isEmpty(dbs)) {
                        shardingResults.addAll(dbs);
                        logger.debug("Matched Datasource {} by Order {}:{}", dbs, shardingColumnName, value);
                    }
                });
                if (shardingResults.size() > 0) return;
            });
        }

        // 复合分片键中包含有自增长id
        if (Arrays.stream(AUTO_INCREMENT_ID_ALIAS.toArray()).anyMatch(columnNameAndShardingValuesMap::containsKey)) {
            Arrays.stream(AUTO_INCREMENT_ID_ALIAS.toArray()).filter(columnNameAndShardingValuesMap::containsKey).forEach(shardingColumnName -> {
                Collection<Object> shardingValues = columnNameAndShardingValuesMap.get(shardingColumnName);
                shardingValues.forEach(value -> {
                    List<String> dbs = NSDbShardingUtils.getShardingDBByAutoIncrementId(shardConfigs, (Long) value);
                    if (!StringUtils.isEmpty(dbs)) {
                        shardingResults.addAll(dbs);
                        logger.debug("Matched Datasource {} by AUTO_INCREMENT_ID {}:{}", dbs, shardingColumnName, value);
                    }
                });
                if (shardingResults.size() > 0) return;
            });
        }

        // 复合分片键中包含有租户Id
        if (Arrays.stream(ENTERPRISEDID_ALIAS.toArray()).anyMatch(columnNameAndShardingValuesMap::containsKey)) {
            Arrays.stream(ENTERPRISEDID_ALIAS.toArray()).filter(columnNameAndShardingValuesMap::containsKey).forEach(shardingColumnName -> {
                Collection<Object> shardingValues = columnNameAndShardingValuesMap.get(shardingColumnName);
                shardingValues.forEach(value -> {
                    List<String> dbs = NSDbShardingUtils.getShardingDBByEnterpriseId(shardConfigs, (Long) value);
                    if (!StringUtils.isEmpty(dbs)) {
                        shardingResults.addAll(dbs);
                        logger.debug("Matched Datasource {} by Tenant {}:{}", dbs, shardingColumnName, value);
                    }
                });
                if (shardingResults.size() > 0) return;
            });
        }

        return shardingResults;
    }


}
