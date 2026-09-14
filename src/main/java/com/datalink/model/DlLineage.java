package com.datalink.model;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;
import java.util.List;

@Data
@TableName("dl_lineage")
public class DlLineage {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String sourceDb;
    private String sourceTable;
    private String targetDb;
    private String targetTable;
    private String transformType;
    private String transformSql;
    private Long jobId;
    private String jobName;
    private String frequency;
    private String owner;
    private String description;
    private String createdBy;
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;

    @TableField(exist = false)
    private List<DlLineageColumn> columns;
}