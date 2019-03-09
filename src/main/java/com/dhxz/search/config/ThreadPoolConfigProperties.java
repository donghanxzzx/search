package com.dhxz.search.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Data
@ConfigurationProperties(prefix = "thread-pool")
public class ThreadPoolConfigProperties {
    private Integer commonCorePoolSize;
    private Integer commonMaxPoolSize;

    private Integer contentCorePoolSize;
    private Integer contentMaxPoolSize;

}
