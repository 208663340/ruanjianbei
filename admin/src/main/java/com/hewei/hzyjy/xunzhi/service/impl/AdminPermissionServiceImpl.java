package com.hewei.hzyjy.xunzhi.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.hewei.hzyjy.xunzhi.dao.entity.AdminPermission;
import com.hewei.hzyjy.xunzhi.service.AdminPermissionService;
import com.hewei.hzyjy.xunzhi.dao.mapper.AdminPermissionMapper;
import com.hewei.hzyjy.xunzhi.dao.entity.UserDO;
import com.hewei.hzyjy.xunzhi.dao.mapper.UserMapper;
import com.hewei.hzyjy.xunzhi.common.convention.exception.ServiceException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/**
* @author 20866
* @description 针对表【admin_permission(管理员权限表)】的数据库操作Service实现
* @createDate 2025-06-08 08:55:34
*/
@Service
@RequiredArgsConstructor
public class AdminPermissionServiceImpl extends ServiceImpl<AdminPermissionMapper, AdminPermission>
    implements AdminPermissionService{

    private final UserMapper userMapper;

    @Override
    public Boolean isAdmin(String username) {
        LambdaQueryWrapper<AdminPermission> queryWrapper = Wrappers.lambdaQuery(AdminPermission.class)
                .eq(AdminPermission::getUsername, username)
                .eq(AdminPermission::getDelFlag, 0);
        AdminPermission adminPermission = getOne(queryWrapper);
        return adminPermission != null && adminPermission.getIsAdmin() != null && adminPermission.getIsAdmin() == 1;
    }

    @Override
    public void setAdminByUserId(Long userId) {
        UserDO user = userMapper.selectById(userId);
        if (user == null) {
            throw new ServiceException("用户不存在");
        }

        AdminPermission adminPermission = getOne(Wrappers.lambdaQuery(AdminPermission.class).eq(AdminPermission::getUserId, userId));
        if (adminPermission == null) {
            adminPermission = new AdminPermission();
            adminPermission.setUserId(userId);
            adminPermission.setUsername(user.getUsername());
            adminPermission.setIsAdmin(1); // 1 for admin
            save(adminPermission);
        } else {
            adminPermission.setIsAdmin(1);
            updateById(adminPermission);
        }
    }
}




