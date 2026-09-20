-- ============================================================================
-- LiveRoomDemo 建表脚本
--
-- 应用配置里 spring.jpa.hibernate.ddl-auto=none，表结构不会自动创建，
-- 首次部署必须先执行这个脚本。
--
--   mysql -u root -p < docs/sql/schema.sql
--
-- 如果改了库名，记得同步修改 MYSQL_DATABASE 环境变量。
-- ============================================================================

CREATE DATABASE IF NOT EXISTS `livedemo`
    DEFAULT CHARACTER SET utf8mb4
    DEFAULT COLLATE utf8mb4_general_ci;

USE `livedemo`;

-- 访客表。用 IP 作为主键：同一个 IP 再次访问时沿用之前分配的随机昵称。
--
-- 注意 user 是 MySQL 的保留字，所有引用都要加反引号。
-- ip 列留到 64 字符是为了容纳 IPv6 地址（最长 45 字符）。
CREATE TABLE IF NOT EXISTS `user` (
    `ip`          VARCHAR(64) NOT NULL COMMENT '访客 IP，主键',
    `random_name` VARCHAR(64) DEFAULT NULL COMMENT '随机分配的中文昵称',
    PRIMARY KEY (`ip`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4 COMMENT = '直播间访客';
