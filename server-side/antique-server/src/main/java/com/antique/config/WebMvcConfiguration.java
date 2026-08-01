package com.antique.config;

import com.antique.interceptor.TokenInterceptor;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Web MVC 配置 — CORS 跨域 + Token 拦截器注册
 *
 * <h3>拦截范围</h3>
 * <p>拦截所有 {@code /api/**} 路径，但排除 {@code /api/auth/**}。
 * 这意味着所有非登录/验证码接口都需要携带有效的 Authorization Header。
 *
 * <h3>CORS 配置</h3>
 * <p>开发环境开放全部跨域请求。生产环境应改为限制具体域名。
 *
 * <h3>与 ciTY Art 的差异</h3>
 * <p>ciTY Art 有 user 和 admin 两个拦截路径组，本项目目前只有用户端，
 * 后续添加管理端时可参考 ciTY Art 模式扩展。
 */
@Configuration
@RequiredArgsConstructor
public class WebMvcConfiguration implements WebMvcConfigurer {

    private final TokenInterceptor tokenInterceptor;

    /**
     * 注册 Token 认证拦截器
     *
     * <p>拦截 /api/**（除 /api/auth/**），在请求到达 Controller 之前
     * 完成 Token 校验、续期、用户上下文设置。
     */
    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(tokenInterceptor)
                .addPathPatterns("/api/**")               // 拦截所有 API 请求
                .excludePathPatterns(
                        "/api/auth/**"                    // 登录/验证码接口无需认证
                );
    }

    /**
     * 配置 CORS 跨域（开发环境全开）
     */
    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/**")                        // 所有路径
                .allowedOriginPatterns("*")               // 允许所有来源
                .allowedMethods("*")                      // 允许所有 HTTP 方法
                .allowedHeaders("*")                      // 允许所有请求头
                .allowCredentials(true)                   // 允许携带 Cookie
                .maxAge(3600);                            // 预检请求缓存 1 小时
    }
}
