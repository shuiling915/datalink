package com.datalink.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.datalink.mapper.*;
import com.datalink.model.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.sql.Connection;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;

/**
 * 平台监控大盘服务 — 汇总平台各模块运行状态
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DashboardService {

    private final DlDatasourceMapper datasourceMapper;
    private final DlScriptMapper scriptMapper;
    private final DlSyncTaskMapper syncTaskMapper;
    private final DlSchedulerRunMapper schedulerRunMapper;
    private final DlTaskExecutionMapper taskExecutionMapper;
    private final DlQualityRuleMapper qualityRuleMapper;
    private final DlQualityRunMapper qualityRunMapper;
    private final DlDataApiMapper dataApiMapper;
    private final DlDataApiLogMapper dataApiLogMapper;
    private final DlUserMapper userMapper;
    private final DlAuditLogMapper auditLogMapper;
    private final DlAssetMapper assetMapper;
    private final DlLifecyclePolicyMapper lifecyclePolicyMapper;
    private final DlLifecycleLogMapper lifecycleLogMapper;
    private final DatasourceConnectionFactory connectionFactory;

    /**
     * 平台整体概览
     */
    public Map<String, Object> getOverview() {
        Map<String, Object> result = new LinkedHashMap<>();

        // 数据源统计
        Long datasourceCount = datasourceMapper.selectCount(null);
        result.put("datasourceCount", datasourceCount);

        // 脚本任务统计
        Long scriptCount = scriptMapper.selectCount(null);
        result.put("scriptCount", scriptCount);

        // 调度任务执行统计
        Long taskExecutionCount = taskExecutionMapper.selectCount(null);
        result.put("taskExecutionCount", taskExecutionCount);

        // 质量规则统计
        Long qualityRuleCount = qualityRuleMapper.selectCount(null);
        result.put("qualityRuleCount", qualityRuleCount);

        // 数据API统计
        Long apiCount = dataApiMapper.selectCount(null);
        result.put("apiCount", apiCount);

        // 用户统计
        Long userCount = userMapper.selectCount(null);
        result.put("userCount", userCount);

        // 资产统计
        Long assetCount = assetMapper.selectCount(null);
        result.put("assetCount", assetCount);

        // 生命周期策略
        Long lifecycleCount = lifecyclePolicyMapper.selectCount(null);
        result.put("lifecycleCount", lifecycleCount);

        return result;
    }

    /**
     * 工作台首页统计数据
     */
    public Map<String, Object> getStats() {
        Map<String, Object> result = new LinkedHashMap<>();

        Long scriptCount = scriptMapper.selectCount(null);
        result.put("scriptCount", scriptCount);

        Long syncTaskCount = syncTaskMapper.selectCount(null);
        result.put("syncTaskCount", syncTaskCount);

        Long onlineCount = scriptMapper.selectCount(
                new QueryWrapper<com.datalink.model.DlScript>().eq("schedule_status", "online"));
        result.put("onlineCount", onlineCount);

        LocalDate today = LocalDate.now();
        List<DlSchedulerRun> todayRuns = schedulerRunMapper.selectList(
                new QueryWrapper<DlSchedulerRun>().eq("run_date", today));
        result.put("todayExec", (long) todayRuns.size());

        long failedCount = todayRuns.stream()
                .filter(r -> r.getStatus() != null && r.getStatus() == 2)
                .count();
        result.put("failedCount", failedCount);

        return result;
    }

    /**
     * 服务健康状态列表
     */
    public List<Map<String, Object>> getServices() {
        List<Map<String, Object>> services = new ArrayList<>();

        Map<String, Object> dbService = new LinkedHashMap<>();
        dbService.put("name", "元数据库");
        dbService.put("desc", "MySQL 元数据存储");
        boolean dbAlive = false;
        try {
            dbAlive = datasourceMapper.selectCount(null) >= 0;
        } catch (Exception ignored) {}
        dbService.put("alive", dbAlive);
        services.add(dbService);

        List<DlDatasource> dss = datasourceMapper.selectList(null);
        for (DlDatasource ds : dss) {
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("name", ds.getName());
            item.put("desc", ds.getType().toUpperCase() + " @ " + ds.getHost());
            boolean alive = false;
            try (Connection conn = connectionFactory.getConnection(ds)) {
                alive = conn.isValid(3);
            } catch (Exception ignored) {}
            item.put("alive", alive);
            services.add(item);
        }

        return services;
    }

    /**
     * 数据源连接状态检测
     */
    public List<Map<String, Object>> getDatasourceStatus() {
        List<DlDatasource> dss = datasourceMapper.selectList(null);
        List<Map<String, Object>> result = new ArrayList<>();
        for (DlDatasource ds : dss) {
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("id", ds.getId());
            item.put("name", ds.getName());
            item.put("type", ds.getType());
            item.put("host", ds.getHost());
            boolean connected = false;
            String errorMsg = null;
            try (Connection conn = connectionFactory.getConnection(ds)) {
                connected = conn.isValid(3);
            } catch (Exception e) {
                errorMsg = e.getMessage();
            }
            item.put("connected", connected);
            item.put("error", errorMsg);
            result.add(item);
        }
        return result;
    }

    /**
     * 近7天数据质量趋势
     */
    public Map<String, Object> getQualityTrend(int days) {
        LocalDateTime since = LocalDateTime.now().minusDays(days);
        List<DlQualityRun> runs = qualityRunMapper.selectList(
                new QueryWrapper<DlQualityRun>().ge("started_at", since));

        Map<String, Map<String, Long>> byDay = new LinkedHashMap<>();
        long totalSuccess = 0, totalFailed = 0;
        for (DlQualityRun run : runs) {
            String day = run.getStartedAt() != null ? run.getStartedAt().toLocalDate().toString() : "unknown";
            byDay.putIfAbsent(day, new LinkedHashMap<>());
            Map<String, Long> dayMap = byDay.get(day);
            dayMap.merge("total", 1L, Long::sum);
            if ("success".equals(run.getRunStatus())) {
                dayMap.merge("success", 1L, Long::sum);
                totalSuccess++;
            } else if ("failed".equals(run.getRunStatus())) {
                dayMap.merge("failed", 1L, Long::sum);
                totalFailed++;
            } else if ("error".equals(run.getRunStatus())) {
                dayMap.merge("error", 1L, Long::sum);
            }
        }

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("totalRuns", runs.size());
        result.put("successCount", totalSuccess);
        result.put("failedCount", totalFailed);
        result.put("successRate", runs.size() > 0 ? (double) totalSuccess / runs.size() : 0);
        result.put("byDay", byDay);
        return result;
    }

    /**
     * 近7天API调用趋势
     */
    public Map<String, Object> getApiTrend(int days) {
        LocalDateTime since = LocalDateTime.now().minusDays(days);
        List<DlDataApiLog> logs = dataApiLogMapper.selectList(
                new QueryWrapper<DlDataApiLog>().ge("created_at", since));

        Map<String, Map<String, Long>> byDay = new LinkedHashMap<>();
        long totalSuccess = 0, totalFailed = 0;
        long totalDuration = 0;
        for (DlDataApiLog l : logs) {
            String day = l.getCreatedAt() != null ? l.getCreatedAt().toLocalDate().toString() : "unknown";
            byDay.putIfAbsent(day, new LinkedHashMap<>());
            Map<String, Long> dayMap = byDay.get(day);
            dayMap.merge("total", 1L, Long::sum);
            if ("SUCCESS".equals(l.getResponseStatus())) {
                dayMap.merge("success", 1L, Long::sum);
                totalSuccess++;
            } else {
                dayMap.merge("failed", 1L, Long::sum);
                totalFailed++;
            }
            if (l.getDurationMs() != null) totalDuration += l.getDurationMs();
        }

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("totalCalls", logs.size());
        result.put("successCount", totalSuccess);
        result.put("failedCount", totalFailed);
        result.put("successRate", logs.size() > 0 ? (double) totalSuccess / logs.size() : 0);
        result.put("avgDurationMs", logs.size() > 0 ? totalDuration / logs.size() : 0);
        result.put("byDay", byDay);
        return result;
    }

    /**
     * 近期操作审计日志
     */
    public List<Map<String, Object>> getRecentAuditLogs(int limit) {
        List<DlAuditLog> logs = auditLogMapper.selectList(
                new QueryWrapper<DlAuditLog>().orderByDesc("created_at").last("LIMIT " + limit));
        List<Map<String, Object>> result = new ArrayList<>();
        for (DlAuditLog l : logs) {
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("user", l.getUsername());
            item.put("module", l.getModule());
            item.put("operation", l.getOperation());
            item.put("ip", l.getIpAddress());
            item.put("time", l.getCreatedAt());
            result.add(item);
        }
        return result;
    }

    /**
     * 生命周期执行情况
     */
    public Map<String, Object> getLifecycleStatus() {
        Long totalPolicies = lifecyclePolicyMapper.selectCount(null);
        Long enabledPolicies = lifecyclePolicyMapper.selectCount(
                new QueryWrapper<DlLifecyclePolicy>().eq("enabled", 1));
        List<DlLifecycleLog> recentLogs = lifecycleLogMapper.selectList(
                new QueryWrapper<DlLifecycleLog>().orderByDesc("started_at").last("LIMIT 5"));

        long success = 0, failed = 0;
        for (DlLifecycleLog l : recentLogs) {
            if ("SUCCESS".equals(l.getRunStatus())) success++;
            else failed++;
        }

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("totalPolicies", totalPolicies);
        result.put("enabledPolicies", enabledPolicies);
        result.put("recentSuccess", success);
        result.put("recentFailed", failed);
        result.put("recentLogs", recentLogs);
        return result;
    }

    /**
     * 完整监控大盘数据
     */
    public Map<String, Object> getFullDashboard() {
        Map<String, Object> dashboard = new LinkedHashMap<>();
        dashboard.put("overview", getOverview());
        dashboard.put("datasourceStatus", getDatasourceStatus());
        dashboard.put("qualityTrend", getQualityTrend(7));
        dashboard.put("apiTrend", getApiTrend(7));
        dashboard.put("recentAuditLogs", getRecentAuditLogs(10));
        dashboard.put("lifecycleStatus", getLifecycleStatus());
        dashboard.put("generatedAt", LocalDateTime.now().toString());
        return dashboard;
    }
}