package com.datalink.model;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 分组实体 — 对应 dl_group 表
 */
@Data
@TableName("dl_group")
public class DlGroup {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String groupName;
    private String description;
    private String adminUser;
    private LocalDateTime createdAt;
}
