package com.datalink.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;

/**
 * 日志广播服务 — 通过 WebSocket 向前端推送实时日志
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class LogBroadcastService {

    private final SimpMessagingTemplate messagingTemplate;
    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("HH:mm:ss");

    /**
     * 广播任务执行日志
     *
     * @param taskId   任务ID
     * @param taskType 任务类型 (script/syncTask)
     * @param level    日志级别 (INFO/WARN/ERROR)
     * @param message  日志内容
     */
    public void broadcastTaskLog(Long taskId, String taskType, String level, String message) {
        Map<String, Object> payload = new HashMap<>();
        payload.put("taskId", taskId);
        payload.put("taskType", taskType);
        payload.put("level", level);
        payload.put("message", message);
        payload.put("time", LocalDateTime.now().format(FMT));
        messagingTemplate.convertAndSend("/topic/task-log", payload);
    }

    /**
     * 广播调度状态变更
     *
     * @param taskId   任务ID
     * @param taskType 任务类型
     * @param status   新状态
     * @param runDate  调度日期
     */
    public void broadcastStatusChange(Long taskId, String taskType, int status, String runDate) {
        Map<String, Object> payload = new HashMap<>();
        payload.put("taskId", taskId);
        payload.put("taskType", taskType);
        payload.put("status", status);
        payload.put("runDate", runDate);
        payload.put("time", LocalDateTime.now().format(FMT));
        messagingTemplate.convertAndSend("/topic/task-status", payload);
    }

    /**
     * 广播系统通知
     *
     * @param level   通知级别
     * @param message 通知内容
     */
    public void broadcastNotification(String level, String message) {
        Map<String, Object> payload = new HashMap<>();
        payload.put("level", level);
        payload.put("message", message);
        payload.put("time", LocalDateTime.now().format(FMT));
        messagingTemplate.convertAndSend("/topic/notification", payload);
    }

    /**
     * 通用广播方法 — 支持实时开发日志等
     */
    public void broadcast(String topic, String level, Long taskId, String taskName, String message) {
        Map<String, Object> payload = new HashMap<>();
        payload.put("level", level);
        payload.put("taskId", taskId);
        payload.put("taskName", taskName);
        payload.put("message", message);
        payload.put("time", LocalDateTime.now().format(FMT));
        String destination = "/topic/" + topic;
        log.info("[WS] 广播 {} -> {}: {}", destination, level, message);
        messagingTemplate.convertAndSend(destination, payload);
    }

    /**
     * 原始数据广播 — 直接发送自定义 payload 到指定 destination
     */
    public void broadcastRaw(String destination, Object payload) {
        log.info("[WS] 广播原始数据 {}", destination);
        messagingTemplate.convertAndSend(destination, payload);
    }
}