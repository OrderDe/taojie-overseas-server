package com.zbkj.overseas.dao;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.zbkj.overseas.model.OverseasChatMessage;
import org.apache.ibatis.annotations.Mapper;

/**
 * Mapper for OverseasChatMessage — MyBatis-Plus BaseMapper, same DAO style as tjMall-service.
 */
@Mapper
public interface OverseasChatMessageDao extends BaseMapper<OverseasChatMessage> {
}
