package com.qingtu.agent.config;

import com.baomidou.mybatisplus.annotation.DbType;
import com.baomidou.mybatisplus.core.handlers.MetaObjectHandler;
import com.baomidou.mybatisplus.extension.plugins.MybatisPlusInterceptor;
import com.baomidou.mybatisplus.extension.plugins.inner.PaginationInnerInterceptor;
import org.apache.ibatis.reflection.MetaObject;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.LocalDateTime;

/**
 * MyBatis-Plus配置类
 * 
 * 功能说明：
 * - 配置分页插件
 * - 配置自动填充（创建时间、更新时间）
 * 
 * @author 青途智伴技术团队
 */
@Configuration
public class MybatisPlusConfig {

    /**
     * 配置分页插件
     * 
     * 说明：
     * - MySQL使用DbType.MYSQL
     * - 分页合理化（pagenum小于1时自动查询第一页）
     */
    @Bean
    public MybatisPlusInterceptor mybatisPlusInterceptor() {
        MybatisPlusInterceptor interceptor = new MybatisPlusInterceptor();
        
        PaginationInnerInterceptor paginationInterceptor = new PaginationInnerInterceptor(DbType.MYSQL);
        paginationInterceptor.setMaxLimit(100L);
        paginationInterceptor.setOverflow(false);
        
        interceptor.addInnerInterceptor(paginationInterceptor);
        return interceptor;
    }

    /**
     * 配置自动填充处理器
     * 
     * 说明：
     * - 创建时间：首次插入时自动填充
     * - 更新时间：每次插入/更新时自动填充
     */
    @Bean
    public MetaObjectHandler metaObjectHandler() {
        return new MetaObjectHandler() {
            @Override
            public void insertFill(MetaObject metaObject) {
                this.strictInsertFill(metaObject, "createTime", LocalDateTime.class, LocalDateTime.now());
                this.strictInsertFill(metaObject, "updateTime", LocalDateTime.class, LocalDateTime.now());
            }

            @Override
            public void updateFill(MetaObject metaObject) {
                this.strictUpdateFill(metaObject, "updateTime", LocalDateTime.class, LocalDateTime.now());
            }
        };
    }
}