package com.datalink.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.datalink.mapper.DlQualityRuleMapper;
import com.datalink.model.DlQualityRule;
import com.datalink.model.DlQualityRun;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.scheduling.support.CronExpression;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 数据质量规则定时调度服务
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class QualityScheduleService {

    private final DlQualityRuleMapper qualityRuleMapper;
    private final QualityService qualityService;
    private final AlertNotifyService alertNotifyService;

    // 记录每个规则上次执行时间，避免一分钟内重复执行
    private final Map<Long, LocalDateTime> lastRunMap = new ConcurrentHashMap<>();

    /**
     * 每分钟扫描一次需要调度的质量规则
     */
    @Scheduled(cron = "0 * * * * ?")
    public void scheduleQualityRules() {
        List<DlQualityRule> rules = qualityRuleMapper.selectList(
                new QueryWrapper<DlQualityRule>()
                        .eq("status", 1)
                        .isNotNull("schedule_cron")
                        .ne("schedule_cron", ""));

        if (rules.isEmpty()) return;

        LocalDateTime now = LocalDateTime.now();
        for (DlQualityRule rule : rules) {
            try {
                if (!shouldRun(rule, now)) continue;

                log.info("定时执行质量规则: id={}, name={}", rule.getId(), rule.getRuleName());
                DlQualityRun run = qualityService.executeRule(rule);
                lastRunMap.put(rule.getId(), now);

                // 检查失败并发送告警
                if ("failed".equals(run.getRunStatus())) {
                    sendAlert(rule, run);
                }
            } catch (Exception e) {
                log.error("质量规则调度执行失败: ruleId={}", rule.getId(), e);
            }
        }
    }

    /**
     * 判断规则是否应该在当前时间执行
     */
    private boolean shouldRun(DlQualityRule rule, LocalDateTime now) {
        String cron = rule.getScheduleCron();
        if (cron == null || cron.trim().isEmpty()) return false;

        try {
            CronExpression expression = CronExpression.parse(cron);
            // 计算当前分钟的起始时间
            LocalDateTime minuteStart = now.withSecond(0).withNano(0);
            // 计算上一分钟之后的下一次执行时间
            LocalDateTime prevMinute = minuteStart.minusMinutes(1);
            LocalDateTime nextRun = expression.next(prevMinute);

            if (nextRun != null && nextRun.equals(minuteStart)) {
                // 检查是否已经执行过
                LocalDateTime lastRun = lastRunMap.get(rule.getId());
                if (lastRun != null && lastRun.isAfter(minuteStart.minusSeconds(30))) {
                    return false;
                }
                return true;
            }
            return false;
        } catch (Exception e) {
            log.warn("解析 cron 表达式失败: ruleId={}, cron={}", rule.getId(), cron, e);
            return false;
        }
    }

    /**
     * 发送质量告警
     */
    private void sendAlert(DlQualityRule rule, DlQualityRun run) {
        try {
            String title = "数据质量告警: " + rule.getRuleName();
            String content = String.format(
                    "规则名称: %s%n规则类型: %s%n数据表: %s.%s%n严重级别: %s%n总行数: %d%n失败行数: %d%n通过率: %.2f%%%n执行时间: %s",
                    rule.getRuleName(),
                    rule.getRuleType(),
                    rule.getDatabaseName(),
                    rule.getTableName(),
                    rule.getSeverity() != null ? rule.getSeverity() : "warning",
                    run.getTotalCount() != null ? run.getTotalCount() : 0,
                    run.getFailCount() != null ? run.getFailCount() : 0,
                    run.getPassRate() != null ? run.getPassRate().doubleValue() : 0,
                    run.getFinishedAt());

            alertNotifyService.sendAlert("quality", rule.getId(),
                    rule.getSeverity() != null ? rule.getSeverity() : "warning", title, content);
            log.info("质量告警已发送: ruleId={}, failCount={}", rule.getId(), run.getFailCount());
        } catch (Exception e) {
            log.error("发送质量告警失败: ruleId={}", rule.getId(), e);
        }
    }
}