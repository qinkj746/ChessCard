# 升级后台服务

## 环境要求

- JDK 17
- Maven 3.9+
- MySQL
- Redis 可选

## 配置

后台默认使用 MySQL，并通过环境变量读取 MySQL 和 Redis 配置：

```powershell
$env:DB_HOST='localhost'
$env:DB_PORT='3306'
$env:DB_NAME='chess_card'
$env:DB_USERNAME='root'
$env:DB_PASSWORD='你的数据库密码'

$env:REDIS_HOST='localhost'
$env:REDIS_PORT='6379'
$env:REDIS_PASSWORD='123456'
$env:REDIS_DATABASE='0'
```

没有设置环境变量时，会使用 `src/main/resources/application.yml` 里的默认值。Redis 连接失败只会在健康检查里显示 `DOWN`，不影响创建牌局。

## 启动

```powershell
$env:JAVA_HOME='D:\Java\jdk17'
$env:Path='D:\Java\jdk17\bin;' + $env:Path
mvn spring-boot:run
```

启动后可访问：

```text
GET http://localhost:8080/api/infrastructure/health
```

## 测试

默认测试只跑不依赖外部服务的规则测试：

```powershell
$env:JAVA_HOME='D:\Java\jdk17'
$env:Path='D:\Java\jdk17\bin;' + $env:Path
mvn test
```

需要验证真实 MySQL/Redis 时，先设置数据库环境变量，然后运行集成测试：

```powershell
mvn -Pintegration test
```
