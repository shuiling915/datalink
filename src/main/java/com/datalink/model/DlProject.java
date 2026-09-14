package com.datalink.model;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("dl_project")
public class DlProject {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String projectCode;
    private String projectName;
    private String description;
    private String owner;
    private String members;
    private String status;
    private String resourceQuota;
    private String envConfig;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}