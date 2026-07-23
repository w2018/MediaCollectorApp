<?php
/**
 * 路由入口（兼容无 PATH_INFO 的 Nginx 配置）
 *
 * 用法：/media-api/route.php?_url=/api/v1/media
 *       /media-api/route.php（健康检查）
 *
 * 原理：Nginx 直接定位到 route.php，通过 _url 查询参数传递路由路径，
 *       不依赖 PATH_INFO，兼容所有常见 Nginx 配置。
 *
 * 注意：php://input 在部分 FastCGI 配置下仅可读取一次，
 *       因此在 route.php 中先读取并缓存到 $_SERVER 供后续使用。
 */

// 先缓存请求体（php://input 在某些 FastCGI 下只能读一次）
$_SERVER['CACHED_REQUEST_BODY'] = file_get_contents('php://input') ?? '';

// 处理 _url 路由参数
if (!empty($_GET['_url'])) {
    $_SERVER['REQUEST_URI'] = $_GET['_url'];
    unset($_GET['_url']);
}

require __DIR__ . '/index.php';
