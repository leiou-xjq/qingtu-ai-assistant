# 青途智伴 - 部署文档

## 一、环境准备

### 1.1 服务器要求
- CPU: 2核+
- 内存: 4GB+
- 磁盘: 50GB+
- 系统: CentOS 7+ / Ubuntu 20.04+

### 1.2 软件环境
- JDK 17
- MySQL 8.0
- Redis 6.0+
- Nginx (可选)

## 二、数据库部署

### 2.1 安装MySQL
```bash
# CentOS
yum install mysql-server

# Ubuntu
apt install mysql-server
```

### 2.2 创建数据库
```sql
CREATE DATABASE qingtu_assistant DEFAULT CHARSET utf8mb4 COLLATE utf8mb4_unicode_ci;
```

### 2.3 导入数据
```bash
mysql -u root -p qingtu_assistant < schema.sql
```

## 三、后端部署

### 3.1 构建项目
```bash
cd backend
mvn clean package -DskipTests
```

### 3.2 配置环境变量
```bash
export DB_PASSWORD=your_password
export REDIS_PASSWORD=your_redis_password
export DASHSCOPE_API_KEY=your_api_key
export HEFENG_API_KEY=your_weather_key
```

### 3.3 创建启动脚本
```bash
#!/bin/bash
nohup java -jar \
  -Dserver.port=8080 \
  -Dspring.datasource.password=$DB_PASSWORD \
  -Dspring.data.redis.password=$REDIS_PASSWORD \
  -Dai.dashscope.api-key=$DASHSCOPE_API_KEY \
  -Dweather.api-key=$HEFENG_API_KEY \
  qingtu-ai-assistant-1.0.0.jar \
  > app.log 2>&1 &
```

### 3.4 启动应用
```bash
chmod +x start.sh
./start.sh
```

### 3.5 验证部署
```bash
curl http://localhost:8080/api/system/health
```

## 四、前端部署

### 4.1 H5部署
```bash
cd frontend
npm install
npm run build:h5
```

将dist目录部署到Nginx：
```nginx
server {
    listen 80;
    server_name your-domain.com;
    root /path/to/dist;
    index index.html;
    
    location / {
        try_files $uri $uri/ /index.html;
    }
    
    location /api {
        proxy_pass http://127.0.0.1:8080/api;
    }
}
```

### 4.2 小程序部署
```bash
npm run build:mp-weixin
```

在微信开发者工具中导入项目并提交审核。

### 4.3 APP打包
使用HBuilderX进行APP打包。

## 五、配置说明

### 5.1 后端配置
编辑`application.yml`或使用环境变量：
```yaml
spring:
  datasource:
    url: jdbc:mysql://localhost:3306/qingtu_assistant
    username: root
    password: ${DB_PASSWORD}
  data:
    redis:
      host: localhost
      port: 6379
      password: ${REDIS_PASSWORD}

ai:
  dashscope:
    api-key: ${DASHSCOPE_API_KEY}

weather:
  api-key: ${HEFENG_API_KEY}
```

### 5.2 API Key获取
- 通义千问: https://dashscope.console.aliyun.com/
- 和风天气: https://dev.qweather.com/

## 六、运维监控

### 6.1 日志查看
```bash
tail -f app.log
```

### 6.2 重启应用
```bash
pkill -f qingtu-ai-assistant
./start.sh
```

### 6.3 健康检查
```bash
curl http://localhost:8080/api/system/health
```

## 七、Docker部署（可选）

### 7.1 Dockerfile
```dockerfile
FROM openjdk:17-slim
COPY target/qingtu-ai-assistant-1.0.0.jar /app/app.jar
WORKDIR /app
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
```

### 7.2 构建运行
```bash
docker build -t qingtu-ai .
docker run -d -p 8080:8080 --name qingtu qingtu-ai
```

## 八、常见问题

### 8.1 AI功能不可用
检查API Key是否配置正确。

### 8.2 天气查询失败
检查和风天气API Key是否有效。

### 8.3 定时任务不执行
检查Redis是否正常运行，任务锁依赖Redis。

### 8.4 推送消息失败
检查Webhook地址是否正确配置。

## 九、性能优化

### 9.1 数据库优化
```sql
-- 添加索引
CREATE INDEX idx_user_id ON cost_record(user_id, trade_time);
CREATE INDEX idx_weekday ON course_schedule(weekday);
```

### 9.2 缓存策略
- 天气数据缓存30分钟
- 课程数据缓存1小时
- 消费统计缓存2小时

### 9.3 连接池配置
```yaml
spring:
  datasource:
    hikari:
      maximum-pool-size: 20
      minimum-idle: 5
```