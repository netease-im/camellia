

CREATE TABLE `camellia_id_info` (
  `tag` varchar(512) NOT NULL COMMENT 'tag',
  `id` bigint(20) DEFAULT NULL COMMENT 'id',
  `createTime` bigint(20) DEFAULT NULL COMMENT '创建时间',
  `updateTime` bigint(20) DEFAULT NULL COMMENT '更新时间',
  PRIMARY KEY (`tag`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='id生成表';