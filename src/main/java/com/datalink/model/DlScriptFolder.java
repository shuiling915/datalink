package com.datalink.model;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

/**
 * 脚本文件夹实体 — 对应 dl_script_folder 表
 */
@Data
@TableName("dl_script_folder")
public class DlScriptFolder {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String folderName;
    private Long parentId;
    private String layer;
    private Integer sortOrder;
    private LocalDateTime createdAt;
}
