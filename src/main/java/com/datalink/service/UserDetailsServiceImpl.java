package com.datalink.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.datalink.mapper.DlPermissionMapper;
import com.datalink.mapper.DlRoleMapper;
import com.datalink.mapper.DlRolePermissionMapper;
import com.datalink.mapper.DlUserMapper;
import com.datalink.mapper.DlUserRoleMapper;
import com.datalink.model.DlPermission;
import com.datalink.model.DlRole;
import com.datalink.model.DlRolePermission;
import com.datalink.model.DlUser;
import com.datalink.model.DlUserRole;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 基于数据库的用户认证服务 — 加载用户、角色和权限
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class UserDetailsServiceImpl implements UserDetailsService {

    private final DlUserMapper userMapper;
    private final DlRoleMapper roleMapper;
    private final DlPermissionMapper permissionMapper;
    private final DlUserRoleMapper userRoleMapper;
    private final DlRolePermissionMapper rolePermissionMapper;

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        DlUser user = userMapper.selectOne(
                new QueryWrapper<DlUser>().eq("username", username));
        if (user == null) {
            throw new UsernameNotFoundException("用户不存在: " + username);
        }
        if (user.getStatus() == null || user.getStatus() != 1) {
            throw new UsernameNotFoundException("用户已被禁用: " + username);
        }

        // 加载用户角色
        List<DlUserRole> userRoles = userRoleMapper.selectList(
                new QueryWrapper<DlUserRole>().eq("user_id", user.getId()));
        Set<Long> roleIds = userRoles.stream()
                .map(DlUserRole::getRoleId)
                .collect(Collectors.toSet());

        // 如果没有关联角色，使用 dl_user.role 字段作为兜底
        if (roleIds.isEmpty() && user.getRole() != null) {
            DlRole fallback = roleMapper.selectOne(
                    new QueryWrapper<DlRole>().eq("role_code", user.getRole()));
            if (fallback != null) {
                roleIds.add(fallback.getId());
            }
        }

        // 加载角色权限
        Set<String> authorities = new HashSet<>();
        if (!roleIds.isEmpty()) {
            List<DlRolePermission> rolePerms = rolePermissionMapper.selectList(
                    new QueryWrapper<DlRolePermission>().in("role_id", roleIds));
            Set<Long> permIds = rolePerms.stream()
                    .map(DlRolePermission::getPermissionId)
                    .collect(Collectors.toSet());
            if (!permIds.isEmpty()) {
                List<DlPermission> perms = permissionMapper.selectList(
                        new QueryWrapper<DlPermission>().in("id", permIds));
                for (DlPermission p : perms) {
                    authorities.add(p.getPermCode());
                }
            }
            // 添加角色编码作为权限（ROLE_ 前缀）
            for (Long roleId : roleIds) {
                DlRole role = roleMapper.selectById(roleId);
                if (role != null) {
                    authorities.add("ROLE_" + role.getRoleCode());
                }
            }
        }

        List<SimpleGrantedAuthority> grantedAuthorities = authorities.stream()
                .map(SimpleGrantedAuthority::new)
                .collect(Collectors.toList());

        return new User(user.getUsername(), user.getPassword(),
                true, true, true, true, grantedAuthorities);
    }
}