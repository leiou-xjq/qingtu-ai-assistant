package com.qingtu.agent.config;

import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.quartz.SchedulerFactoryBean;
import org.springframework.scheduling.quartz.SpringBeanJobFactory;

/**
 * Quartz定时任务配置类
 * 配置使用 application.yml 中的 quartz.job-store-type
 */
@Configuration
public class QuartzConfig {

    private final ApplicationContext applicationContext;

    public QuartzConfig(ApplicationContext applicationContext) {
        this.applicationContext = applicationContext;
    }

    @Bean
    public SpringBeanJobFactory jobFactory() {
        SpringBeanJobFactory jobFactory = new SpringBeanJobFactory();
        jobFactory.setApplicationContext(applicationContext);
        return jobFactory;
    }

    @Bean
    public SchedulerFactoryBean schedulerFactoryBean(SpringBeanJobFactory jobFactory) {
        SchedulerFactoryBean factory = new SchedulerFactoryBean();
        factory.setOverwriteExistingJobs(true);
        factory.setStartupDelay(10);
        factory.setAutoStartup(true);
        factory.setJobFactory(jobFactory);
        return factory;
    }
}