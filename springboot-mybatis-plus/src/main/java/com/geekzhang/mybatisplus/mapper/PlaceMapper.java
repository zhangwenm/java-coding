package com.geekzhang.mybatisplus.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.geekzhang.mybatisplus.entity.Place;
import org.apache.ibatis.annotations.Mapper;

/**
 * t_place 场所表 Mapper
 */
@Mapper
public interface PlaceMapper extends BaseMapper<Place> {
}
