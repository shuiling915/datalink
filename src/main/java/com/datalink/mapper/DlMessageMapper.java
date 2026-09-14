package com.datalink.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.datalink.model.DlMessage;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface DlMessageMapper extends BaseMapper<DlMessage> {
}