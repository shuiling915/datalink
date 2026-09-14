package com.datalink.service;

import com.datalink.mapper.DlAuditLogMapper;
import com.datalink.model.DlAuditLog;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

/**
 * 操作审计日志服务 — 异步记录用户操作
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AuditLogService {

    private final DlAuditLogMapper auditLogMapper;

    @Async
    public void record(DlAuditLog auditLog) {
        try {
            auditLogMapper.insert(auditLog);
        } catch (Exception e) {
            log.error("记录审计日志失败", e);
        }
    }
}