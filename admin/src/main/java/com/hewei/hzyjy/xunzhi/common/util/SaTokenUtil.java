package com.hewei.hzyjy.xunzhi.common.util;

import cn.dev33.satoken.stp.StpUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.hewei.hzyjy.xunzhi.common.convention.exception.ClientException;
import com.hewei.hzyjy.xunzhi.dao.entity.UserDO;
import com.hewei.hzyjy.xunzhi.dao.mapper.UserMapper;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;


import java.util.concurrent.TimeUnit;



/**
 * Sa-Token 工具类
 * 提供token解析、用户信息获取等功能
 * 
 * @author nageoffer
 * @date 2023/12/01
 */
@Slf4j
@Component
public class SaTokenUtil {
    
    /**
     * 用户ID缓存前缀
     */
    private static final String USER_ID_CACHE_KEY = "xunzhi:user:id:";
    
    @Autowired
    private StringRedisTemplate stringRedisTemplate;
    
    @Autowired
    private UserMapper userMapper;

    /**
     * 从请求中获取 token
     *
     * @param request HTTP请求对象
     * @return token值，如果不存在则返回null
     */
    public String getTokenFromRequest(HttpServletRequest request) {
        // 优先从 Header 中获取
        String token = request.getHeader(StpUtil.getTokenName());
        if (StrUtil.isNotBlank(token)) {
            // 处理Bearer前缀
            if (token.startsWith("Bearer ")) {
                token = token.substring(7); // 去掉"Bearer "前缀
            }
            return token;
        }
        
        // 从参数中获取
        token = request.getParameter(StpUtil.getTokenName());
        if (StrUtil.isNotBlank(token)) {
            // 处理Bearer前缀
            if (token.startsWith("Bearer ")) {
                token = token.substring(7); // 去掉"Bearer "前缀
            }
            return token;
        }
        
        return null;
    }

    /**
     * 检查 token 是否有效
     *
     * @param token token值
     * @return true-有效，false-无效
     */
    public boolean isValidToken(String token) {
        if (StrUtil.isBlank(token)) {
            return false;
        }
        try {
            return StpUtil.getLoginIdByToken(token) != null;
        } catch (Exception e) {
            log.warn("Token validation failed: {}", e.getMessage());
            return false;
        }
    }

    /**
     * 从请求中获取当前登录用户名
     *
     * @param request HTTP请求对象
     * @return 用户名，如果未登录则抛出异常
     */
    public String getUsernameFromRequest(HttpServletRequest request) {
        String token = getTokenFromRequest(request);
        if (StrUtil.isBlank(token)) {
            throw new RuntimeException("Token不存在");
        }
        
        if (!isValidToken(token)) {
            throw new RuntimeException("Token无效或已过期");
        }
        
        try {
            // 通过token获取登录ID（在这里是username）
            Object loginId = StpUtil.getLoginIdByToken(token);
            return loginId != null ? loginId.toString() : null;
        } catch (Exception e) {
            log.error("获取用户名失败: {}", e.getMessage());
            throw new RuntimeException("获取用户信息失败");
        }
    }

    /**
     * 根据用户名获取用户ID（优先从缓存获取，缓存不存在则查询数据库并缓存）
     *
     * @param username 用户名
     * @return 用户ID
     */
    public Long getUserIdByUsername(String username) {
        if (StrUtil.isBlank(username)) {
            throw new ClientException("用户名不能为空");
        }
        
        // 先从缓存获取
        String cacheKey = USER_ID_CACHE_KEY + username;
        String cachedUserId = stringRedisTemplate.opsForValue().get(cacheKey);
        
        if (StrUtil.isNotBlank(cachedUserId)) {
            // 续期缓存
            stringRedisTemplate.expire(cacheKey, 30L, TimeUnit.MINUTES);
            return Long.valueOf(cachedUserId);
        }
        
        // 缓存不存在，查询数据库
        LambdaQueryWrapper<UserDO> queryWrapper = Wrappers.lambdaQuery(UserDO.class)
                .eq(UserDO::getUsername, username)
                .select(UserDO::getId);
        
        UserDO userDO = userMapper.selectOne(queryWrapper);
        if (userDO == null || userDO.getId() == null) {
            throw new ClientException("用户不存在: " + username);
        }
        
        // 将用户ID写入缓存，设置30分钟过期时间
        stringRedisTemplate.opsForValue().set(cacheKey, userDO.getId().toString(), 30L, TimeUnit.MINUTES);
        
        log.info("用户ID已缓存: username={}, userId={}", username, userDO.getId());
        return userDO.getId();
    }
    
    /**
     * 从请求中获取当前登录用户ID
     *
     * @param request HTTP请求对象
     * @return 用户ID
     */
    public Long getUserIdFromRequest(HttpServletRequest request) {
        String username = getUsernameFromRequest(request);
        return getUserIdByUsername(username);
    }

    /**
     * 获取当前登录用户名
     * 
     * @return 用户名，如果未登录则返回null
     */
    public String getCurrentUsername() {
        try {
            if (!StpUtil.isLogin()) {
                return null;
            }
            Object loginId = StpUtil.getLoginId();
            return loginId != null ? loginId.toString() : null;
        } catch (Exception e) {
            log.error("获取当前登录用户名失败: {}", e.getMessage());
            return null;
        }
    }

    /**
     * 检查当前是否已登录
     * 
     * @return true表示已登录，false表示未登录
     */
    public boolean isLogin() {
        try {
            return StpUtil.isLogin();
        } catch (Exception e) {
            log.error("检查登录状态失败: {}", e.getMessage());
            return false;
        }
    }

    /**
     * 获取当前token值
     * 
     * @return token值，如果未登录则返回null
     */
    public String getCurrentToken() {
        try {
            return StpUtil.getTokenValue();
        } catch (Exception e) {
            log.error("获取当前token失败: {}", e.getMessage());
            return null;
        }
    }

    /**
     * 根据token检查是否登录
     * 
     * @param token Sa-Token的token值
     * @return true表示已登录，false表示未登录
     */
    public boolean isLoginByToken(String token) {
        if (StrUtil.isBlank(token)) {
            return false;
        }
        
        try {
            return StpUtil.getLoginIdByToken(token) != null;
        } catch (Exception e) {
            log.error("根据token检查登录状态失败: {}", e.getMessage());
            return false;
        }
    }

    /**
     * 强制指定token下线
     * 
     * @param token Sa-Token的token值
     */
    public void logoutByToken(String token) {
        if (StrUtil.isBlank(token)) {
            return;
        }
        
        try {
            StpUtil.logoutByTokenValue(token);
            log.info("Token已强制下线: {}", token);
        } catch (Exception e) {
            log.error("强制token下线失败: {}", e.getMessage());
        }
    }

    /**
     * 获取token剩余有效时间（秒）
     * 
     * @param token Sa-Token的token值
     * @return 剩余有效时间（秒），-1表示永不过期，-2表示token无效
     */
    public long getTokenTimeout(String token) {
        if (StrUtil.isBlank(token)) {
            return -2;
        }
        
        try {
            return StpUtil.getTokenTimeout(token);
        } catch (Exception e) {
            log.error("获取token剩余时间失败: {}", e.getMessage());
            return -2;
        }
    }
}