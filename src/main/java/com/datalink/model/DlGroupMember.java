package com.datalink.model;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 分组成员实体 — 对应 dl_group_member 表
 */
@Data
@TableName("dl_group_member")
public class DlGroupMember {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long groupId;
    private String username;
    private String role;
    private LocalDateTime createdAt;
}
