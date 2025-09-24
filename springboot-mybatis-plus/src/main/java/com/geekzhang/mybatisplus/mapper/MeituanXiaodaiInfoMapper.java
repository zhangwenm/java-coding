package com.geekzhang.mybatisplus.mapper;

/**
 * @author zwm
 * @desc MeituanXiaodaiInfoMapper
 * @date 2025年09月24日 11:09
 */

import com.baomidou.mybatisplus.core.mapper.BaseMapper;

import com.geekzhang.mybatisplus.entity.MeituanXiaodaiInfo;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * 美团小袋注册信息表 Mapper 接口
 *
 * @author system
 * @since 2025-09-24
 */
@Mapper
public interface MeituanXiaodaiInfoMapper extends BaseMapper<MeituanXiaodaiInfo> {

    /**
     * 根据场所ID查询信息
     *
     * @param placeId 场所ID
     * @return 美团小袋信息列表
     */
    @Select("SELECT * FROM t_meituan_xiaodai_info WHERE place_id = #{placeId} AND status = 1")
    List<MeituanXiaodaiInfo> selectByPlaceId(@Param("placeId") String placeId);

    /**
     * 根据产品ID查询信息
     *
     * @param productId 产品ID
     * @return 美团小袋信息列表
     */
    @Select("SELECT * FROM t_meituan_xiaodai_info WHERE product_id = #{productId} AND status = 1")
    List<MeituanXiaodaiInfo> selectByProductId(@Param("productId") String productId);

    /**
     * 根据设备ID查询信息
     *
     * @param deviceId 设备ID
     * @return 美团小袋信息
     */
    @Select("SELECT * FROM t_meituan_xiaodai_info WHERE device_id = #{deviceId} AND status = 1 LIMIT 1")
    MeituanXiaodaiInfo selectByDeviceId(@Param("deviceId") String deviceId);

    /**
     * 根据类型查询信息
     *
     * @param type 类型
     * @return 美团小袋信息列表
     */
    @Select("SELECT * FROM t_meituan_xiaodai_info WHERE type = #{type} AND status = 1 ORDER BY create_time DESC")
    List<MeituanXiaodaiInfo> selectByType(@Param("type") String type);

    /**
     * 根据经纬度范围查询附近的信息
     *
     * @param minLat 最小纬度
     * @param maxLat 最大纬度
     * @param minLng 最小经度
     * @param maxLng 最大经度
     * @return 美团小袋信息列表
     */
    @Select("SELECT * FROM t_meituan_xiaodai_info WHERE lat BETWEEN #{minLat} AND #{maxLat} " +
            "AND lng BETWEEN #{minLng} AND #{maxLng} AND status = 1 ORDER BY create_time DESC")
    List<MeituanXiaodaiInfo> selectByLocationRange(@Param("minLat") Double minLat,
                                                   @Param("maxLat") Double maxLat,
                                                   @Param("minLng") Double minLng,
                                                   @Param("maxLng") Double maxLng);

    /**
     * 根据关键词模糊查询
     *
     * @param keywords 关键词
     * @return 美团小袋信息列表
     */
    @Select("SELECT * FROM t_meituan_xiaodai_info WHERE keywords LIKE CONCAT('%', #{keywords}, '%') " +
            "AND status = 1 ORDER BY create_time DESC")
    List<MeituanXiaodaiInfo> selectByKeywords(@Param("keywords") String keywords);

    /**
     * 统计各类型的数量
     *
     * @return 统计结果
     */
    @Select("SELECT type, COUNT(*) as count FROM t_meituan_xiaodai_info WHERE status = 1 GROUP BY type")
    List<TypeStatistics> selectTypeStatistics();

    /**
     * 类型统计结果内部类
     */
    class TypeStatistics {
        private String type;
        private Long count;

        public String getType() {
            return type;
        }

        public void setType(String type) {
            this.type = type;
        }

        public Long getCount() {
            return count;
        }

        public void setCount(Long count) {
            this.count = count;
        }
    }
}
