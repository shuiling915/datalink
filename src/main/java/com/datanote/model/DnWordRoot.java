package com.datanote.model;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("dn_word_root")
public class DnWordRoot {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String wordCode;
    private String wordName;
    private String wordType;
    private String description;
    private Integer status;
    private String createdBy;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}