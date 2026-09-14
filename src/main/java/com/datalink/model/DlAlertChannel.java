package com.datalink.model;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("dl_alert_channel")
public class DlAlertChannel {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String channelName;
    private String channelType;
    private String config;
    private Integer enabled;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}