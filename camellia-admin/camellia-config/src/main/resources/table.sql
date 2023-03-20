
CREATE TABLE `camellia_config` (
  `id` bigint(20) NOT NULL primary key auto_increment comment '自增字段',
  `namespace` varchar(128) NOT NULL comment 'namespace',
  `ckey` varchar(256) NOT NULL comment '配置key',
  `cvalue` varchar(4096) DEFAULT NULL comment '配置value',
  `ctype` int(9) NOT NULL comment '配置类型',
  `info` varchar(4096) DEFAULT NULL comment '配置描述',
  `version` bigint(20) NOT NULL comment '版本',
  `valid_flag` tinyint(4) DEFAULT NULL comment '是否valid',
  `creator` varchar(256) DEFAULT NULL comment '创建者',
  `operator` varchar(256) DEFAULT NULL comment '最后更新者',
  `create_time` bigint(20) DEFAULT NULL comment '创建时间',
  `update_time` bigint(20) DEFAULT NULL comment '更新时间',
  unique key(`namespace`, `ckey`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='配置表';

CREATE TABLE `camellia_config_namespace` (
  `id` bigint(20) NOT NULL primary key auto_increment comment '自增字段',
  `namespace` varchar(128) NOT NULL comment 'namespace',
  `alias` varchar(128) NOT NULL comment 'alias',
  `info` varchar(4096) DEFAULT NULL comment '配置描述',
  `version` bigint(20) NOT NULL comment '版本',
  `valid_flag` tinyint(4) DEFAULT NULL comment '是否valid',
  `creator` varchar(256) DEFAULT NULL comment '创建者',
  `operator` varchar(256) DEFAULT NULL comment '最后更新者',
  `create_time` bigint(20) DEFAULT NULL comment '创建时间',
  `update_time` bigint(20) DEFAULT NULL comment '更新时间',
  unique key(`namespace`),
  unique key(`alias`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='配置命名空间表';


CREATE TABLE `camellia_config_history` (
  `id` bigint(20) NOT NULL primary key auto_increment comment '自增字段',
  `ctype` int(9) NOT NULL comment '配置类型',
  `namespace` varchar(128) NOT NULL comment 'namespace',
  `config_id` bigint(20) DEFAULT NULL comment '配置id',
  `old_config` text DEFAULT NULL comment '老配置',
  `new_config` text DEFAULT NULL comment '新配置',
  `operator_type` varchar(256) DEFAULT NULL comment '配置类型',
  `operator` varchar(256) DEFAULT NULL comment '最后更新者',
  `create_time` bigint(20) DEFAULT NULL comment '创建时间'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='配置变更历史';
