package com.datalink.model;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 告警配置实体 — 对应 dl_alert_config 表
 */
@Data
@TableName("dl_alert_config")
public class DlAlertConfig {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long scriptId;
    private String alertTypes;
    private Integer delayThresholdMin;
    private String qualityRuleIds;
    private String alertScope;
    private Long groupId;
    private Integer enabled;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
