<?php
/**
 * 媒体采集系统 - 数据库连接封装
 * 使用 PDO + SQLite3，支持读写分离、错误处理
 */

require_once __DIR__ . '/config.php';

class Database
{
    private static ?PDO $instance = null;
    private static int $queryCount = 0;

    /**
     * 获取数据库连接实例（单例）
     */
    public static function getInstance(): PDO
    {
        if (self::$instance === null) {
            $dbDir = dirname(DB_PATH);
            if (!is_dir($dbDir)) {
                mkdir($dbDir, 0755, true);
            }

            self::$instance = new PDO('sqlite:' . DB_PATH, null, null, [
                PDO::ATTR_ERRMODE            => PDO::ERRMODE_EXCEPTION,
                PDO::ATTR_DEFAULT_FETCH_MODE => PDO::FETCH_ASSOC,
                PDO::ATTR_EMULATE_PREPARES   => false,
            ]);

            // 启用 WAL 模式和外键约束
            self::$instance->exec('PRAGMA journal_mode = WAL');
            self::$instance->exec('PRAGMA foreign_keys = ON');
            self::$instance->exec('PRAGMA synchronous = NORMAL');
            self::$instance->exec('PRAGMA cache_size = -8000');       // 8MB 缓存
            self::$instance->exec('PRAGMA busy_timeout = 5000');     // 5秒超时
        }

        return self::$instance;
    }

    /**
     * 执行查询并返回所有结果
     */
    public static function query(string $sql, array $params = []): array
    {
        self::$queryCount++;
        $stmt = self::getInstance()->prepare($sql);
        $stmt->execute($params);
        return $stmt->fetchAll();
    }

    /**
     * 查询单行
     */
    public static function queryOne(string $sql, array $params = []): ?array
    {
        self::$queryCount++;
        $stmt = self::getInstance()->prepare($sql);
        $stmt->execute($params);
        $result = $stmt->fetch();
        return $result ?: null;
    }

    /**
     * 执行写操作（INSERT/UPDATE/DELETE），返回受影响行数
     */
    public static function execute(string $sql, array $params = []): int
    {
        self::$queryCount++;
        $stmt = self::getInstance()->prepare($sql);
        $stmt->execute($params);
        return $stmt->rowCount();
    }

    /**
     * 插入并返回自增 ID
     */
    public static function insert(string $sql, array $params = []): int
    {
        self::$queryCount++;
        $db = self::getInstance();
        $stmt = $db->prepare($sql);
        $stmt->execute($params);
        return (int) $db->lastInsertId();
    }

    /**
     * 构建分页 SQL 片段
     */
    public static function buildPagination(int $page, int $pageSize): string
    {
        $page = max(1, $page);
        $pageSize = min(max(1, $pageSize), PAGE_SIZE_MAX);
        $offset = ($page - 1) * $pageSize;
        return " LIMIT {$pageSize} OFFSET {$offset}";
    }

    /**
     * 构建查询条件（安全拼接）
     */
    public static function buildWhere(array $filters, array &$params): string
    {
        $conditions = [];

        foreach ($filters as $key => $value) {
            if ($value === null || $value === '') {
                continue;
            }

            // 支持运算符前缀: >, <, >=, <=, !=, LIKE
            if (is_array($value)) {
                // 数组 = IN 查询
                $placeholders = [];
                foreach ($value as $i => $v) {
                    $paramKey = "{$key}_{$i}";
                    $placeholders[] = ":{$paramKey}";
                    $params[$paramKey] = $v;
                }
                $conditions[] = "{$key} IN (" . implode(', ', $placeholders) . ")";
            } elseif (str_starts_with($value, '>=')) {
                $params[$key] = substr($value, 2);
                $conditions[] = "{$key} >= :{$key}";
            } elseif (str_starts_with($value, '<=')) {
                $params[$key] = substr($value, 2);
                $conditions[] = "{$key} <= :{$key}";
            } elseif (str_starts_with($value, '>')) {
                $params[$key] = substr($value, 1);
                $conditions[] = "{$key} > :{$key}";
            } elseif (str_starts_with($value, '<')) {
                $params[$key] = substr($value, 1);
                $conditions[] = "{$key} < :{$key}";
            } elseif (str_starts_with($value, '!=')) {
                $params[$key] = substr($value, 2);
                $conditions[] = "{$key} != :{$key}";
            } elseif (str_starts_with($value, '%')) {
                $params[$key] = $value;
                $conditions[] = "{$key} LIKE :{$key}";
            } else {
                $params[$key] = $value;
                $conditions[] = "{$key} = :{$key}";
            }
        }

        return $conditions ? 'WHERE ' . implode(' AND ', $conditions) : '';
    }

    /**
     * 获取查询次数
     */
    public static function getQueryCount(): int
    {
        return self::$queryCount;
    }

    /**
     * 关闭连接
     */
    public static function close(): void
    {
        self::$instance = null;
    }
}
