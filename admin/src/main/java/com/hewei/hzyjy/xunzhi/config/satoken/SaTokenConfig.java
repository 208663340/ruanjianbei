package com.hewei.hzyjy.xunzhi.config.satoken;

import cn.dev33.satoken.interceptor.SaInterceptor;
import cn.dev33.satoken.router.SaRouter;
import cn.dev33.satoken.stp.StpUtil;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Sa-Token 配置类 (SpringBoot3版本)
 */
@Configuration
public class SaTokenConfig implements WebMvcConfigurer {

    /**
     * 注册Sa-Token拦截器，打开注解式鉴权功能
     */
    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        // 注册Sa-Token拦截器，校验规则为StpUtil.checkLogin()登录校验
        registry.addInterceptor(new SaInterceptor(handle -> {
            SaRouter
                    .match("/api/xunzhi/v1/**")    // 拦截的路径
                    .notMatch("/api/xunzhi/v1/users/login")    // 排除登录接口
                    .notMatch("/api/xunzhi/v1/users/register")          // 排除注册接口
                    .notMatch("/api/xunzhi/v1/users/has-username") // 排除用户名检查接口
                    .check(() -> StpUtil.checkLogin());        // 校验是否登录
        })).addPathPatterns("/**");
    }
}