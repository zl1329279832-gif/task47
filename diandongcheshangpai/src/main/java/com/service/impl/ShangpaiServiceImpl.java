package com.service.impl;

import com.utils.StringUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.lang.reflect.Field;
import java.util.*;
import com.baomidou.mybatisplus.plugins.Page;
import com.baomidou.mybatisplus.service.impl.ServiceImpl;
import org.springframework.transaction.annotation.Transactional;
import com.utils.PageUtils;
import com.utils.Query;
import org.springframework.web.context.ContextLoader;
import javax.servlet.ServletContext;

import com.dao.ShangpaiDao;
import com.dao.YuyueJiluDao;
import com.entity.ShangpaiEntity;
import com.entity.XuanpaiEntity;
import com.entity.BaoxianEntity;
import com.entity.YuyueJiluEntity;
import com.service.ShangpaiService;
import com.service.XuanpaiService;
import com.service.BaoxianService;
import com.entity.view.ShangpaiView;

/**
 * 上牌信息 服务实现类
 * @author
 * @since 2021-04-27
 */
@Service("shangpaiService")
@Transactional
public class ShangpaiServiceImpl extends ServiceImpl<ShangpaiDao, ShangpaiEntity> implements ShangpaiService {

    @Autowired
    private XuanpaiService xuanpaiService;

    @Autowired
    private BaoxianService baoxianService;

    @Autowired
    private YuyueJiluDao yuyueJiluDao;

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
    public void auditApplication(Integer shangpaiId, Integer result) {
        // 悲观锁查询
        ShangpaiEntity shangpai = baseMapper.selectForUpdate(shangpaiId);
        if (shangpai == null) {
            throw new RuntimeException("申请不存在");
        }
        if (shangpai.getShangpaiTypes() != 1) {
            throw new RuntimeException("申请已审核，不可重复操作");
        }

        // 获取关联的选牌信息
        XuanpaiEntity xuanpai = xuanpaiService.selectById(shangpai.getXuanpaiId());
        if (xuanpai == null) {
            throw new RuntimeException("关联的车牌不存在");
        }

        if (result == 2) {
            // 审核通过: 车牌状态改为已上牌(3)
            xuanpai.setZhuangtaiTypes(3);
            xuanpai.setYuyueUserId(null);
            xuanpai.setYuyueExpireTime(null);
            xuanpaiService.updateById(xuanpai);
        } else if (result == 3) {
            // 审核驳回: 车牌状态改为可用(1)
            xuanpai.setZhuangtaiTypes(1);
            xuanpai.setYuyueUserId(null);
            xuanpai.setYuyueExpireTime(null);
            xuanpaiService.updateById(xuanpai);
        }

        // 更新上牌申请状态
        shangpai.setShangpaiTypes(result);
        baseMapper.updateById(shangpai);

        // 记录日志
        YuyueJiluEntity jilu = new YuyueJiluEntity();
        jilu.setYonghuId(shangpai.getYonghuId());
        jilu.setXuanpaiId(shangpai.getXuanpaiId());
        jilu.setYuyueJiluTypes(result == 2 ? 4 : 3);
        jilu.setInsertTime(new Date());
        yuyueJiluDao.insert(jilu);
    }

    @Override
    public ShangpaiEntity submitApplication(Integer xuanpaiId, Integer baoxianId, Integer yonghuId) {
        // 校验车牌状态
        XuanpaiEntity xuanpai = xuanpaiService.selectById(xuanpaiId);
        if (xuanpai == null) {
            throw new RuntimeException("车牌不存在");
        }
        if (xuanpai.getZhuangtaiTypes() == null || xuanpai.getZhuangtaiTypes() != 2) {
            throw new RuntimeException("车牌当前不可申请");
        }
        if (xuanpai.getYuyueUserId() == null || !xuanpai.getYuyueUserId().equals(yonghuId)) {
            throw new RuntimeException("该车牌未被当前用户预占");
        }
        if (xuanpai.getYuyueExpireTime() != null && xuanpai.getYuyueExpireTime().before(new Date())) {
            throw new RuntimeException("预占已过期，请重新预占");
        }

        // 校验保险
        BaoxianEntity baoxian = baoxianService.selectById(baoxianId);
        if (baoxian == null) {
            throw new RuntimeException("保险不存在");
        }

        // 创建上牌申请
        ShangpaiEntity shangpai = new ShangpaiEntity();
        shangpai.setYonghuId(yonghuId);
        shangpai.setXuanpaiId(xuanpaiId);
        shangpai.setBaoxianId(baoxianId);
        shangpai.setShangpaiTypes(1);
        shangpai.setInsertTime(new Date());
        baseMapper.insert(shangpai);

        // 记录日志 (type=4 转为申请)
        YuyueJiluEntity jilu = new YuyueJiluEntity();
        jilu.setYonghuId(yonghuId);
        jilu.setXuanpaiId(xuanpaiId);
        jilu.setYuyueJiluTypes(4);
        jilu.setInsertTime(new Date());
        yuyueJiluDao.insert(jilu);

        return shangpai;
    }


}
