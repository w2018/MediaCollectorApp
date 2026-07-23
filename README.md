# MediaCollectorApp 媒体采集系统

[![GitHub Release](https://img.shields.io/github/v/release/w2018/MediaCollectorApp)](https://github.com/w2018/MediaCollectorApp/releases)
[![Build](https://github.com/w2018/MediaCollectorApp/actions/workflows/build.yml/badge.svg)](https://github.com/w2018/MediaCollectorApp/actions/workflows/build.yml)
[![License](https://img.shields.io/badge/license-MIT-blue.svg)](LICENSE)

> 一款基于 PHP + Android 的媒体采集与管理工具。
>
> 作者：**曾先生**（w2018）

---

## 📱 功能特性

| 模块 | 功能 |
|------|------|
| 🏠 **媒体浏览** | 照片/视频网格展示，随机排序，无限滚动加载 |
| 📷 **图片查看** | 全屏浏览，双指缩放，左右滑动切换，图片分享 |
| 🎬 **视频播放** | HLS/MP4 播放，全屏切换，进度控制，投屏 |
| ❤️ **收藏系统** | 添加/取消收藏，图片/视频分类，跨设备同步 |
| 🕐 **浏览历史** | 自动记录观看历史，分类查看，单条/全部删除 |
| 🔍 **搜索功能** | 全文检索（标题/描述/作者/来源），分类筛选，分页 |
| 💬 **加密聊天** | MQTT 实时聊天，AES 加密，消息同步，在线用户 |
| ⬇️ **下载管理** | 前台服务下载，HLS 流解析合并，进度通知 |
| 📤 **图片分享** | 系统分享菜单，FileProvider 临时文件 |
| 🎨 **深色模式** | 跟随系统/强制深色/强制亮色 |
| 🔄 **数据同步** | 收藏/历史/聊天跟随用户账户，跨设备恢复 |

## 🧱 技术栈

### 前端（Android）

| 技术 | 用途 |
|------|------|
| **Kotlin** + **Jetpack Compose** | UI 框架 |
| **Material 3** | 设计语言 |
| **MVVM** + **Hilt** | 架构 + 依赖注入 |
| **Navigation Compose** | 页面路由 |
| **Retrofit2** + **OkHttp4** | 网络请求 |
| **kotlinx.serialization** | JSON 序列化 |
| **Coil 3** | 图片加载 |
| **Media3 ExoPlayer** | 视频播放 |
| **Room** | 本地数据库 |
| **DataStore** | 键值存储 |
| **MQTT Paho** | 实时聊天 |

### 后端（PHP）

| 技术 | 用途 |
|------|------|
| **PHP 8.3+** | 运行时 |
| **SQLite3** (PDO) | 数据库 |
| **Bearer Token** | 用户认证 |
| **RESTful API** | 接口设计 |

## 📁 项目结构

```
MediaCollectorApp/
├── app/
│   └── src/main/java/com/mediacollector/app/
│       ├── data/
│       │   ├── local/              # Room 数据库、DAO、实体
│       │   ├── remote/
│       │   │   ├── api/            # Retrofit 接口定义
│       │   │   ├── dto/            # 数据传输对象
│       │   │   └── interceptor/    # OkHttp 拦截器
│       │   ├── repository/         # 数据仓库
│       │   └── settings/           # 设置存储
│       ├── di/                     # Hilt 依赖注入模块
│       ├── ui/
│       │   ├── auth/               # 登录/注册/个人中心
│       │   ├── chat/               # MQTT 聊天室
│       │   ├── collection/         # 集合管理
│       │   ├── detail/             # 媒体详情
│       │   ├── download/           # 下载管理
│       │   ├── favorite/           # 收藏列表
│       │   ├── history/            # 浏览历史
│       │   ├── media/              # 首页媒体网格
│       │   ├── navigation/         # 导航图
│       │   ├── photo/              # 图片全屏查看
│       │   ├── search/             # 搜索
│       │   ├── settings/           # 设置/关于
│       │   └── video/              # 视频播放
│       └── util/                   # 工具类
├── server/api/                     # PHP 后端源码
│   ├── index.php                   # 路由入口
│   ├── route.php                   # 查询参数路由
│   ├── MediaCollectorAPI.php       # API 实现
│   ├── AuthMiddleware.php          # 认证中间件
│   ├── config.php                  # 配置文件
│   └── db.php                      # 数据库连接
├── docs/                           # 设计文档
├── .github/workflows/build.yml     # CI: 编译 APK → 发布到 Releases
└── app/build.gradle.kts            # Android 构建配置
```

## 🚀 后端部署

### 环境要求
- PHP 8.3+（需开启 `pdo_sqlite`、`mbstring` 扩展）
- SQLite3
- Nginx / Apache

### 安装步骤

```bash
# 1. 将 server/api/ 部署到 Web 服务器
sudo cp -r server/api /var/www/html/media-api

# 2. 设置目录权限
sudo chmod -R 755 /var/www/html/media-api
sudo chown -R www-data:www-data /var/www/html/media-api/data

# 3. Nginx 配置（PHP-FPM）
# 确保 .php 文件能正常解析

# 4. 重启服务
sudo systemctl restart nginx php8.3-fpm

# 5. 验证安装
curl http://localhost/media-api/route.php?_url=/api/v1
```

### 注意事项
- 数据库文件位于 `data/media_collector.db`，首次访问时自动创建
- 媒体资源 URL 为外部链接，后端不存储实际文件
- 如需导入大量数据，可直接操作 SQLite 文件

## 📲 前端编译

### 环境要求
- Android Studio / IntelliJ IDEA
- JDK 17
- Android SDK 35
- Gradle 8.11+

### 编译步骤

```bash
# Debug APK（无需签名配置）
./gradlew assembleDebug

# Release APK（CI 自动使用 debug 签名）
./gradlew assembleRelease

# 安装到设备
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

### 配置 API 地址
打开 APP → **我 → 设置 → 服务器配置**，输入后端地址：
```
http://your-server-ip/media-api/
```

## 🔧 API 使用

所有 API 通过 `route.php` 查询参数方式访问：

```
# 端点列表
GET /media-api/route.php?_url=/api/v1

# 媒体列表（分页）
GET /media-api/route.php?_url=/api/v1/media&page=1&page_size=20

# 用户登录
POST /media-api/route.php?_url=/api/v1/auth/login
Content-Type: application/json

{"username": "admin", "password": "your_password"}

# 搜索
GET /media-api/route.php?_url=/api/v1/search&keyword=xxx
```

完整 API 文档见 `docs/` 目录。

## 📄 开源协议

本项目基于 **MIT 协议** 开源。

## 👤 作者

**曾先生**（w2018）

- GitHub: [https://github.com/w2018](https://github.com/w2018)
- 项目地址: [https://github.com/w2018/MediaCollectorApp](https://github.com/w2018/MediaCollectorApp)

## ⭐ 贡献

欢迎提交 [Issue](https://github.com/w2018/MediaCollectorApp/issues) 和 [Pull Request](https://github.com/w2018/MediaCollectorApp/pulls)！
