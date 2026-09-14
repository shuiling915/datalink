package com.datalink.model;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

/**
 * 基线任务关联实体 — 对应 dl_baseline_task 表
 */
@Data
@TableName("dl_baseline_task")
public class DlBaselineTask {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long baselineId;
    private Long taskId;
    private String taskType;
    private String taskName;
    private LocalDateTime createdAt;
}
