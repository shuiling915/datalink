package com.datalink.model;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("dl_asset")
public class DlAsset {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long datasourceId;
    private String tableName;
    private String assetName;
    private String description;
    private Integer assetLevel;
    private String owner;
    private String businessDomain;
    private String tags;
    private Long accessCount;
    private LocalDateTime lastAccessAt;
    private Integer status;
    private String createdBy;
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
}