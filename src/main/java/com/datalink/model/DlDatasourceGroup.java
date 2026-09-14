package com.datalink.model;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("dl_datasource_group")
public class DlDatasourceGroup {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String groupName;
    private String groupCode;
    private String description;
    private Integer sortOrder;
    private String createdBy;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}