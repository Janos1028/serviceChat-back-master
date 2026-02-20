# 支撑服务平台

## 📋 项目简介
本项目是 **支撑服务平台** 的后端核心服务。
基于 **Spring Boot 2.3.1** 构建，采用 **WebSocket** 实现高性能的即时通讯。
支持单聊、群聊、消息持久化、文件上传及邮件验证等功能。
系统集成了 **Jasypt** 对敏感配置（如数据库密码、密钥）进行加密保护。

## ✨ 核心功能特性

本项目不仅仅是一个基础的即时通讯工具，而是完整复刻了企业级客服系统的业务流转和状态管理，主要包含以下核心功能模块：

### 💬 1. 实时通讯与消息漫游 (Core Chat)

- **全双工实时通信**：底层基于 **WebSocket + STOMP** 协议，实现毫秒级消息收发，支持断线重连。
- **多端消息同步**：采用“在线推、离线拉”的同步模型。支持历史聊天记录漫游，用户多端登录或离线重连后消息不丢失。
- **丰富的消息载体**：支持发送**纯文本、图片和文件**，并集成阿里云 OSS 实现图片的秒传与预览。
- **精准的消息状态**：支持消息已读/未读状态的**实时更新**与多端同步，降低沟通成本。

### 👨‍💻2. 智能客服调度与会话流转 (ACD & Workflow)

- **自动分配机制 (ACD)**：用户发起咨询时，系统自动根据业务分类（Domain）查询在线客服列表，并进行智能负载分配。
- **人工无缝转接**：客服人员遇到复杂问题时，可一键将当前会话**转接**给其他业务线在线客服，转接且保留完整聊天上下文，转接后的客服也可以查看上下文。
- **精细化状态机**：完整实现了客服会话的生命周期管理，涵盖“排队等待”、“服务进行中”、“等待用户确认”、“强制结束”、“正常结单”等闭环状态。

### 👨‍🔧3.客服与用户互动

- **服务域隔离**：用户在系统左侧侧边栏选择目标“服务域”（如：客服务中心、IT技术支持等）。系统支持多业务线并存，数据与会话状态相互隔离，不同服务域的客服是无法共享聊天记录的。
- **精准意图收集与智能调度：**触发人工服务后，系统要求用户选择具体的“服务类型”，进一步细化业务颗粒度，并智能分配对应的客服。
- **客服转接：**客服确定当前问题是否能解决，无法解决则可以转接给其他服务类型，系统将随机分配客服服务用户。
- **会话关闭请求：**客服发起结束服务请求，用户确定当前问题是否已解决；未解决则当前会话继续，已解决则结束会话。
- **客服强制关闭会话：**当会话处于僵持阶段，客服可以主动强制结束会话，以免会话一直拖着无法结束。

### ⏱️ 4. SLA 服务质量监控 (基于延时队列)

- **首问超时转接**：用户进线后开启倒计时，若被分配的客服在 5 分钟内未作答复，系统自动触发流转机制，转接至其他客服并发送安抚话术，保障响应时效。
- **超时自动结单**：客服发起“结束服务”申请后，若用户在 30 分钟内未手动确认（已解决/未解决），系统将自动介入强制结单。
- **自动默认评价**：服务结束后开启评价倒计时，若用户未主动评价，系统将自动提交五星好评，形成业务闭环。

### 🔔 5. 用户交互与体验优化 (UX)

- **系统级提示**：集成 HTML5 Notification API 与原生 Audio，实现页面弹窗通知与新消息提示音，当有消息时可以实时提醒。
- **交互式卡片消息**：在会话开启、客服转接、请求结单、发起评价等关键业务节点，系统会下发结构化的卡片（Card）消息，引导用户点击操作。
- **会话列表动态管理**：实时感知**在线/离线**用户状态，动态更新会话列表与未读消息角标（Badge）。

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
- **缓存中间件**: Redis 5.0+ 
- **消息队列：**RabbitMQ

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
| **RabbitMQ** |  | 消息队列，监听过期key |

## ⚙️ 关键配置说明 (必读)

### 1. 数据库初始化
请根据环境导入相应的 SQL 脚本：
* **库名**: `subtlechat`

* **配置信息：**需要替换成自己的配置信息。

* ```yaml
  datasource:
    url: jdbc:mysql://localhost:3306/subtlechat?serverTimezone=Asia/Shanghai&useUnicode=true&characterEncoding=utf-8&useSSL=false&allowPublicKeyRetrieval=true
    driver-class-name: com.mysql.cj.jdbc.Driver
    username: root
    password: 1234
  ```

### 2. 阿里云 OSS

需要在application里面替换自己的阿里云配置信息，否则无法使用文件存储和发送功能。

```yaml
aliyun:
  oss:
    endpoint: oss-cn-hangzhou.aliyuncs.com # OSS 外网访问域名
    access-key-id: your-access-key-id       # AccessKey ID
    access-key-secret: your-access-key-secret # AccessKey Secret
    bucket-name: your-bucket-name           # Bucket 名称
```

### 3. Redis配置

需要在application里面替换自己的redis配置。

```yaml
redis:
  host: localhost
  port: 6379
  database: 1
```

### 4. RabbitMQ配置

需要在application里面替换自己的redis配置。

```yaml
rabbitmq:
  host: localhost
  port: 5672
  username: guest
  password: guest
  virtual-host: /
```
