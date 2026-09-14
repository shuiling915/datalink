package com.datalink.model;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

/**
 * 数据服务 API 实体
 */
@Data
@TableName("dl_data_api")
public class DlDataApi {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String apiCode;
    private String apiName;
    private String description;
    private Long datasourceId;
    private String sqlTemplate;
    private String method;
    private String responseType;
    private Integer rowLimit;
    private Integer cacheSeconds;
    private String apiKey;
    private Integer status;
    private Long callCount;
    private Integer rateLimit;
    private String createdBy;
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
}