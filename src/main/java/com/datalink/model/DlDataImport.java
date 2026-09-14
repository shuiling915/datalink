package com.datalink.model;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

/**
 * 数据导入记录实体
 */
@Data
@TableName("dl_data_import")
public class DlDataImport {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String fileName;
    private Long fileSize;
    private Long datasourceId;
    private String databaseName;
    private String tableName;
    private Integer rowCount;
    private Integer columnCount;
    private String columnsInfo;
    private String status;
    private String errorMessage;
    private String createdBy;
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
}