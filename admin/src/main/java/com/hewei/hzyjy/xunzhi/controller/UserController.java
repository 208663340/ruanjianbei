/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.hewei.hzyjy.xunzhi.controller;

import cn.dev33.satoken.annotation.SaCheckRole;
import cn.dev33.satoken.stp.StpUtil;
import cn.hutool.core.bean.BeanUtil;
import com.hewei.hzyjy.xunzhi.common.convention.result.Result;
import com.hewei.hzyjy.xunzhi.common.convention.result.Results;
import com.hewei.hzyjy.xunzhi.dto.req.user.UserLoginReqDTO;
import com.hewei.hzyjy.xunzhi.dto.req.user.UserRegisterReqDTO;
import com.hewei.hzyjy.xunzhi.dto.req.user.UserUpdateReqDTO;
import com.hewei.hzyjy.xunzhi.dto.resp.user.UserActualRespDTO;
import com.hewei.hzyjy.xunzhi.dto.resp.user.UserLoginRespDTO;
import com.hewei.hzyjy.xunzhi.dto.resp.user.UserRespDTO;
import com.hewei.hzyjy.xunzhi.dto.req.admin.AdminUserReqDTO;
import com.hewei.hzyjy.xunzhi.service.AdminPermissionService;
import com.hewei.hzyjy.xunzhi.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

/**
 * 用户管理控制层
 */
@RestController
@RequestMapping("/api/xunzhi/v1/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;
    private final AdminPermissionService adminPermissionService;

    /**
     * 根据用户名查询用户信息
     */
    @GetMapping("/{username}")
    public Result<UserRespDTO> getUserByUsername(@PathVariable("username") String username) {
        return Results.success(userService.getUserByUsername(username));
    }

    /**
     * 根据用户名查询无脱敏用户信息
     */
    @GetMapping("/actual/{username}")
    public Result<UserActualRespDTO> getActualUserByUsername(@PathVariable("username") String username) {
        return Results.success(BeanUtil.toBean(userService.getUserByUsername(username), UserActualRespDTO.class));
    }

    /**
     * 查询用户名是否存在
     */
    @GetMapping("/has-username")
    public Result<Boolean> hasUsername(@RequestParam("username") String username) {
        return Results.success(userService.hasUsername(username));
    }

    /**
     * 注册用户
     */
    @PostMapping("/register")
    public Result<Void> register(@RequestBody UserRegisterReqDTO requestParam) {
        userService.register(requestParam);
        return Results.success();
    }

    /**
     * 修改用户
     */
    @PutMapping
    public Result<Void> update(@RequestBody UserUpdateReqDTO requestParam) {
        userService.update(requestParam);
        return Results.success();
    }

    /**
     * 用户登录
     */
    @PostMapping("/login")
    public Result<Map<String, Object>> login(@RequestBody UserLoginReqDTO requestParam) {
        // 验证用户登录
        UserLoginRespDTO loginResult = userService.login(requestParam);
        
        // 使用Sa-Token进行登录
        StpUtil.login(requestParam.getUsername());
        
        // 返回token和用户信息
        Map<String, Object> result = new HashMap<>();
        result.put("token", StpUtil.getTokenValue());
        result.put("username", requestParam.getUsername());
        result.put("isAdmin", adminPermissionService.isAdmin(requestParam.getUsername()));
        
        return Results.success(result);
    }

    /**
     * 检查用户是否登录
     */
    @GetMapping("/check-login")
    public Result<Map<String, Object>> checkLogin() {
        Map<String, Object> result = new HashMap<>();
        result.put("isLogin", StpUtil.isLogin());
        if (StpUtil.isLogin()) {
            result.put("username", StpUtil.getLoginId());
            result.put("token", StpUtil.getTokenValue());
        }
        return Results.success(result);
    }

    /**
     * 用户退出登录
     */
    @PostMapping("/logout")
    public Result<Void> logout() {
        StpUtil.logout();
        return Results.success();
    }

    /**
     * 校验当前用户是否为管理员
     */
    @GetMapping("/is-admin")
    public Result<Map<String, Object>> isAdmin() {
        Map<String, Object> result = new HashMap<>();
        if (StpUtil.isLogin()) {
            String username = (String) StpUtil.getLoginId();
            result.put("isAdmin", adminPermissionService.isAdmin(username));
            result.put("username", username);
        } else {
            result.put("isAdmin", false);
        }
        return Results.success(result);
    }

    /**
     * 添加管理员 (需要管理员权限)
     */
    @PostMapping("/admin")
    @SaCheckRole("admin")
    public Result<Void> addAdmin(@RequestBody AdminUserReqDTO requestParam) {
        adminPermissionService.setAdminByUserId(requestParam.getUserId());
        return Results.success();
    }

    /**
     * 删除管理员
     */
//    @DeleteMapping("/api/xunzhi-agent/admin/v1/admin/{username}")
//    public Result<Void> deleteAdmin(@PathVariable("username") String username) {
//         adminPermissionService.deleteAdmin(username);
//        return Results.success();
//    }

    /**
     * 修改管理员
     */
//    @PutMapping("/api/xunzhi-agent/admin/v1/admin")
//    public Result<Void> updateAdmin(@RequestBody /*AdminUserDTO*/ Object requestParam) { // Placeholder for AdminUserDTO
//         adminPermissionService.updateAdmin(requestParam);
//        return Results.success();
//    }

    /**
     * 根据用户名查询管理员信息
     */
//    @GetMapping("/api/xunzhi-agent/admin/v1/admin/{username}")
//    public Result</*AdminUserRespDTO*/ Object> getAdminByUsername(@PathVariable("username") String username) { // Placeholder for AdminUserRespDTO
//         return Results.success(adminPermissionService.getAdminByUsername(username));
//    }

    /**
     * 分页查询所有管理员信息
     */
//    @GetMapping("/api/xunzhi-agent/admin/v1/admins")
//    public Result</*Page<AdminUserRespDTO>*/ Object> listAdmins(/*@RequestBody AdminUserPageReqDTO requestParam*/) { // Placeholders
//         return Results.success(adminPermissionService.listAdmins(requestParam));
//    }
}
