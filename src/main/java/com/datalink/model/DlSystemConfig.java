package com.datalink.model;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("dl_system_config")
public class DlSystemConfig {
    @TableId(value = "config_key", type = IdType.INPUT)
    private String configKey;
    private String configValue;
    private String description;
    private LocalDateTime updatedAt;
}
