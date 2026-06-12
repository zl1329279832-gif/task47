package com.service;

import com.baomidou.mybatisplus.service.IService;
import com.utils.PageUtils;
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
     * 审核申请
     * @param shangpaiId 上牌ID
     * @param result 审核结果: 2=通过, 3=驳回
     */
    void auditApplication(Integer shangpaiId, Integer result);

    /**
     * 提交上牌申请
     * @param xuanpaiId 选牌ID
     * @param baoxianId 保险ID
     * @param yonghuId 用户ID
     * @return 创建的上牌实体
     */
    ShangpaiEntity submitApplication(Integer xuanpaiId, Integer baoxianId, Integer yonghuId);
}