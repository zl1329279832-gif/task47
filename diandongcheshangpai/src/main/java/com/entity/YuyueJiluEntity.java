package com.entity;

import com.baomidou.mybatisplus.annotations.TableId;
import com.baomidou.mybatisplus.annotations.TableName;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.lang.reflect.InvocationTargetException;

import java.io.Serializable;
import java.util.Date;
import java.util.List;

import org.springframework.format.annotation.DateTimeFormat;
import com.fasterxml.jackson.annotation.JsonFormat;
import org.apache.commons.beanutils.BeanUtils;
import com.baomidou.mybatisplus.annotations.TableField;
import com.baomidou.mybatisplus.enums.FieldFill;
import com.baomidou.mybatisplus.enums.IdType;

/**
 * 预占日志
 *
 * @author
 * @email
 * @date 2021-04-27
 */
@TableName("yuyue_jilu")
public class YuyueJiluEntity<T> implements Serializable {
    private static final long serialVersionUID = 1L;


	public YuyueJiluEntity() {

	}

	public YuyueJiluEntity(T t) {
		try {
			BeanUtils.copyProperties(this, t);
		} catch (IllegalAccessException | InvocationTargetException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}


    /**
     * 主键
     */
    @TableId(type = IdType.AUTO)
    @TableField(value = "id")

    private Integer id;


    /**
     * 预占用户
     */
    @TableField(value = "yonghu_id")

    private Integer yonghuId;


    /**
     * 所选牌号
     */
    @TableField(value = "xuanpai_id")

    private Integer xuanpaiId;


    /**
     * 预占日志类型
     */
    @TableField(value = "yuyue_jilu_types")

    private Integer yuyueJiluTypes;


    /**
     * 操作时间
     */
    @JsonFormat(locale="zh", timezone="GMT+8", pattern="yyyy-MM-dd HH:mm:ss")
	@DateTimeFormat
    @TableField(value = "insert_time",fill = FieldFill.INSERT)

    private Date insertTime;


    /**
     * 创建时间
     */
    @JsonFormat(locale="zh", timezone="GMT+8", pattern="yyyy-MM-dd HH:mm:ss")
	@DateTimeFormat
    @TableField(value = "create_time",fill = FieldFill.INSERT)

    private Date createTime;


    /**
	 * 设置：主键
	 */
    public Integer getId() {
        return id;
    }


    /**
	 * 获取：主键
	 */

    public void setId(Integer id) {
        this.id = id;
    }
    /**
	 * 设置：预占用户
	 */
    public Integer getYonghuId() {
        return yonghuId;
    }


    /**
	 * 获取：预占用户
	 */

    public void setYonghuId(Integer yonghuId) {
        this.yonghuId = yonghuId;
    }
    /**
	 * 设置：所选牌号
	 */
    public Integer getXuanpaiId() {
        return xuanpaiId;
    }


    /**
	 * 获取：所选牌号
	 */

    public void setXuanpaiId(Integer xuanpaiId) {
        this.xuanpaiId = xuanpaiId;
    }
    /**
	 * 设置：预占日志类型
	 */
    public Integer getYuyueJiluTypes() {
        return yuyueJiluTypes;
    }


    /**
	 * 获取：预占日志类型
	 */

    public void setYuyueJiluTypes(Integer yuyueJiluTypes) {
        this.yuyueJiluTypes = yuyueJiluTypes;
    }
    /**
	 * 设置：操作时间
	 */
    public Date getInsertTime() {
        return insertTime;
    }


    /**
	 * 获取：操作时间
	 */

    public void setInsertTime(Date insertTime) {
        this.insertTime = insertTime;
    }
    /**
	 * 设置：创建时间
	 */
    public Date getCreateTime() {
        return createTime;
    }


    /**
	 * 获取：创建时间
	 */

    public void setCreateTime(Date createTime) {
        this.createTime = createTime;
    }

    @Override
    public String toString() {
        return "YuyueJilu{" +
            "id=" + id +
            ", yonghuId=" + yonghuId +
            ", xuanpaiId=" + xuanpaiId +
            ", yuyueJiluTypes=" + yuyueJiluTypes +
            ", insertTime=" + insertTime +
            ", createTime=" + createTime +
        "}";
    }
}
