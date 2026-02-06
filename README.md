# 支撑服务平台

## 📋 项目简介
本项目是 **支撑服务平台** 的后端核心服务。
基于 **Spring Boot 2.3.1** 构建，采用 **WebSocket** 实现高性能的即时通讯。
支持单聊、群聊、消息持久化、文件上传及邮件验证等功能。
系统集成了 **Jasypt** 对敏感配置（如数据库密码、密钥）进行加密保护。

## 🛠️ 技术栈 (Tech Stack)

### 核心框架
- **开发语言**: Java 8
- **Web 框架**: Spring Boot 2.3.1.RELEASE
- **安全框架**: Spring Security + JWT
- **即时通讯**: Spring WebSocket (STOMP协议)

### 数据存储
- **数据库**: MySQL 5.7+
- **ORM 框架**: MyBatis Plus 3.3.1
- **数据库连接池**: Druid 1.1.10
- **缓存中间件**: Redis 5.0+ (⚠️ **必须开启键过期监听**)

### 工具与组件
- **文件存储**:
  - 阿里云 OSS (aliyun-sdk-oss)
  - FastDFS (fastdfs-client-java)
- **配置加密**: Jasypt (jasypt-spring-boot-starter)
- **工具库**: Apache Commons Lang3
- **Emoji 处理**: java-emoji-converter
- **邮件服务**: Spring Boot Mail

## ⚡ 环境准备 (Prerequisites)

在运行项目前，请确保本地已安装以下环境：

| 环境 | 版本要求 | 说明 |
| :--- | :--- | :--- |
| **JDK** | 1.8+ | 核心运行环境 |
| **MySQL** | 5.7 或 8.0 | 需导入 SQL 脚本初始化 |
| **Redis** | 5.0+ | **核心依赖**，未启动会报错 |
| **Maven** | 3.6+ | 构建工具 |

## ⚙️ 关键配置说明 (必读)

### 1. 数据库初始化
请根据环境导入相应的 SQL 脚本：
* **库名**: `subtlechat`

### 2. Redis 开启过期监听 (⚠️ 重要)
本项目依赖 Redis 的 **Key Expiration Events** (键过期通知) 来处理用户状态自动更新。
默认 Redis 不开启此功能，**必须手动开启**，否则用户在线状态功能将失效，项目也无法运行。

* **修改 `redis.conf`**:
  ```conf
  notify-keyspace-events "Ex"
或者命令行开启：CONFIG SET notify-keyspace-events Ex
