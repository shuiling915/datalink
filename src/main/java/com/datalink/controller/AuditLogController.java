package com.datalink.controller;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.datalink.mapper.DlAuditLogMapper;
import com.datalink.model.DlAuditLog;
import com.datalink.model.R;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

/**
 * 操作审计日志 Controller
 */
@Tag(name = "审计日志")
@RestController
@RequestMapping("/api/audit")
@RequiredArgsConstructor
public class AuditLogController {

    private final DlAuditLogMapper auditLogMapper;

    @Operation(summary = "审计日志分页查询")
    @GetMapping("/page")
    @PreAuthorize("hasAuthority('system:config')")
    public R<Page<DlAuditLog>> page(
            @RequestParam(defaultValue = "1") int pageNum,
            @RequestParam(defaultValue = "20") int pageSize,
            @RequestParam(required = false) String username,
            @RequestParam(required = false) String module) {

        QueryWrapper<DlAuditLog> qw = new QueryWrapper<>();
        if (username != null && !username.isEmpty()) {
            qw.like("username", username);
        }
        if (module != null && !module.isEmpty()) {
            qw.eq("module", module);
        }
        qw.orderByDesc("created_at");

        Page<DlAuditLog> page = auditLogMapper.selectPage(new Page<>(pageNum, pageSize), qw);
        return R.ok(page);
    }
}