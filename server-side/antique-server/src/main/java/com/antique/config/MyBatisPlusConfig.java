package com.antique.config;

import com.baomidou.mybatisplus.annotation.DbType;
import com.baomidou.mybatisplus.extension.plugins.MybatisPlusInterceptor;
import com.baomidou.mybatisplus.extension.plugins.inner.PaginationInnerInterceptor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * MyBatis-Plus 插件配置
 *
 * <h3>当前注册的插件</h3>
 * <ul>
 *   <li>分页插件（PaginationInnerInterceptor）：使 Page/selectPage 生效，
 *       自动生成 LIMIT 语句和 COUNT 查询</li>
 * </ul>
 *
 * <p>参考来源：ciTY Art 项目中的同名配置。
 */
@Configuration
public class MyBatisPlusConfig {

    /**
     * 注册 MyBatis-Plus 拦截器链
     *
     * <p>分页插件必须注册后才能使用分页功能，
     * 否则 {@code selectPage} 和自定义分页 SQL 不会拼接 LIMIT。
     */
    @Bean
    public MybatisPlusInterceptor mybatisPlusInterceptor() {
        MybatisPlusInterceptor interceptor = new MybatisPlusInterceptor();
        // MySQL 方言分页：SELECT ... LIMIT offset, size
        interceptor.addInnerInterceptor(new PaginationInnerInterceptor(DbType.MYSQL));
        return interceptor;
    }
}
