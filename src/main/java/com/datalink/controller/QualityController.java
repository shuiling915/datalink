package com.datalink.controller;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.datalink.exception.ResourceNotFoundException;
import com.datalink.mapper.DlQualityRuleMapper;
import com.datalink.mapper.DlQualityRunMapper;
import com.datalink.model.DlQualityRule;
import com.datalink.model.DlQualityRun;
import com.datalink.model.R;
import com.datalink.service.AlertNotifyService;
import com.datalink.service.QualityService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 数据质量管理 Controller
 */
@RestController
@RequestMapping("/api/quality")
@Tag(name = "数据质量管理", description = "质量规则的增删改查、执行检查、结果查询")
@RequiredArgsConstructor
public class QualityController {

    private final DlQualityRuleMapper ruleMapper;
    private final DlQualityRunMapper runMapper;
    private final QualityService qualityService;
    private final AlertNotifyService alertNotifyService;

    /**
     * 获取所有质量规则
     */
    @Operation(summary = "质量规则列表")
    @GetMapping("/rules")
    public R<List<DlQualityRule>> listRules() {
        QueryWrapper<DlQualityRule> qw = new QueryWrapper<>();
        qw.orderByDesc("updated_at");
        return R.ok(ruleMapper.selectList(qw));
    }

    /**
     * 根据 ID 获取质量规则
     */
    @Operation(summary = "查询质量规则详情")
    @GetMapping("/rule/{id}")
    public R<DlQualityRule> getRule(@PathVariable Long id) {
        DlQualityRule rule = ruleMapper.selectById(id);
        if (rule == null) {
            throw new ResourceNotFoundException("质量规则");
        }
        return R.ok(rule);
    }

    /**
     * 保存质量规则（新增或更新）
     */
    @Operation(summary = "保存质量规则")
    @PostMapping("/rule/save")
    public R<DlQualityRule> saveRule(@RequestBody DlQualityRule rule) {
        if (rule.getId() != null) {
            rule.setUpdatedAt(LocalDateTime.now());
            ruleMapper.updateById(rule);
        } else {
            rule.setCreatedAt(LocalDateTime.now());
            rule.setUpdatedAt(LocalDateTime.now());
            if (rule.getStatus() == null) {
                rule.setStatus(1);
            }
            ruleMapper.insert(rule);
        }
        return R.ok(rule);
    }

    /**
     * 删除质量规则
     */
    @Operation(summary = "删除质量规则")
    @DeleteMapping("/rule/{id}")
    public R<String> deleteRule(@PathVariable Long id) {
        ruleMapper.deleteById(id);
        return R.ok("删除成功");
    }

    /**
     * 手动执行质量检查
     */
    @Operation(summary = "执行质量检查")
    @PostMapping("/rule/{id}/run")
    public R<DlQualityRun> runRule(@PathVariable Long id) {
        DlQualityRule rule = ruleMapper.selectById(id);
        if (rule == null) {
            throw new ResourceNotFoundException("质量规则");
        }
        DlQualityRun result = qualityService.executeRule(rule);
        if ("failed".equals(result.getRunStatus())) {
            String title = "数据质量告警: " + rule.getRuleName();
            String content = String.format(
                    "规则名称: %s%n规则类型: %s%n数据表: %s.%s%n严重级别: %s%n总行数: %d%n失败行数: %d%n通过率: %.2f%%",
                    rule.getRuleName(), rule.getRuleType(), rule.getDatabaseName(), rule.getTableName(),
                    rule.getSeverity() != null ? rule.getSeverity() : "warning",
                    result.getTotalCount() != null ? result.getTotalCount() : 0,
                    result.getFailCount() != null ? result.getFailCount() : 0,
                    result.getPassRate() != null ? result.getPassRate().doubleValue() : 0);
            alertNotifyService.sendAlert("quality", rule.getId(),
                    rule.getSeverity() != null ? rule.getSeverity() : "warning", title, content);
        }
        return R.ok(result);
    }

    /**
     * 批量执行所有启用的质量规则
     */
    @Transactional(rollbackFor = Exception.class)
    @Operation(summary = "批量执行质量检查")
    @PostMapping("/run-all")
    public R<String> runAll() {
        QueryWrapper<DlQualityRule> qw = new QueryWrapper<>();
        qw.eq("status", 1);
        List<DlQualityRule> rules = ruleMapper.selectList(qw);
        int count = 0;
        for (DlQualityRule rule : rules) {
            qualityService.executeRule(rule);
            count++;
        }
        return R.ok("已执行 " + count + " 条规则");
    }

    /**
     * 获取指定规则的执行历史
     */
    @Operation(summary = "获取规则执行历史")
    @GetMapping("/rule/{id}/runs")
    public R<List<DlQualityRun>> listRuns(@PathVariable Long id) {
        QueryWrapper<DlQualityRun> qw = new QueryWrapper<>();
        qw.eq("rule_id", id).orderByDesc("started_at").last("LIMIT 20");
        return R.ok(runMapper.selectList(qw));
    }

    /**
     * 获取质量概览统计
     */
    @Operation(summary = "质量概览统计")
    @GetMapping("/overview")
    public R<Map<String, Object>> overview() {
        Map<String, Object> data = new HashMap<>();

        long totalRules = ruleMapper.selectCount(null);
        QueryWrapper<DlQualityRule> enabledQw = new QueryWrapper<>();
        enabledQw.eq("status", 1);
        long enabledRules = ruleMapper.selectCount(enabledQw);

        data.put("totalRules", totalRules);
        data.put("enabledRules", enabledRules);

        // 最近24小时的执行统计
        QueryWrapper<DlQualityRun> recentQw = new QueryWrapper<>();
        recentQw.ge("started_at", LocalDateTime.now().minusHours(24));
        List<DlQualityRun> recentRuns = runMapper.selectList(recentQw);

        long totalRuns = recentRuns.size();
        long successRuns = recentRuns.stream().filter(r -> "success".equals(r.getRunStatus())).count();
        long failedRuns = recentRuns.stream().filter(r -> "failed".equals(r.getRunStatus())).count();
        long errorRuns = recentRuns.stream().filter(r -> "error".equals(r.getRunStatus())).count();

        data.put("totalRuns", totalRuns);
        data.put("successRuns", successRuns);
        data.put("failedRuns", failedRuns);
        data.put("errorRuns", errorRuns);

        return R.ok(data);
    }
}