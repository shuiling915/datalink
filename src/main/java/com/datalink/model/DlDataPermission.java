package com.datalink.model;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

/**
 * 数据权限实体（行级/列级）
 */
@Data
@TableName("dl_data_permission")
public class DlDataPermission {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long roleId;
    private Long datasourceId;
    private String tableName;
    private String columnName;
    private String permissionType;
    private String ruleValue;
    private Integer enabled;
    private String description;
    private String createdBy;
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
}