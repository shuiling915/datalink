package com.datalink.model;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("dl_asset_tag")
public class DlAssetTag {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String tagName;
    private String tagColor;
    private String category;
    private String createdBy;
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
}