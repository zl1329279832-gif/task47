-- 用户在线选牌与上牌申请进度查询 - 数据库变更脚本
-- 日期: 2026-06-12

-- 1. ALTER xuanpai 表: 增加预占相关字段
ALTER TABLE xuanpai ADD COLUMN yuyue_user_id INT(11) DEFAULT NULL COMMENT '预占用户ID';
ALTER TABLE xuanpai ADD COLUMN yuyue_expire_time DATETIME DEFAULT NULL COMMENT '预占过期时间';
ALTER TABLE xuanpai ADD COLUMN version INT(11) DEFAULT 0 COMMENT '乐观锁版本号';
ALTER TABLE xuanpai ADD INDEX idx_zhuangtai_types (zhuangtai_types);
ALTER TABLE xuanpai ADD INDEX idx_yuyue_expire_time (yuyue_expire_time);

-- 2. ALTER shangpai 表: 增加保险关联字段
ALTER TABLE shangpai ADD COLUMN baoxian_id INT(11) DEFAULT NULL COMMENT '保险ID';
ALTER TABLE shangpai ADD INDEX idx_yonghu_id (yonghu_id);
ALTER TABLE shangpai ADD INDEX idx_xuanpai_id (xuanpai_id);
ALTER TABLE shangpai ADD INDEX idx_shangpai_types (shangpai_types);

-- 3. CREATE yuyue_jilu 表: 预占操作日志
CREATE TABLE yuyue_jilu (
    id INT(11) NOT NULL AUTO_INCREMENT COMMENT '主键',
    yonghu_id INT(11) NOT NULL COMMENT '用户ID',
    xuanpai_id INT(11) NOT NULL COMMENT '号牌ID',
    yuyue_jilu_types INT(11) NOT NULL COMMENT '操作类型: 1=预占 2=过期释放 3=用户取消 4=转为申请',
    insert_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '操作时间',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (id),
    KEY idx_yuyue_jilu_yonghu_id (yonghu_id),
    KEY idx_yuyue_jilu_xuanpai_id (xuanpai_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8 COMMENT='预占操作日志表';

-- 4. 字典表初始数据: 预占操作类型
INSERT INTO dictionary (dic_code, dic_name, code_index, index_name, super_types, create_time) VALUES
('yuyue_jilu_types', '预占操作类型', 1, '预占', NULL, NOW()),
('yuyue_jilu_types', '预占操作类型', 2, '过期释放', NULL, NOW()),
('yuyue_jilu_types', '预占操作类型', 3, '用户取消', NULL, NOW()),
('yuyue_jilu_types', '预占操作类型', 4, '转为申请', NULL, NOW());
