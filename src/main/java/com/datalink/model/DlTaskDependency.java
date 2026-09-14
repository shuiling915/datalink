package com.datalink.model;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

/**
 * 任务依赖关系实体 — 对应 dl_task_dependency 表
 */
@Data
@TableName("dl_task_dependency")
public class DlTaskDependency {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long taskId;
    private String taskType;
    private Long upstreamTaskId;
    private String upstreamTaskType;
    private String depTable;
}
