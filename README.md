# 媒体采集系统 MediaCollectorApp

一款支持照片/视频管理、分类、全文搜索和 MQTT 加密聊天的媒体采集客户端和服务端。

## 功能特性

### 📱 Android 客户端

| 功能 | 说明 |
|------|------|
| **媒体管理** | 浏览、查看照片和视频，支持分页和筛选 |
| **分类管理** | 树形文件夹/专辑分类，自定义排序 |
| **标签系统** | 多维度标签，颜色标记，批量管理 |
| **全文搜索** | 按标题、描述、作者等搜索媒体内容 |
| **用户系统** | 注册/登录，Token 自动管理，个人资料 |
| **收藏历史** | 收藏喜爱的媒体，自动记录浏览历史 |
| **MQTT 聊天** | AES-256-CBC 加密聊天室，WebSocket 连接，自动重连 |
| **投屏支持** | Google Cast 投屏到电视 |
| **深色模式** | 跟随系统/始终亮色/始终深色 |
| **API 检测** | 内置 API 兼容性检测工具 |

### 🖥 PHP 后端

| 功能 | 说明 |
|------|------|
| **RESTful API** | 34+ 端点，完整的 CRUD 操作 |
| **SQLite 存储** | 零配置数据库，WAL 模式高并发 |
| **Token 认证** | Bearer Token 认证，自动过期清理 |
| **分页搜索** | 支持分页、排序、多条件筛选 |
| **收藏/历史** | 用户维度的收藏和浏览记录 |
| **MQTT 支持** | 聊天记录同步、在线状态、心跳检测 |
| **CORS 跨域** | 支持前端跨域访问 |

## 技术栈

### Android 端
- **语言**: Kotlin
- **UI**: Jetpack Compose + Material3
- **架构**: MVVM + Hilt 依赖注入
- **网络**: Retrofit2 + OkHttp4 + Kotlinx Serialization
- **本地存储**: Room + DataStore Preferences
- **图片**: Coil 3
- **视频**: Media3 ExoPlayer
- **MQTT**: Eclipse Paho 1.2.5
- **投屏**: Google Cast SDK

### 服务端
- **语言**: PHP 8.1+
- **数据库**: SQLite3 via PDO (WAL 模式)
- **服务器**: Nginx / OpenResty + PHP-FPM

## 快速开始

### 后端部署

**环境要求**: PHP 8.1+ (需开启 pdo_sqlite 扩展), SQLite3, Nginx/OpenResty

1. 将 `server/` 目录上传到网站根目录（例如 `/var/www/html/media-api/`）

2. 确保 `server/api/data/` 目录可写（PHP 运行时自动创建 SQLite 数据库）

3. Nginx 配置示例（添加到 server 块中）：

```nginx
location /media-api/ {
    try_files $uri $uri/ /media-api/route.php?$args;
}

location ~ ^/media-api/.*\.php$ {
    include fastcgi-php.conf;
    fastcgi_pass 127.0.0.1:9000;
    fastcgi_param SCRIPT_FILENAME $document_root$fastcgi_script_name;
    include fastcgi_params;
}
```

4. 访问测试：
```
http://your-server.com/media-api/route.php?_url=/api/v1
```

### Android 客户端构建

```bash
# 克隆项目
git clone https://github.com/w2018/MediaCollectorApp.git
cd MediaCollectorApp

# 编译 Debug APK
./gradlew assembleDebug

# 编译 Release APK
./gradlew assembleRelease
```

### 配置 API 地址

打开 APP → 设置 → API 服务器配置，输入后端地址：
```
http://your-server.com/media-api/
```

## API 使用说明

所有 API 通过 `route.php` 查询参数方式访问：

```
# 健康检查
GET /media-api/route.php?_url=/api/v1

# 媒体列表
GET /media-api/route.php?_url=/api/v1/media&page=1&page_size=20

# 用户注册
POST /media-api/route.php?_url=/api/v1/auth/register
Content-Type: application/json

{"username": "test", "password": "123456"}

# 用户登录
POST /media-api/route.php?_url=/api/v1/auth/login
Content-Type: application/json

{"username": "test", "password": "123456"}
```

完整 API 端点列表访问 `/media-api/route.php?_url=/api/v1` 查看。

## 项目结构

```
MediaCollectorApp/
├── app/                           # Android 客户端
│   ├── src/main/java/com/mediacollector/app/
│   │   ├── data/                  # 数据层（API、数据库、Repository）
│   │   ├── di/                    # Hilt 依赖注入模块
│   │   ├── domain/                # 业务逻辑
│   │   └── ui/                    # UI 层（Compose 页面）
│   └── build.gradle.kts
├── server/                        # PHP 后端
│   ├── api/                       # API 入口和路由
│   │   ├── index.php              # 路由分发入口
│   │   ├── route.php              # 查询参数路由（兼容无 PATH_INFO）
│   │   ├── MediaCollectorAPI.php  # 34+ API 端点的完整实现
│   │   ├── config.php             # 数据库路径等配置
│   │   ├── db.php                 # PDO 数据库封装
│   │   └── AuthMiddleware.php     # Token 认证中间件
│   └── db/schema.sql              # 数据库建表脚本
├── .github/workflows/build.yml    # CI: 编译 APK → 发布到 Releases
├── README.md
└── build.gradle.kts
```

## 许可证

[MIT](LICENSE)
