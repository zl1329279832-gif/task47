package com.controller;

import com.entity.BaoxianEntity;
import com.entity.ShangpaiEntity;
import com.entity.XuanpaiEntity;
import com.entity.YonghuEntity;
import com.entity.view.ShangpaiView;
import com.service.*;
import com.utils.PageUtils;
import com.utils.R;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;
import java.util.*;

import static org.junit.Assert.*;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.*;

/**
 * ShangpaiController 单元测试
 * 覆盖：接口鉴权、进度查询权限隔离、管理员审核权限
 */
@RunWith(MockitoJUnitRunner.class)
public class ShangpaiControllerTest {

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
    @Mock
    private HttpServletRequest request;
    @Mock
    private HttpSession session;

    @InjectMocks
    private ShangpaiController shangpaiController;

    @Before
    public void setUp() {
        when(request.getSession()).thenReturn(session);
    }

    // ==================== 选牌预占接口测试 ====================

    @Test
    public void testXuanzhe_notLoggedIn() {
        when(session.getAttribute("userId")).thenReturn(null);

        R result = shangpaiController.xuanzhe(1, request);
        assertEquals(401, result.get("code"));
    }

    @Test
    public void testXuanzhe_success() {
        when(session.getAttribute("userId")).thenReturn(100);
        when(shangpaiService.reservePlate(1, 100)).thenReturn(R.ok("选牌预占成功，请在30分钟内提交上牌申请"));

        R result = shangpaiController.xuanzhe(1, request);
        assertEquals(0, result.get("code"));
        verify(shangpaiService).reservePlate(1, 100);
    }

    // ==================== 提交申请接口测试 ====================

    @Test
    public void testTijiao_notLoggedIn() {
        when(session.getAttribute("userId")).thenReturn(null);

        R result = shangpaiController.tijiao(1, 10, request);
        assertEquals(401, result.get("code"));
    }

    @Test
    public void testTijiao_success() {
        when(session.getAttribute("userId")).thenReturn(100);
        when(shangpaiService.submitApplication(1, 10, 100)).thenReturn(R.ok("上牌申请提交成功，请等待审核"));

        R result = shangpaiController.tijiao(1, 10, request);
        assertEquals(0, result.get("code"));
        verify(shangpaiService).submitApplication(1, 10, 100);
    }

    // ==================== 申请进度查询测试 ====================

    @Test
    public void testJindu_notLoggedIn() {
        when(session.getAttribute("userId")).thenReturn(null);

        R result = shangpaiController.jindu(1L, request);
        assertEquals(401, result.get("code"));
    }

    @Test
    public void testJindu_applicationNotFound() {
        when(session.getAttribute("userId")).thenReturn(100);
        when(shangpaiService.selectById(1L)).thenReturn(null);

        R result = shangpaiController.jindu(1L, request);
        assertEquals(511, result.get("code"));
        assertEquals("申请记录不存在", result.get("msg"));
    }

    @Test
    public void testJindu_viewOtherUserApplication_forbidden() {
        when(session.getAttribute("userId")).thenReturn(200); // 当前用户200

        ShangpaiEntity otherUserApp = new ShangpaiEntity();
        otherUserApp.setId(1);
        otherUserApp.setYonghuId(100); // 申请属于用户100
        otherUserApp.setXuanpaiId(1);
        otherUserApp.setShangpaiTypes(1);
        when(shangpaiService.selectById(1L)).thenReturn(otherUserApp);

        R result = shangpaiController.jindu(1L, request);
        assertEquals(511, result.get("code"));
        assertEquals("无权查看他人的上牌申请", result.get("msg"));
    }

    @Test
    public void testJindu_viewOwnApplication_success() {
        when(session.getAttribute("userId")).thenReturn(100);

        ShangpaiEntity myApp = new ShangpaiEntity();
        myApp.setId(1);
        myApp.setYonghuId(100);
        myApp.setXuanpaiId(2);
        myApp.setBaoxianId(10);
        myApp.setShangpaiTypes(1);
        when(shangpaiService.selectById(1L)).thenReturn(myApp);

        XuanpaiEntity plate = new XuanpaiEntity();
        plate.setId(2);
        plate.setXuanpaiName("京A12345");
        plate.setZhuangtaiTypes(2);
        when(xuanpaiService.selectById(2)).thenReturn(plate);

        YonghuEntity user = new YonghuEntity();
        user.setId(100);
        user.setYonghuName("张三");
        when(yonghuService.selectById(100)).thenReturn(user);

        BaoxianEntity insurance = new BaoxianEntity();
        insurance.setId(10);
        insurance.setBaoxianName("交强险");
        insurance.setBaoxianTypes(1);
        insurance.setBaoxianMoney(950.0);
        when(baoxianService.selectById(10)).thenReturn(insurance);

        R result = shangpaiController.jindu(1L, request);
        assertEquals(0, result.get("code"));
        assertNotNull(result.get("data"));

        ShangpaiView view = (ShangpaiView) result.get("data");
        assertEquals("京A12345", view.getXuanpaiName());
        assertEquals("交强险", view.getBaoxianName());
    }

    // ==================== 审核接口测试 ====================

    @Test
    public void testShenhe_notAdmin() {
        when(session.getAttribute("role")).thenReturn("用户");

        R result = shangpaiController.shenhe(1, 2, request);
        assertEquals(511, result.get("code"));
        assertEquals("仅管理员可审核上牌申请", result.get("msg"));
    }

    @Test
    public void testShenhe_adminApprove() {
        when(session.getAttribute("role")).thenReturn("管理员");
        when(shangpaiService.reviewApplication(1, 2)).thenReturn(R.ok("审核通过"));

        R result = shangpaiController.shenhe(1, 2, request);
        assertEquals(0, result.get("code"));
        verify(shangpaiService).reviewApplication(1, 2);
    }

    @Test
    public void testShenhe_adminReject() {
        when(session.getAttribute("role")).thenReturn("管理员");
        when(shangpaiService.reviewApplication(1, 3)).thenReturn(R.ok("审核已拒绝，号牌已释放"));

        R result = shangpaiController.shenhe(1, 3, request);
        assertEquals(0, result.get("code"));
        verify(shangpaiService).reviewApplication(1, 3);
    }

    // ==================== 可选号牌列表测试 ====================

    @Test
    public void testAvailablePlates_filtersAvailableOnly() {
        Map<String, Object> params = new HashMap<String, Object>();
        List<Object> emptyList = new ArrayList<Object>();
        PageUtils page = new PageUtils(emptyList, 0, 10, 1);
        when(xuanpaiService.queryPage(any(Map.class))).thenReturn(page);

        R result = shangpaiController.availablePlates(params, request);
        assertEquals(0, result.get("code"));
        // 验证参数中 zhuangtaiTypes 被设置为1
        assertEquals(1, params.get("zhuangtaiTypes"));
    }

    // ==================== 后端列表权限隔离测试 ====================

    @Test
    public void testPage_userOnlySeesOwnRecords() {
        when(session.getAttribute("role")).thenReturn("用户");
        when(session.getAttribute("userId")).thenReturn(100);

        Map<String, Object> params = new HashMap<String, Object>();
        List<ShangpaiView> emptyList = new ArrayList<ShangpaiView>();
        PageUtils page = new PageUtils(emptyList, 0, 10, 1);
        when(shangpaiService.queryPage(any(Map.class))).thenReturn(page);

        shangpaiController.page(params, request);

        // 验证用户ID被注入到查询参数中
        assertEquals(100, params.get("yonghuId"));
    }

    // ==================== info/detail 保险级联测试 ====================

    @Test
    public void testInfo_withInsuranceCascade() {
        ShangpaiEntity app = new ShangpaiEntity();
        app.setId(1);
        app.setYonghuId(100);
        app.setXuanpaiId(2);
        app.setBaoxianId(10);
        app.setShangpaiTypes(2);
        when(shangpaiService.selectById(1L)).thenReturn(app);

        XuanpaiEntity plate = new XuanpaiEntity();
        plate.setId(2);
        plate.setXuanpaiName("京A12345");
        when(xuanpaiService.selectById(2)).thenReturn(plate);

        YonghuEntity user = new YonghuEntity();
        user.setId(100);
        when(yonghuService.selectById(100)).thenReturn(user);

        BaoxianEntity insurance = new BaoxianEntity();
        insurance.setId(10);
        insurance.setBaoxianName("第三者责任险");
        insurance.setBaoxianTypes(2);
        insurance.setBaoxianMoney(1200.0);
        when(baoxianService.selectById(10)).thenReturn(insurance);

        R result = shangpaiController.info(1L);
        assertEquals(0, result.get("code"));

        ShangpaiView view = (ShangpaiView) result.get("data");
        assertEquals("第三者责任险", view.getBaoxianName());
        assertEquals(Integer.valueOf(2), view.getBaoxianTypes());
        assertEquals(Double.valueOf(1200.0), view.getBaoxianMoney());
    }
}
