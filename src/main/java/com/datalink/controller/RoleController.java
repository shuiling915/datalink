package com.datalink.controller;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.datalink.annotation.AuditLog;
import com.datalink.mapper.DlPermissionMapper;
import com.datalink.mapper.DlRoleMapper;
import com.datalink.mapper.DlRolePermissionMapper;
import com.datalink.mapper.DlUserRoleMapper;
import com.datalink.model.DlPermission;
import com.datalink.model.DlRole;
import com.datalink.model.DlRolePermission;
import com.datalink.model.DlUserRole;
import com.datalink.model.R;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 角色与权限管理 Controller
 */
@Tag(name = "角色权限管理")
@RestController
@RequestMapping("/api/role")
@RequiredArgsConstructor
public class RoleController {

    private final DlRoleMapper roleMapper;
    private final DlPermissionMapper permissionMapper;
    private final DlRolePermissionMapper rolePermissionMapper;
    private final DlUserRoleMapper userRoleMapper;

    @Operation(summary = "角色列表")
    @GetMapping("/list")
    public R<List<DlRole>> listRoles() {
        return R.ok(roleMapper.selectList(
                new QueryWrapper<DlRole>().orderByAsc("id")));
    }

    @Operation(summary = "所有权限（按分组）")
    @GetMapping("/permissions")
    public R<List<Map<String, Object>>> listPermissions() {
        List<DlPermission> all = permissionMapper.selectList(
                new QueryWrapper<DlPermission>().orderByAsc("perm_group", "id"));
        Map<String, List<DlPermission>> grouped = all.stream()
                .collect(Collectors.groupingBy(
                        p -> p.getPermGroup() != null ? p.getPermGroup() : "其他",
                        LinkedHashMap::new, Collectors.toList()));
        List<Map<String, Object>> result = new ArrayList<>();
        grouped.forEach((group, perms) -> {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("group", group);
            m.put("permissions", perms);
            result.add(m);
        });
        return R.ok(result);
    }

    @Operation(summary = "查询角色的权限ID列表")
    @GetMapping("/{roleId}/permissions")
    public R<List<Long>> getRolePermissions(@PathVariable Long roleId) {
        List<DlRolePermission> rps = rolePermissionMapper.selectList(
                new QueryWrapper<DlRolePermission>().eq("role_id", roleId));
        return R.ok(rps.stream().map(DlRolePermission::getPermissionId).collect(Collectors.toList()));
    }

    @Operation(summary = "保存角色")
    @PostMapping("/save")
    @PreAuthorize("hasAuthority('role:manage')")
    @AuditLog(module = "系统管理", operation = "保存角色")
    public R<DlRole> saveRole(@RequestBody DlRole role) {
        if (role.getId() != null) {
            role.setUpdatedAt(LocalDateTime.now());
            roleMapper.updateById(role);
        } else {
            role.setCreatedAt(LocalDateTime.now());
            role.setUpdatedAt(LocalDateTime.now());
            if (role.getStatus() == null) role.setStatus(1);
            roleMapper.insert(role);
        }
        return R.ok(role);
    }

    @Operation(summary = "分配角色权限")
    @PostMapping("/{roleId}/permissions")
    @PreAuthorize("hasAuthority('role:manage')")
    @AuditLog(module = "系统管理", operation = "分配角色权限")
    public R<Void> assignPermissions(@PathVariable Long roleId, @RequestBody List<Long> permissionIds) {
        rolePermissionMapper.delete(
                new QueryWrapper<DlRolePermission>().eq("role_id", roleId));
        if (permissionIds != null) {
            for (Long permId : permissionIds) {
                DlRolePermission rp = new DlRolePermission();
                rp.setRoleId(roleId);
                rp.setPermissionId(permId);
                rp.setCreatedAt(LocalDateTime.now());
                rolePermissionMapper.insert(rp);
            }
        }
        return R.ok();
    }

    @Operation(summary = "删除角色")
    @DeleteMapping("/{roleId}")
    @PreAuthorize("hasAuthority('role:manage')")
    @AuditLog(module = "系统管理", operation = "删除角色")
    public R<Void> deleteRole(@PathVariable Long roleId) {
        DlRole role = roleMapper.selectById(roleId);
        if (role != null && "ADMIN".equals(role.getRoleCode())) {
            return R.fail("管理员角色不可删除");
        }
        rolePermissionMapper.delete(
                new QueryWrapper<DlRolePermission>().eq("role_id", roleId));
        userRoleMapper.delete(
                new QueryWrapper<DlUserRole>().eq("role_id", roleId));
        roleMapper.deleteById(roleId);
        return R.ok();
    }

    @Operation(summary = "查询用户的角色")
    @GetMapping("/user/{userId}")
    public R<List<DlRole>> getUserRoles(@PathVariable Long userId) {
        List<DlUserRole> urs = userRoleMapper.selectList(
                new QueryWrapper<DlUserRole>().eq("user_id", userId));
        Set<Long> roleIds = urs.stream().map(DlUserRole::getRoleId).collect(Collectors.toSet());
        if (roleIds.isEmpty()) {
            return R.ok(Collections.emptyList());
        }
        return R.ok(roleMapper.selectList(
                new QueryWrapper<DlRole>().in("id", roleIds)));
    }

    @Operation(summary = "分配用户角色")
    @PostMapping("/user/{userId}/roles")
    @PreAuthorize("hasAuthority('user:manage')")
    @AuditLog(module = "系统管理", operation = "分配用户角色")
    public R<Void> assignUserRoles(@PathVariable Long userId, @RequestBody List<Long> roleIds) {
        userRoleMapper.delete(
                new QueryWrapper<DlUserRole>().eq("user_id", userId));
        if (roleIds != null) {
            for (Long roleId : roleIds) {
                DlUserRole ur = new DlUserRole();
                ur.setUserId(userId);
                ur.setRoleId(roleId);
                ur.setCreatedAt(LocalDateTime.now());
                userRoleMapper.insert(ur);
            }
        }
        return R.ok();
    }
}