<?php
/**
 * 媒体采集系统 - Token 认证中间件
 *
 * 使用方法：
 *   $user = AuthMiddleware::authenticate();
 *   if (!$user) { 返回 401 错误 }
 */

require_once __DIR__ . '/db.php';

class AuthMiddleware
{
    /**
     * 从请求头提取并验证 Token
     * @return array|null 用户信息 ['id', 'username', 'display_name']，验证失败返回 null
     */
    public static function authenticate(): ?array
    {
        $header = $_SERVER['HTTP_AUTHORIZATION']
               ?? $_SERVER['REDIRECT_HTTP_AUTHORIZATION']
               ?? '';

        // 提取 Bearer token
        if (!preg_match('/^Bearer\s+(.+)$/i', $header, $matches)) {
            return null;
        }

        $token = trim($matches[1]);

        if (empty($token)) {
            return null;
        }

        // 查询 token 是否存在且未过期
        $user = Database::queryOne(
            "SELECT u.id, u.username, u.display_name
             FROM users u
             JOIN user_tokens t ON t.user_id = u.id
             WHERE t.token = :token AND t.expires_at > datetime('now', 'localtime')
               AND u.status = 'active'",
            [':token' => $token]
        );

        return $user ?: null;
    }

    /**
     * 生成新 Token
     * @param int    $userId   用户 ID
     * @param string $device   设备名称（可选）
     * @param int    $days     有效期天数
     * @return string          生成的 Token
     */
    public static function generateToken(int $userId, string $device = '', int $days = 30): string
    {
        $token = 'tk_' . bin2hex(random_bytes(32));
        $expiresAt = date('Y-m-d H:i:s', strtotime("+{$days} days"));

        Database::execute(
            "INSERT INTO user_tokens (user_id, token, device_name, expires_at)
             VALUES (:user_id, :token, :device_name, :expires_at)",
            [
                ':user_id'     => $userId,
                ':token'       => $token,
                ':device_name' => $device,
                ':expires_at'  => $expiresAt,
            ]
        );

        return $token;
    }

    /**
     * 获取 Token 过期时间
     * @param string $token
     * @return string|null
     */
    public static function getTokenExpiry(string $token): ?string
    {
        $result = Database::queryOne(
            "SELECT expires_at FROM user_tokens WHERE token = :token",
            [':token' => $token]
        );
        return $result['expires_at'] ?? null;
    }

    /**
     * 删除 Token（退出登录用）
     * @param string $token
     */
    public static function revokeToken(string $token): void
    {
        Database::execute(
            "DELETE FROM user_tokens WHERE token = :token",
            [':token' => $token]
        );
    }

    /**
     * 删除用户所有 Token（强制退出所有设备）
     * @param int $userId
     */
    public static function revokeAllUserTokens(int $userId): void
    {
        Database::execute(
            "DELETE FROM user_tokens WHERE user_id = :user_id",
            [':user_id' => $userId]
        );
    }

    /**
     * 清理过期 Token（可定时调用）
     * @return int 清理数量
     */
    public static function cleanExpiredTokens(): int
    {
        Database::execute(
            "DELETE FROM user_tokens WHERE expires_at <= datetime('now', 'localtime')"
        );
        return Database::getQueryCount(); // 简化版，实际应用可返回具体行数
    }
}
