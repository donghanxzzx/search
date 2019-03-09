package com.dhxz.search.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

@Configuration
@EnableConfigurationProperties(ThreadPoolConfigProperties.class)
public class ThreadPoolConfig {

    private ThreadPoolConfigProperties threadPoolConfigProperties;

    public ThreadPoolConfig(ThreadPoolConfigProperties threadPoolConfigProperties) {
        this.threadPoolConfigProperties = threadPoolConfigProperties;
    }

    @Bean
    public ThreadPoolTaskExecutor commonTaskExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(threadPoolConfigProperties.getCommonCorePoolSize());
        executor.setMaxPoolSize(threadPoolConfigProperties.getCommonMaxPoolSize());
        executor.setKeepAliveSeconds(30);
        executor.setThreadNamePrefix("common-task-thread-");
        return executor;
    }

    @Bean
    public ThreadPoolTaskExecutor contentTaskExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(threadPoolConfigProperties.getContentCorePoolSize());
        executor.setMaxPoolSize(threadPoolConfigProperties.getContentMaxPoolSize());
        executor.setKeepAliveSeconds(30);
        executor.setThreadNamePrefix("content-task-thread-");
        return executor;
    }
}
