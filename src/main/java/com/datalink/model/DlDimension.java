package com.datalink.model;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("dl_dimension")
public class DlDimension {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String dimCode;
    private String dimName;
    private Long domainId;
    private String description;
    private String layer;
    private String status;
    private Integer publishVersion;
    private String createdBy;
    private String publishedBy;
    private LocalDateTime publishedAt;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}