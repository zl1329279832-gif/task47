package com.service.impl;

import com.utils.StringUtil;
import org.springframework.stereotype.Service;
import java.lang.reflect.Field;
import java.util.*;
import com.baomidou.mybatisplus.mapper.EntityWrapper;
import com.baomidou.mybatisplus.mapper.Wrapper;
import com.baomidou.mybatisplus.plugins.Page;
import com.baomidou.mybatisplus.service.impl.ServiceImpl;
import org.springframework.transaction.annotation.Transactional;
import com.utils.PageUtils;
import com.utils.Query;
import com.utils.R;
import org.springframework.web.context.ContextLoader;
import javax.servlet.ServletContext;

import com.dao.ShangpaiDao;
import com.entity.ShangpaiEntity;
import com.entity.XuanpaiEntity;
import com.entity.BaoxianEntity;
import com.service.ShangpaiService;
import com.service.XuanpaiService;
import com.service.BaoxianService;
import com.entity.view.ShangpaiView;
import org.springframework.beans.factory.annotation.Autowired;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 上牌信息 服务实现类
 * @author
 * @since 2021-04-27
 */
@Service("shangpaiService")
@Transactional
public class ShangpaiServiceImpl extends ServiceImpl<ShangpaiDao, ShangpaiEntity> implements ShangpaiService {

    private static final Logger logger = LoggerFactory.getLogger(ShangpaiServiceImpl.class);

    /** 预占有效时长：30分钟 */
    private static final long RESERVATION_TIMEOUT_MS = 30 * 60 * 1000L;

    @Autowired
    private XuanpaiService xuanpaiService;

    @Autowired
    private BaoxianService baoxianService;

    @Override
    public PageUtils queryPage(Map<String,Object> params) {
        if(params != null && (params.get("limit") == null || params.get("page") == null)){
            params.put("page","1");
            params.put("limit","10");
        }
        Page<ShangpaiView> page =new Query<ShangpaiView>(params).getPage();
        page.setRecords(baseMapper.selectListView(page,params));
        return new PageUtils(page);
    }

    @Override
    public R reservePlate(Integer xuanpaiId, Integer yonghuId) {
        if (xuanpaiId == null || yonghuId == null) {
            return R.error("参数不完整");
        }

        XuanpaiEntity xuanpai = xuanpaiService.selectById(xuanpaiId);
        if (xuanpai == null) {
            return R.error("号牌不存在");
        }

        Integer status = xuanpai.getZhuangtaiTypes();
        if (status == null) {
            return R.error("号牌状态异常");
        }

        // 号牌已被占用（审核通过），不可再选
        if (status == 3) {
            return R.error("该号牌已被占用，不可选择");
        }

        // 号牌已被预占
        if (status == 2) {
            boolean expired = isReservationExpired(xuanpai.getYuezhanTime());
            if (!expired) {
                // 预占未过期
                if (yonghuId.equals(xuanpai.getYonghuId())) {
                    return R.error("您已预占该号牌，请直接提交申请");
                } else {
                    return R.error("该号牌已被其他用户预占，请选择其他号牌");
                }
            }
            // 预占已过期，释放后重新预占
            logger.info("号牌[{}]预占已过期，释放并重新预占给用户[{}]", xuanpaiId, yonghuId);
        }

        // 检查用户是否已有该号牌的待审核申请
        Wrapper<ShangpaiEntity> pendingWrapper = new EntityWrapper<ShangpaiEntity>()
                .eq("yonghu_id", yonghuId)
                .eq("xuanpai_id", xuanpaiId)
                .eq("shangpai_types", 1);
        ShangpaiEntity pendingApplication = this.selectOne(pendingWrapper);
        if (pendingApplication != null) {
            return R.error("您已有该号牌的待审核申请，不可重复选牌");
        }

        // 执行预占
        xuanpai.setZhuangtaiTypes(2);
        xuanpai.setYonghuId(yonghuId);
        xuanpai.setYuezhanTime(new Date());
        xuanpaiService.updateById(xuanpai);

        return R.ok("选牌预占成功，请在30分钟内提交上牌申请");
    }

    @Override
    public R submitApplication(Integer xuanpaiId, Integer baoxianId, Integer yonghuId) {
        if (xuanpaiId == null || baoxianId == null || yonghuId == null) {
            return R.error("参数不完整");
        }

        // 校验号牌
        XuanpaiEntity xuanpai = xuanpaiService.selectById(xuanpaiId);
        if (xuanpai == null) {
            return R.error("号牌不存在");
        }

        Integer status = xuanpai.getZhuangtaiTypes();
        if (status == null || status != 2) {
            return R.error("该号牌未被预占，请先选牌");
        }

        // 校验是否本人预占
        if (!yonghuId.equals(xuanpai.getYonghuId())) {
            return R.error("该号牌非您预占，无法提交申请");
        }

        // 校验预占是否过期
        if (isReservationExpired(xuanpai.getYuezhanTime())) {
            // 过期则释放号牌
            xuanpai.setZhuangtaiTypes(1);
            xuanpai.setYonghuId(null);
            xuanpai.setYuezhanTime(null);
            xuanpaiService.updateById(xuanpai);
            return R.error("预占已过期，请重新选牌");
        }

        // 校验保险
        BaoxianEntity baoxian = baoxianService.selectById(baoxianId);
        if (baoxian == null) {
            return R.error("保险信息不存在");
        }

        // 检查重复申请
        Wrapper<ShangpaiEntity> duplicateWrapper = new EntityWrapper<ShangpaiEntity>()
                .eq("yonghu_id", yonghuId)
                .eq("xuanpai_id", xuanpaiId)
                .eq("shangpai_types", 1);
        ShangpaiEntity existing = this.selectOne(duplicateWrapper);
        if (existing != null) {
            return R.error("您已提交过该号牌的上牌申请，请勿重复提交");
        }

        // 创建上牌申请
        ShangpaiEntity shangpai = new ShangpaiEntity();
        shangpai.setYonghuId(yonghuId);
        shangpai.setXuanpaiId(xuanpaiId);
        shangpai.setBaoxianId(baoxianId);
        shangpai.setShangpaiTypes(1); // 待审核
        shangpai.setInsertTime(new Date());
        shangpai.setCreateTime(new Date());
        this.insert(shangpai);

        return R.ok("上牌申请提交成功，请等待审核");
    }

    @Override
    public R reviewApplication(Integer shangpaiId, Integer jieguo) {
        if (shangpaiId == null || jieguo == null) {
            return R.error("参数不完整");
        }

        // 审核结果只能是2（通过）或3（拒绝）
        if (jieguo != 2 && jieguo != 3) {
            return R.error("审核结果无效");
        }

        ShangpaiEntity shangpai = this.selectById(shangpaiId);
        if (shangpai == null) {
            return R.error("申请记录不存在");
        }

        // 并发审核保护：只有待审核状态才能审核
        if (shangpai.getShangpaiTypes() == null || shangpai.getShangpaiTypes() != 1) {
            return R.error("该申请已被审核，不可重复操作");
        }

        XuanpaiEntity xuanpai = xuanpaiService.selectById(shangpai.getXuanpaiId());
        if (xuanpai == null) {
            return R.error("关联号牌不存在");
        }

        // 校验号牌状态合法性：审核时号牌应为预占状态
        if (xuanpai.getZhuangtaiTypes() == null || xuanpai.getZhuangtaiTypes() == 3) {
            return R.error("号牌状态异常，无法审核");
        }

        if (jieguo == 2) {
            // 审核通过：号牌变为已占用
            shangpai.setShangpaiTypes(2);
            xuanpai.setZhuangtaiTypes(3);
        } else {
            // 审核拒绝：释放号牌
            shangpai.setShangpaiTypes(3);
            xuanpai.setZhuangtaiTypes(1);
            xuanpai.setYonghuId(null);
            xuanpai.setYuezhanTime(null);
        }

        this.updateById(shangpai);
        xuanpaiService.updateById(xuanpai);

        return R.ok(jieguo == 2 ? "审核通过" : "审核已拒绝，号牌已释放");
    }

    /**
     * 判断预占是否已过期
     */
    private boolean isReservationExpired(Date yuezhanTime) {
        if (yuezhanTime == null) {
            return true;
        }
        return System.currentTimeMillis() - yuezhanTime.getTime() > RESERVATION_TIMEOUT_MS;
    }
}
