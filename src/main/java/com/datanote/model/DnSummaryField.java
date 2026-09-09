package com.datanote.model;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("dn_summary_field")
public class DnSummaryField {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long summaryId;
    private String fieldName;
    private String fieldType;
    private String fieldComment;
    private Integer isPrimary;
    private Integer isPartition;
    private Integer sortOrder;
    private LocalDateTime createdAt;
}