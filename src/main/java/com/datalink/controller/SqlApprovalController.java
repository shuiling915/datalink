package com.datalink.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.datalink.annotation.AuditLog;
import com.datalink.model.DlSqlApproval;
import com.datalink.model.R;
import com.datalink.service.SqlApprovalService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * SQL 审批流程 Controller
 */
@Tag(name = "SQL审批流程")
@RestController
@RequestMapping("/api/sql-approval")
@RequiredArgsConstructor
public class SqlApprovalController {

    private final SqlApprovalService sqlApprovalService;

    @Operation(summary = "检测SQL是否需要审批")
    @PostMapping("/check")
    public R<Map<String, Object>> check(@RequestBody Map<String, String> body) {
        String sql = body.get("sql");
        boolean need = sqlApprovalService.needsApproval(sql);
        return R.ok(Map.of(
                "needsApproval", need,
                "sqlType", sqlApprovalService.detectSqlType(sql != null ? sql : "")
        ));
    }

    @Operation(summary = "提交审批申请")
    @PostMapping("/submit")
    @AuditLog(module = "SQL审批", operation = "提交审批申请")
    public R<DlSqlApproval> submit(@RequestBody Map<String, String> body,
                                   @AuthenticationPrincipal UserDetails user) {
        String title = body.get("title");
        String sql = body.get("sql");
        String applicant = user != null ? user.getUsername() : body.get("applicant");
        String remark = body.get("remark");
        Long dsId = body.get("datasourceId") != null ? Long.parseLong(body.get("datasourceId")) : null;
        String dbName = body.get("databaseName");
        return R.ok(sqlApprovalService.submit(title, sql, dsId, dbName, applicant, remark));
    }

    @Operation(summary = "审批通过")
    @PostMapping("/{id}/approve")
    @PreAuthorize("hasAuthority('sql:approve')")
    @AuditLog(module = "SQL审批", operation = "审批通过")
    public R<DlSqlApproval> approve(@PathVariable Long id,
                                    @RequestBody Map<String, String> body,
                                    @AuthenticationPrincipal UserDetails user) {
        String approver = user != null ? user.getUsername() : "admin";
        String remark = body != null ? body.get("remark") : null;
        return R.ok(sqlApprovalService.approve(id, approver, remark));
    }

    @Operation(summary = "审批驳回")
    @PostMapping("/{id}/reject")
    @PreAuthorize("hasAuthority('sql:approve')")
    @AuditLog(module = "SQL审批", operation = "审批驳回")
    public R<DlSqlApproval> reject(@PathVariable Long id,
                                   @RequestBody Map<String, String> body,
                                   @AuthenticationPrincipal UserDetails user) {
        String approver = user != null ? user.getUsername() : "admin";
        String remark = body != null ? body.get("remark") : null;
        return R.ok(sqlApprovalService.reject(id, approver, remark));
    }

    @Operation(summary = "审批列表")
    @GetMapping("/page")
    public R<IPage<DlSqlApproval>> page(
            @RequestParam(defaultValue = "1") int pageNum,
            @RequestParam(defaultValue = "20") int pageSize,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String applicant) {
        return R.ok(sqlApprovalService.page(pageNum, pageSize, status, applicant));
    }

    @Operation(summary = "审批详情")
    @GetMapping("/{id}")
    public R<DlSqlApproval> detail(@PathVariable Long id) {
        return R.ok(sqlApprovalService.getById(id));
    }
}