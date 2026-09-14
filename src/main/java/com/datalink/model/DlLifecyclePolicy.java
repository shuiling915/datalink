package com.datalink.model;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("dl_lifecycle_policy")
public class DlLifecyclePolicy {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String policyName;
    private Long datasourceId;
    private String databaseName;
    private String tableName;
    private String partitionColumn;
    private String policyType;
    private Integer retentionDays;
    private String archiveTarget;
    private Integer enabled;
    private String scheduleCron;
    private LocalDateTime lastRunAt;
    private String description;
    private String createdBy;
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
}