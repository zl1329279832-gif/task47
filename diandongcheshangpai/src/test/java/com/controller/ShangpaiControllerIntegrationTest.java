package com.controller;

import com.alibaba.fastjson.JSONObject;
import com.entity.*;
import com.entity.view.ShangpaiView;
import com.service.*;
import com.utils.PageUtils;
import com.utils.R;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * ShangpaiController integration tests using MockMvc standalone setup.
 * No Spring context or database required -- all services are mocked via Mockito.
 */
@RunWith(MockitoJUnitRunner.class)
public class ShangpaiControllerIntegrationTest {

    private MockMvc mockMvc;

    @InjectMocks
    private ShangpaiController shangpaiController;

    @Mock
    private ShangpaiService shangpaiService;

    @Mock
    private XuanpaiService xuanpaiService;

    @Mock
    private YonghuService yonghuService;

    @Mock
    private BaoxianService baoxianService;

    @Mock
    private TokenService tokenService;

    @Mock
    private DictionaryService dictionaryService;

    @Before
    public void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(shangpaiController).build();
    }

    // ---------------------------------------------------------------
    // 1. POST /shangpai/apply -- successful application submission
    // ---------------------------------------------------------------
    @Test
    public void testApply_Success() throws Exception {
        // Arrange: build the request payload
        ShangpaiEntity inputEntity = new ShangpaiEntity();
        inputEntity.setXuanpaiId(1);
        inputEntity.setBaoxianId(2);

        // Expected result returned by the service
        ShangpaiEntity expectedResult = new ShangpaiEntity();
        expectedResult.setId(10);
        expectedResult.setXuanpaiId(1);
        expectedResult.setBaoxianId(2);
        expectedResult.setYonghuId(1);
        expectedResult.setShangpaiTypes(1);
        expectedResult.setInsertTime(new Date());
        expectedResult.setCreateTime(new Date());

        when(shangpaiService.submitApplication(eq(1), eq(2), eq(1)))
                .thenReturn(expectedResult);

        // Act & Assert
        mockMvc.perform(post("/shangpai/apply")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(JSONObject.toJSONString(inputEntity))
                        .sessionAttr("userId", 1))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data").exists());

        verify(shangpaiService).submitApplication(1, 2, 1);
    }

    // ---------------------------------------------------------------
    // 2. POST /shangpai/apply -- service throws RuntimeException
    // ---------------------------------------------------------------
    @Test
    public void testApply_NoReservation() throws Exception {
        // Arrange: service throws an exception (e.g. plate not reserved)
        when(shangpaiService.submitApplication(any(), any(), any()))
                .thenThrow(new RuntimeException("未预约号牌，请先预约"));

        ShangpaiEntity inputEntity = new ShangpaiEntity();
        inputEntity.setXuanpaiId(1);
        inputEntity.setBaoxianId(2);

        // Act & Assert: the controller catches the exception and returns code 500
        mockMvc.perform(post("/shangpai/apply")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(JSONObject.toJSONString(inputEntity))
                        .sessionAttr("userId", 1))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(500))
                .andExpect(jsonPath("$.msg").value("未预约号牌，请先预约"));
    }

    // ---------------------------------------------------------------
    // 3. GET /shangpai/progress -- successful progress list query
    // ---------------------------------------------------------------
    @Test
    public void testProgress_Success() throws Exception {
        // Arrange: queryPage returns a PageUtils wrapping an empty ShangpaiView list
        PageUtils pageUtils = new PageUtils(new ArrayList<ShangpaiView>(), 0, 10, 1);

        when(shangpaiService.queryPage(any())).thenReturn(pageUtils);

        // Act & Assert
        mockMvc.perform(get("/shangpai/progress")
                        .sessionAttr("userId", 1))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data").exists())
                .andExpect(jsonPath("$.data.total").value(0));

        verify(shangpaiService).queryPage(any());
    }

    // ---------------------------------------------------------------
    // 4. GET /shangpai/progress/{id} -- access denied for other user's application
    // ---------------------------------------------------------------
    @Test
    public void testProgressDetail_AccessControl() throws Exception {
        // Arrange: the requested shangpai belongs to a DIFFERENT user (yonghuId=2)
        ShangpaiEntity shangpai = new ShangpaiEntity();
        shangpai.setId(1);
        shangpai.setYonghuId(2);   // belongs to user 2
        shangpai.setXuanpaiId(10);
        shangpai.setShangpaiTypes(1);

        when(shangpaiService.selectById(1L)).thenReturn(shangpai);

        // Act & Assert: current user (userId=1, role="用户") should be denied
        mockMvc.perform(get("/shangpai/progress/1")
                        .sessionAttr("userId", 1)
                        .sessionAttr("role", "用户"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(511))
                .andExpect(jsonPath("$.msg").value("无权查看他人申请"));

        // The service should still have been called to look up the entity
        verify(shangpaiService).selectById(1L);
        // But cascading lookups should NOT be invoked because access was denied
        verify(xuanpaiService, never()).selectById(any());
        verify(yonghuService, never()).selectById(any());
    }

    // ---------------------------------------------------------------
    // 5. GET /shangpai/progress/{id} -- own application, access granted
    // ---------------------------------------------------------------
    @Test
    public void testProgressDetail_OwnApplication() throws Exception {
        // Arrange: the requested shangpai belongs to the SAME user (yonghuId=1)
        ShangpaiEntity shangpai = new ShangpaiEntity();
        shangpai.setId(1);
        shangpai.setYonghuId(1);   // belongs to user 1 (same as session)
        shangpai.setXuanpaiId(10);
        shangpai.setShangpaiTypes(1);

        when(shangpaiService.selectById(1L)).thenReturn(shangpai);

        // Cascading entity lookups
        XuanpaiEntity xuanpai = new XuanpaiEntity();
        xuanpai.setId(10);
        when(xuanpaiService.selectById(10)).thenReturn(xuanpai);

        YonghuEntity yonghu = new YonghuEntity();
        yonghu.setId(1);
        when(yonghuService.selectById(1)).thenReturn(yonghu);

        // Act & Assert: current user (userId=1, role="用户") should get full data
        mockMvc.perform(get("/shangpai/progress/1")
                        .sessionAttr("userId", 1)
                        .sessionAttr("role", "用户"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data").exists());

        // Verify all cascading lookups were invoked
        verify(shangpaiService).selectById(1L);
        verify(xuanpaiService).selectById(10);
        verify(yonghuService).selectById(1);
        verify(dictionaryService).dictionaryConvert(any());
    }
}
