package com.datalink.model;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("dl_lifecycle_log")
public class DlLifecycleLog {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long policyId;
    private String runStatus;
    private Long affectedRows;
    private String execSql;
    private String errorMessage;
    private Integer durationMs;
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime startedAt;
    private LocalDateTime finishedAt;
}