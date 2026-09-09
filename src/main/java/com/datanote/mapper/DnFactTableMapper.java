package com.datanote.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.datanote.model.DnFactTable;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface DnFactTableMapper extends BaseMapper<DnFactTable> {
}