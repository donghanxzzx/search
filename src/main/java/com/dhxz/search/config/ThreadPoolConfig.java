package com.dhxz.search.config;

import java.util.concurrent.ThreadPoolExecutor.AbortPolicy;
import java.util.concurrent.ThreadPoolExecutor.DiscardPolicy;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

/**
 * @author 10066610
 * @description 线程池配置
 * @date 2019/3/8 15:52
 **/
@Configuration
public class ThreadPoolConfig {

    @Bean
    @Primary
    public ThreadPoolTaskExecutor commonTaskExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(16);
        executor.setKeepAliveSeconds(60);
        executor.setThreadNamePrefix("CommonTask-");
        executor.setRejectedExecutionHandler(new AbortPolicy());
        executor.setWaitForTasksToCompleteOnShutdown(true);
        return executor;
    }

    @Bean
    public ThreadPoolTaskExecutor contentTaskExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(16);
        executor.setKeepAliveSeconds(60);
        executor.setThreadNamePrefix("ContentTask-");
        executor.setRejectedExecutionHandler(new DiscardPolicy());
        executor.setWaitForTasksToCompleteOnShutdown(true);
        return executor;
    }

}
