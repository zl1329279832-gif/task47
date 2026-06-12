package com.service;

import com.baomidou.mybatisplus.service.IService;
import com.utils.PageUtils;
import com.utils.R;
import com.entity.ShangpaiEntity;
import java.util.Map;

/**
 * 上牌信息 服务类
 * @author
 * @since 2021-04-27
 */
public interface ShangpaiService extends IService<ShangpaiEntity> {

    /**
    * @param params 查询参数
    * @return 带分页的查询出来的数据
    */
     PageUtils queryPage(Map<String, Object> params);

    /**
     * 选牌预占
     * @param xuanpaiId 号牌ID
     * @param yonghuId 用户ID
     * @return 操作结果
     */
    R reservePlate(Integer xuanpaiId, Integer yonghuId);

    /**
     * 提交上牌申请
     * @param xuanpaiId 号牌ID
     * @param baoxianId 保险ID
     * @param yonghuId 用户ID
     * @return 操作结果
     */
    R submitApplication(Integer xuanpaiId, Integer baoxianId, Integer yonghuId);

    /**
     * 审核上牌申请
     * @param shangpaiId 申请ID
     * @param jieguo 审核结果 2=通过 3=拒绝
     * @return 操作结果
     */
    R reviewApplication(Integer shangpaiId, Integer jieguo);
}