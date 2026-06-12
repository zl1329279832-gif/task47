package com.service;

import com.dao.ShangpaiDao;
import com.dao.YuyueJiluDao;
import com.entity.BaoxianEntity;
import com.entity.ShangpaiEntity;
import com.entity.XuanpaiEntity;
import com.entity.YuyueJiluEntity;
import com.service.impl.ShangpaiServiceImpl;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import java.util.Date;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for {@link ShangpaiServiceImpl}
 * JUnit 4.12 + Mockito 2.28.2 + MyBatis-Plus v2.3
 */
@RunWith(MockitoJUnitRunner.class)
public class ShangpaiServiceTest {

    @Mock
    private ShangpaiDao shangpaiDao;

    @Mock
    private XuanpaiService xuanpaiService;

    @Mock
    private BaoxianService baoxianService;

    @Mock
    private YuyueJiluDao yuyueJiluDao;

    @InjectMocks
    private ShangpaiServiceImpl shangpaiService;

    @Before
    public void setUp() throws Exception {
        // ServiceImpl<ShangpaiDao, ShangpaiEntity> holds baseMapper as a protected field.
        // @InjectMocks cannot inject into the parent class field, so use reflection.
        java.lang.reflect.Field baseMapperField =
                com.baomidou.mybatisplus.service.impl.ServiceImpl.class.getDeclaredField("baseMapper");
        baseMapperField.setAccessible(true);
        baseMapperField.set(shangpaiService, shangpaiDao);
    }

    // -----------------------------------------------------------------------
    // 1. auditApplication - Approve (result = 2)
    // -----------------------------------------------------------------------
    @Test
    public void testAuditApplication_Approve() {
        // Arrange
        Integer shangpaiId = 100;
        Integer xuanpaiId = 200;
        Integer yonghuId = 300;

        ShangpaiEntity shangpai = new ShangpaiEntity();
        shangpai.setId(shangpaiId);
        shangpai.setXuanpaiId(xuanpaiId);
        shangpai.setYonghuId(yonghuId);
        shangpai.setShangpaiTypes(1); // pending

        XuanpaiEntity xuanpai = new XuanpaiEntity();
        xuanpai.setId(xuanpaiId);
        xuanpai.setZhuangtaiTypes(2); // reserved

        when(shangpaiDao.selectForUpdate(shangpaiId)).thenReturn(shangpai);
        when(xuanpaiService.selectById(xuanpaiId)).thenReturn(xuanpai);
        when(xuanpaiService.updateById(any(XuanpaiEntity.class))).thenReturn(true);
        when(shangpaiDao.updateById(any(ShangpaiEntity.class))).thenReturn(1);
        when(yuyueJiluDao.insert(any(YuyueJiluEntity.class))).thenReturn(1);

        // Act
        shangpaiService.auditApplication(shangpaiId, 2);

        // Assert - xuanpai status set to 3 (plate registered)
        ArgumentCaptor<XuanpaiEntity> xuanpaiCaptor = ArgumentCaptor.forClass(XuanpaiEntity.class);
        verify(xuanpaiService).updateById(xuanpaiCaptor.capture());
        assertEquals(Integer.valueOf(3), xuanpaiCaptor.getValue().getZhuangtaiTypes());
        assertNull(xuanpaiCaptor.getValue().getYuyueUserId());
        assertNull(xuanpaiCaptor.getValue().getYuyueExpireTime());

        // Assert - shangpai status set to 2 (approved)
        ArgumentCaptor<ShangpaiEntity> shangpaiCaptor = ArgumentCaptor.forClass(ShangpaiEntity.class);
        verify(shangpaiDao).updateById(shangpaiCaptor.capture());
        assertEquals(Integer.valueOf(2), shangpaiCaptor.getValue().getShangpaiTypes());

        // Assert - yuyue jilu inserted with type 4 (approve log)
        ArgumentCaptor<YuyueJiluEntity> jiluCaptor = ArgumentCaptor.forClass(YuyueJiluEntity.class);
        verify(yuyueJiluDao).insert(jiluCaptor.capture());
        assertEquals(Integer.valueOf(4), jiluCaptor.getValue().getYuyueJiluTypes());
        assertEquals(yonghuId, jiluCaptor.getValue().getYonghuId());
        assertEquals(xuanpaiId, jiluCaptor.getValue().getXuanpaiId());
        assertNotNull(jiluCaptor.getValue().getInsertTime());
    }

    // -----------------------------------------------------------------------
    // 2. auditApplication - Reject (result = 3)
    // -----------------------------------------------------------------------
    @Test
    public void testAuditApplication_Reject() {
        // Arrange
        Integer shangpaiId = 101;
        Integer xuanpaiId = 201;
        Integer yonghuId = 301;

        ShangpaiEntity shangpai = new ShangpaiEntity();
        shangpai.setId(shangpaiId);
        shangpai.setXuanpaiId(xuanpaiId);
        shangpai.setYonghuId(yonghuId);
        shangpai.setShangpaiTypes(1); // pending

        XuanpaiEntity xuanpai = new XuanpaiEntity();
        xuanpai.setId(xuanpaiId);
        xuanpai.setZhuangtaiTypes(2); // reserved

        when(shangpaiDao.selectForUpdate(shangpaiId)).thenReturn(shangpai);
        when(xuanpaiService.selectById(xuanpaiId)).thenReturn(xuanpai);
        when(xuanpaiService.updateById(any(XuanpaiEntity.class))).thenReturn(true);
        when(shangpaiDao.updateById(any(ShangpaiEntity.class))).thenReturn(1);
        when(yuyueJiluDao.insert(any(YuyueJiluEntity.class))).thenReturn(1);

        // Act
        shangpaiService.auditApplication(shangpaiId, 3);

        // Assert - xuanpai status set to 1 (available again)
        ArgumentCaptor<XuanpaiEntity> xuanpaiCaptor = ArgumentCaptor.forClass(XuanpaiEntity.class);
        verify(xuanpaiService).updateById(xuanpaiCaptor.capture());
        assertEquals(Integer.valueOf(1), xuanpaiCaptor.getValue().getZhuangtaiTypes());
        assertNull(xuanpaiCaptor.getValue().getYuyueUserId());
        assertNull(xuanpaiCaptor.getValue().getYuyueExpireTime());

        // Assert - shangpai status set to 3 (rejected)
        ArgumentCaptor<ShangpaiEntity> shangpaiCaptor = ArgumentCaptor.forClass(ShangpaiEntity.class);
        verify(shangpaiDao).updateById(shangpaiCaptor.capture());
        assertEquals(Integer.valueOf(3), shangpaiCaptor.getValue().getShangpaiTypes());

        // Assert - yuyue jilu inserted with type 3 (reject log)
        ArgumentCaptor<YuyueJiluEntity> jiluCaptor = ArgumentCaptor.forClass(YuyueJiluEntity.class);
        verify(yuyueJiluDao).insert(jiluCaptor.capture());
        assertEquals(Integer.valueOf(3), jiluCaptor.getValue().getYuyueJiluTypes());
        assertEquals(yonghuId, jiluCaptor.getValue().getYonghuId());
        assertEquals(xuanpaiId, jiluCaptor.getValue().getXuanpaiId());
        assertNotNull(jiluCaptor.getValue().getInsertTime());
    }

    // -----------------------------------------------------------------------
    // 3. auditApplication - Already Audited (shangpaiTypes != 1)
    // -----------------------------------------------------------------------
    @Test
    public void testAuditApplication_AlreadyAudited() {
        // Arrange
        Integer shangpaiId = 102;

        ShangpaiEntity shangpai = new ShangpaiEntity();
        shangpai.setId(shangpaiId);
        shangpai.setShangpaiTypes(2); // already approved

        when(shangpaiDao.selectForUpdate(shangpaiId)).thenReturn(shangpai);

        // Act & Assert
        try {
            shangpaiService.auditApplication(shangpaiId, 2);
            fail("Expected RuntimeException for already audited application");
        } catch (RuntimeException e) {
            assertEquals("申请已审核，不可重复操作", e.getMessage());
        }

        // Verify no updates were performed
        verify(xuanpaiService, never()).selectById(anyInt());
        verify(xuanpaiService, never()).updateById(any(XuanpaiEntity.class));
        verify(shangpaiDao, never()).updateById(any(ShangpaiEntity.class));
        verify(yuyueJiluDao, never()).insert(any(YuyueJiluEntity.class));
    }

    // -----------------------------------------------------------------------
    // 4. auditApplication - Not Found (selectForUpdate returns null)
    // -----------------------------------------------------------------------
    @Test
    public void testAuditApplication_NotFound() {
        // Arrange
        Integer shangpaiId = 999;
        when(shangpaiDao.selectForUpdate(shangpaiId)).thenReturn(null);

        // Act & Assert
        try {
            shangpaiService.auditApplication(shangpaiId, 2);
            fail("Expected RuntimeException for non-existent application");
        } catch (RuntimeException e) {
            assertEquals("申请不存在", e.getMessage());
        }

        // Verify no downstream calls
        verify(xuanpaiService, never()).selectById(anyInt());
        verify(xuanpaiService, never()).updateById(any(XuanpaiEntity.class));
        verify(shangpaiDao, never()).updateById(any(ShangpaiEntity.class));
        verify(yuyueJiluDao, never()).insert(any(YuyueJiluEntity.class));
    }

    // -----------------------------------------------------------------------
    // 5. submitApplication - Success
    // -----------------------------------------------------------------------
    @Test
    public void testSubmitApplication_Success() {
        // Arrange
        Integer xuanpaiId = 200;
        Integer baoxianId = 400;
        Integer yonghuId = 300;

        XuanpaiEntity xuanpai = new XuanpaiEntity();
        xuanpai.setId(xuanpaiId);
        xuanpai.setZhuangtaiTypes(2); // reserved
        xuanpai.setYuyueUserId(yonghuId);
        // Expire time in the future (1 hour from now)
        xuanpai.setYuyueExpireTime(new Date(System.currentTimeMillis() + 3600_000));

        BaoxianEntity baoxian = new BaoxianEntity();
        baoxian.setId(baoxianId);
        baoxian.setBaoxianName("Test Insurance");

        when(xuanpaiService.selectById(xuanpaiId)).thenReturn(xuanpai);
        when(baoxianService.selectById(baoxianId)).thenReturn(baoxian);
        when(shangpaiDao.insert(any(ShangpaiEntity.class))).thenReturn(1);
        when(yuyueJiluDao.insert(any(YuyueJiluEntity.class))).thenReturn(1);

        // Act
        ShangpaiEntity result = shangpaiService.submitApplication(xuanpaiId, baoxianId, yonghuId);

        // Assert - return value is non-null
        assertNotNull(result);
        assertEquals(yonghuId, result.getYonghuId());
        assertEquals(xuanpaiId, result.getXuanpaiId());
        assertEquals(baoxianId, result.getBaoxianId());
        assertEquals(Integer.valueOf(1), result.getShangpaiTypes());
        assertNotNull(result.getInsertTime());

        // Assert - shangpaiDao.insert called with correct entity
        ArgumentCaptor<ShangpaiEntity> shangpaiCaptor = ArgumentCaptor.forClass(ShangpaiEntity.class);
        verify(shangpaiDao).insert(shangpaiCaptor.capture());
        ShangpaiEntity insertedShangpai = shangpaiCaptor.getValue();
        assertEquals(yonghuId, insertedShangpai.getYonghuId());
        assertEquals(xuanpaiId, insertedShangpai.getXuanpaiId());
        assertEquals(baoxianId, insertedShangpai.getBaoxianId());
        assertEquals(Integer.valueOf(1), insertedShangpai.getShangpaiTypes());
        assertNotNull(insertedShangpai.getInsertTime());

        // Assert - yuyueJiluDao.insert called with type 4
        ArgumentCaptor<YuyueJiluEntity> jiluCaptor = ArgumentCaptor.forClass(YuyueJiluEntity.class);
        verify(yuyueJiluDao).insert(jiluCaptor.capture());
        assertEquals(Integer.valueOf(4), jiluCaptor.getValue().getYuyueJiluTypes());
        assertEquals(yonghuId, jiluCaptor.getValue().getYonghuId());
        assertEquals(xuanpaiId, jiluCaptor.getValue().getXuanpaiId());
        assertNotNull(jiluCaptor.getValue().getInsertTime());
    }

    // -----------------------------------------------------------------------
    // 6. submitApplication - No Reservation (plate status != 2)
    // -----------------------------------------------------------------------
    @Test
    public void testSubmitApplication_NoReservation() {
        // Arrange
        Integer xuanpaiId = 201;
        Integer baoxianId = 401;
        Integer yonghuId = 301;

        XuanpaiEntity xuanpai = new XuanpaiEntity();
        xuanpai.setId(xuanpaiId);
        xuanpai.setZhuangtaiTypes(1); // available, not reserved

        when(xuanpaiService.selectById(xuanpaiId)).thenReturn(xuanpai);

        // Act & Assert
        try {
            shangpaiService.submitApplication(xuanpaiId, baoxianId, yonghuId);
            fail("Expected RuntimeException for non-reserved plate");
        } catch (RuntimeException e) {
            assertEquals("车牌当前不可申请", e.getMessage());
        }

        // Verify no downstream operations
        verify(baoxianService, never()).selectById(anyInt());
        verify(shangpaiDao, never()).insert(any(ShangpaiEntity.class));
        verify(yuyueJiluDao, never()).insert(any(YuyueJiluEntity.class));
    }

    // -----------------------------------------------------------------------
    // 7. submitApplication - Expired Reservation
    // -----------------------------------------------------------------------
    @Test
    public void testSubmitApplication_ExpiredReservation() {
        // Arrange
        Integer xuanpaiId = 202;
        Integer baoxianId = 402;
        Integer yonghuId = 302;

        XuanpaiEntity xuanpai = new XuanpaiEntity();
        xuanpai.setId(xuanpaiId);
        xuanpai.setZhuangtaiTypes(2); // reserved
        xuanpai.setYuyueUserId(yonghuId);
        // Expire time in the past (1 hour ago)
        xuanpai.setYuyueExpireTime(new Date(System.currentTimeMillis() - 3600_000));

        when(xuanpaiService.selectById(xuanpaiId)).thenReturn(xuanpai);

        // Act & Assert
        try {
            shangpaiService.submitApplication(xuanpaiId, baoxianId, yonghuId);
            fail("Expected RuntimeException for expired reservation");
        } catch (RuntimeException e) {
            assertEquals("预占已过期，请重新预占", e.getMessage());
        }

        // Verify no downstream operations
        verify(baoxianService, never()).selectById(anyInt());
        verify(shangpaiDao, never()).insert(any(ShangpaiEntity.class));
        verify(yuyueJiluDao, never()).insert(any(YuyueJiluEntity.class));
    }

    // -----------------------------------------------------------------------
    // 8. submitApplication - Invalid Insurance (baoxianService returns null)
    // -----------------------------------------------------------------------
    @Test
    public void testSubmitApplication_InvalidInsurance() {
        // Arrange
        Integer xuanpaiId = 203;
        Integer baoxianId = 403;
        Integer yonghuId = 303;

        XuanpaiEntity xuanpai = new XuanpaiEntity();
        xuanpai.setId(xuanpaiId);
        xuanpai.setZhuangtaiTypes(2); // reserved
        xuanpai.setYuyueUserId(yonghuId);
        // Expire time in the future
        xuanpai.setYuyueExpireTime(new Date(System.currentTimeMillis() + 3600_000));

        when(xuanpaiService.selectById(xuanpaiId)).thenReturn(xuanpai);
        when(baoxianService.selectById(baoxianId)).thenReturn(null);

        // Act & Assert
        try {
            shangpaiService.submitApplication(xuanpaiId, baoxianId, yonghuId);
            fail("Expected RuntimeException for non-existent insurance");
        } catch (RuntimeException e) {
            assertEquals("保险不存在", e.getMessage());
        }

        // Verify no insert operations
        verify(shangpaiDao, never()).insert(any(ShangpaiEntity.class));
        verify(yuyueJiluDao, never()).insert(any(YuyueJiluEntity.class));
    }
}
