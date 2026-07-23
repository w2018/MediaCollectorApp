<?php
/**
 * 媒体采集系统 - 配置文件
 */

// 错误报告（调试时开启，生产环境关闭）
error_reporting(E_ALL);
ini_set('display_errors', '0');
// 日志记录开启，方便排查问题
ini_set('log_errors', '1');
ini_set('error_log', __DIR__ . '/data/php_error.log');

// 时区
date_default_timezone_set('Asia/Shanghai');

// 数据库路径（SQLite 文件路径）
define('DB_PATH', __DIR__ . '/data/media_collector.db');

// API 配置
define('API_VERSION', 'v1');
define('API_PREFIX', '/api/' . API_VERSION);
define('PAGE_SIZE_DEFAULT', 20);
define('PAGE_SIZE_MAX', 100);

// CORS 跨域配置（按需修改允许的域名）
define('CORS_ORIGIN', '*');
define('CORS_METHODS', 'GET, POST, PUT, PATCH, DELETE, OPTIONS');
define('CORS_HEADERS', 'Content-Type, Authorization, X-Requested-With');

// 允许的来源列表（安全模式）
$allowed_origins = [
    'http://localhost',
    'http://localhost:3000',
    'http://localhost:5173',
    'http://127.0.0.1',
    'http://127.0.0.1:3000',
];
