package com.datalink.model;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("dl_sql_approval")
public class DlSqlApproval {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String title;
    private String sqlText;
    private String sqlType;
    private Long datasourceId;
    private String databaseName;
    private String applicant;
    private String applicantRemark;
    private String status;
    private String approver;
    private String approveRemark;
    private LocalDateTime approveTime;
    private String executor;
    private LocalDateTime executeTime;
    private String executeResult;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public static final String STATUS_PENDING = "PENDING";
    public static final String STATUS_APPROVED = "APPROVED";
    public static final String STATUS_REJECTED = "REJECTED";
    public static final String STATUS_EXECUTED = "EXECUTED";
}