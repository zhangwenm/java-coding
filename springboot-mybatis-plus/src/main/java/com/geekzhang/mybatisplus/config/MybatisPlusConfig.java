package com.geekzhang.mybatisplus.config;

import com.baomidou.mybatisplus.annotation.DbType;
import com.baomidou.mybatisplus.core.handlers.MetaObjectHandler;
import com.baomidou.mybatisplus.extension.plugins.MybatisPlusInterceptor;
import com.baomidou.mybatisplus.extension.plugins.inner.BlockAttackInnerInterceptor;
import com.baomidou.mybatisplus.extension.plugins.inner.OptimisticLockerInnerInterceptor;
import com.baomidou.mybatisplus.extension.plugins.inner.PaginationInnerInterceptor;
import lombok.extern.slf4j.Slf4j;
import org.apache.ibatis.reflection.MetaObject;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.LocalDateTime;

/**
 * MyBatis-Plus配置类
 *
 * @author geekzhang
 * @since 2025-09-24
 */
@Slf4j
@Configuration
public class MybatisPlusConfig {

    /**
     * MyBatis-Plus拦截器配置
     */
    @Bean
    public MybatisPlusInterceptor mybatisPlusInterceptor() {
        MybatisPlusInterceptor interceptor = new MybatisPlusInterceptor();
        
        // 分页插件
        interceptor.addInnerInterceptor(new PaginationInnerInterceptor(DbType.MYSQL));
        
        // 乐观锁插件
        interceptor.addInnerInterceptor(new OptimisticLockerInnerInterceptor());
        
        // 防止全表更新与删除插件
        interceptor.addInnerInterceptor(new BlockAttackInnerInterceptor());
        
        log.info("MyBatis-Plus拦截器配置完成");
        return interceptor;
    }

    /**
     * 自动填充配置
     */
    @Bean
    public MetaObjectHandler metaObjectHandler() {
        return new MetaObjectHandler() {
            @Override
            public void insertFill(MetaObject metaObject) {
                log.info("开始insert填充...");
                // 自动填充创建时间和更新时间
                this.strictInsertFill(metaObject, "createTime", LocalDateTime.class, LocalDateTime.now());
                this.strictInsertFill(metaObject, "updateTime", LocalDateTime.class, LocalDateTime.now());
                
                // 自动填充创建人和更新人（可以从当前登录用户获取）
                this.strictInsertFill(metaObject, "createUser", String.class, getCurrentUser());
                this.strictInsertFill(metaObject, "updateUser", String.class, getCurrentUser());
                
                // 自动填充删除标志
                this.strictInsertFill(metaObject, "deleted", Integer.class, 0);
            }

            @Override
            public void updateFill(MetaObject metaObject) {
                log.info("开始update填充...");
                // 自动填充更新时间
                this.strictUpdateFill(metaObject, "updateTime", LocalDateTime.class, LocalDateTime.now());
                
                // 自动填充更新人
                this.strictUpdateFill(metaObject, "updateUser", String.class, getCurrentUser());
            }
            
            /**
             * 获取当前用户
             * 实际项目中可以从SecurityContext或ThreadLocal中获取
             */
            private String getCurrentUser() {
                // 这里可以集成Spring Security或其他认证框架获取当前用户
                // 暂时返回系统用户
                return "system";
            }
        };
    }
}
