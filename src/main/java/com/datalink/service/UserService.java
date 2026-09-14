package com.datalink.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.datalink.mapper.DlRoleMapper;
import com.datalink.mapper.DlUserMapper;
import com.datalink.mapper.DlUserRoleMapper;
import com.datalink.model.DlRole;
import com.datalink.model.DlUser;
import com.datalink.model.DlUserRole;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 用户服务 — 用户增删改查、登录校验、密码加密
 */
@Service
@RequiredArgsConstructor
public class UserService {

    private final DlUserMapper userMapper;
    private final DlRoleMapper roleMapper;
    private final DlUserRoleMapper userRoleMapper;
    private final PasswordEncoder passwordEncoder;

    /**
     * 启动时若用户表为空，创建默认管理员 admin/admin123
     */
    @PostConstruct
    public void initDefaultAdmin() {
        Long count = userMapper.selectCount(null);
        if (count == null || count == 0) {
            DlUser admin = new DlUser();
            admin.setUsername("admin");
            admin.setPassword(passwordEncoder.encode("admin123"));
            admin.setNickname("超级管理员");
            admin.setRole("ADMIN");
            admin.setStatus(1);
            admin.setCreatedAt(LocalDateTime.now());
            userMapper.insert(admin);

            // 关联 ADMIN 角色
            assignRoleByCode(admin.getId(), "ADMIN");
        }
    }

    /** 用户列表（密码脱敏） */
    public List<DlUser> list() {
        List<DlUser> users = userMapper.selectList(null);
        users.forEach(u -> u.setPassword(null));
        return users;
    }

    /** 按用户名查（含密码，供登录用） */
    public DlUser findByUsername(String username) {
        return userMapper.selectOne(new QueryWrapper<DlUser>().eq("username", username));
    }

    /**
     * 登录校验：成功返回脱敏用户，失败返回 null
     */
    public DlUser login(String username, String password) {
        DlUser u = findByUsername(username);
        if (u == null || u.getStatus() == null || u.getStatus() != 1) {
            return null;
        }
        if (!passwordEncoder.matches(password, u.getPassword())) {
            return null;
        }
        u.setPassword(null);
        return u;
    }

    /** 新建用户 */
    public DlUser create(String username, String password, String nickname, String role) {
        if (username == null || username.trim().isEmpty()) {
            throw new IllegalArgumentException("用户名不能为空");
        }
        if (findByUsername(username) != null) {
            throw new IllegalArgumentException("用户名已存在");
        }
        String roleCode = "ADMIN".equals(role) ? "ADMIN" : "USER";
        DlUser u = new DlUser();
        u.setUsername(username.trim());
        u.setPassword(passwordEncoder.encode(isBlank(password) ? "123456" : password));
        u.setNickname(nickname);
        u.setRole(roleCode);
        u.setStatus(1);
        u.setCreatedAt(LocalDateTime.now());
        userMapper.insert(u);

        // 关联角色表（开发者角色）
        assignRoleByCode(u.getId(), "ADMIN".equals(roleCode) ? "ADMIN" : "DEVELOPER");

        u.setPassword(null);
        return u;
    }

    /** 编辑用户（昵称/角色/状态） */
    public void update(Long id, String nickname, String role, Integer status) {
        DlUser u = userMapper.selectById(id);
        if (u == null) {
            throw new IllegalArgumentException("用户不存在");
        }
        if (nickname != null) u.setNickname(nickname);
        if (role != null) {
            String roleCode = "ADMIN".equals(role) ? "ADMIN" : "USER";
            u.setRole(roleCode);
            // 同步角色关联
            userRoleMapper.delete(new QueryWrapper<DlUserRole>().eq("user_id", id));
            assignRoleByCode(id, "ADMIN".equals(roleCode) ? "ADMIN" : "DEVELOPER");
        }
        if (status != null) u.setStatus(status);
        userMapper.updateById(u);
    }

    /** 重置密码 */
    public void resetPassword(Long id, String newPassword) {
        DlUser u = userMapper.selectById(id);
        if (u == null) {
            throw new IllegalArgumentException("用户不存在");
        }
        u.setPassword(passwordEncoder.encode(isBlank(newPassword) ? "123456" : newPassword));
        userMapper.updateById(u);
    }

    /** 删除用户（默认 admin 不可删） */
    public void delete(Long id) {
        DlUser u = userMapper.selectById(id);
        if (u == null) {
            return;
        }
        if ("admin".equals(u.getUsername())) {
            throw new IllegalArgumentException("默认管理员不可删除");
        }
        userRoleMapper.delete(new QueryWrapper<DlUserRole>().eq("user_id", id));
        userMapper.deleteById(id);
    }

    /** 按角色编码分配角色 */
    private void assignRoleByCode(Long userId, String roleCode) {
        try {
            DlRole role = roleMapper.selectOne(
                    new QueryWrapper<DlRole>().eq("role_code", roleCode));
            if (role != null) {
                DlUserRole ur = new DlUserRole();
                ur.setUserId(userId);
                ur.setRoleId(role.getId());
                ur.setCreatedAt(LocalDateTime.now());
                userRoleMapper.insert(ur);
            }
        } catch (Exception e) {
            // 角色表可能尚未初始化，忽略
        }
    }

    private boolean isBlank(String s) {
        return s == null || s.trim().isEmpty();
    }
}