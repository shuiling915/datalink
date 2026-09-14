package com.datalink.model;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("dl_sql_history")
public class DlSqlHistory {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long userId;
    private String username;
    private Long datasourceId;
    private String datasourceName;
    private String databaseName;
    private String tableName;
    private String sqlText;
    private String sqlType;
    private String executionStatus;
    private Long durationMs;
    private Long affectedRows;
    private String errorMsg;
    private Integer rowLimit;
    private String clientIp;
    private LocalDateTime createdAt;
}