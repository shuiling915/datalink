package com.datanote.model;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("dn_realtime_task")
public class DnRealtimeTask {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long folderId;
    private String taskName;
    private String taskType;
    private String content;
    private String description;
    private String status;
    private Integer parallelism;
    private Integer checkpointIntervalSec;
    private String checkpointPath;
    private String kafkaBootstrapServers;
    private String sourceTopic;
    private String sinkTable;
    private String properties;
    private String jobId;
    private String applicationId;
    private String lastError;
    private LocalDateTime lastStartTime;
    private LocalDateTime lastStopTime;
    private String createdBy;
    private String updatedBy;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}