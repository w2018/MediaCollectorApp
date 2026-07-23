<?php
/**
 * 媒体采集系统 - API 入口
 *
 * 使用方式：
 *   PHP 内置服务器： php -S localhost:8000 -t api/
 *   Nginx/Apache：  配置 URL 重写指向此文件
 */

// 加载配置
require_once __DIR__ . '/config.php';
require_once __DIR__ . '/AuthMiddleware.php';
require_once __DIR__ . '/MediaCollectorAPI.php';

// CORS 跨域处理
header("Access-Control-Allow-Origin: " . CORS_ORIGIN);
header("Access-Control-Allow-Methods: " . CORS_METHODS);
header("Access-Control-Allow-Headers: " . CORS_HEADERS);
header("Access-Control-Max-Age: 86400");

// 预检请求直接返回
if ($_SERVER['REQUEST_METHOD'] === 'OPTIONS') {
    http_response_code(204);
    exit;
}

// 初始化数据库（自动建表）
initDatabase();

// 路由处理
$method = $_SERVER['REQUEST_METHOD'];
$uri = $_SERVER['REQUEST_URI'];

// 移除查询参数用于路由匹配
$uriPath = parse_url($uri, PHP_URL_PATH);

// 处理 script-name 前缀路径（如 /media-api/index.php/api/v1/media）
$scriptName = $_SERVER['SCRIPT_NAME'] ?? '';
if ($scriptName !== '/' && $scriptName !== '' && str_starts_with($uriPath, $scriptName)) {
    $trimmed = substr($uriPath, strlen($scriptName));
    $uriPath = $trimmed !== '' ? $trimmed : '/';
}

// 健康检查
if ($uriPath === '/' || $uriPath === '/api' || $uriPath === '/api/v1') {
    header('Content-Type: application/json; charset=utf-8');
    echo json_encode([
        'success' => true,
        'message' => '媒体采集系统 API',
        'version' => API_VERSION,
        'endpoints' => [
            'GET    /api/v1/media',
            'GET    /api/v1/media/{id}',
            'POST   /api/v1/media',
            'PUT    /api/v1/media/{id}',
            'DELETE /api/v1/media/{id}',
            'GET    /api/v1/collections',
            'GET    /api/v1/collections/tree',
            'GET    /api/v1/collections/{id}',
            'POST   /api/v1/collections',
            'POST   /api/v1/collections/{id}/items',
            'GET    /api/v1/tags',
            'POST   /api/v1/media/{id}/tags',
            'GET    /api/v1/search',
            'GET    /api/v1/stats',
            // ── 新增 ──
            'POST   /api/v1/auth/register    ← 用户注册',
            'POST   /api/v1/auth/login       ← 用户登录',
            'POST   /api/v1/auth/logout      ← 退出登录',
            'GET    /api/v1/auth/me          ← 当前用户',
            'GET    /api/v1/user/favorites   ← 收藏列表',
            'POST   /api/v1/user/favorites   ← 添加收藏',
            'DELETE /api/v1/user/favorites/{mediaId}  ← 取消收藏',
            'GET    /api/v1/user/history     ← 浏览历史',
            'POST   /api/v1/user/history     ← 记录历史',
            'DELETE /api/v1/user/history     ← 清空历史',
            'DELETE /api/v1/user/history/{mediaId}  ← 删除单条历史',
            'GET    /api/v1/system/check     ← 兼容性检测',
            // ── 聊天 ──
            'GET    /api/v1/chat/rooms       ← 聊天室列表',
            'GET    /api/v1/chat/messages    ← 聊天历史消息',
            'POST   /api/v1/chat/messages    ← 同步消息到服务端',
            'DELETE /api/v1/chat/messages    ← 清空自己的聊天记录',
            'POST   /api/v1/chat/heartbeat   ← 心跳（更新在线状态）',
            'GET    /api/v1/chat/online      ← 在线用户列表',
            'GET    /api/v1/chat/online/count ← 在线人数（轻量）',
        ],
    ], JSON_UNESCAPED_UNICODE | JSON_PRETTY_PRINT);
    exit;
}

// 检查是否是 API 请求
if (!str_starts_with($uriPath, '/api/v1/')) {
    http_response_code(404);
    header('Content-Type: application/json; charset=utf-8');
    echo json_encode([
        'success' => false,
        'code'    => 404,
        'message' => '接口不存在，请访问 /api/v1 查看可用端点',
    ], JSON_UNESCAPED_UNICODE);
    exit;
}

// 处理 API 请求
$api = new MediaCollectorAPI();
$api->handle($method, $uri);

// ============================================================
// 初始化数据库
// ============================================================
function initDatabase(): void
{
    $dbDir = dirname(DB_PATH);

    if (!is_dir($dbDir)) {
        mkdir($dbDir, 0755, true);
    }

    $schemaFile = __DIR__ . '/../db/schema.sql';
    if (!file_exists($schemaFile)) {
        return; // 没有 schema 文件时静默跳过
    }

    try {
        $db = new PDO('sqlite:' . DB_PATH, null, null, [
            PDO::ATTR_ERRMODE => PDO::ERRMODE_EXCEPTION,
        ]);

        $sql = file_get_contents($schemaFile);
        // 使用完整 SQL 交给 SQLite 解析（sqlite3_exec 能正确处理 CREATE TRIGGER 内的分号，
        // 避免 explode(';') 切断触发器内语句导致后续 CREATE TABLE 不执行的问题）
        $db->exec($sql);

        $db = null;
    } catch (Exception $e) {
        // 建表失败时记录错误但不中断请求
        error_log('数据库初始化失败：' . $e->getMessage());
    }
}
