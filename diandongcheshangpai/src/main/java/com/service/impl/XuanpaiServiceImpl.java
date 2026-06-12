package com.service.impl;

import com.utils.StringUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.lang.reflect.Field;
import java.util.*;
import com.baomidou.mybatisplus.mapper.EntityWrapper;
import com.baomidou.mybatisplus.plugins.Page;
import com.baomidou.mybatisplus.service.impl.ServiceImpl;
import org.springframework.transaction.annotation.Transactional;
import com.utils.PageUtils;
import com.utils.Query;
import org.springframework.web.context.ContextLoader;
import javax.servlet.ServletContext;

import com.dao.XuanpaiDao;
import com.dao.YuyueJiluDao;
import com.entity.XuanpaiEntity;
import com.entity.YuyueJiluEntity;
import com.service.XuanpaiService;
import com.entity.view.XuanpaiView;

/**
 * 选牌信息 服务实现类
 * @author
 * @since 2021-04-27
 */
@Service("xuanpaiService")
@Transactional
public class XuanpaiServiceImpl extends ServiceImpl<XuanpaiDao, XuanpaiEntity> implements XuanpaiService {

    private static final int DEFAULT_RESERVE_MINUTES = 30;

    @Autowired
    private YuyueJiluDao yuyueJiluDao;

    @Override
    public PageUtils queryPage(Map<String,Object> params) {
        if(params != null && (params.get("limit") == null || params.get("page") == null)){
            params.put("page","1");
            params.put("limit","10");
        }
        Page<XuanpaiView> page =new Query<XuanpaiView>(params).getPage();
        page.setRecords(baseMapper.selectListView(page,params));
        return new PageUtils(page);
    }

    @Override
    public boolean reservePlate(Integer xuanpaiId, Integer yonghuId, int expireMinutes) {
        // 检查用户是否已有活跃的预占
        EntityWrapper<XuanpaiEntity> wrapper = new EntityWrapper<>();
        wrapper.eq("yuyue_user_id", yonghuId)
               .eq("zhuangtai_types", 2);
        List<XuanpaiEntity> existing = baseMapper.selectList(wrapper);
        if (existing != null && !existing.isEmpty()) {
            throw new RuntimeException("该用户已有活跃的预占记录，请先释放后再预占");
        }

        // 获取车牌当前版本
        XuanpaiEntity plate = baseMapper.selectById(xuanpaiId);
        if (plate == null) {
            throw new RuntimeException("车牌不存在");
        }
        if (plate.getZhuangtaiTypes() != 1) {
            throw new RuntimeException("车牌当前不可预占");
        }

        // 计算过期时间
        Calendar calendar = Calendar.getInstance();
        calendar.add(Calendar.MINUTE, expireMinutes > 0 ? expireMinutes : DEFAULT_RESERVE_MINUTES);
        Date expireTime = calendar.getTime();

        // 乐观锁更新
        int affected = baseMapper.updateWithVersion(xuanpaiId, plate.getVersion(), 2, yonghuId, expireTime);
        if (affected == 0) {
            throw new RuntimeException("预占失败，车牌可能已被其他用户预占");
        }

        // 记录预占日志
        YuyueJiluEntity jilu = new YuyueJiluEntity();
        jilu.setYonghuId(yonghuId);
        jilu.setXuanpaiId(xuanpaiId);
        jilu.setYuyueJiluTypes(1);
        jilu.setInsertTime(new Date());
        yuyueJiluDao.insert(jilu);

        return true;
    }

    @Override
    public boolean releasePlate(Integer xuanpaiId) {
        XuanpaiEntity plate = baseMapper.selectById(xuanpaiId);
        if (plate == null) {
            throw new RuntimeException("车牌不存在");
        }

        plate.setZhuangtaiTypes(1);
        plate.setYuyueUserId(null);
        plate.setYuyueExpireTime(null);
        plate.setVersion(plate.getVersion() == null ? 0 : plate.getVersion() + 1);
        baseMapper.updateById(plate);

        return true;
    }

    @Override
    public int releaseExpiredReservations() {
        // 先查出过期记录（含用户信息），再批量释放，否则 UPDATE 后 yuyue_user_id 已被清空，无法落库日志
        List<XuanpaiEntity> expired = baseMapper.selectExpiredReservations();

        int affected = baseMapper.batchReleaseExpired();

        // 记录过期释放日志
        for (XuanpaiEntity entity : expired) {
            YuyueJiluEntity jilu = new YuyueJiluEntity();
            jilu.setYonghuId(entity.getYuyueUserId());
            jilu.setXuanpaiId(entity.getId());
            jilu.setYuyueJiluTypes(2);
            jilu.setInsertTime(new Date());
            yuyueJiluDao.insert(jilu);
        }

        return affected;
    }

    @Override
    public XuanpaiEntity getPlateWithLazyExpire(Integer xuanpaiId) {
        XuanpaiEntity plate = baseMapper.selectById(xuanpaiId);
        if (plate == null) {
            return null;
        }

        // 如果状态为预占中(2)且已过期，则释放并重新查询
        if (plate.getZhuangtaiTypes() != null && plate.getZhuangtaiTypes() == 2
                && plate.getYuyueExpireTime() != null
                && plate.getYuyueExpireTime().before(new Date())) {
            releasePlate(xuanpaiId);
            plate = baseMapper.selectById(xuanpaiId);
        }

        return plate;
    }


}
