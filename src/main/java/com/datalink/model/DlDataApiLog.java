package com.datalink.model;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

/**
 * 数据 API 调用日志实体
 */
@Data
@TableName("dl_data_api_log")
public class DlDataApiLog {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long apiId;
    private String apiCode;
    private String caller;
    private String requestParams;
    private String responseStatus;
    private Integer rowCount;
    private Integer durationMs;
    private String errorMessage;
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
}