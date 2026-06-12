package com.entity.view;

import com.entity.YuyueJiluEntity;

import com.baomidou.mybatisplus.annotations.TableName;
import org.apache.commons.beanutils.BeanUtils;
import java.lang.reflect.InvocationTargetException;
import org.springframework.format.annotation.DateTimeFormat;
import com.fasterxml.jackson.annotation.JsonFormat;

import java.io.Serializable;
import java.util.Date;

/**
 * 预占日志
 * 后端返回视图实体辅助类
 * （通常后端关联的表或者自定义的字段需要返回使用）
 * @author
 * @email
 * @date 2021-04-27
 */
@TableName("yuyue_jilu")
public class YuyueJiluView extends YuyueJiluEntity implements Serializable {
    private static final long serialVersionUID = 1L;
		/**
		* 预占日志类型的值
		*/
		private String yuyueJiluValue;



		//级联表 xuanpai
			/**
			* 车牌号
			*/
			private String xuanpaiName;
			/**
			* 车牌类型
			*/
			private Integer xuanpaiTypes;
				/**
				* 车牌类型的值
				*/
				private String xuanpaiValue;
			/**
			* 车牌状态
			*/
			private Integer zhuangtaiTypes;
				/**
				* 车牌状态的值
				*/
				private String zhuangtaiValue;

		//级联表 yonghu
			/**
			* 用户姓名
			*/
			private String yonghuName;
			/**
			* 手机号
			*/
			private String yonghuPhone;

	public YuyueJiluView() {

	}

	public YuyueJiluView(YuyueJiluEntity yuyueJiluEntity) {
		try {
			BeanUtils.copyProperties(this, yuyueJiluEntity);
		} catch (IllegalAccessException | InvocationTargetException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}



			/**
			* 获取： 预占日志类型的值
			*/
			public String getYuyueJiluValue() {
				return yuyueJiluValue;
			}
			/**
			* 设置： 预占日志类型的值
			*/
			public void setYuyueJiluValue(String yuyueJiluValue) {
				this.yuyueJiluValue = yuyueJiluValue;
			}




			//级联表的get和set xuanpai
				/**
				* 获取： 车牌号
				*/
				public String getXuanpaiName() {
					return xuanpaiName;
				}
				/**
				* 设置： 车牌号
				*/
				public void setXuanpaiName(String xuanpaiName) {
					this.xuanpaiName = xuanpaiName;
				}
				/**
				* 获取： 车牌类型
				*/
				public Integer getXuanpaiTypes() {
					return xuanpaiTypes;
				}
				/**
				* 设置： 车牌类型
				*/
				public void setXuanpaiTypes(Integer xuanpaiTypes) {
					this.xuanpaiTypes = xuanpaiTypes;
				}


					/**
					* 获取： 车牌类型的值
					*/
					public String getXuanpaiValue() {
						return xuanpaiValue;
					}
					/**
					* 设置： 车牌类型的值
					*/
					public void setXuanpaiValue(String xuanpaiValue) {
						this.xuanpaiValue = xuanpaiValue;
					}
				/**
				* 获取： 车牌状态
				*/
				public Integer getZhuangtaiTypes() {
					return zhuangtaiTypes;
				}
				/**
				* 设置： 车牌状态
				*/
				public void setZhuangtaiTypes(Integer zhuangtaiTypes) {
					this.zhuangtaiTypes = zhuangtaiTypes;
				}


					/**
					* 获取： 车牌状态的值
					*/
					public String getZhuangtaiValue() {
						return zhuangtaiValue;
					}
					/**
					* 设置： 车牌状态的值
					*/
					public void setZhuangtaiValue(String zhuangtaiValue) {
						this.zhuangtaiValue = zhuangtaiValue;
					}


			//级联表的get和set yonghu
				/**
				* 获取： 用户姓名
				*/
				public String getYonghuName() {
					return yonghuName;
				}
				/**
				* 设置： 用户姓名
				*/
				public void setYonghuName(String yonghuName) {
					this.yonghuName = yonghuName;
				}
				/**
				* 获取： 手机号
				*/
				public String getYonghuPhone() {
					return yonghuPhone;
				}
				/**
				* 设置： 手机号
				*/
				public void setYonghuPhone(String yonghuPhone) {
					this.yonghuPhone = yonghuPhone;
				}


}
