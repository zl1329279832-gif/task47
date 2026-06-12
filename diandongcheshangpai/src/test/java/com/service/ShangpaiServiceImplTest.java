package com.service;

import com.dao.ShangpaiDao;
import com.entity.BaoxianEntity;
import com.entity.ShangpaiEntity;
import com.entity.XuanpaiEntity;
import com.service.impl.ShangpaiServiceImpl;
import com.utils.R;
import com.baomidou.mybatisplus.mapper.Wrapper;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.*;
import org.mockito.runners.MockitoJUnitRunner;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import static org.junit.Assert.*;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyInt;
import static org.mockito.Mockito.*;

/**
 * ShangpaiServiceImpl 单元测试
 * 覆盖：选牌预占、提交申请、审核流程及各类异常边界
 */
@RunWith(MockitoJUnitRunner.class)
public class ShangpaiServiceImplTest {

    @Mock
    private ShangpaiDao shangpaiDao;

    @Mock
    private XuanpaiService xuanpaiService;

    @Mock
    private BaoxianService baoxianService;

    @Spy
    @InjectMocks
    private ShangpaiServiceImpl shangpaiService;

    private XuanpaiEntity availablePlate;
    private XuanpaiEntity reservedPlate;
    private XuanpaiEntity occupiedPlate;
    private BaoxianEntity validInsurance;
    private ShangpaiEntity pendingApplication;

    @Before
    public void setUp() {
        // 可选号牌
        availablePlate = new XuanpaiEntity();
        availablePlate.setId(1);
        availablePlate.setXuanpaiName("京A12345");
        availablePlate.setZhuangtaiTypes(1);
        availablePlate.setYonghuId(null);
        availablePlate.setYuezhanTime(null);

        // 已预占号牌（未过期，用户100预占）
        reservedPlate = new XuanpaiEntity();
        reservedPlate.setId(2);
        reservedPlate.setXuanpaiName("京B67890");
        reservedPlate.setZhuangtaiTypes(2);
        reservedPlate.setYonghuId(100);
        reservedPlate.setYuezhanTime(new Date()); // 刚预占

        // 已占用号牌
        occupiedPlate = new XuanpaiEntity();
        occupiedPlate.setId(3);
        occupiedPlate.setXuanpaiName("京C11111");
        occupiedPlate.setZhuangtaiTypes(3);

        // 有效保险
        validInsurance = new BaoxianEntity();
        validInsurance.setId(10);
        validInsurance.setBaoxianName("交强险");
        validInsurance.setBaoxianTypes(1);
        validInsurance.setBaoxianMoney(950.0);

        // 待审核申请
        pendingApplication = new ShangpaiEntity();
        pendingApplication.setId(1);
        pendingApplication.setYonghuId(100);
        pendingApplication.setXuanpaiId(2);
        pendingApplication.setBaoxianId(10);
        pendingApplication.setShangpaiTypes(1);
    }

    // ==================== 选牌预占测试 ====================

    @Test
    public void testReservePlate_success() {
        when(xuanpaiService.selectById(1)).thenReturn(availablePlate);
        doReturn(null).when(shangpaiService).selectOne(Matchers.<Wrapper<ShangpaiEntity>>any());
        when(xuanpaiService.updateById(any(XuanpaiEntity.class))).thenReturn(true);

        R result = shangpaiService.reservePlate(1, 100);
        assertEquals(0, result.get("code"));

        // 验证号牌状态被更新为预占
        ArgumentCaptor<XuanpaiEntity> captor = ArgumentCaptor.forClass(XuanpaiEntity.class);
        verify(xuanpaiService).updateById(captor.capture());
        XuanpaiEntity updated = captor.getValue();
        assertEquals(Integer.valueOf(2), updated.getZhuangtaiTypes());
        assertEquals(Integer.valueOf(100), updated.getYonghuId());
        assertNotNull(updated.getYuezhanTime());
    }

    @Test
    public void testReservePlate_plateNotFound() {
        when(xuanpaiService.selectById(999)).thenReturn(null);

        R result = shangpaiService.reservePlate(999, 100);
        assertEquals(500, result.get("code"));
        assertEquals("号牌不存在", result.get("msg"));
    }

    @Test
    public void testReservePlate_plateOccupied() {
        when(xuanpaiService.selectById(3)).thenReturn(occupiedPlate);

        R result = shangpaiService.reservePlate(3, 100);
        assertEquals(500, result.get("code"));
        assertEquals("该号牌已被占用，不可选择", result.get("msg"));
    }

    @Test
    public void testReservePlate_alreadyReservedBySameUser() {
        when(xuanpaiService.selectById(2)).thenReturn(reservedPlate);

        R result = shangpaiService.reservePlate(2, 100);
        assertEquals(500, result.get("code"));
        assertEquals("您已预占该号牌，请直接提交申请", result.get("msg"));
    }

    @Test
    public void testReservePlate_reservedByOtherUserNotExpired() {
        when(xuanpaiService.selectById(2)).thenReturn(reservedPlate);

        R result = shangpaiService.reservePlate(2, 200);
        assertEquals(500, result.get("code"));
        assertEquals("该号牌已被其他用户预占，请选择其他号牌", result.get("msg"));
    }

    @Test
    public void testReservePlate_expiredReservationReleased() {
        // 设置预占时间为31分钟前（已过期）
        XuanpaiEntity expiredPlate = new XuanpaiEntity();
        expiredPlate.setId(2);
        expiredPlate.setZhuangtaiTypes(2);
        expiredPlate.setYonghuId(100);
        expiredPlate.setYuezhanTime(new Date(System.currentTimeMillis() - 31 * 60 * 1000L));

        when(xuanpaiService.selectById(2)).thenReturn(expiredPlate);
        doReturn(null).when(shangpaiService).selectOne(Matchers.<Wrapper<ShangpaiEntity>>any());
        when(xuanpaiService.updateById(any(XuanpaiEntity.class))).thenReturn(true);

        // 新用户预占过期的号牌应成功
        R result = shangpaiService.reservePlate(2, 200);
        assertEquals(0, result.get("code"));

        ArgumentCaptor<XuanpaiEntity> captor = ArgumentCaptor.forClass(XuanpaiEntity.class);
        verify(xuanpaiService).updateById(captor.capture());
        assertEquals(Integer.valueOf(200), captor.getValue().getYonghuId());
    }

    @Test
    public void testReservePlate_duplicatePendingApplication() {
        when(xuanpaiService.selectById(1)).thenReturn(availablePlate);
        doReturn(pendingApplication).when(shangpaiService).selectOne(Matchers.<Wrapper<ShangpaiEntity>>any());

        R result = shangpaiService.reservePlate(1, 100);
        assertEquals(500, result.get("code"));
        assertTrue(result.get("msg").toString().contains("不可重复选牌"));
    }

    @Test
    public void testReservePlate_nullParams() {
        R result = shangpaiService.reservePlate(null, 100);
        assertEquals(500, result.get("code"));
        assertEquals("参数不完整", result.get("msg"));
    }

    // ==================== 提交上牌申请测试 ====================

    @Test
    public void testSubmitApplication_success() {
        when(xuanpaiService.selectById(2)).thenReturn(reservedPlate);
        when(baoxianService.selectById(10)).thenReturn(validInsurance);
        doReturn(null).when(shangpaiService).selectOne(Matchers.<Wrapper<ShangpaiEntity>>any());
        doReturn(true).when(shangpaiService).insert(any(ShangpaiEntity.class));

        R result = shangpaiService.submitApplication(2, 10, 100);
        assertEquals(0, result.get("code"));

        ArgumentCaptor<ShangpaiEntity> captor = ArgumentCaptor.forClass(ShangpaiEntity.class);
        verify(shangpaiService).insert(captor.capture());
        ShangpaiEntity inserted = captor.getValue();
        assertEquals(Integer.valueOf(100), inserted.getYonghuId());
        assertEquals(Integer.valueOf(2), inserted.getXuanpaiId());
        assertEquals(Integer.valueOf(10), inserted.getBaoxianId());
        assertEquals(Integer.valueOf(1), inserted.getShangpaiTypes());
    }

    @Test
    public void testSubmitApplication_plateNotReserved() {
        when(xuanpaiService.selectById(1)).thenReturn(availablePlate); // 状态1不是预占

        R result = shangpaiService.submitApplication(1, 10, 100);
        assertEquals(500, result.get("code"));
        assertEquals("该号牌未被预占，请先选牌", result.get("msg"));
    }

    @Test
    public void testSubmitApplication_notReservedByThisUser() {
        when(xuanpaiService.selectById(2)).thenReturn(reservedPlate); // 用户100预占的

        R result = shangpaiService.submitApplication(2, 10, 200); // 用户200尝试提交
        assertEquals(500, result.get("code"));
        assertEquals("该号牌非您预占，无法提交申请", result.get("msg"));
    }

    @Test
    public void testSubmitApplication_reservationExpired() {
        XuanpaiEntity expiredPlate = new XuanpaiEntity();
        expiredPlate.setId(2);
        expiredPlate.setZhuangtaiTypes(2);
        expiredPlate.setYonghuId(100);
        expiredPlate.setYuezhanTime(new Date(System.currentTimeMillis() - 31 * 60 * 1000L));

        when(xuanpaiService.selectById(2)).thenReturn(expiredPlate);
        when(xuanpaiService.updateById(any(XuanpaiEntity.class))).thenReturn(true);

        R result = shangpaiService.submitApplication(2, 10, 100);
        assertEquals(500, result.get("code"));
        assertTrue(result.get("msg").toString().contains("过期"));

        // 验证号牌被释放
        ArgumentCaptor<XuanpaiEntity> captor = ArgumentCaptor.forClass(XuanpaiEntity.class);
        verify(xuanpaiService).updateById(captor.capture());
        assertEquals(Integer.valueOf(1), captor.getValue().getZhuangtaiTypes());
        assertNull(captor.getValue().getYonghuId());
    }

    @Test
    public void testSubmitApplication_insuranceNotFound() {
        when(xuanpaiService.selectById(2)).thenReturn(reservedPlate);
        when(baoxianService.selectById(999)).thenReturn(null);

        R result = shangpaiService.submitApplication(2, 999, 100);
        assertEquals(500, result.get("code"));
        assertEquals("保险信息不存在", result.get("msg"));
    }

    @Test
    public void testSubmitApplication_duplicateApplication() {
        when(xuanpaiService.selectById(2)).thenReturn(reservedPlate);
        when(baoxianService.selectById(10)).thenReturn(validInsurance);
        doReturn(pendingApplication).when(shangpaiService).selectOne(Matchers.<Wrapper<ShangpaiEntity>>any());

        R result = shangpaiService.submitApplication(2, 10, 100);
        assertEquals(500, result.get("code"));
        assertTrue(result.get("msg").toString().contains("重复提交"));
    }

    @Test
    public void testSubmitApplication_nullParams() {
        R result = shangpaiService.submitApplication(null, 10, 100);
        assertEquals(500, result.get("code"));
        assertEquals("参数不完整", result.get("msg"));
    }

    // ==================== 审核流程测试 ====================

    @Test
    public void testReviewApplication_approve() {
        doReturn(pendingApplication).when(shangpaiService).selectById(1);
        when(xuanpaiService.selectById(2)).thenReturn(reservedPlate);
        doReturn(true).when(shangpaiService).updateById(any(ShangpaiEntity.class));
        when(xuanpaiService.updateById(any(XuanpaiEntity.class))).thenReturn(true);

        R result = shangpaiService.reviewApplication(1, 2);
        assertEquals(0, result.get("code"));

        // 验证申请状态变为通过
        assertEquals(Integer.valueOf(2), pendingApplication.getShangpaiTypes());
        // 验证号牌状态变为已占用
        assertEquals(Integer.valueOf(3), reservedPlate.getZhuangtaiTypes());
    }

    @Test
    public void testReviewApplication_reject() {
        doReturn(pendingApplication).when(shangpaiService).selectById(1);
        when(xuanpaiService.selectById(2)).thenReturn(reservedPlate);
        doReturn(true).when(shangpaiService).updateById(any(ShangpaiEntity.class));
        when(xuanpaiService.updateById(any(XuanpaiEntity.class))).thenReturn(true);

        R result = shangpaiService.reviewApplication(1, 3);
        assertEquals(0, result.get("code"));

        // 验证申请状态变为拒绝
        assertEquals(Integer.valueOf(3), pendingApplication.getShangpaiTypes());
        // 验证号牌被释放
        assertEquals(Integer.valueOf(1), reservedPlate.getZhuangtaiTypes());
        assertNull(reservedPlate.getYonghuId());
        assertNull(reservedPlate.getYuezhanTime());
    }

    @Test
    public void testReviewApplication_alreadyReviewed_concurrentProtection() {
        ShangpaiEntity approvedApplication = new ShangpaiEntity();
        approvedApplication.setId(1);
        approvedApplication.setShangpaiTypes(2); // 已通过

        doReturn(approvedApplication).when(shangpaiService).selectById(1);

        R result = shangpaiService.reviewApplication(1, 3);
        assertEquals(500, result.get("code"));
        assertEquals("该申请已被审核，不可重复操作", result.get("msg"));

        // 验证没有进行任何更新操作
        verify(shangpaiService, never()).updateById(any(ShangpaiEntity.class));
        verify(xuanpaiService, never()).updateById(any(XuanpaiEntity.class));
    }

    @Test
    public void testReviewApplication_notFound() {
        doReturn(null).when(shangpaiService).selectById(999);

        R result = shangpaiService.reviewApplication(999, 2);
        assertEquals(500, result.get("code"));
        assertEquals("申请记录不存在", result.get("msg"));
    }

    @Test
    public void testReviewApplication_invalidResult() {
        R result = shangpaiService.reviewApplication(1, 5);
        assertEquals(500, result.get("code"));
        assertEquals("审核结果无效", result.get("msg"));
    }

    @Test
    public void testReviewApplication_plateAlreadyOccupied_illegalTransition() {
        doReturn(pendingApplication).when(shangpaiService).selectById(1);
        // 将pendingApplication的xuanpaiId改为3以匹配occupiedPlate
        pendingApplication.setXuanpaiId(3);
        when(xuanpaiService.selectById(3)).thenReturn(occupiedPlate); // 号牌状态已是3

        R result = shangpaiService.reviewApplication(1, 2);
        assertEquals(500, result.get("code"));
        assertEquals("号牌状态异常，无法审核", result.get("msg"));

        // 还原
        pendingApplication.setXuanpaiId(2);
    }

    @Test
    public void testReviewApplication_nullParams() {
        R result = shangpaiService.reviewApplication(null, 2);
        assertEquals(500, result.get("code"));
        assertEquals("参数不完整", result.get("msg"));
    }

    // ==================== 号牌状态流转完整性测试 ====================

    @Test
    public void testFullFlow_reserveSubmitApprove() {
        // 步骤1: 预占
        when(xuanpaiService.selectById(1)).thenReturn(availablePlate);
        doReturn(null).when(shangpaiService).selectOne(Matchers.<Wrapper<ShangpaiEntity>>any());
        when(xuanpaiService.updateById(any(XuanpaiEntity.class))).thenReturn(true);

        R reserveResult = shangpaiService.reservePlate(1, 100);
        assertEquals(0, reserveResult.get("code"));

        // 模拟预占后号牌状态
        availablePlate.setZhuangtaiTypes(2);
        availablePlate.setYonghuId(100);
        availablePlate.setYuezhanTime(new Date());

        // 步骤2: 提交申请
        when(xuanpaiService.selectById(1)).thenReturn(availablePlate);
        when(baoxianService.selectById(10)).thenReturn(validInsurance);
        doReturn(null).when(shangpaiService).selectOne(Matchers.<Wrapper<ShangpaiEntity>>any());
        doReturn(true).when(shangpaiService).insert(any(ShangpaiEntity.class));

        R submitResult = shangpaiService.submitApplication(1, 10, 100);
        assertEquals(0, submitResult.get("code"));

        // 步骤3: 审核通过
        ShangpaiEntity newApp = new ShangpaiEntity();
        newApp.setId(1);
        newApp.setYonghuId(100);
        newApp.setXuanpaiId(1);
        newApp.setBaoxianId(10);
        newApp.setShangpaiTypes(1);

        doReturn(newApp).when(shangpaiService).selectById(1);
        doReturn(true).when(shangpaiService).updateById(any(ShangpaiEntity.class));

        R reviewResult = shangpaiService.reviewApplication(1, 2);
        assertEquals(0, reviewResult.get("code"));
    }
}
