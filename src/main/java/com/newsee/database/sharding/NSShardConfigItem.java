package com.newsee.database.sharding;

import java.util.List;

/**
 * 租户、项目分片设置
 */
public class NSShardConfigItem {
    // 分片名称
    private String rangeName;
    // 分区编号，从10开始到99
    private Short rangeNum = 10;
    // 分片组
    private String rangeGroup;
    // 租户id列表
    private List<Long> enterpriseIds;
    // 项目id列表
    private List<Long> precinctIds;
    private Long precinctIdStart = 0L;
    private Long precinctIdEnd = 0L;
    // 项目/房产id起始值
    private Long houseIdStart = 0L;
    private Long houseIdEnd = 0L;
    // 数据源名称
    private String datasourceName;
    // 项目/房产id起始值
    private Long autoIncrementIdStart = 0L;
    private Long autoIncrementIdEnd = 0L;

    public String getRangeName() {
        return rangeName;
    }

    public void setRangeName(String rangeName) {
        this.rangeName = rangeName;
    }

    public Short getRangeNum() { return rangeNum; }

    public void setRangeNum(Short rangeNum) { this.rangeNum = rangeNum; }

    public String getRangeGroup() { return rangeGroup; }

    public void setRangeGroup(String rangeGroup) { this.rangeGroup = rangeGroup; }

    public List<Long> getEnterpriseIds() {
        return enterpriseIds;
    }

    public void setEnterpriseIds(List<Long> enterpriseIds) {
        this.enterpriseIds = enterpriseIds;
    }

    public List<Long> getPrecinctIds() {
        return precinctIds;
    }

    public void setPrecinctIds(List<Long> precinctIds) {
        this.precinctIds = precinctIds;
    }

    public Long getPrecinctIdStart() {
        return precinctIdStart;
    }

    public void setPrecinctIdStart(Long precinctIdStart) {
        this.precinctIdStart = precinctIdStart;
    }

    public Long getPrecinctIdEnd() {
        return precinctIdEnd;
    }

    public void setPrecinctIdEnd(Long precinctIdEnd) {
        this.precinctIdEnd = precinctIdEnd;
    }

    public Long getHouseIdStart() {
        return houseIdStart;
    }

    public void setHouseIdStart(Long houseIdStart) {
        this.houseIdStart = houseIdStart;
    }

    public Long getHouseIdEnd() {
        return houseIdEnd;
    }

    public void setHouseIdEnd(Long houseIdEnd) {
        this.houseIdEnd = houseIdEnd;
    }

    public String getDatasourceName() {
        return datasourceName;
    }

    public void setDatasourceName(String datasourceName) {
        this.datasourceName = datasourceName;
    }

    public Long getAutoIncrementIdStart() {
        return autoIncrementIdStart;
    }

    public void setAutoIncrementIdStart(Long autoIncrementIdStart) {
        this.autoIncrementIdStart = autoIncrementIdStart;
    }

    public Long getAutoIncrementIdEnd() {
        return autoIncrementIdEnd;
    }

    public void setAutoIncrementIdEnd(Long autoIncrementIdEnd) {
        this.autoIncrementIdEnd = autoIncrementIdEnd;
    }
}
