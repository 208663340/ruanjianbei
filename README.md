# 讯智Agent后端服务启动文档

## 项目简介

讯智Agent是一个基于Spring Boot 3的智能面试助手系统，集成了讯飞AI能力，提供AI对话、智能面试、语音识别、表情识别等功能。

## 技术栈

- **框架**: Spring Boot 3.x
- **数据库**: MySQL 8.0 + MongoDB + Redis
- **分库分表**: Apache ShardingSphere
- **权限认证**: Sa-Token
- **AI服务**: 讯飞星火大模型
- **构建工具**: Maven 3.6+
- **JDK版本**: JDK 17+

## 环境要求

### 必需环境

1. **JDK 17+**
2. **Maven 3.6+**
3. **MySQL 8.0+**
4. **MongoDB 4.4+**
5. **Redis 6.0+**

### 可选环境

- **IDE**: IntelliJ IDEA 2023+ (推荐)
- **Git**: 用于代码管理

## 数据库配置

### MySQL配置

1. 创建数据库：
```sql
CREATE DATABASE mainshi_agent CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
```

2. 配置信息（开发环境）：
   - 主机：127.0.0.1
   - 端口：3306
   - 数据库：mainshi_agent
   - 用户名：root
   - 密码：123456

### MongoDB配置

- 主机：127.0.0.1
- 端口：27017
- 数据库：xunzhi_agent

### Redis配置

- 主机：127.0.0.1
- 端口：6379
- 无密码（开发环境）

## 配置文件说明

### 主配置文件

**application.yaml**
- 服务端口：8002
- 文件上传配置：最大100MB
- 数据源配置：使用ShardingSphere
- 讯飞API配置
- 流量限制配置

### 数据库分片配置

**shardingsphere-config-dev.yaml**
- 用户表按username分16个表
- 消息表按session_id分16个表
- 敏感信息加密（手机号、邮箱）

### Sa-Token配置

**application-satoken.yml**
- Token名称：Authorization
- Token有效期：30天
- Token前缀：Bearer

## 启动步骤

### 1. 环境准备

```bash
# 启动MySQL
sudo systemctl start mysql

# 启动MongoDB
sudo systemctl start mongod

# 启动Redis
sudo systemctl start redis
```

### 2. 代码获取

```bash
git clone <repository-url>
cd xunzhi-agent-backrond
```

### 3. 依赖安装

```bash
cd admin
mvn clean install
```

### 4. 数据库初始化

执行数据库脚本（如果有）：
```bash
# 在MySQL中执行初始化脚本
mysql -u root -p mainshi_agent < resources/database/init.sql
```

### 5. 配置修改

根据实际环境修改配置文件：
- `application.yaml` - 基础配置
- `shardingsphere-config-dev.yaml` - 数据库配置
- `application-satoken.yml` - 认证配置

### 6. 启动应用

#### 方式一：IDE启动
1. 导入项目到IntelliJ IDEA
2. 找到主启动类
3. 右键运行

#### 方式二：命令行启动
```bash
cd admin
mvn spring-boot:run
```

#### 方式三：jar包启动
```bash
cd admin
mvn clean package
java -jar target/xunzhi-agent-admin-*.jar
```

### 7. 验证启动

访问健康检查接口：
```bash
curl http://localhost:8002/actuator/health
```

## 接口文档

### 基础信息

- **服务地址**: http://localhost:8002
- **API前缀**: /api/xunzhi/v1
- **认证方式**: Bearer Token
- **数据格式**: JSON

### 认证说明

除以下接口外，所有接口都需要在请求头中携带Token：
```
Authorization: Bearer <your_token>
```

**免认证接口**：
- POST /api/xunzhi/v1/users/login - 用户登录
- POST /api/xunzhi/v1/users/register - 用户注册
- GET /api/xunzhi/v1/users/has-username - 检查用户名

### 主要接口模块

#### 1. 用户管理模块 (/api/xunzhi/v1/users)

| 方法 | 路径 | 描述 | 认证 |
|------|------|------|------|
| POST | /login | 用户登录 | 否 |
| POST | /register | 用户注册 | 否 |
| GET | /has-username | 检查用户名是否存在 | 否 |
| GET | /{username} | 获取用户信息 | 是 |
| PUT | /{username} | 更新用户信息 | 是 |
| DELETE | /{username} | 删除用户 | 是 |

#### 2. AI对话模块 (/api/xunzhi/v1/ai)

| 方法 | 路径 | 描述 | 认证 |
|------|------|------|------|
| POST | /sessions/{sessionId}/chat | AI聊天（SSE流式） | 是 |
| GET | /history/{sessionId} | 获取对话历史 | 是 |

#### 3. AI会话管理 (/api/xunzhi/v1/ai/conversations)

| 方法 | 路径 | 描述 | 认证 |
|------|------|------|------|
| POST | / | 创建AI会话 | 是 |
| GET | / | 分页查询会话列表 | 是 |
| GET | /{sessionId} | 获取会话详情 | 是 |
| PUT | /{sessionId} | 更新会话信息 | 是 |
| PUT | /{sessionId}/end | 结束会话 | 是 |
| DELETE | /{sessionId} | 删除会话 | 是 |

#### 4. Agent智能面试 (/api/xunzhi/v1/agents)

| 方法 | 路径 | 描述 | 认证 |
|------|------|------|------|
| POST | /sessions | 创建面试会话 | 是 |
| POST | /sessions/{sessionId}/chat | 面试对话（SSE流式） | 是 |
| GET | /conversations | 分页查询面试会话 | 是 |
| GET | /history/{sessionId} | 获取面试历史 | 是 |
| PUT | /conversations/{sessionId}/end | 结束面试会话 | 是 |
| POST | /interview/question | 生成面试题目 | 是 |
| POST | /interview/answer | 提交面试答案 | 是 |
| POST | /evaluate/demeanor | 神态评估 | 是 |
| GET | /radar-chart | 获取雷达图数据 | 是 |
| GET | /demeanor-score | 获取神态评分详情 | 是 |

#### 5. 讯飞服务模块 (/api/xunzhi/v1/xunfei)

| 方法 | 路径 | 描述 | 认证 |
|------|------|------|------|
| POST | /audio/transcribe | 语音转文字 | 是 |
| POST | /face/expression | 表情识别 | 是 |
| POST | /upload | 文件上传 | 是 |

#### 6. AI配置管理 (/api/xunzhi/v1/ai-properties)

| 方法 | 路径 | 描述 | 认证 |
|------|------|------|------|
| POST | / | 创建AI配置 | 是 |
| GET | / | 分页查询AI配置 | 是 |
| GET | /list | 获取所有AI配置 | 是 |
| GET | /{id} | 获取AI配置详情 | 是 |
| PUT | /{id} | 更新AI配置 | 是 |
| DELETE | /{id} | 删除AI配置 | 是 |

#### 7. Agent配置管理 (/api/xunzhi/v1/agent-properties)

| 方法 | 路径 | 描述 | 认证 |
|------|------|------|------|
| POST | / | 创建Agent配置 | 是 |
| GET | / | 分页查询Agent配置 | 是 |
| GET | /list | 获取所有Agent配置 | 是 |
| GET | /{id} | 获取Agent配置详情 | 是 |
| PUT | /{id} | 更新Agent配置 | 是 |
| DELETE | /{id} | 删除Agent配置 | 是 |

### 响应格式

#### 成功响应
```json
{
  "code": "0",
  "message": "success",
  "data": {
    // 具体数据
  },
  "success": true
}
```

#### 错误响应
```json
{
  "code": "A000001",
  "message": "用户请求参数错误",
  "data": null,
  "success": false
}
```

### SSE流式接口说明

聊天接口使用Server-Sent Events (SSE) 实现流式响应：

```javascript
// 前端调用示例
const eventSource = new EventSource('/api/xunzhi/v1/ai/sessions/123/chat', {
  headers: {
    'Authorization': 'Bearer your_token'
  }
});

eventSource.onmessage = function(event) {
  console.log('收到消息:', event.data);
};

eventSource.onerror = function(event) {
  console.log('连接错误:', event);
};
```

## 常见问题

### 1. 启动失败

**问题**: 端口被占用
```
Port 8002 was already in use
```
**解决**: 修改application.yaml中的server.port或杀死占用进程

**问题**: 数据库连接失败
```
Communications link failure
```
**解决**: 检查MySQL是否启动，配置是否正确

### 2. 接口调用失败

**问题**: 403 Forbidden
**解决**: 检查Token是否正确携带，是否已过期

**问题**: 跨域错误
**解决**: 已配置CORS，如仍有问题请检查前端请求方式

### 3. 功能异常

**问题**: 讯飞API调用失败
**解决**: 检查讯飞API密钥配置是否正确

**问题**: 文件上传失败
**解决**: 检查上传目录权限，文件大小是否超限

## 开发调试

### 日志配置

日志级别可在logback.xml中配置：
```xml
<logger name="com.hewei.hzyjy.xunzhi" level="DEBUG"/>
```

### 数据库调试

ShardingSphere SQL日志已开启：
```yaml
props:
  sql-show: true
```

### 性能监控

- 流量限制：每秒最多20次请求
- 连接池监控：HikariCP
- Redis会话缓存：7天过期

## 部署说明

### 生产环境配置

1. 修改数据库配置文件：`shardingsphere-config-prod.yaml`
2. 更新讯飞API密钥
3. 配置HTTPS证书
4. 设置合适的JVM参数：

```bash
java -Xms2g -Xmx4g -jar xunzhi-agent-admin.jar --spring.profiles.active=prod
```

### Docker部署

```dockerfile
FROM openjdk:17-jre-slim
COPY target/xunzhi-agent-admin-*.jar app.jar
EXPOSE 8002
ENTRYPOINT ["java", "-jar", "/app.jar"]
```

## 联系方式

如有问题，请联系开发团队或查看项目文档。

---

**最后更新**: 2024年12月
**版本**: v1.0.0