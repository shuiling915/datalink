package com.datalink.model;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("dl_model_publish_history")
public class DlModelPublishHistory {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String modelType;
    private Long modelId;
    private String modelCode;
    private String modelName;
    private Integer version;
    private String ddlContent;
    private String publishStatus;
    private String publishMsg;
    private String publishedBy;
    private LocalDateTime publishedAt;
}