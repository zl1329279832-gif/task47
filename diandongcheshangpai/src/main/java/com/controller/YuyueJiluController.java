package com.controller;


import java.text.SimpleDateFormat;
import com.alibaba.fastjson.JSONObject;
import java.util.*;
import org.springframework.beans.BeanUtils;
import javax.servlet.http.HttpServletRequest;
import org.springframework.web.context.ContextLoader;
import javax.servlet.ServletContext;
import com.service.TokenService;
import com.utils.StringUtil;
import java.lang.reflect.InvocationTargetException;

import com.service.DictionaryService;
import org.apache.commons.lang3.StringUtils;
import com.annotation.IgnoreAuth;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import com.baomidou.mybatisplus.mapper.EntityWrapper;
import com.baomidou.mybatisplus.mapper.Wrapper;

import com.entity.YuyueJiluEntity;

import com.service.YuyueJiluService;
import com.entity.view.YuyueJiluView;
import com.service.XuanpaiService;
import com.entity.XuanpaiEntity;
import com.service.YonghuService;
import com.entity.YonghuEntity;

import com.utils.PageUtils;
import com.utils.R;

/**
 * 预占日志
 * 后端接口
 * @author
 * @email
 * @date 2021-04-27
*/
@RestController
@Controller
@RequestMapping("/yuyueJilu")
public class YuyueJiluController {
    private static final Logger logger = LoggerFactory.getLogger(YuyueJiluController.class);

    @Autowired
    private YuyueJiluService yuyueJiluService;


    @Autowired
    private TokenService tokenService;
    @Autowired
    private DictionaryService dictionaryService;



    //级联表service
    @Autowired
    private XuanpaiService xuanpaiService;
    @Autowired
    private YonghuService yonghuService;


    /**
    * 后端列表
    */
    @RequestMapping("/page")
    public R page(@RequestParam Map<String, Object> params, HttpServletRequest request){
        logger.debug("page方法:,,Controller:{},,params:{}",this.getClass().getName(),JSONObject.toJSONString(params));

        String role = String.valueOf(request.getSession().getAttribute("role"));
        if(StringUtil.isNotEmpty(role) && "用户".equals(role)){
            params.put("yonghuId",request.getSession().getAttribute("userId"));
        }
        params.put("orderBy","id");
        PageUtils page = yuyueJiluService.queryPage(params);

        //字典表数据转换
        List<YuyueJiluView> list =(List<YuyueJiluView>)page.getList();
        for(YuyueJiluView c:list){
            //修改对应字典表字段
            dictionaryService.dictionaryConvert(c);
        }
        return R.ok().put("data", page);
    }

    /**
    * 后端详情
    */
    @RequestMapping("/info/{id}")
    public R info(@PathVariable("id") Long id){
        logger.debug("info方法:,,Controller:{},,id:{}",this.getClass().getName(),id);
        YuyueJiluEntity yuyueJilu = yuyueJiluService.selectById(id);
        if(yuyueJilu !=null){
            //entity转view
            YuyueJiluView view = new YuyueJiluView();
            BeanUtils.copyProperties( yuyueJilu , view );//把实体数据重构到view中

            //级联表
            XuanpaiEntity xuanpai = xuanpaiService.selectById(yuyueJilu.getXuanpaiId());
            if(xuanpai != null){
                BeanUtils.copyProperties( xuanpai , view ,new String[]{ "id", "createDate"});//把级联的数据添加到view中,并排除id和创建时间字段
                view.setXuanpaiId(xuanpai.getId());
            }
            //级联表
            YonghuEntity yonghu = yonghuService.selectById(yuyueJilu.getYonghuId());
            if(yonghu != null){
                BeanUtils.copyProperties( yonghu , view ,new String[]{ "id", "createDate"});//把级联的数据添加到view中,并排除id和创建时间字段
                view.setYonghuId(yonghu.getId());
            }
            //修改对应字典表字段
            dictionaryService.dictionaryConvert(view);
            return R.ok().put("data", view);
        }else {
            return R.error(511,"查不到数据");
        }

    }

    /**
    * 删除
    */
    @RequestMapping("/delete")
    public R delete(Integer ids){
        logger.debug("delete:,,Controller:{},,ids:{}",this.getClass().getName(),ids.toString());
        yuyueJiluService.deleteBatchIds(Arrays.asList(ids));
        return R.ok();
    }


}
