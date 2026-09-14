package com.datalink.model;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

/**
 * 表元数据实体 — 对应 dl_table_meta 表
 */
@Data
@TableName("dl_table_meta")
public class DlTableMeta {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long datasourceId;
    private String databaseName;
    private String tableName;
    private String tableComment;
    private String owner;
    private String tags;
    private String importance;
    private Integer viewCount;
    private Long rowCount;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
