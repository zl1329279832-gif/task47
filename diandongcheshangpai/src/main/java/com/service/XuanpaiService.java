package com.service;

import com.baomidou.mybatisplus.service.IService;
import com.utils.PageUtils;
import com.entity.XuanpaiEntity;
import java.util.Map;

/**
 * 选牌信息 服务类
 * @author
 * @since 2021-04-27
 */
public interface XuanpaiService extends IService<XuanpaiEntity> {

    /**
    * @param params 查询参数
    * @return 带分页的查询出来的数据
    */
     PageUtils queryPage(Map<String, Object> params);

    /**
     * 预占车牌
     * @param xuanpaiId 选牌ID
     * @param yonghuId 用户ID
     * @param expireMinutes 过期分钟数
     * @return 是否成功
     */
    boolean reservePlate(Integer xuanpaiId, Integer yonghuId, int expireMinutes);

    /**
     * 释放车牌
     * @param xuanpaiId 选牌ID
     * @return 是否成功
     */
    boolean releasePlate(Integer xuanpaiId);

    /**
     * 释放过期预占
     * @return 释放数量
     */
    int releaseExpiredReservations();

    /**
     * 获取车牌（懒过期处理）
     * @param xuanpaiId 选牌ID
     * @return 选牌实体
     */
    XuanpaiEntity getPlateWithLazyExpire(Integer xuanpaiId);
}