package com.datalink.model;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@TableName("dl_backfill_instance")
public class DlBackfillInstance {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long backfillId;
    private LocalDate runDate;
    private String status;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private Integer duration;
    private String log;
}
