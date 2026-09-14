package com.datalink.controller;

import com.datalink.annotation.AuditLog;
import com.datalink.model.DlUser;
import com.datalink.model.R;
import com.datalink.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 用户管理控制器 — 用户的增删改查、重置密码
 */
@Tag(name = "用户管理")
@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    @Operation(summary = "用户列表")
    @GetMapping
    @PreAuthorize("hasAuthority('user:manage')")
    public R<List<DlUser>> list() {
        return R.ok(userService.list());
    }

    @Operation(summary = "新建用户")
    @PostMapping
    @PreAuthorize("hasAuthority('user:manage')")
    @AuditLog(module = "系统管理", operation = "新建用户")
    public R<DlUser> create(@RequestBody UserForm f) {
        try {
            return R.ok(userService.create(f.getUsername(), f.getPassword(), f.getNickname(), f.getRole()));
        } catch (Exception e) {
            return R.fail(e.getMessage());
        }
    }

    @Operation(summary = "编辑用户")
    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('user:manage')")
    @AuditLog(module = "系统管理", operation = "编辑用户")
    public R<Void> update(@PathVariable Long id, @RequestBody UserForm f) {
        try {
            userService.update(id, f.getNickname(), f.getRole(), f.getStatus());
            return R.ok();
        } catch (Exception e) {
            return R.fail(e.getMessage());
        }
    }

    @Operation(summary = "重置密码")
    @PutMapping("/{id}/password")
    @PreAuthorize("hasAuthority('user:manage')")
    @AuditLog(module = "系统管理", operation = "重置密码")
    public R<Void> resetPassword(@PathVariable Long id, @RequestBody UserForm f) {
        try {
            userService.resetPassword(id, f.getPassword());
            return R.ok();
        } catch (Exception e) {
            return R.fail(e.getMessage());
        }
    }

    @Operation(summary = "删除用户")
    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('user:manage')")
    @AuditLog(module = "系统管理", operation = "删除用户")
    public R<Void> delete(@PathVariable Long id) {
        try {
            userService.delete(id);
            return R.ok();
        } catch (Exception e) {
            return R.fail(e.getMessage());
        }
    }

    @Data
    public static class UserForm {
        private String username;
        private String password;
        private String nickname;
        private String role;
        private Integer status;
    }
}