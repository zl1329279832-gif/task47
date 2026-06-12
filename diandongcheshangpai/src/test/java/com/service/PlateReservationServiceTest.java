package com.service;

import com.baomidou.mybatisplus.mapper.EntityWrapper;
import com.baomidou.mybatisplus.service.impl.ServiceImpl;
import com.dao.XuanpaiDao;
import com.dao.YuyueJiluDao;
import com.entity.XuanpaiEntity;
import com.entity.YuyueJiluEntity;
import com.service.impl.XuanpaiServiceImpl;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.List;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

@RunWith(MockitoJUnitRunner.class)
public class PlateReservationServiceTest {

    @Mock
    private XuanpaiDao xuanpaiDao;

    @Mock
    private YuyueJiluDao yuyueJiluDao;

    @InjectMocks
    private XuanpaiServiceImpl xuanpaiService;

    @Before
    public void setUp() throws Exception {
        // Use reflection to set baseMapper in parent ServiceImpl class
        Field baseMapperField = ServiceImpl.class.getDeclaredField("baseMapper");
        baseMapperField.setAccessible(true);
        baseMapperField.set(xuanpaiService, xuanpaiDao);
    }

    private XuanpaiEntity createPlate(Integer id, String name, int status, Integer version) {
        XuanpaiEntity plate = new XuanpaiEntity();
        plate.setId(id);
        plate.setXuanpaiName(name);
        plate.setZhuangtaiTypes(status);
        plate.setVersion(version);
        return plate;
    }

    // ---- reservePlate tests ----

    @Test
    public void testReservePlate_Success() {
        Integer plateId = 1;
        Integer userId = 100;

        XuanpaiEntity plate = createPlate(plateId, "京A00001", 1, 0);

        when(xuanpaiDao.selectList(any(EntityWrapper.class))).thenReturn(Collections.emptyList());
        when(xuanpaiDao.selectById(plateId)).thenReturn(plate);
        when(xuanpaiDao.updateWithVersion(eq(plateId), eq(0), eq(2), eq(userId), any(Date.class)))
                .thenReturn(1);

        boolean result = xuanpaiService.reservePlate(plateId, userId, 30);

        assertTrue(result);
        verify(xuanpaiDao).updateWithVersion(eq(plateId), eq(0), eq(2), eq(userId), any(Date.class));

        ArgumentCaptor<YuyueJiluEntity> jiluCaptor = ArgumentCaptor.forClass(YuyueJiluEntity.class);
        verify(yuyueJiluDao).insert(jiluCaptor.capture());
        YuyueJiluEntity jilu = jiluCaptor.getValue();
        assertEquals(Integer.valueOf(1), jilu.getYuyueJiluTypes());
        assertEquals(userId, jilu.getYonghuId());
        assertEquals(plateId, jilu.getXuanpaiId());
    }

    @Test
    public void testReservePlate_PlateNotAvailable() {
        Integer plateId = 1;
        Integer userId = 100;

        XuanpaiEntity plate = createPlate(plateId, "京A00001", 2, 0);

        when(xuanpaiDao.selectList(any(EntityWrapper.class))).thenReturn(Collections.emptyList());
        when(xuanpaiDao.selectById(plateId)).thenReturn(plate);

        try {
            xuanpaiService.reservePlate(plateId, userId, 30);
            fail("Expected RuntimeException");
        } catch (RuntimeException e) {
            assertTrue(e.getMessage().contains("车牌当前不可预占"));
        }
    }

    @Test
    public void testReservePlate_UserAlreadyHasReservation() {
        Integer plateId = 1;
        Integer userId = 100;

        List<XuanpaiEntity> existing = new ArrayList<>();
        existing.add(createPlate(2, "京B00002", 2, 0));

        when(xuanpaiDao.selectList(any(EntityWrapper.class))).thenReturn(existing);

        try {
            xuanpaiService.reservePlate(plateId, userId, 30);
            fail("Expected RuntimeException");
        } catch (RuntimeException e) {
            assertTrue(e.getMessage().contains("该用户已有活跃的预占记录"));
        }
    }

    @Test
    public void testReservePlate_OptimisticLockConflict() {
        Integer plateId = 1;
        Integer userId = 100;

        XuanpaiEntity plate = createPlate(plateId, "京A00001", 1, 0);

        when(xuanpaiDao.selectList(any(EntityWrapper.class))).thenReturn(Collections.emptyList());
        when(xuanpaiDao.selectById(plateId)).thenReturn(plate);
        when(xuanpaiDao.updateWithVersion(eq(plateId), eq(0), eq(2), eq(userId), any(Date.class)))
                .thenReturn(0);

        try {
            xuanpaiService.reservePlate(plateId, userId, 30);
            fail("Expected RuntimeException");
        } catch (RuntimeException e) {
            assertTrue(e.getMessage().contains("预占失败"));
        }

        verify(yuyueJiluDao, never()).insert(any(YuyueJiluEntity.class));
    }

    @Test
    public void testReservePlate_PlateNotFound() {
        Integer plateId = 1;
        Integer userId = 100;

        when(xuanpaiDao.selectList(any(EntityWrapper.class))).thenReturn(Collections.emptyList());
        when(xuanpaiDao.selectById(plateId)).thenReturn(null);

        try {
            xuanpaiService.reservePlate(plateId, userId, 30);
            fail("Expected RuntimeException");
        } catch (RuntimeException e) {
            assertTrue(e.getMessage().contains("车牌不存在"));
        }
    }

    // ---- releasePlate tests ----

    @Test
    public void testCancelReservation_Success() {
        Integer plateId = 1;

        XuanpaiEntity plate = createPlate(plateId, "京A00001", 2, 1);
        plate.setYuyueUserId(100);
        Calendar cal = Calendar.getInstance();
        cal.add(Calendar.MINUTE, 30);
        plate.setYuyueExpireTime(cal.getTime());

        when(xuanpaiDao.selectById(plateId)).thenReturn(plate);

        boolean result = xuanpaiService.releasePlate(plateId);

        assertTrue(result);

        ArgumentCaptor<XuanpaiEntity> captor = ArgumentCaptor.forClass(XuanpaiEntity.class);
        verify(xuanpaiDao).updateById(captor.capture());
        XuanpaiEntity updated = captor.getValue();
        assertEquals(Integer.valueOf(1), updated.getZhuangtaiTypes());
        assertNull(updated.getYuyueUserId());
        assertNull(updated.getYuyueExpireTime());
        assertEquals(Integer.valueOf(2), updated.getVersion());
    }

    @Test
    public void testCancelReservation_PlateNotFound() {
        Integer plateId = 1;

        when(xuanpaiDao.selectById(plateId)).thenReturn(null);

        try {
            xuanpaiService.releasePlate(plateId);
            fail("Expected RuntimeException");
        } catch (RuntimeException e) {
            assertTrue(e.getMessage().contains("车牌不存在"));
        }
    }

    // ---- releaseExpiredReservations tests ----

    @Test
    public void testReleaseExpiredReservations_HasExpired() {
        XuanpaiEntity expired1 = createPlate(1, "京A00001", 2, 1);
        expired1.setYuyueUserId(100);
        XuanpaiEntity expired2 = createPlate(2, "京B00002", 2, 1);
        expired2.setYuyueUserId(200);

        List<XuanpaiEntity> expiredList = new ArrayList<>();
        expiredList.add(expired1);
        expiredList.add(expired2);

        when(xuanpaiDao.batchReleaseExpired()).thenReturn(2);
        when(xuanpaiDao.selectExpiredReservations()).thenReturn(expiredList);

        int result = xuanpaiService.releaseExpiredReservations();

        assertEquals(2, result);

        ArgumentCaptor<YuyueJiluEntity> jiluCaptor = ArgumentCaptor.forClass(YuyueJiluEntity.class);
        verify(yuyueJiluDao, times(2)).insert(jiluCaptor.capture());

        List<YuyueJiluEntity> jilus = jiluCaptor.getAllValues();
        assertEquals(Integer.valueOf(2), jilus.get(0).getYuyueJiluTypes());
        assertEquals(Integer.valueOf(100), jilus.get(0).getYonghuId());
        assertEquals(Integer.valueOf(1), jilus.get(0).getXuanpaiId());

        assertEquals(Integer.valueOf(2), jilus.get(1).getYuyueJiluTypes());
        assertEquals(Integer.valueOf(200), jilus.get(1).getYonghuId());
        assertEquals(Integer.valueOf(2), jilus.get(1).getXuanpaiId());
    }

    @Test
    public void testReleaseExpiredReservations_NoneExpired() {
        when(xuanpaiDao.selectExpiredReservations()).thenReturn(Collections.<XuanpaiEntity>emptyList());
        when(xuanpaiDao.batchReleaseExpired()).thenReturn(0);

        int result = xuanpaiService.releaseExpiredReservations();

        assertEquals(0, result);
        verify(yuyueJiluDao, never()).insert(any(YuyueJiluEntity.class));
    }

    // ---- getPlateWithLazyExpire tests ----

    @Test
    public void testGetPlateWithLazyExpire_NotExpired() {
        Integer plateId = 1;

        XuanpaiEntity plate = createPlate(plateId, "京A00001", 2, 1);
        Calendar cal = Calendar.getInstance();
        cal.add(Calendar.HOUR, 1); // 1 hour in the future
        plate.setYuyueExpireTime(cal.getTime());

        when(xuanpaiDao.selectById(plateId)).thenReturn(plate);

        XuanpaiEntity result = xuanpaiService.getPlateWithLazyExpire(plateId);

        assertNotNull(result);
        assertEquals(Integer.valueOf(2), result.getZhuangtaiTypes());
        verify(xuanpaiDao, times(1)).selectById(plateId);
        // releasePlate should NOT have been triggered
        verify(xuanpaiDao, never()).updateById(any(XuanpaiEntity.class));
    }

    @Test
    public void testGetPlateWithLazyExpire_Expired() {
        Integer plateId = 1;

        XuanpaiEntity expiredPlate = createPlate(plateId, "京A00001", 2, 1);
        Calendar cal = Calendar.getInstance();
        cal.add(Calendar.HOUR, -1); // 1 hour in the past
        expiredPlate.setYuyueExpireTime(cal.getTime());

        XuanpaiEntity releasedPlate = createPlate(plateId, "京A00001", 1, 2);

        when(xuanpaiDao.selectById(plateId))
                .thenReturn(expiredPlate)   // 1st call: getPlateWithLazyExpire
                .thenReturn(releasedPlate)  // 2nd call: releasePlate -> selectById
                .thenReturn(releasedPlate); // 3rd call: getPlateWithLazyExpire re-fetch

        XuanpaiEntity result = xuanpaiService.getPlateWithLazyExpire(plateId);

        assertNotNull(result);
        assertEquals(Integer.valueOf(1), result.getZhuangtaiTypes());
        verify(xuanpaiDao, times(3)).selectById(plateId);

        // Verify releasePlate was called (which triggers updateById)
        ArgumentCaptor<XuanpaiEntity> captor = ArgumentCaptor.forClass(XuanpaiEntity.class);
        verify(xuanpaiDao).updateById(captor.capture());
        XuanpaiEntity updated = captor.getValue();
        assertEquals(Integer.valueOf(1), updated.getZhuangtaiTypes());
        assertNull(updated.getYuyueUserId());
        assertNull(updated.getYuyueExpireTime());
    }

    @Test
    public void testGetPlateWithLazyExpire_Null() {
        Integer plateId = 1;

        when(xuanpaiDao.selectById(plateId)).thenReturn(null);

        XuanpaiEntity result = xuanpaiService.getPlateWithLazyExpire(plateId);

        assertNull(result);
        verify(xuanpaiDao, times(1)).selectById(plateId);
    }
}
