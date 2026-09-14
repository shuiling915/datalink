package com.datalink.controller;

import com.datalink.model.R;
import com.datalink.service.DashboardService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.*;

/**
 * 平台监控大盘 Controller
 */
@Slf4j
@RestController
@RequestMapping("/api/dashboard")
@RequiredArgsConstructor
@Tag(name = "监控大盘", description = "平台运行状态概览")
public class DashboardController {

    private final DashboardService dashboardService;

    @Operation(summary = "平台整体概览")
    @GetMapping("/overview")
    public R<Map<String, Object>> overview() {
        return R.ok(dashboardService.getOverview());
    }

    @Operation(summary = "工作台首页统计")
    @GetMapping("/stats")
    public R<Map<String, Object>> stats() {
        return R.ok(dashboardService.getStats());
    }

    @Operation(summary = "服务健康状态列表")
    @GetMapping("/services")
    public R<List<Map<String, Object>>> services() {
        return R.ok(dashboardService.getServices());
    }

    @Operation(summary = "数据源连接状态")
    @GetMapping("/datasource-status")
    public R<List<Map<String, Object>>> datasourceStatus() {
        return R.ok(dashboardService.getDatasourceStatus());
    }

    @Operation(summary = "数据质量趋势")
    @GetMapping("/quality-trend")
    public R<Map<String, Object>> qualityTrend(@RequestParam(defaultValue = "7") int days) {
        return R.ok(dashboardService.getQualityTrend(days));
    }

    @Operation(summary = "API调用趋势")
    @GetMapping("/api-trend")
    public R<Map<String, Object>> apiTrend(@RequestParam(defaultValue = "7") int days) {
        return R.ok(dashboardService.getApiTrend(days));
    }

    @Operation(summary = "近期操作审计日志")
    @GetMapping("/recent-audit-logs")
    public R<List<Map<String, Object>>> recentAuditLogs(@RequestParam(defaultValue = "10") int limit) {
        return R.ok(dashboardService.getRecentAuditLogs(limit));
    }

    @Operation(summary = "生命周期执行情况")
    @GetMapping("/lifecycle-status")
    public R<Map<String, Object>> lifecycleStatus() {
        return R.ok(dashboardService.getLifecycleStatus());
    }

    @Operation(summary = "完整监控大盘")
    @GetMapping("/full")
    @PreAuthorize("hasAnyAuthority('admin:view','dashboard:view')")
    public R<Map<String, Object>> fullDashboard() {
        return R.ok(dashboardService.getFullDashboard());
    }
}