package com.datalink.model;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("dl_search_history")
public class DlSearchHistory {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String databaseName;
    private String tableName;
    private String createdBy;
    private LocalDateTime searchedAt;
}
