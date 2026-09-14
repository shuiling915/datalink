package com.datalink.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.datalink.mapper.DlAlertEventMapper;
import com.datalink.model.DlAlertEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class AlertCenterService {

    private final DlAlertEventMapper alertEventMapper;

    public DlAlertEvent recordAlert(String sourceType, Long sourceId, String severity,
                                    String title, String content) {
        try {
            DlAlertEvent event = new DlAlertEvent();
            event.setSourceType(sourceType);
            event.setSourceId(sourceId);
            event.setSeverity(severity != null ? severity : "warning");
            event.setTitle(title);
            event.setContent(content);
            event.setStatus("OPEN");
            event.setCreatedAt(LocalDateTime.now());
            alertEventMapper.insert(event);
            return event;
        } catch (Exception e) {
            log.warn("记录告警事件失败: {}", e.getMessage());
            return null;
        }
    }

    public Page<DlAlertEvent> queryEvents(int page, int size, String status, String severity,
                                           String sourceType, String keyword) {
        QueryWrapper<DlAlertEvent> qw = new QueryWrapper<>();
        if (status != null && !status.isEmpty()) qw.eq("status", status);
        if (severity != null && !severity.isEmpty()) qw.eq("severity", severity);
        if (sourceType != null && !sourceType.isEmpty()) qw.eq("source_type", sourceType);
        if (keyword != null && !keyword.isEmpty()) {
            qw.and(w -> w.like("title", keyword).or().like("content", keyword));
        }
        qw.orderByDesc("created_at");
        return alertEventMapper.selectPage(new Page<>(page, size), qw);
    }

    public DlAlertEvent acknowledge(Long id, String operator) {
        DlAlertEvent event = alertEventMapper.selectById(id);
        if (event == null) return null;
        event.setStatus("ACKNOWLEDGED");
        event.setAcknowledgedBy(operator);
        event.setAcknowledgedAt(LocalDateTime.now());
        event.setUpdatedAt(LocalDateTime.now());
        alertEventMapper.updateById(event);
        return event;
    }

    public DlAlertEvent resolve(Long id, String operator, String note) {
        DlAlertEvent event = alertEventMapper.selectById(id);
        if (event == null) return null;
        event.setStatus("RESOLVED");
        event.setResolvedBy(operator);
        event.setResolvedAt(LocalDateTime.now());
        event.setResolveNote(note);
        event.setUpdatedAt(LocalDateTime.now());
        alertEventMapper.updateById(event);
        return event;
    }

    public boolean assign(Long id, String assignee) {
        DlAlertEvent event = alertEventMapper.selectById(id);
        if (event == null) return false;
        event.setAssignee(assignee);
        event.setUpdatedAt(LocalDateTime.now());
        alertEventMapper.updateById(event);
        return true;
    }

    public Map<String, Object> getStatistics(int days) {
        Map<String, Object> stats = new LinkedHashMap<>();

        LocalDateTime since = LocalDateTime.now().minusDays(days);
        QueryWrapper<DlAlertEvent> qw = new QueryWrapper<>();
        qw.ge("created_at", since);
        List<DlAlertEvent> events = alertEventMapper.selectList(qw);

        stats.put("periodDays", days);
        stats.put("totalAlerts", events.size());

        long openCount = events.stream().filter(e -> "OPEN".equals(e.getStatus())).count();
        long ackCount = events.stream().filter(e -> "ACKNOWLEDGED".equals(e.getStatus())).count();
        long resolvedCount = events.stream().filter(e -> "RESOLVED".equals(e.getStatus())).count();
        stats.put("openCount", openCount);
        stats.put("acknowledgedCount", ackCount);
        stats.put("resolvedCount", resolvedCount);

        long critical = events.stream().filter(e -> "critical".equals(e.getSeverity())).count();
        long warning = events.stream().filter(e -> "warning".equals(e.getSeverity())).count();
        long info = events.stream().filter(e -> "info".equals(e.getSeverity())).count();
        stats.put("criticalCount", critical);
        stats.put("warningCount", warning);
        stats.put("infoCount", info);

        Map<String, Long> sourceDist = new LinkedHashMap<>();
        for (DlAlertEvent e : events) {
            sourceDist.merge(e.getSourceType(), 1L, Long::sum);
        }
        stats.put("sourceDistribution", sourceDist);

        Map<String, Long> dailyTrend = new LinkedHashMap<>();
        for (DlAlertEvent e : events) {
            String day = e.getCreatedAt().toLocalDate().toString();
            dailyTrend.merge(day, 1L, Long::sum);
        }
        stats.put("dailyTrend", dailyTrend);

        double resolveRate = events.isEmpty() ? 0 : (double) resolvedCount / events.size() * 100;
        stats.put("resolveRate", Math.round(resolveRate * 100) / 100.0);

        return stats;
    }
}