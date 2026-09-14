package com.datalink.service;

import com.datalink.model.DlLifecyclePolicy;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.scheduling.support.CronExpression;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 生命周期策略定时调度服务
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class LifecycleScheduleService {

    private final LifecycleService lifecycleService;
    private final Map<Long, LocalDateTime> lastRunMap = new ConcurrentHashMap<>();

    /**
     * 每分钟扫描需要执行的生命周期策略
     */
    @Scheduled(cron = "0 * * * * ?")
    public void scheduleLifecycle() {
        List<DlLifecyclePolicy> policies = lifecycleService.listEnabledPolicies();
        if (policies.isEmpty()) return;

        LocalDateTime now = LocalDateTime.now();
        for (DlLifecyclePolicy policy : policies) {
            try {
                if (!shouldRun(policy, now)) continue;
                log.info("定时执行生命周期策略: id={}, name={}", policy.getId(), policy.getPolicyName());
                lifecycleService.doExecute(policy);
                lastRunMap.put(policy.getId(), now);
            } catch (Exception e) {
                log.error("生命周期策略调度失败: policyId={}", policy.getId(), e);
            }
        }
    }

    private boolean shouldRun(DlLifecyclePolicy policy, LocalDateTime now) {
        String cron = policy.getScheduleCron();
        if (cron == null || cron.trim().isEmpty()) return false;

        try {
            CronExpression expression = CronExpression.parse(cron);
            LocalDateTime minuteStart = now.withSecond(0).withNano(0);
            LocalDateTime prevMinute = minuteStart.minusMinutes(1);
            LocalDateTime nextRun = expression.next(prevMinute);

            if (nextRun != null && nextRun.equals(minuteStart)) {
                LocalDateTime lastRun = lastRunMap.get(policy.getId());
                if (lastRun != null && lastRun.isAfter(minuteStart.minusSeconds(30))) {
                    return false;
                }
                return true;
            }
            return false;
        } catch (Exception e) {
            log.warn("解析 cron 失败: policyId={}, cron={}", policy.getId(), cron, e);
            return false;
        }
    }
}