package com.datalink.model;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("dl_fact_field")
public class DlFactField {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long factId;
    private String fieldName;
    private String fieldType;
    private String fieldComment;
    private String fieldCategory;
    private Long relatedDimId;
    private Integer isPrimary;
    private Integer isPartition;
    private Integer sortOrder;
    private LocalDateTime createdAt;
}