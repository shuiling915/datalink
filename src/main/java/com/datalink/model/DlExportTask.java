package com.datalink.model;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("dl_export_task")
public class DlExportTask {
    private Long id;
    private String taskName;
    private Long datasourceId;
    private String databaseName;
    private String sqlText;
    private String fileName;
    private String filePath;
    private Long fileSize;
    private Long rowCount;
    private String status;
    private String errorMsg;
    private String createdBy;
    private LocalDateTime createdAt;
    private LocalDateTime startedAt;
    private LocalDateTime finishedAt;
    private LocalDateTime expiredAt;
}