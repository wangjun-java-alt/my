package com.newsee.database.sharding;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.AutowireCapableBeanFactory;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
@ConfigurationProperties(prefix = "newsee.sharding", ignoreUnknownFields = false)
public class NSShardConfig implements ApplicationContextAware {

    static ApplicationContext applicationContext = null;
    public void setApplicationContext(ApplicationContext context) throws BeansException {
        applicationContext = context;
    }
    public static ApplicationContext getContext() {
        return applicationContext;
    }

    private String defaultDatasourceName;
    private List<NSShardConfigItem> shardConfigs;

    public String getDefaultDatasourceName() {
        return defaultDatasourceName;
    }

    public void setDefaultDatasourceName(String defaultDatasourceName) {
        this.defaultDatasourceName = defaultDatasourceName;
    }

    public List<NSShardConfigItem> getShardConfigs() {
        return shardConfigs;
    }

    public void setShardConfigs(List<NSShardConfigItem> shardConfigs) {
        this.shardConfigs = shardConfigs;
    }
}
