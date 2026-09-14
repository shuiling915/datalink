package com.datalink.model;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;
import java.time.LocalTime;

/**
 * 基线实体 — 对应 dl_baseline 表
 */
@Data
@TableName("dl_baseline")
public class DlBaseline {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String baselineName;
    private String description;
    private LocalTime commitTime;
    private Integer priority;
    private String status;
    private String createdBy;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public static final String STATUS_ENABLED = "enabled";
    public static final String STATUS_DISABLED = "disabled";
}
