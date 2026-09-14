package com.datalink.controller;

import com.datalink.annotation.AuditLog;
import com.datalink.model.DlDataPermission;
import com.datalink.model.R;
import com.datalink.service.DataPermissionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 数据权限管理 Controller — 行级/列级权限规则管理
 */
@Tag(name = "数据权限管理")
@RestController
@RequestMapping("/api/data-permission")
@RequiredArgsConstructor
public class DataPermissionController {

    private final DataPermissionService dataPermissionService;

    @Operation(summary = "权限规则列表")
    @GetMapping("/list")
    public R<List<DlDataPermission>> list(@RequestParam(required = false) Long roleId,
                                          @RequestParam(required = false) Long datasourceId,
                                          @RequestParam(required = false) String tableName) {
        return R.ok(dataPermissionService.list(roleId, datasourceId, tableName));
    }

    @Operation(summary = "保存权限规则")
    @PostMapping("/save")
    @PreAuthorize("hasAuthority('system:config')")
    @AuditLog(module = "数据权限", operation = "保存数据权限规则")
    public R<DlDataPermission> save(@RequestBody DlDataPermission perm,
                                    @AuthenticationPrincipal UserDetails user) {
        return R.ok(dataPermissionService.save(perm, user != null ? user.getUsername() : "anonymous"));
    }

    @Operation(summary = "切换启用状态")
    @PostMapping("/{id}/toggle")
    @PreAuthorize("hasAuthority('system:config')")
    public R<DlDataPermission> toggle(@PathVariable Long id) {
        return R.ok(dataPermissionService.toggle(id));
    }

    @Operation(summary = "删除权限规则")
    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('system:config')")
    @AuditLog(module = "数据权限", operation = "删除数据权限规则")
    public R<String> delete(@PathVariable Long id) {
        dataPermissionService.delete(id);
        return R.ok("删除成功");
    }
}