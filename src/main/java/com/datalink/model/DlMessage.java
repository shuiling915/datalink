package com.datalink.model;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

/**
 * 站内消息实体
 */
@Data
@TableName("dl_message")
public class DlMessage {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String receiver;
    private String title;
    private String content;
    private String type;
    private Long bizId;
    private Integer isRead;
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
}