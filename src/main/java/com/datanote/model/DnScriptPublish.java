package com.datanote.model;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("dn_script_publish")
public class DnScriptPublish {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long devScriptId;
    private Long prodScriptId;
    private String publishVersion;
    private String publishType;
    private String publishStatus;
    private String devContent;
    private String prodContentBefore;
    private Integer grayscaleEnabled;
    private Integer grayscaleLimit;
    private Long grayscaleDevRows;
    private Long grayscaleProdRows;
    private Long grayscaleDevTime;
    private Long grayscaleProdTime;
    private Integer grayscaleMatch;
    private String grayscaleDetail;
    private String publishedBy;
    private String publishComment;
    private LocalDateTime publishedAt;
    private LocalDateTime createdAt;
}