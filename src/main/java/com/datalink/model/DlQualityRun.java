package com.datalink.model;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 数据质量检查执行记录实体 — 对应 dl_quality_run 表
 */
@Data
@TableName("dl_quality_run")
public class DlQualityRun {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long ruleId;
    private String runStatus;
    private Long totalCount;
    private Long passCount;
    private Long failCount;
    private BigDecimal passRate;
    private String errorSample;
    private String execSql;
    private Long durationMs;
    private String errorMsg;
    private LocalDateTime startedAt;
    private LocalDateTime finishedAt;
}
