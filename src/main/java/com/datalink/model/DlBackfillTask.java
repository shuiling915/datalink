package com.datalink.model;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@TableName("dl_backfill_task")
public class DlBackfillTask {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long taskId;
    private String taskType;
    private String taskName;
    private LocalDate startDate;
    private LocalDate endDate;
    private Integer totalDays;
    private Integer completed;
    private String status;
    private LocalDateTime startedAt;
    private LocalDateTime finishedAt;
    private String createdBy;
}
