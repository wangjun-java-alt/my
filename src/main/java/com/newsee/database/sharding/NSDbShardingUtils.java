package com.newsee.database.sharding;

import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;
import java.util.regex.Pattern;

public class NSDbShardingUtils {

    /**
     * 根据项目ID找到对应的分库
     * @param shardConfig 水平分库范围配置
     * @param precinctd 项目ID
     * @return 对应的分库数据源
     */
    public static List<String> getShardingDBByPrecinctId(NSShardConfig shardConfig, Long precinctd) {
        AtomicReference<List<String>> targetShardingDb = new AtomicReference(new ArrayList<String>());
        if (shardConfig.getShardConfigs() != null) {
            shardConfig.getShardConfigs().forEach(nsShardConfigItem -> {
                if ((!CollectionUtils.isEmpty(nsShardConfigItem.getPrecinctIds())
                        && nsShardConfigItem.getPrecinctIds().contains(precinctd))
                        || (nsShardConfigItem.getPrecinctIdStart() > 0L
                        && nsShardConfigItem.getPrecinctIdEnd() > 0L
                        && nsShardConfigItem.getPrecinctIdStart() <= precinctd
                        && nsShardConfigItem.getPrecinctIdEnd() >= precinctd)
                ) {
                    targetShardingDb.get().add((nsShardConfigItem.getDatasourceName()));
                }
            });
        }
        return targetShardingDb.get();
    }

    /**
     * 查找项目ID列表所分布的分库数据源
     * @param shardConfigs 水平分库范围配置
     * @param precinctIdList 项目ID列表
     * @return 分布的分库数据源列表
     */
    public static List<String> getShardingDBByPrecinctIdList(NSShardConfig shardConfigs, Set<?> precinctIdList) {
        List<String> targetShardingDb = new ArrayList<>();
        if (!CollectionUtils.isEmpty(precinctIdList) && Objects.nonNull(shardConfigs)) {
            precinctIdList.forEach(precinctId -> {
                targetShardingDb.addAll(NSDbShardingUtils.getShardingDBByPrecinctId(
                        shardConfigs,
                        precinctId instanceof String ? Long.valueOf((String)precinctId) : (Long)precinctId));
            });
        }
        return targetShardingDb;
    }

    /**
     * 根据项目ID找到对应的分库
     * @param shardConfig 水平分库范围配置
     * @param precinctd 项目ID
     * @param rangeGroup 数据源分组
     * @return 对应的分库数据源
     */
    public static List<String> getShardingDBByPrecinctId(NSShardConfig shardConfig, Long precinctd, String rangeGroup) {
        AtomicReference<List<String>> targetShardingDb = new AtomicReference(new ArrayList<String>());
        if (shardConfig.getShardConfigs() != null) {
            shardConfig.getShardConfigs().forEach(nsShardConfigItem -> {
                if ((!StringUtils.isEmpty(rangeGroup) && rangeGroup.equals(nsShardConfigItem.getRangeGroup()))
                        && ((!CollectionUtils.isEmpty(nsShardConfigItem.getPrecinctIds())
                                && nsShardConfigItem.getPrecinctIds().contains(precinctd))
                            || (nsShardConfigItem.getPrecinctIdStart() > 0L
                                && nsShardConfigItem.getPrecinctIdEnd() > 0L
                                && nsShardConfigItem.getPrecinctIdStart() <= precinctd
                                && nsShardConfigItem.getPrecinctIdEnd() >= precinctd))
                ) {
                    targetShardingDb.get().add(nsShardConfigItem.getDatasourceName());
                }
            });
        }
        return targetShardingDb.get();
    }
    /**
     * 查找项目ID列表所对应的分库数据源
     * @param shardConfigs 水平分库范围配置
     * @param precinctIdList 项目ID列表
     * @param rangeGroup 数据源分组
     * @return 对应的分库数据源列表
     */
    public static List<String> getShardingDBByPrecinctIdList(NSShardConfig shardConfigs, Set<?> precinctIdList, String rangeGroup) {
        List<String> targetShardingDb = new ArrayList<>();
        if (!CollectionUtils.isEmpty(precinctIdList) && Objects.nonNull(shardConfigs)) {
            precinctIdList.forEach(precinctId -> {
                targetShardingDb.addAll(NSDbShardingUtils.getShardingDBByPrecinctId(
                        shardConfigs,
                        precinctId instanceof String ? Long.valueOf((String)precinctId) : (Long)precinctId,
                        rangeGroup));
            });
        }
        return targetShardingDb;
    }

    public static List<String> getShardingDBByHouseId(NSShardConfig shardConfig, Long houseId) {
        AtomicReference<List<String>> targetShardingDb =  new AtomicReference(new ArrayList<String>());
        if (shardConfig.getShardConfigs() != null) {
            shardConfig.getShardConfigs().forEach(nsShardConfigItem -> {
                if (nsShardConfigItem.getHouseIdStart() > 0L
                        && nsShardConfigItem.getHouseIdEnd() > 0L
                        && nsShardConfigItem.getHouseIdStart() <= houseId
                        && nsShardConfigItem.getHouseIdEnd() >= houseId) {
                    targetShardingDb.get().add(nsShardConfigItem.getDatasourceName());
                }
            });
        }
        return targetShardingDb.get();
    }
    public static List<String> getShardingDBByHouseIdList(NSShardConfig shardConfigs, Set<?> houseIdList) {
        List<String> targetShardingDb = new ArrayList<>();
        if (!CollectionUtils.isEmpty(houseIdList) && Objects.nonNull(shardConfigs)) {
            houseIdList.forEach(houseId -> {
                targetShardingDb.addAll(NSDbShardingUtils.getShardingDBByHouseId(
                        shardConfigs,
                        houseId instanceof String ? Long.valueOf((String)houseId) : (Long)houseId));
            });
        }
        return targetShardingDb;
    }

    public static List<String> getShardingDBByOrderNo(NSShardConfig shardConfig, String orderNo) {
        AtomicReference<List<String>> targetShardingDb =  new AtomicReference(new ArrayList<String>());
        if(!StringUtils.isEmpty(orderNo) && Pattern.matches("NS\\d{19,}",orderNo)){
            Short rangeNum = Short.parseShort(orderNo.substring(2, 4));
            if (shardConfig.getShardConfigs() != null) {
                shardConfig.getShardConfigs().forEach(nsShardConfigItem -> {
                    if (nsShardConfigItem.getRangeNum() == rangeNum) {
                        targetShardingDb.get().add(nsShardConfigItem.getDatasourceName());
                        return;
                    }
                });
            }
        }
        return targetShardingDb.get();
    }
    public static List<String> getShardingDBByOrderNoList(NSShardConfig shardConfigs, Set<?> orderNoList) {
        List<String> targetShardingDb = new ArrayList<>();
        if (!CollectionUtils.isEmpty(orderNoList) && Objects.nonNull(shardConfigs)) {
            orderNoList.forEach(orderNo -> {
                targetShardingDb.addAll(NSDbShardingUtils.getShardingDBByOrderNo(
                        shardConfigs,
                        orderNo instanceof String ? (String)orderNo : String.valueOf(orderNo)));
            });
        }
        return targetShardingDb;
    }

    public static List<String> getShardingDBByAutoIncrementId(NSShardConfig shardConfig, Long autoIncrementId) {
        AtomicReference<List<String>> targetShardingDb =  new AtomicReference(new ArrayList<String>());
        if (shardConfig.getShardConfigs() != null) {
            shardConfig.getShardConfigs().forEach(nsShardConfigItem -> {
                if (nsShardConfigItem.getAutoIncrementIdStart() > 0L
                        && nsShardConfigItem.getAutoIncrementIdEnd() > 0L
                        && nsShardConfigItem.getAutoIncrementIdStart() <= autoIncrementId
                        && nsShardConfigItem.getAutoIncrementIdEnd() >= autoIncrementId) {
                    targetShardingDb.get().add(nsShardConfigItem.getDatasourceName());
                }
            });
        }
        return targetShardingDb.get();
    }
    public static List<String> getShardingDBByAutoIncrementIdList(NSShardConfig shardConfigs, Set<?> autoIncrementIdList) {
        List<String> targetShardingDb = new ArrayList<>();
        if (!CollectionUtils.isEmpty(autoIncrementIdList) && Objects.nonNull(shardConfigs)) {
            autoIncrementIdList.forEach(autoIncrementId -> {
                targetShardingDb.addAll(NSDbShardingUtils.getShardingDBByAutoIncrementId(
                        shardConfigs,
                        autoIncrementId instanceof String ? Long.valueOf((String)autoIncrementId) : (Long)autoIncrementId));
            });
        }
        return targetShardingDb;
    }

    public static List<String> getShardingDBByEnterpriseId(NSShardConfig shardConfig, Long enterpriseId) {
        AtomicReference<List<String>> targetShardingDb =  new AtomicReference(new ArrayList<String>());
        if (shardConfig.getShardConfigs() != null) {
            shardConfig.getShardConfigs().forEach(nsShardConfigItem -> {
                if (!CollectionUtils.isEmpty(nsShardConfigItem.getEnterpriseIds())
                        && nsShardConfigItem.getEnterpriseIds().contains(enterpriseId)) {
                    targetShardingDb.get().add(nsShardConfigItem.getDatasourceName());
                }
            });
        }
        return targetShardingDb.get();
    }
    public static List<String> getShardingDBByEnterpriseIdList(NSShardConfig shardConfigs, Set<?> enterpriseIdList) {
        List<String> targetShardingDb = new ArrayList<>();
        if (!CollectionUtils.isEmpty(enterpriseIdList) && Objects.nonNull(shardConfigs)) {
            enterpriseIdList.forEach(enterpriseId -> {
                targetShardingDb.addAll(NSDbShardingUtils.getShardingDBByEnterpriseId(
                        shardConfigs,
                        enterpriseId instanceof String ? Long.valueOf((String)enterpriseId) : (Long)enterpriseId));
            });
        }
        return targetShardingDb;
    }

    /**
     * 找到所有分库
     * @param shardConfig 水平分库范围配置
     * @return 所有分库
     */
    public static List<String> getAllShardingDB(NSShardConfig shardConfig) {
        AtomicReference<List<String>> targetShardingDb = new AtomicReference(new ArrayList<String>());
        if (shardConfig.getShardConfigs() != null) {
            shardConfig.getShardConfigs().forEach(nsShardConfigItem -> {
                    targetShardingDb.get().add(nsShardConfigItem.getDatasourceName());
            });
        }
        return targetShardingDb.get();
    }

    /**
     * 找到分组下的所有分库
     * @param shardConfig 水平分库范围配置
     * @param rangeGroup 数据源分组
     * @return 分组下的所有分库
     */
    public static List<String> getAllShardingDB(NSShardConfig shardConfig, String rangeGroup) {
        AtomicReference<List<String>> targetShardingDb = new AtomicReference(new ArrayList<String>());
        if (StringUtils.isEmpty(rangeGroup) ){
            return targetShardingDb.get();
        }
        if (shardConfig.getShardConfigs() != null) {
            shardConfig.getShardConfigs().forEach(nsShardConfigItem -> {
                if (rangeGroup.equals(nsShardConfigItem.getRangeGroup())) {
                    targetShardingDb.get().add(nsShardConfigItem.getDatasourceName());
                }
            });
        }
        return targetShardingDb.get();
    }
}
