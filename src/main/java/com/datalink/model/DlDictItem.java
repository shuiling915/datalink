package com.datalink.model;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

/**
 * 字典项实体
 */
@Data
@TableName("dl_dict_item")
public class DlDictItem {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String dictCode;
    private String itemLabel;
    private String itemValue;
    private Integer sortOrder;
    private Integer status;
    private String remark;
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
}