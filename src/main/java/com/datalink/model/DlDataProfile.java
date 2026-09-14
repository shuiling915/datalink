package com.datalink.model;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("dl_data_profile")
public class DlDataProfile {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String dbName;
    private String tableName;
    private String rawStats;
    private String aiReport;
    private LocalDateTime profileTime;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
