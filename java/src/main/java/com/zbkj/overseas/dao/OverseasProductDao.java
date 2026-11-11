package com.zbkj.overseas.dao;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.zbkj.overseas.model.OverseasProduct;
import org.apache.ibatis.annotations.Mapper;

/**
 * Mapper for OverseasProduct — MyBatis-Plus BaseMapper, same DAO style as tjMall-service.
 */
@Mapper
public interface OverseasProductDao extends BaseMapper<OverseasProduct> {
}
