# 青途智伴AI助手 - 压力测试

## 环境要求

- JMeter 5.6+ ([下载](https://jmeter.apache.org/download_jmeter.cgi))
- Java 17+
- 项目后端已启动
- MySQL + Redis 已启动 (docker-compose up -d)

## 文件说明

| 文件 | 说明 | 并发 | 时长 |
|------|------|------|------|
| `01-baseline.jmx` | 基准测试 - 单接口验证 | 4×10 | ~5min |
| `02-ai-limit.jmx` | AI 极限测试 - 阶梯式 | 5→10→20 | ~10min |
| `03-mixed-load.jmx` | 混合负载 - 真实流量模拟 | 50→100 | ~10min |
| `04-stability.jmx` | 稳定性测试 - 长时高压 | 100 | 30min |
| `init-test-data.sh` | 测试数据准备脚本 | - | ~2min |
| `user-tokens.csv` | JWT Token 列表（自动生成） | - | - |

## 快速开始

### 第一步：准备测试数据

```bash
# 启动后端后执行
bash stress-test/init-test-data.sh

# 验证 Token 文件
cat stress-test/user-tokens.csv
```

### 第二步：启动 JMeter GUI（编辑脚本）

```bash
# Windows
jmeter.bat -t stress-test/01-baseline.jmx

# Mac/Linux
jmeter -t stress-test/01-baseline.jmx
```

### 第三步：命令行执行压测

```bash
# 基准测试
jmeter -n -t stress-test/01-baseline.jmx -l result-baseline.jtl -e -o report-baseline

# AI 极限测试
jmeter -n -t stress-test/02-ai-limit.jmx -l result-ai.jtl -e -o report-ai

# 混合负载
jmeter -n -t stress-test/03-mixed-load.jmx -l result-mixed.jtl -e -o report-mixed

# 稳定性测试
jmeter -n -t stress-test/04-stability.jmx -l result-stability.jtl -e -o report-stability
```

### 第四步：查看报告

```bash
# 生成 HTML 报告
jmeter -g result-baseline.jtl -o report-baseline
# 打开 report-baseline/index.html
```

## 执行顺序建议

```
1. 准备数据  →  init-test-data.sh
2. 基准测试  →  01-baseline.jmx    (验证接口可用)
3. AI 极限   →  02-ai-limit.jmx    (找 AI 接口瓶颈)
4. 混合负载  →  03-mixed-load.jmx  (真实场景模拟)
5. 稳定性    →  04-stability.jmx   (长时间验证)
```

## 监控要点

压测期间同时监控：

```bash
# Java 进程 (JVM)
jconsole  # 或 jvisualvm
arthas    # 生产级诊断: https://arthas.aliyun.com/

# 数据库连接池
curl http://localhost:8080/api/actuator/health  # 如果启用 actuator

# Redis
redis-cli INFO stats
redis-cli INFO clients

# MySQL
mysql -u root -p -e "SHOW PROCESSLIST"
mysql -u root -p -e "SHOW STATUS LIKE 'Threads%'"
```

## 关键风险点

| 风险 | 位置 | 建议 |
|------|------|------|
| AiClient 无超时 | `AiClient.java:88` | 设置 connectTimeout + readTimeout |
| HikariCP 最大 50 | `application.yml` | 按需调整 maximum-pool-size |
| Redis 最大 50 连接 | 应用配置 | 高并发时需扩容 |
| Resilience4j 限流 100/min | 应用配置 | AI 调用上限 |

## 预期结果

| 接口 | 预期 P50 | 预期 P95 | 预期 QPS |
|------|----------|----------|----------|
| `/system/health` | <10ms | <50ms | 2000+ |
| `/course/today` (缓存命中) | <20ms | <100ms | 500+ |
| `/user/login` | <50ms | <200ms | 800+ |
| `/cost/list` | <50ms | <200ms | 300+ |
| `/weather/current` (外部 API) | <500ms | <2000ms | 50+ |
| `/weather/outfit` (AI ×2) | <5s | <15s | 10+ |
| `/rag/ask-stream` (SSE) | <3s | <10s | 10+ |
