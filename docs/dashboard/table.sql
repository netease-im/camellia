

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


create table ip_checker
(
    id          bigint(64) auto_increment comment 'Auto increment field'
        primary key,
    bid         bigint(64)    null comment 'bid',
    bgroup      varchar(64)   null comment 'bgroup',
    `ipCheckMode`        tinyint(1)    not null comment '0=UNKNOWN, 1=BLACK, 2=WHITE',
    ip_list     varchar(1024) not null comment 'support ip, also supports network segment, comma separated.ex:2.2.2.2,5.5.5.5,3.3.3.0/24,6.6.0.0/16',
    create_time varchar(64)   null comment 'create time',
    update_time varchar(64)   null comment 'Update time',
    constraint bid_bgroup_unique
        unique (bid, bgroup)
)
    comment 'IP checker table' charset = utf8;


