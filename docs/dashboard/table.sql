

CREATE TABLE `camellia_resource_info` (
  `id` bigint(64) NOT NULL primary key auto_increment comment '自增字段',
  `url` varchar(1024) NOT NULL comment '资源url',
  `info` varchar(1024) NOT NULL comment '描述',
  `tids` varchar(1024) DEFAULT NULL comment '引用的tids',
  `create_time` varchar(2000) DEFAULT NULL comment '创建时间',
  `update_time` varchar(64) DEFAULT NULL comment '更新时间'
) ENGINE=InnoDB DEFAULT CHARSET=utf8 COMMENT='资源信息表';

CREATE TABLE `camellia_table` (
  `tid` bigint(64) NOT NULL primary key auto_increment comment '自增字段',
  `detail` varchar(4096) NOT NULL comment '详情',
  `info` varchar(1024) NOT NULL comment '描述',
  `valid_flag` tinyint(4) DEFAULT NULL comment '是否valid',
  `create_time` varchar(2000) DEFAULT NULL comment '创建时间',
  `update_time` varchar(64) DEFAULT NULL comment '更新时间'
) ENGINE=InnoDB DEFAULT CHARSET=utf8 COMMENT='资源表';

CREATE TABLE `camellia_table_ref` (
  `id` bigint(64) NOT NULL primary key auto_increment comment '自增字段',
  `bid` bigint(64) NOT NULL comment 'bid',
  `bgroup` varchar(64) NOT NULL comment 'bgroup',
  `tid` bigint(64) NOT NULL comment 'tid',
  `info` varchar(1024) NOT NULL comment '描述',
  `valid_flag` tinyint(4) DEFAULT NULL comment '是否valid',
  `create_time` varchar(2000) DEFAULT NULL comment '创建时间',
  `update_time` varchar(64) DEFAULT NULL comment '更新时间',
  unique key (`bid`, `bgroup`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8 COMMENT='资源引用表';
