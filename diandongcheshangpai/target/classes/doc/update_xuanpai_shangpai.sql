-- 上牌管理系统：选牌预占与上牌申请功能 数据库变更
-- 执行时间: 请在应用更新前执行

-- 1. xuanpai 表增加预占相关字段
ALTER TABLE `xuanpai` ADD COLUMN `yonghu_id` INT(11) DEFAULT NULL COMMENT '预占用户ID' AFTER `zhuangtai_types`;
ALTER TABLE `xuanpai` ADD COLUMN `yuezhan_time` DATETIME DEFAULT NULL COMMENT '预占时间' AFTER `yonghu_id`;

-- 2. shangpai 表增加保险关联字段
ALTER TABLE `shangpai` ADD COLUMN `baoxian_id` INT(11) DEFAULT NULL COMMENT '保险ID' AFTER `xuanpai_id`;
