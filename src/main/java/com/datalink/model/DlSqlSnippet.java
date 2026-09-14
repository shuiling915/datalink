package com.datalink.model;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("dl_sql_snippet")
public class DlSqlSnippet {
    private Long id;
    private String title;
    private String snippetType;
    private String sqlText;
    private String description;
    private String category;
    private String variables;
    private String tags;
    private String createdBy;
    private Integer isPublic;
    private Integer usageCount;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}