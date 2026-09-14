package com.datalink.model;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

@Data
@TableName("dl_lineage_column")
public class DlLineageColumn {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long lineageId;
    private String sourceColumn;
    private String targetColumn;
    private String transformExpr;
}