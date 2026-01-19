# JNet 反向代理配置指南

## 概述

JNet 提供了 ReverseApp 和 ReverseProxyServer 用于快速搭建反向代理服务器，支持静态资源托管、HTTP 代理转发和 SSL/TLS 加密。

## 配置文件方式（推荐）

### reverse.config 格式

使用 YAML 格式配置反向代理规则：

```yaml
8080:                          # 监听端口
  /static:                     # URL 路径
    type: resource             # 类型：resource（静态资源）或 proxy（代理）
    path: file:/var/www/static # 资源路径
    order: 2                   # 优先级（数字越小优先级越高）
  /api/*:                      # 前缀匹配（* 表示匹配所有子路径）
    type: proxy
    path: http://backend:8080/
    order: 1
  ssl:                         # SSL 配置（可选）
    enable: true
    cert: /path/to/keystore.jks
    password: your_password

9000:                          # 可配置多个端口
  /:
    type: resource
    path: classpath:/static
    order: 1
```

### 配置说明

**路径类型：**
- `resource` - 静态资源托管
- `proxy` - HTTP 代理转发

**路径匹配：**
- `/api/users` - 完全匹配
- `/api/*` - 前缀匹配（以 * 结尾）

**资源路径格式：**
- `file:/absolute/path` - 文件系统绝对路径
- `classpath:/path` - classpath 资源

**优先级：**
- `order` 值越小优先级越高
- 建议：静态资源 order=2，代理 order=1

### 启动 ReverseApp

```bash
# 将 reverse.config 放在 jar 包同级目录
java -jar jnet-app.jar
```

ReverseApp 会自动读取 `reverse.config` 并启动所有配置的端口。

## 编程方式

### 基础示例

```java
import cc.jfire.jnet.extend.reverse.proxy.ReverseProxyServer;
import cc.jfire.jnet.extend.reverse.proxy.api.ResourceConfig;
import java.util.ArrayList;
import java.util.List;

List<ResourceConfig> configs = new ArrayList<>();

// 静态资源
configs.add(ResourceConfig.io("/static", "file:/var/www/static", 2));

// 代理转发（完全匹配）
configs.add(ResourceConfig.fullMatch("/api/users", "http://backend:8080/", 1));

// 代理转发（前缀匹配）
configs.add(ResourceConfig.prefixMatch("/api/*", "http://backend:8080/", 1));

// 启动服务器
ReverseProxyServer server = new ReverseProxyServer(8080, configs);
server.start();
```

### 带 SSL 的示例

```java
import cc.jfire.jnet.extend.reverse.app.SslInfo;

SslInfo sslInfo = new SslInfo()
    .setEnable(true)
    .setCert("/path/to/keystore.jks")
    .setPassword("your_password");

ReverseProxyServer server = new ReverseProxyServer(8443, configs, sslInfo);
server.start();
```

## SSL 证书配置

### 生成自签名证书（测试用）

```bash
keytool -genkeypair -alias myserver \
  -keyalg RSA -keysize 2048 \
  -validity 365 \
  -keystore keystore.jks \
  -storepass password123 \
  -dname "CN=localhost, OU=Dev, O=MyCompany, L=City, ST=State, C=CN"
```

### 使用 Let's Encrypt 证书

```bash
# 1. 获取证书（使用 certbot）
certbot certonly --standalone -d yourdomain.com

# 2. 转换为 JKS 格式
openssl pkcs12 -export \
  -in /etc/letsencrypt/live/yourdomain.com/fullchain.pem \
  -inkey /etc/letsencrypt/live/yourdomain.com/privkey.pem \
  -out cert.p12 -name myserver

keytool -importkeystore \
  -srckeystore cert.p12 -srcstoretype PKCS12 \
  -destkeystore keystore.jks -deststoretype JKS
```

### 配置文件中使用

```yaml
8443:
  /:
    type: resource
    path: file:/var/www
    order: 1
  ssl:
    enable: true
    cert: /path/to/keystore.jks
    password: password123
```

## 完整配置示例

### 示例 1：静态网站 + API 代理

```yaml
80:
  /:
    type: resource
    path: file:/var/www/html
    order: 2
  /api/*:
    type: proxy
    path: http://localhost:3000/
    order: 1

443:
  /:
    type: resource
    path: file:/var/www/html
    order: 2
  /api/*:
    type: proxy
    path: http://localhost:3000/
    order: 1
  ssl:
    enable: true
    cert: /etc/ssl/keystore.jks
    password: secure_password
```

### 示例 2：多服务代理

```yaml
8080:
  /user/*:
    type: proxy
    path: http://user-service:8081/
    order: 1
  /order/*:
    type: proxy
    path: http://order-service:8082/
    order: 1
  /product/*:
    type: proxy
    path: http://product-service:8083/
    order: 1
```

### 示例 3：前端 + 后端分离

```yaml
80:
  /:
    type: resource
    path: classpath:/static
    order: 2
  /api/*:
    type: proxy
    path: http://backend:8080/
    order: 1
```

## ResourceConfig API

### 静态资源

```java
// 文件系统路径
ResourceConfig.io("/static", "file:/var/www/static", 2)

// Classpath 资源
ResourceConfig.io("/", "classpath:/public", 2)
```

### 代理转发

```java
// 完全匹配
ResourceConfig.fullMatch("/api/users", "http://backend:8080/", 1)

// 前缀匹配
ResourceConfig.prefixMatch("/api/*", "http://backend:8080/", 1)
```

## 常见问题

### 1. 路径匹配优先级

- 优先级由 `order` 值决定，数字越小优先级越高
- 相同 order 时，完全匹配优先于前缀匹配
- 建议：代理 order=1，静态资源 order=2

### 2. SSL 证书路径

- 使用绝对路径：`/etc/ssl/keystore.jks`
- 相对路径相对于 jar 包所在目录

### 3. 代理路径处理

```yaml
# 请求：/api/users
# 配置：/api/* -> http://backend:8080/
# 转发：http://backend:8080/users

# 请求：/api/users
# 配置：/api/users -> http://backend:8080/
# 转发：http://backend:8080/
```

### 4. 静态资源 404

- 检查路径格式：`file:` 或 `classpath:`
- 确认文件权限和路径存在
- 使用绝对路径避免相对路径问题

## 最佳实践

1. **使用配置文件** - 便于维护和热更新
2. **合理设置优先级** - 避免路径冲突
3. **启用 HTTPS** - 生产环境必须使用 SSL
4. **证书管理** - 定期更新证书，使用 Let's Encrypt
5. **日志监控** - 监控代理转发和错误日志
