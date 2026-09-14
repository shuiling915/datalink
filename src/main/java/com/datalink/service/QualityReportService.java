package com.datalink.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.datalink.mapper.DlQualityRuleMapper;
import com.datalink.mapper.DlQualityRunMapper;
import com.datalink.model.DlQualityRule;
import com.datalink.model.DlQualityRun;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 数据质量报告服务 — 基于质量规则执行历史生成质量评分和趋势报告
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class QualityReportService {

    private final DlQualityRuleMapper qualityRuleMapper;
    private final DlQualityRunMapper qualityRunMapper;

    /**
     * 计算质量评分（0-100）
     * 评分逻辑：成功率 * 100 * 权重
     */
    public double calculateScore(int days) {
        LocalDateTime since = LocalDateTime.now().minusDays(days);
        List<DlQualityRun> runs = qualityRunMapper.selectList(
                new QueryWrapper<DlQualityRun>().ge("started_at", since));
        if (runs.isEmpty()) return 100.0;

        long success = runs.stream()
                .filter(r -> "success".equals(r.getRunStatus()))
                .count();
        return (double) success / runs.size() * 100;
    }

    /**
     * 按规则统计质量情况
     */
    public List<Map<String, Object>> getRuleQualityStats(int days) {
        LocalDateTime since = LocalDateTime.now().minusDays(days);
        List<DlQualityRule> rules = qualityRuleMapper.selectList(null);
        List<Map<String, Object>> result = new ArrayList<>();

        for (DlQualityRule rule : rules) {
            List<DlQualityRun> runs = qualityRunMapper.selectList(
                    new QueryWrapper<DlQualityRun>()
                            .eq("rule_id", rule.getId())
                            .ge("started_at", since));

            long total = runs.size();
            long success = runs.stream().filter(r -> "success".equals(r.getRunStatus())).count();
            long failed = runs.stream().filter(r -> "failed".equals(r.getRunStatus())).count();

            Map<String, Object> item = new LinkedHashMap<>();
            item.put("ruleId", rule.getId());
            item.put("ruleName", rule.getRuleName());
            item.put("ruleType", rule.getRuleType());
            item.put("tableName", rule.getTableName());
            item.put("totalRuns", total);
            item.put("successCount", success);
            item.put("failedCount", failed);
            item.put("successRate", total > 0 ? (double) success / total : 0);
            item.put("status", failed > 0 ? "WARNING" : "HEALTHY");
            result.add(item);
        }
        return result;
    }

    /**
     * 按表统计质量情况
     */
    public List<Map<String, Object>> getTableQualityStats(int days) {
        List<Map<String, Object>> ruleStats = getRuleQualityStats(days);
        Map<String, List<Map<String, Object>>> byTable = ruleStats.stream()
                .collect(Collectors.groupingBy(s -> (String) s.get("tableName")));

        List<Map<String, Object>> result = new ArrayList<>();
        for (Map.Entry<String, List<Map<String, Object>>> entry : byTable.entrySet()) {
            List<Map<String, Object>> tableRules = entry.getValue();
            long totalRuns = tableRules.stream().mapToLong(s -> (Long) s.get("totalRuns")).sum();
            long successCount = tableRules.stream().mapToLong(s -> (Long) s.get("successCount")).sum();
            long failedCount = tableRules.stream().mapToLong(s -> (Long) s.get("failedCount")).sum();

            Map<String, Object> item = new LinkedHashMap<>();
            item.put("tableName", entry.getKey());
            item.put("ruleCount", tableRules.size());
            item.put("totalRuns", totalRuns);
            item.put("successCount", successCount);
            item.put("failedCount", failedCount);
            item.put("successRate", totalRuns > 0 ? (double) successCount / totalRuns : 0);
            item.put("qualityLevel", calculateQualityLevel(totalRuns > 0 ? (double) successCount / totalRuns : 1));
            result.add(item);
        }
        result.sort((a, b) -> Double.compare((Double) b.get("successRate"), (Double) a.get("successRate")));
        return result;
    }

    private String calculateQualityLevel(double successRate) {
        if (successRate >= 0.99) return "EXCELLENT";
        if (successRate >= 0.95) return "GOOD";
        if (successRate >= 0.90) return "FAIR";
        if (successRate >= 0.80) return "POOR";
        return "CRITICAL";
    }

    /**
     * 按规则类型统计问题分布
     */
    public Map<String, Long> getIssueDistribution(int days) {
        LocalDateTime since = LocalDateTime.now().minusDays(days);
        List<DlQualityRun> failedRuns = qualityRunMapper.selectList(
                new QueryWrapper<DlQualityRun>()
                        .eq("run_status", "failed")
                        .ge("started_at", since));

        Map<String, Long> distribution = new LinkedHashMap<>();
        for (DlQualityRun run : failedRuns) {
            DlQualityRule rule = qualityRuleMapper.selectById(run.getRuleId());
            if (rule != null) {
                distribution.merge(rule.getRuleType(), 1L, Long::sum);
            }
        }
        return distribution;
    }

    /**
     * 生成完整质量报告
     */
    public Map<String, Object> generateReport(int days) {
        Map<String, Object> report = new LinkedHashMap<>();

        report.put("periodDays", days);
        report.put("generatedAt", LocalDateTime.now().toString());

        // 总体评分
        double score = calculateScore(days);
        report.put("overallScore", Math.round(score * 100) / 100.0);
        report.put("qualityLevel", calculateQualityLevel(score / 100));

        // 规则统计
        List<Map<String, Object>> ruleStats = getRuleQualityStats(days);
        report.put("ruleStats", ruleStats);
        report.put("totalRules", ruleStats.size());
        report.put("healthyRules", ruleStats.stream().filter(s -> "HEALTHY".equals(s.get("status"))).count());
        report.put("warningRules", ruleStats.stream().filter(s -> "WARNING".equals(s.get("status"))).count());

        // 表质量统计
        report.put("tableStats", getTableQualityStats(days));

        // 问题分布
        report.put("issueDistribution", getIssueDistribution(days));

        return report;
    }
}