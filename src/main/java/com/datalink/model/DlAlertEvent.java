package com.datalink.model;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("dl_alert_event")
public class DlAlertEvent {
    private Long id;
    private String sourceType;
    private Long sourceId;
    private String severity;
    private String title;
    private String content;
    private String status;
    private String assignee;
    private String acknowledgedBy;
    private LocalDateTime acknowledgedAt;
    private String resolvedBy;
    private LocalDateTime resolvedAt;
    private String resolveNote;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}