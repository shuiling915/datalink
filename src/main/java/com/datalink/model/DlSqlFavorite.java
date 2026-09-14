package com.datalink.model;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("dl_sql_favorite")
public class DlSqlFavorite {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long userId;
    private String username;
    private String title;
    private String sqlText;
    private Long datasourceId;
    private String databaseName;
    private String description;
    private String tags;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}