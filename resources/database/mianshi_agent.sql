create schema mainshi_agent collate utf8mb4_0900_ai_ci;

CREATE TABLE `admin_permission` (
                                    `id` bigint NOT NULL AUTO_INCREMENT COMMENT 'ID',
                                    `user_id` bigint NOT NULL COMMENT '用户ID',
                                    `username` varchar(256) NOT NULL COMMENT '用户名',
                                    `is_admin` tinyint(1) DEFAULT '0' COMMENT '是否管理员 0：普通用户 1：管理员',
                                    `create_time` datetime DEFAULT NULL COMMENT '创建时间',
                                    `update_time` datetime DEFAULT NULL COMMENT '修改时间',
                                    `del_flag` tinyint(1) DEFAULT '0' COMMENT '删除标识 0：未删除 1：已删除',
                                    PRIMARY KEY (`id`),
                                    UNIQUE KEY `idx_unique_user_id` (`user_id`) USING BTREE,
                                    KEY `idx_username` (`username`) USING BTREE,
                                    KEY `idx_is_admin` (`is_admin`) USING BTREE
) ENGINE=InnoDB AUTO_INCREMENT=2 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='管理员权限表';

CREATE TABLE `agent_properties` (
                                    `id` bigint NOT NULL AUTO_INCREMENT COMMENT 'ID',
                                    `agent_name` varchar(256) DEFAULT NULL COMMENT '智能体名称',
                                    `api_secret` varchar(256) DEFAULT NULL COMMENT '鉴权密钥',
                                    `api_key` varchar(512) DEFAULT NULL COMMENT '鉴权key',
                                    `api_flow_id` varchar(256) DEFAULT NULL COMMENT '工作流id',
                                    `create_time` datetime DEFAULT NULL COMMENT '创建时间',
                                    `update_time` datetime DEFAULT NULL COMMENT '修改时间',
                                    `del_flag` tinyint(1) DEFAULT NULL COMMENT '删除标识 0：未删除 1：已删除',
                                    PRIMARY KEY (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=12 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE `agent_tag` (
                             `id` bigint NOT NULL AUTO_INCREMENT COMMENT 'ID',
                             `tag_name` varchar(256) NOT NULL COMMENT '标签名称',
                             `agent_id` bigint NOT NULL COMMENT '关联的智能体ID',
                             `description` varchar(512) DEFAULT NULL COMMENT '标签描述',
                             `create_time` datetime DEFAULT NULL COMMENT '创建时间',
                             `update_time` datetime DEFAULT NULL COMMENT '修改时间',
                             `del_flag` tinyint(1) DEFAULT '0' COMMENT '删除标识 0：未删除 1：已删除',
                             PRIMARY KEY (`id`),
                             KEY `idx_agent_id` (`agent_id`) USING BTREE,
                             KEY `idx_tag_name` (`tag_name`) USING BTREE,
                             KEY `idx_create_time` (`create_time`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='智能体标签表';

CREATE TABLE `ai_properties` (
                                 `id` bigint NOT NULL AUTO_INCREMENT COMMENT 'ID',
                                 `ai_name` varchar(256) NOT NULL COMMENT 'AI名称',
                                 `ai_type` varchar(64) NOT NULL COMMENT 'AI类型：spark、openai、claude等',
                                 `api_key` varchar(512) NOT NULL COMMENT 'API密钥',
                                 `api_secret` varchar(512) DEFAULT NULL COMMENT 'API密钥（部分AI需要）',
                                 `api_url` varchar(512) DEFAULT NULL COMMENT 'API地址',
                                 `model_name` varchar(256) DEFAULT NULL COMMENT '模型名称',
                                 `max_tokens` int DEFAULT '4096' COMMENT '最大token数',
                                 `temperature` decimal(3,2) DEFAULT '0.70' COMMENT '温度参数',
                                 `system_prompt` text COMMENT '系统提示词',
                                 `is_enabled` tinyint(1) DEFAULT '1' COMMENT '是否启用 0：禁用 1：启用',
                                 `create_time` datetime DEFAULT NULL COMMENT '创建时间',
                                 `update_time` datetime DEFAULT NULL COMMENT '修改时间',
                                 `del_flag` tinyint(1) DEFAULT '0' COMMENT '删除标识 0：未删除 1：已删除',
                                 PRIMARY KEY (`id`),
                                 KEY `idx_ai_type` (`ai_type`) USING BTREE,
                                 KEY `idx_is_enabled` (`is_enabled`) USING BTREE,
                                 KEY `idx_create_time` (`create_time`) USING BTREE
) ENGINE=InnoDB AUTO_INCREMENT=2 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='AI配置表';

CREATE TABLE `t_user_0` (
                            `id` bigint NOT NULL AUTO_INCREMENT COMMENT 'ID',
                            `username` varchar(256) DEFAULT NULL COMMENT '用户名',
                            `password` varchar(512) DEFAULT NULL COMMENT '密码',
                            `real_name` varchar(256) DEFAULT NULL COMMENT '真实姓名',
                            `phone` varchar(128) DEFAULT NULL COMMENT '手机号',
                            `mail` varchar(512) DEFAULT NULL COMMENT '邮箱',
                            `deletion_time` bigint DEFAULT NULL COMMENT '注销时间戳',
                            `create_time` datetime DEFAULT NULL COMMENT '创建时间',
                            `update_time` datetime DEFAULT NULL COMMENT '修改时间',
                            `del_flag` tinyint(1) DEFAULT NULL COMMENT '删除标识 0：未删除 1：已删除',
                            PRIMARY KEY (`id`),
                            UNIQUE KEY `idx_unique_username` (`username`) USING BTREE
) ENGINE=InnoDB AUTO_INCREMENT=1926919132800401410 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE `t_user_1` (
                            `id` bigint NOT NULL AUTO_INCREMENT COMMENT 'ID',
                            `username` varchar(256) DEFAULT NULL COMMENT '用户名',
                            `password` varchar(512) DEFAULT NULL COMMENT '密码',
                            `real_name` varchar(256) DEFAULT NULL COMMENT '真实姓名',
                            `phone` varchar(128) DEFAULT NULL COMMENT '手机号',
                            `mail` varchar(512) DEFAULT NULL COMMENT '邮箱',
                            `deletion_time` bigint DEFAULT NULL COMMENT '注销时间戳',
                            `create_time` datetime DEFAULT NULL COMMENT '创建时间',
                            `update_time` datetime DEFAULT NULL COMMENT '修改时间',
                            `del_flag` tinyint(1) DEFAULT NULL COMMENT '删除标识 0：未删除 1：已删除',
                            PRIMARY KEY (`id`),
                            UNIQUE KEY `idx_unique_username` (`username`) USING BTREE
) ENGINE=InnoDB AUTO_INCREMENT=1726253659068588035 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE `t_user_2` (
                            `id` bigint NOT NULL AUTO_INCREMENT COMMENT 'ID',
                            `username` varchar(256) DEFAULT NULL COMMENT '用户名',
                            `password` varchar(512) DEFAULT NULL COMMENT '密码',
                            `real_name` varchar(256) DEFAULT NULL COMMENT '真实姓名',
                            `phone` varchar(128) DEFAULT NULL COMMENT '手机号',
                            `mail` varchar(512) DEFAULT NULL COMMENT '邮箱',
                            `deletion_time` bigint DEFAULT NULL COMMENT '注销时间戳',
                            `create_time` datetime DEFAULT NULL COMMENT '创建时间',
                            `update_time` datetime DEFAULT NULL COMMENT '修改时间',
                            `del_flag` tinyint(1) DEFAULT NULL COMMENT '删除标识 0：未删除 1：已删除',
                            PRIMARY KEY (`id`),
                            UNIQUE KEY `idx_unique_username` (`username`) USING BTREE
) ENGINE=InnoDB AUTO_INCREMENT=1726260205890691074 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE `t_user_3` (
                            `id` bigint NOT NULL AUTO_INCREMENT COMMENT 'ID',
                            `username` varchar(256) DEFAULT NULL COMMENT '用户名',
                            `password` varchar(512) DEFAULT NULL COMMENT '密码',
                            `real_name` varchar(256) DEFAULT NULL COMMENT '真实姓名',
                            `phone` varchar(128) DEFAULT NULL COMMENT '手机号',
                            `mail` varchar(512) DEFAULT NULL COMMENT '邮箱',
                            `deletion_time` bigint DEFAULT NULL COMMENT '注销时间戳',
                            `create_time` datetime DEFAULT NULL COMMENT '创建时间',
                            `update_time` datetime DEFAULT NULL COMMENT '修改时间',
                            `del_flag` tinyint(1) DEFAULT NULL COMMENT '删除标识 0：未删除 1：已删除',
                            PRIMARY KEY (`id`),
                            UNIQUE KEY `idx_unique_username` (`username`) USING BTREE
) ENGINE=InnoDB AUTO_INCREMENT=1716826815625977859 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE `t_user_4` (
                            `id` bigint NOT NULL AUTO_INCREMENT COMMENT 'ID',
                            `username` varchar(256) DEFAULT NULL COMMENT '用户名',
                            `password` varchar(512) DEFAULT NULL COMMENT '密码',
                            `real_name` varchar(256) DEFAULT NULL COMMENT '真实姓名',
                            `phone` varchar(128) DEFAULT NULL COMMENT '手机号',
                            `mail` varchar(512) DEFAULT NULL COMMENT '邮箱',
                            `deletion_time` bigint DEFAULT NULL COMMENT '注销时间戳',
                            `create_time` datetime DEFAULT NULL COMMENT '创建时间',
                            `update_time` datetime DEFAULT NULL COMMENT '修改时间',
                            `del_flag` tinyint(1) DEFAULT NULL COMMENT '删除标识 0：未删除 1：已删除',
                            PRIMARY KEY (`id`),
                            UNIQUE KEY `idx_unique_username` (`username`) USING BTREE
) ENGINE=InnoDB AUTO_INCREMENT=1716777824704053251 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE `t_user_5` (
                            `id` bigint NOT NULL AUTO_INCREMENT COMMENT 'ID',
                            `username` varchar(256) DEFAULT NULL COMMENT '用户名',
                            `password` varchar(512) DEFAULT NULL COMMENT '密码',
                            `real_name` varchar(256) DEFAULT NULL COMMENT '真实姓名',
                            `phone` varchar(128) DEFAULT NULL COMMENT '手机号',
                            `mail` varchar(512) DEFAULT NULL COMMENT '邮箱',
                            `deletion_time` bigint DEFAULT NULL COMMENT '注销时间戳',
                            `create_time` datetime DEFAULT NULL COMMENT '创建时间',
                            `update_time` datetime DEFAULT NULL COMMENT '修改时间',
                            `del_flag` tinyint(1) DEFAULT NULL COMMENT '删除标识 0：未删除 1：已删除',
                            PRIMARY KEY (`id`),
                            UNIQUE KEY `idx_unique_username` (`username`) USING BTREE
) ENGINE=InnoDB AUTO_INCREMENT=1716835362095034371 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE `t_user_6` (
                            `id` bigint NOT NULL AUTO_INCREMENT COMMENT 'ID',
                            `username` varchar(256) DEFAULT NULL COMMENT '用户名',
                            `password` varchar(512) DEFAULT NULL COMMENT '密码',
                            `real_name` varchar(256) DEFAULT NULL COMMENT '真实姓名',
                            `phone` varchar(128) DEFAULT NULL COMMENT '手机号',
                            `mail` varchar(512) DEFAULT NULL COMMENT '邮箱',
                            `deletion_time` bigint DEFAULT NULL COMMENT '注销时间戳',
                            `create_time` datetime DEFAULT NULL COMMENT '创建时间',
                            `update_time` datetime DEFAULT NULL COMMENT '修改时间',
                            `del_flag` tinyint(1) DEFAULT NULL COMMENT '删除标识 0：未删除 1：已删除',
                            PRIMARY KEY (`id`),
                            UNIQUE KEY `idx_unique_username` (`username`) USING BTREE
) ENGINE=InnoDB AUTO_INCREMENT=1716991700406161411 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE `t_user_7` (
                            `id` bigint NOT NULL AUTO_INCREMENT COMMENT 'ID',
                            `username` varchar(256) DEFAULT NULL COMMENT '用户名',
                            `password` varchar(512) DEFAULT NULL COMMENT '密码',
                            `real_name` varchar(256) DEFAULT NULL COMMENT '真实姓名',
                            `phone` varchar(128) DEFAULT NULL COMMENT '手机号',
                            `mail` varchar(512) DEFAULT NULL COMMENT '邮箱',
                            `deletion_time` bigint DEFAULT NULL COMMENT '注销时间戳',
                            `create_time` datetime DEFAULT NULL COMMENT '创建时间',
                            `update_time` datetime DEFAULT NULL COMMENT '修改时间',
                            `del_flag` tinyint(1) DEFAULT NULL COMMENT '删除标识 0：未删除 1：已删除',
                            PRIMARY KEY (`id`),
                            UNIQUE KEY `idx_unique_username` (`username`) USING BTREE
) ENGINE=InnoDB AUTO_INCREMENT=1716834641844936706 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE `t_user_8` (
                            `id` bigint NOT NULL AUTO_INCREMENT COMMENT 'ID',
                            `username` varchar(256) DEFAULT NULL COMMENT '用户名',
                            `password` varchar(512) DEFAULT NULL COMMENT '密码',
                            `real_name` varchar(256) DEFAULT NULL COMMENT '真实姓名',
                            `phone` varchar(128) DEFAULT NULL COMMENT '手机号',
                            `mail` varchar(512) DEFAULT NULL COMMENT '邮箱',
                            `deletion_time` bigint DEFAULT NULL COMMENT '注销时间戳',
                            `create_time` datetime DEFAULT NULL COMMENT '创建时间',
                            `update_time` datetime DEFAULT NULL COMMENT '修改时间',
                            `del_flag` tinyint(1) DEFAULT NULL COMMENT '删除标识 0：未删除 1：已删除',
                            PRIMARY KEY (`id`),
                            UNIQUE KEY `idx_unique_username` (`username`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE `t_user_9` (
                            `id` bigint NOT NULL AUTO_INCREMENT COMMENT 'ID',
                            `username` varchar(256) DEFAULT NULL COMMENT '用户名',
                            `password` varchar(512) DEFAULT NULL COMMENT '密码',
                            `real_name` varchar(256) DEFAULT NULL COMMENT '真实姓名',
                            `phone` varchar(128) DEFAULT NULL COMMENT '手机号',
                            `mail` varchar(512) DEFAULT NULL COMMENT '邮箱',
                            `deletion_time` bigint DEFAULT NULL COMMENT '注销时间戳',
                            `create_time` datetime DEFAULT NULL COMMENT '创建时间',
                            `update_time` datetime DEFAULT NULL COMMENT '修改时间',
                            `del_flag` tinyint(1) DEFAULT NULL COMMENT '删除标识 0：未删除 1：已删除',
                            PRIMARY KEY (`id`),
                            UNIQUE KEY `idx_unique_username` (`username`) USING BTREE
) ENGINE=InnoDB AUTO_INCREMENT=1726852231086505986 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE `t_user_10` (
                             `id` bigint NOT NULL AUTO_INCREMENT COMMENT 'ID',
                             `username` varchar(256) DEFAULT NULL COMMENT '用户名',
                             `password` varchar(512) DEFAULT NULL COMMENT '密码',
                             `real_name` varchar(256) DEFAULT NULL COMMENT '真实姓名',
                             `phone` varchar(128) DEFAULT NULL COMMENT '手机号',
                             `mail` varchar(512) DEFAULT NULL COMMENT '邮箱',
                             `deletion_time` bigint DEFAULT NULL COMMENT '注销时间戳',
                             `create_time` datetime DEFAULT NULL COMMENT '创建时间',
                             `update_time` datetime DEFAULT NULL COMMENT '修改时间',
                             `del_flag` tinyint(1) DEFAULT NULL COMMENT '删除标识 0：未删除 1：已删除',
                             PRIMARY KEY (`id`),
                             UNIQUE KEY `idx_unique_username` (`username`) USING BTREE
) ENGINE=InnoDB AUTO_INCREMENT=1726262175087058946 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE `t_user_11` (
                             `id` bigint NOT NULL AUTO_INCREMENT COMMENT 'ID',
                             `username` varchar(256) DEFAULT NULL COMMENT '用户名',
                             `password` varchar(512) DEFAULT NULL COMMENT '密码',
                             `real_name` varchar(256) DEFAULT NULL COMMENT '真实姓名',
                             `phone` varchar(128) DEFAULT NULL COMMENT '手机号',
                             `mail` varchar(512) DEFAULT NULL COMMENT '邮箱',
                             `deletion_time` bigint DEFAULT NULL COMMENT '注销时间戳',
                             `create_time` datetime DEFAULT NULL COMMENT '创建时间',
                             `update_time` datetime DEFAULT NULL COMMENT '修改时间',
                             `del_flag` tinyint(1) DEFAULT NULL COMMENT '删除标识 0：未删除 1：已删除',
                             PRIMARY KEY (`id`),
                             UNIQUE KEY `idx_unique_username` (`username`) USING BTREE
) ENGINE=InnoDB AUTO_INCREMENT=1716835884998893571 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE `t_user_12` (
                             `id` bigint NOT NULL AUTO_INCREMENT COMMENT 'ID',
                             `username` varchar(256) DEFAULT NULL COMMENT '用户名',
                             `password` varchar(512) DEFAULT NULL COMMENT '密码',
                             `real_name` varchar(256) DEFAULT NULL COMMENT '真实姓名',
                             `phone` varchar(128) DEFAULT NULL COMMENT '手机号',
                             `mail` varchar(512) DEFAULT NULL COMMENT '邮箱',
                             `deletion_time` bigint DEFAULT NULL COMMENT '注销时间戳',
                             `create_time` datetime DEFAULT NULL COMMENT '创建时间',
                             `update_time` datetime DEFAULT NULL COMMENT '修改时间',
                             `del_flag` tinyint(1) DEFAULT NULL COMMENT '删除标识 0：未删除 1：已删除',
                             PRIMARY KEY (`id`),
                             UNIQUE KEY `idx_unique_username` (`username`) USING BTREE
) ENGINE=InnoDB AUTO_INCREMENT=1943196838016086018 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE `t_user_13` (
                             `id` bigint NOT NULL AUTO_INCREMENT COMMENT 'ID',
                             `username` varchar(256) DEFAULT NULL COMMENT '用户名',
                             `password` varchar(512) DEFAULT NULL COMMENT '密码',
                             `real_name` varchar(256) DEFAULT NULL COMMENT '真实姓名',
                             `phone` varchar(128) DEFAULT NULL COMMENT '手机号',
                             `mail` varchar(512) DEFAULT NULL COMMENT '邮箱',
                             `deletion_time` bigint DEFAULT NULL COMMENT '注销时间戳',
                             `create_time` datetime DEFAULT NULL COMMENT '创建时间',
                             `update_time` datetime DEFAULT NULL COMMENT '修改时间',
                             `del_flag` tinyint(1) DEFAULT NULL COMMENT '删除标识 0：未删除 1：已删除',
                             PRIMARY KEY (`id`),
                             UNIQUE KEY `idx_unique_username` (`username`) USING BTREE
) ENGINE=InnoDB AUTO_INCREMENT=1716777589441347586 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE `t_user_14` (
                             `id` bigint NOT NULL AUTO_INCREMENT COMMENT 'ID',
                             `username` varchar(256) DEFAULT NULL COMMENT '用户名',
                             `password` varchar(512) DEFAULT NULL COMMENT '密码',
                             `real_name` varchar(256) DEFAULT NULL COMMENT '真实姓名',
                             `phone` varchar(128) DEFAULT NULL COMMENT '手机号',
                             `mail` varchar(512) DEFAULT NULL COMMENT '邮箱',
                             `deletion_time` bigint DEFAULT NULL COMMENT '注销时间戳',
                             `create_time` datetime DEFAULT NULL COMMENT '创建时间',
                             `update_time` datetime DEFAULT NULL COMMENT '修改时间',
                             `del_flag` tinyint(1) DEFAULT NULL COMMENT '删除标识 0：未删除 1：已删除',
                             PRIMARY KEY (`id`),
                             UNIQUE KEY `idx_unique_username` (`username`) USING BTREE
) ENGINE=InnoDB AUTO_INCREMENT=1926922121669894147 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE `t_user_15` (
                             `id` bigint NOT NULL AUTO_INCREMENT COMMENT 'ID',
                             `username` varchar(256) DEFAULT NULL COMMENT '用户名',
                             `password` varchar(512) DEFAULT NULL COMMENT '密码',
                             `real_name` varchar(256) DEFAULT NULL COMMENT '真实姓名',
                             `phone` varchar(128) DEFAULT NULL COMMENT '手机号',
                             `mail` varchar(512) DEFAULT NULL COMMENT '邮箱',
                             `deletion_time` bigint DEFAULT NULL COMMENT '注销时间戳',
                             `create_time` datetime DEFAULT NULL COMMENT '创建时间',
                             `update_time` datetime DEFAULT NULL COMMENT '修改时间',
                             `del_flag` tinyint(1) DEFAULT NULL COMMENT '删除标识 0：未删除 1：已删除',
                             PRIMARY KEY (`id`),
                             UNIQUE KEY `idx_unique_username` (`username`) USING BTREE
) ENGINE=InnoDB AUTO_INCREMENT=1752265616481370114 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci

