package com.datalink.controller;

import com.datalink.annotation.AuditLog;
import com.datalink.model.DlLifecycleLog;
import com.datalink.model.DlLifecyclePolicy;
import com.datalink.model.R;
import com.datalink.service.LifecycleService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/lifecycle")
@RequiredArgsConstructor
@Tag(name = "数据生命周期管理")
public class LifecycleController {

    private final LifecycleService lifecycleService;

    @Operation(summary = "策略列表")
    @GetMapping("/list")
    public R<List<DlLifecyclePolicy>> list(@RequestParam(required = false) Long datasourceId,
                                           @RequestParam(required = false) String keyword,
                                           @RequestParam(required = false) String policyType) {
        return R.ok(lifecycleService.list(datasourceId, keyword, policyType));
    }

    @Operation(summary = "策略详情")
    @GetMapping("/{id}")
    public R<DlLifecyclePolicy> detail(@PathVariable Long id) {
        return R.ok(lifecycleService.getById(id));
    }

    @Operation(summary = "保存策略")
    @PostMapping("/save")
    @PreAuthorize("hasAuthority('data:governance')")
    @AuditLog(module = "生命周期", operation = "保存策略")
    public R<DlLifecyclePolicy> save(@RequestBody DlLifecyclePolicy policy,
                                     @AuthenticationPrincipal UserDetails user) {
        return R.ok(lifecycleService.save(policy, user != null ? user.getUsername() : "anonymous"));
    }

    @Operation(summary = "删除策略")
    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('data:governance')")
    @AuditLog(module = "生命周期", operation = "删除策略")
    public R<String> delete(@PathVariable Long id) {
        lifecycleService.delete(id);
        return R.ok("删除成功");
    }

    @Operation(summary = "启用/禁用策略")
    @PostMapping("/{id}/toggle")
    @PreAuthorize("hasAuthority('data:governance')")
    public R<DlLifecyclePolicy> toggle(@PathVariable Long id) {
        return R.ok(lifecycleService.toggle(id));
    }

    @Operation(summary = "手动执行策略")
    @PostMapping("/{id}/execute")
    @PreAuthorize("hasAuthority('data:governance')")
    @AuditLog(module = "生命周期", operation = "执行策略")
    public R<DlLifecycleLog> execute(@PathVariable Long id) {
        return R.ok(lifecycleService.execute(id));
    }

    @Operation(summary = "执行日志")
    @GetMapping("/logs")
    public R<List<DlLifecycleLog>> logs(@RequestParam(required = false) Long policyId,
                                        @RequestParam(defaultValue = "20") int limit) {
        return R.ok(lifecycleService.listLogs(policyId, limit));
    }
}