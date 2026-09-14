package com.datalink.model;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("dl_user_preference")
public class DlUserPreference {
    private Long id;
    private String username;
    private String prefKey;
    private String prefValue;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}