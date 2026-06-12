package com.dao;

import com.entity.YuyueJiluEntity;
import com.baomidou.mybatisplus.mapper.BaseMapper;
import java.util.List;
import java.util.Map;
import com.baomidou.mybatisplus.plugins.pagination.Pagination;

import org.apache.ibatis.annotations.Param;
import com.entity.view.YuyueJiluView;

/**
 * 预占日志 Dao 接口
 *
 * @author
 * @since 2021-04-27
 */
public interface YuyueJiluDao extends BaseMapper<YuyueJiluEntity> {

   List<YuyueJiluView> selectListView(Pagination page,@Param("params")Map<String,Object> params);

}
