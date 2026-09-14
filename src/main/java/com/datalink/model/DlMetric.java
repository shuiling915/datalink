package com.datalink.model;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

/**
 * 指标定义实体 — 对应 dl_metric 表
 */
@Data
@TableName("dl_metric")
public class DlMetric {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String metricName;
    private String metricCode;
    private String category;
    private String description;
    private String calcFormula;
    private String dataSource;
    private String dimensions;
    private String unit;
    private String owner;
    private Integer status;
    private String tags;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
