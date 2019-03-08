package com.dhxz.search.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;

/**
 * @author 10066610
 * @description 定时任务配置
 * @date 2019/2/21 11:24
 **/
@Slf4j
@Configuration
@EnableScheduling
public class ScheduleConfig {

    @Bean
    ThreadPoolTaskScheduler taskScheduler() {
        ThreadPoolTaskScheduler scheduler = new ThreadPoolTaskScheduler();
        scheduler.setThreadNamePrefix("定时任务");
        scheduler.setErrorHandler(t -> log.error("任务执行失败:{}", t));
        scheduler.setPoolSize(2);
        return scheduler;
    }
}
