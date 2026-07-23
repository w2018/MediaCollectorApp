<?php
/**
 * 媒体采集系统 - API 处理器
 *
 * 路由对照：
 *   GET    /api/v1/media                    → 媒体列表（分页、筛选）
 *   GET    /api/v1/media/:id                → 媒体详情（含标签、元数据）
 *   POST   /api/v1/media                    → 新增媒体
 *   PUT    /api/v1/media/:id                → 更新媒体
 *   PATCH  /api/v1/media/:id                → 部分更新媒体
 *   DELETE /api/v1/media/:id                → 删除媒体（软删除）
 *
 *   GET    /api/v1/media/:id/exif           → 媒体 EXIF 信息
 *   PUT    /api/v1/media/:id/exif           → 更新 EXIF
 *
 *   GET    /api/v1/collections              → 集合列表（树形）
 *   GET    /api/v1/collections/:id          → 集合详情（含媒体列表）
 *   POST   /api/v1/collections              → 新增集合
 *   PUT    /api/v1/collections/:id          → 更新集合
 *   DELETE /api/v1/collections/:id          → 删除集合
 *
 *   POST   /api/v1/collections/:id/items    → 向集合添加媒体
 *   DELETE /api/v1/collections/:id/items/:mediaId  → 从集合移除媒体
 *
 *   GET    /api/v1/tags                     → 标签列表
 *   POST   /api/v1/tags                     → 新增标签
 *   DELETE /api/v1/tags/:id                 → 删除标签
 *
 *   POST   /api/v1/media/:id/tags           → 给媒体添加标签
 *   DELETE /api/v1/media/:id/tags/:tagId    → 移除媒体标签
 *
 *   GET    /api/v1/search                   → 搜索（全文检索）
 *   GET    /api/v1/stats                    → 统计信息
 */

require_once __DIR__ . '/db.php';
require_once __DIR__ . '/AuthMiddleware.php';

class MediaCollectorAPI
{
    private PDO $db;
    private array $response = [
        'success' => true,
        'code'    => 200,
        'message' => '',
        'data'    => null,
    ];

    public function __construct()
    {
        $this->db = Database::getInstance();
    }

    // ============================================================
    // 路由分发
    // ============================================================
    public function handle(string $method, string $uri): void
    {
        try {
            // 解析路径（去掉 /api/v1 前缀）
            $path = preg_replace('#^/api/v1#', '', parse_url($uri, PHP_URL_PATH));
            $path = rtrim($path, '/') ?: '/';
            $queryParams = $_GET;

            // 路由匹配
            $routes = [
                // 媒体
                ['GET',    '/media',              'listMedia'],
                ['GET',    '/media/stats',        'mediaStats'],
                ['GET',    '/media/{id}',         'getMedia'],
                ['GET',    '/media/{id}/exif',    'getExif'],
                ['POST',   '/media',              'createMedia'],
                ['PUT',    '/media/{id}',         'updateMedia'],
                ['PATCH',  '/media/{id}',         'patchMedia'],
                ['DELETE', '/media/{id}',         'deleteMedia'],
                ['PUT',    '/media/{id}/exif',    'updateExif'],
                ['POST',   '/media/{id}/tags',    'addMediaTag'],
                ['DELETE', '/media/{id}/tags/{tagId}', 'removeMediaTag'],
                // 集合
                ['GET',    '/collections',        'listCollections'],
                ['GET',    '/collections/tree',   'collectionTree'],
                ['GET',    '/collections/{id}',   'getCollection'],
                ['POST',   '/collections',        'createCollection'],
                ['PUT',    '/collections/{id}',   'updateCollection'],
                ['DELETE', '/collections/{id}',   'deleteCollection'],
                ['POST',   '/collections/{id}/items', 'addCollectionItem'],
                ['DELETE', '/collections/{id}/items/{mediaId}', 'removeCollectionItem'],
                // 标签
                ['GET',    '/tags',               'listTags'],
                ['POST',   '/tags',               'createTag'],
                ['DELETE', '/tags/{id}',           'deleteTag'],
                // 通用
                ['GET',    '/search',             'search'],
                ['GET',    '/stats',              'stats'],
                // 认证（无需 Token）
                ['POST',   '/auth/register',      'register'],
                ['POST',   '/auth/login',         'login'],
                // 认证（需要 Token）
                ['POST',   '/auth/logout',        'logout'],
                ['GET',    '/auth/me',            'me'],
                // 用户收藏
                ['GET',    '/user/favorites',     'listFavorites'],
                ['GET',    '/user/favorites/check', 'checkFavorites'],
                ['POST',   '/user/favorites',     'addFavorite'],
                ['DELETE', '/user/favorites/{mediaId}', 'removeFavorite'],
                // 用户历史
                ['GET',    '/user/history',       'listHistory'],
                ['POST',   '/user/history',       'addHistory'],
                ['DELETE', '/user/history',       'clearHistory'],
                ['DELETE', '/user/history/{mediaId}', 'removeHistory'],
                // 系统
                ['GET',    '/system/check',       'systemCheck'],
                // 聊天
                ['GET',    '/chat/rooms',         'chatRooms'],
                ['GET',    '/chat/messages',      'chatMessages'],
                ['POST',   '/chat/messages',      'chatSyncMessage'],
                ['DELETE', '/chat/messages',      'chatClearMessages'],
                ['POST',   '/chat/heartbeat',     'chatHeartbeat'],
                ['GET',    '/chat/online',        'chatOnline'],
                ['GET',    '/chat/online/count',  'chatOnlineCount'],
            ];

            $matched = false;
            foreach ($routes as [$routeMethod, $routePath, $handler]) {
                if ($method !== $routeMethod) continue;

                $regex = $this->pathToRegex($routePath);
                if (preg_match($regex, $path, $matches)) {
                    // 提取路径参数
                    $params = array_filter($matches, 'is_string', ARRAY_FILTER_USE_KEY);
                    // 合并查询参数
                    $params['query'] = $queryParams;
                    // 合并请求体
                    $params['body'] = $this->getRequestBody();

                    $this->$handler($params);
                    $matched = true;
                    break;
                }
            }

            if (!$matched) {
                $this->error(404, '接口不存在');
            }
        } catch (PDOException $e) {
            $this->error(500, '数据库错误：' . $e->getMessage());
        } catch (Exception $e) {
            $this->error(500, $e->getMessage());
        }

        $this->sendResponse();
    }

    // ============================================================
    // 媒体相关
    // ============================================================

    /**
     * 媒体列表（分页 + 筛选 + 排序）
     */
    private function listMedia(array $params): void
    {
        $query = $params['query'];
        $page = max(1, (int)($query['page'] ?? 1));
        $pageSize = min(max(1, (int)($query['page_size'] ?? PAGE_SIZE_DEFAULT)), PAGE_SIZE_MAX);

        // 组装筛选条件
        $filters = [];
        $whereParams = [];

        if (!empty($query['type']))       $filters['type'] = $query['type'];
        if (!empty($query['status']))     $filters['status'] = $query['status'];
        if (!empty($query['source']))     $filters['source'] = $query['source'];
        if (!empty($query['is_favorite'])) $filters['is_favorite'] = (int)$query['is_favorite'];
        if (!empty($query['author']))     $filters['author'] = '%' . $query['author'] . '%';
        if (!empty($query['date_from']))  $filters['created_at'] = '>=' . $query['date_from'];
        if (!empty($query['date_to']))    $filters['created_at'] = '<=' . $query['date_to'];

        // 关键词搜索（标题+描述）
        $keywordWhere = '';
        if (!empty($query['keyword'])) {
            $keywordWhere = " AND (title LIKE :keyword OR description LIKE :keyword2)";
            $whereParams['keyword'] = '%' . $query['keyword'] . '%';
            $whereParams['keyword2'] = '%' . $query['keyword'] . '%';
        }

        $where = Database::buildWhere($filters, $whereParams);
        $pagination = Database::buildPagination($page, $pageSize);

        // 排序
        $sortMap = [
            'created_at'  => 'created_at',
            'updated_at'  => 'updated_at',
            'title'       => 'title',
            'file_size'   => 'file_size',
            'sort_order'  => 'sort_order',
            'rating'      => 'rating',
        ];
        $sortField = $sortMap[$query['sort'] ?? 'created_at'] ?? 'created_at';
        $sortDir = strtoupper($query['order'] ?? 'DESC') === 'ASC' ? 'ASC' : 'DESC';

        // 总数
        $countSql = "SELECT COUNT(*) as total FROM media_items {$where} {$keywordWhere}";
        $total = Database::queryOne($countSql, $whereParams)['total'];

        // 列表
        $listSql = "SELECT id, title, description, url, local_path, type, mime_type,
                           file_size, width, height, duration, thumbnail_url,
                           source, author, status, is_favorite, rating, sort_order,
                           created_at, updated_at
                    FROM media_items {$where} {$keywordWhere}
                    ORDER BY {$sortField} {$sortDir}
                    {$pagination}";
        $list = Database::query($listSql, $whereParams);

        $this->response['data'] = [
            'items'      => $list,
            'pagination' => [
                'page'       => $page,
                'page_size'  => $pageSize,
                'total'      => (int)$total,
                'total_pages'=> (int)ceil($total / max($pageSize, 1)),
            ],
            'filters' => [
                'type'   => Database::query("SELECT type, COUNT(*) as count FROM media_items GROUP BY type"),
                'source' => Database::query("SELECT source, COUNT(*) as count FROM media_items WHERE source != '' GROUP BY source ORDER BY count DESC LIMIT 20"),
                'status' => Database::query("SELECT status, COUNT(*) as count FROM media_items GROUP BY status"),
            ],
        ];
    }

    /**
     * 媒体详情
     */
    private function getMedia(array $params): void
    {
        $id = (int)$params['id'];
        $media = Database::queryOne(
            "SELECT * FROM media_items WHERE id = ?",
            [$id]
        );

        if (!$media) {
            $this->error(404, '媒体不存在');
            return;
        }

        // 获取标签
        $media['tags'] = Database::query(
            "SELECT t.id, t.name, t.slug, t.type, t.color
             FROM tags t
             JOIN media_tags mt ON mt.tag_id = t.id
             WHERE mt.media_id = ?
             ORDER BY t.sort_order",
            [$id]
        );

        // 获取所属集合
        $media['collections'] = Database::query(
            "SELECT c.id, c.name, c.type
             FROM collections c
             JOIN collection_items ci ON ci.collection_id = c.id
             WHERE ci.media_id = ?",
            [$id]
        );

        // 获取扩展元数据
        $media['metadata'] = Database::query(
            "SELECT meta_key, meta_value, value_type FROM metadata WHERE target_type = 'media' AND target_id = ?",
            [$id]
        );

        // 获取 EXIF（仅照片）
        if ($media['type'] === 'photo') {
            $media['exif'] = Database::queryOne(
                "SELECT * FROM exif_data WHERE media_id = ?",
                [$id]
            );
        }

        // 记录访问
        Database::execute(
            "INSERT INTO access_log (media_id, action) VALUES (?, 'view')",
            [$id]
        );

        $this->response['data'] = $media;
    }

    /**
     * 新增媒体
     */
    private function createMedia(array $params): void
    {
        $body = $params['body'];

        // 必填校验
        if (empty($body['url'])) {
            $this->error(400, '链接地址不能为空');
            return;
        }
        if (empty($body['type'])) {
            $this->error(400, '媒体类型不能为空（photo/video）');
            return;
        }

        $id = Database::insert(
            "INSERT INTO media_items (title, description, url, local_path, type, mime_type,
             file_size, width, height, duration, thumbnail_url, source, source_id,
             source_url, author, status, is_favorite, rating, sort_order)
             VALUES (:title, :description, :url, :local_path, :type, :mime_type,
             :file_size, :width, :height, :duration, :thumbnail_url, :source, :source_id,
             :source_url, :author, :status, :is_favorite, :rating, :sort_order)",
            [
                ':title'        => $body['title'] ?? '',
                ':description'  => $body['description'] ?? '',
                ':url'          => $body['url'],
                ':local_path'   => $body['local_path'] ?? '',
                ':type'         => $body['type'],
                ':mime_type'    => $body['mime_type'] ?? '',
                ':file_size'    => (int)($body['file_size'] ?? 0),
                ':width'        => (int)($body['width'] ?? 0),
                ':height'       => (int)($body['height'] ?? 0),
                ':duration'     => (int)($body['duration'] ?? 0),
                ':thumbnail_url'=> $body['thumbnail_url'] ?? '',
                ':source'       => $body['source'] ?? '',
                ':source_id'    => $body['source_id'] ?? '',
                ':source_url'   => $body['source_url'] ?? '',
                ':author'       => $body['author'] ?? '',
                ':status'       => $body['status'] ?? 'active',
                ':is_favorite'  => (int)($body['is_favorite'] ?? 0),
                ':rating'       => (int)($body['rating'] ?? 0),
                ':sort_order'   => (int)($body['sort_order'] ?? 0),
            ]
        );

        // 处理标签关联
        if (!empty($body['tags']) && is_array($body['tags'])) {
            foreach ($body['tags'] as $tagId) {
                Database::execute(
                    "INSERT OR IGNORE INTO media_tags (media_id, tag_id) VALUES (?, ?)",
                    [$id, (int)$tagId]
                );
            }
        }

        // 处理扩展元数据
        if (!empty($body['metadata']) && is_array($body['metadata'])) {
            foreach ($body['metadata'] as $key => $value) {
                Database::execute(
                    "INSERT INTO metadata (target_type, target_id, meta_key, meta_value) VALUES ('media', ?, ?, ?)",
                    [$id, $key, is_array($value) ? json_encode($value, JSON_UNESCAPED_UNICODE) : $value]
                );
            }
        }

        $this->response['code'] = 201;
        $this->response['message'] = '创建成功';
        $this->response['data'] = ['id' => $id];
    }

    /**
     * 更新媒体
     */
    private function updateMedia(array $params): void
    {
        $id = (int)$params['id'];
        $body = $params['body'];

        $existing = Database::queryOne("SELECT id FROM media_items WHERE id = ?", [$id]);
        if (!$existing) {
            $this->error(404, '媒体不存在');
            return;
        }

        Database::execute(
            "UPDATE media_items SET
                title = :title, description = :description, url = :url,
                local_path = :local_path, type = :type, mime_type = :mime_type,
                file_size = :file_size, width = :width, height = :height,
                duration = :duration, thumbnail_url = :thumbnail_url,
                source = :source, source_id = :source_id, source_url = :source_url,
                author = :author, status = :status, is_favorite = :is_favorite,
                rating = :rating, sort_order = :sort_order
             WHERE id = :id",
            [
                ':id'           => $id,
                ':title'        => $body['title'] ?? '',
                ':description'  => $body['description'] ?? '',
                ':url'          => $body['url'] ?? '',
                ':local_path'   => $body['local_path'] ?? '',
                ':type'         => $body['type'] ?? 'photo',
                ':mime_type'    => $body['mime_type'] ?? '',
                ':file_size'    => (int)($body['file_size'] ?? 0),
                ':width'        => (int)($body['width'] ?? 0),
                ':height'       => (int)($body['height'] ?? 0),
                ':duration'     => (int)($body['duration'] ?? 0),
                ':thumbnail_url'=> $body['thumbnail_url'] ?? '',
                ':source'       => $body['source'] ?? '',
                ':source_id'    => $body['source_id'] ?? '',
                ':source_url'   => $body['source_url'] ?? '',
                ':author'       => $body['author'] ?? '',
                ':status'       => $body['status'] ?? 'active',
                ':is_favorite'  => (int)($body['is_favorite'] ?? 0),
                ':rating'       => (int)($body['rating'] ?? 0),
                ':sort_order'   => (int)($body['sort_order'] ?? 0),
            ]
        );

        $this->response['message'] = '更新成功';
        $this->response['data'] = ['id' => $id];
    }

    /**
     * 部分更新媒体（仅修改传入的字段）
     */
    private function patchMedia(array $params): void
    {
        $id = (int)$params['id'];
        $body = $params['body'];

        $existing = Database::queryOne("SELECT id FROM media_items WHERE id = ?", [$id]);
        if (!$existing) {
            $this->error(404, '媒体不存在');
            return;
        }

        // 只更新传入了的字段
        $allowedFields = [
            'title', 'description', 'url', 'local_path', 'type', 'mime_type',
            'file_size', 'width', 'height', 'duration', 'thumbnail_url',
            'source', 'source_id', 'source_url', 'author',
            'status', 'is_favorite', 'rating', 'sort_order', 'md5_hash',
        ];

        $setClauses = [];
        $updateParams = [':id' => $id];

        foreach ($allowedFields as $field) {
            if (array_key_exists($field, $body)) {
                $setClauses[] = "{$field} = :{$field}";
                $updateParams[":{$field}"] = $body[$field];
            }
        }

        if (!empty($setClauses)) {
            Database::execute(
                "UPDATE media_items SET " . implode(', ', $setClauses) . " WHERE id = :id",
                $updateParams
            );
        }

        $this->response['message'] = '更新成功';
        $this->response['data'] = ['id' => $id];
    }

    /**
     * 删除媒体（软删除）
     */
    private function deleteMedia(array $params): void
    {
        $id = (int)$params['id'];
        Database::execute(
            "UPDATE media_items SET status = 'trashed' WHERE id = ?",
            [$id]
        );
        $this->response['message'] = '已移入回收站';
    }

    /**
     * 媒体统计
     */
    private function mediaStats(array $params): void
    {
        $this->response['data'] = Database::query("
            SELECT
                type,
                COUNT(*) as total,
                SUM(file_size) as total_size
            FROM media_items
            WHERE status = 'active'
            GROUP BY type
        ");
    }

    // ============================================================
    // EXIF 相关
    // ============================================================

    private function getExif(array $params): void
    {
        $id = (int)$params['id'];
        $exif = Database::queryOne("SELECT * FROM exif_data WHERE media_id = ?", [$id]);
        $this->response['data'] = $exif ?: (object)[];
    }

    private function updateExif(array $params): void
    {
        $id = (int)$params['id'];
        $body = $params['body'];

        $existing = Database::queryOne("SELECT id FROM exif_data WHERE media_id = ?", [$id]);

        $data = [
            ':media_id'      => $id,
            ':camera_make'   => $body['camera_make'] ?? '',
            ':camera_model'  => $body['camera_model'] ?? '',
            ':lens_model'    => $body['lens_model'] ?? '',
            ':focal_length'  => $body['focal_length'] ?? '',
            ':aperture'      => $body['aperture'] ?? '',
            ':shutter_speed' => $body['shutter_speed'] ?? '',
            ':iso'           => (int)($body['iso'] ?? 0),
            ':flash'         => (int)($body['flash'] ?? 0),
            ':gps_lat'       => $body['gps_lat'] ?? null,
            ':gps_lng'       => $body['gps_lng'] ?? null,
            ':gps_altitude'  => $body['gps_altitude'] ?? null,
            ':gps_accuracy'  => $body['gps_accuracy'] ?? null,
            ':location_name' => $body['location_name'] ?? '',
            ':taken_at'      => $body['taken_at'] ?? null,
            ':color_space'   => $body['color_space'] ?? '',
            ':orientation'   => (int)($body['orientation'] ?? 0),
        ];

        if ($existing) {
            $sql = "UPDATE exif_data SET
                camera_make = :camera_make, camera_model = :camera_model,
                lens_model = :lens_model, focal_length = :focal_length,
                aperture = :aperture, shutter_speed = :shutter_speed,
                iso = :iso, flash = :flash,
                gps_lat = :gps_lat, gps_lng = :gps_lng,
                gps_altitude = :gps_altitude, gps_accuracy = :gps_accuracy,
                location_name = :location_name, taken_at = :taken_at,
                color_space = :color_space, orientation = :orientation
                WHERE media_id = :media_id";
        } else {
            $sql = "INSERT INTO exif_data (media_id, camera_make, camera_model, lens_model,
                focal_length, aperture, shutter_speed, iso, flash,
                gps_lat, gps_lng, gps_altitude, gps_accuracy,
                location_name, taken_at, color_space, orientation)
                VALUES (:media_id, :camera_make, :camera_model, :lens_model,
                :focal_length, :aperture, :shutter_speed, :iso, :flash,
                :gps_lat, :gps_lng, :gps_altitude, :gps_accuracy,
                :location_name, :taken_at, :color_space, :orientation)";
        }

        Database::execute($sql, $data);
        $this->response['message'] = 'EXIF 更新成功';
    }

    // ============================================================
    // 集合相关
    // ============================================================

    /**
     * 集合列表（扁平）
     */
    private function listCollections(array $params): void
    {
        $query = $params['query'];
        $type = $query['type'] ?? '';

        $sql = "SELECT * FROM collections";
        $whereParams = [];

        if ($type) {
            $sql .= " WHERE type = :type";
            $whereParams[':type'] = $type;
        }

        $sql .= " ORDER BY sort_order ASC, created_at DESC";

        $this->response['data'] = Database::query($sql, $whereParams);
    }

    /**
     * 集合树形结构
     */
    private function collectionTree(array $params): void
    {
        $all = Database::query("SELECT * FROM collections ORDER BY sort_order ASC, name ASC");
        $tree = $this->buildTree($all);
        $this->response['data'] = $tree;
    }

    /**
     * 集合详情（含媒体列表）
     */
    private function getCollection(array $params): void
    {
        $id = (int)$params['id'];
        $query = $params['query'];
        $page = max(1, (int)($query['page'] ?? 1));
        $pageSize = min(max(1, (int)($query['page_size'] ?? PAGE_SIZE_DEFAULT)), PAGE_SIZE_MAX);

        $collection = Database::queryOne("SELECT * FROM collections WHERE id = ?", [$id]);
        if (!$collection) {
            $this->error(404, '集合不存在');
            return;
        }

        // 获取集合内媒体
        $countSql = "SELECT COUNT(*) FROM collection_items WHERE collection_id = ?";
        $total = Database::queryOne($countSql, [$id])['COUNT(*)'];

        $pagination = Database::buildPagination($page, $pageSize);
        $items = Database::query(
            "SELECT m.*, ci.sort_order as item_order, ci.note as item_note, ci.added_at as added_at
             FROM media_items m
             JOIN collection_items ci ON ci.media_id = m.id
             WHERE ci.collection_id = ?
             ORDER BY ci.sort_order ASC, ci.added_at DESC
             {$pagination}",
            [$id]
        );

        // 获取子集合
        $children = Database::query(
            "SELECT * FROM collections WHERE parent_id = ? ORDER BY sort_order ASC",
            [$id]
        );

        $collection['items'] = $items;
        $collection['children'] = $children;
        $collection['pagination'] = [
            'page'        => $page,
            'page_size'   => $pageSize,
            'total'       => (int)$total,
            'total_pages' => (int)ceil($total / max($pageSize, 1)),
        ];

        $this->response['data'] = $collection;
    }

    /**
     * 创建集合
     */
    private function createCollection(array $params): void
    {
        $body = $params['body'];

        if (empty($body['name'])) {
            $this->error(400, '集合名称不能为空');
            return;
        }

        $id = Database::insert(
            "INSERT INTO collections (name, description, cover_url, type, parent_id, sort_order, is_public, password)
             VALUES (:name, :description, :cover_url, :type, :parent_id, :sort_order, :is_public, :password)",
            [
                ':name'        => $body['name'],
                ':description' => $body['description'] ?? '',
                ':cover_url'   => $body['cover_url'] ?? '',
                ':type'        => $body['type'] ?? 'album',
                ':parent_id'   => !empty($body['parent_id']) ? (int)$body['parent_id'] : null,
                ':sort_order'  => (int)($body['sort_order'] ?? 0),
                ':is_public'   => (int)($body['is_public'] ?? 1),
                ':password'    => $body['password'] ?? '',
            ]
        );

        $this->response['code'] = 201;
        $this->response['message'] = '集合创建成功';
        $this->response['data'] = ['id' => $id];
    }

    /**
     * 更新集合
     */
    private function updateCollection(array $params): void
    {
        $id = (int)$params['id'];
        $body = $params['body'];

        Database::execute(
            "UPDATE collections SET
                name = :name, description = :description, cover_url = :cover_url,
                cover_media_id = :cover_media_id, type = :type,
                parent_id = :parent_id, sort_order = :sort_order,
                is_public = :is_public, password = :password
             WHERE id = :id",
            [
                ':id'             => $id,
                ':name'           => $body['name'] ?? '',
                ':description'    => $body['description'] ?? '',
                ':cover_url'      => $body['cover_url'] ?? '',
                ':cover_media_id' => !empty($body['cover_media_id']) ? (int)$body['cover_media_id'] : null,
                ':type'           => $body['type'] ?? 'album',
                ':parent_id'      => $body['parent_id'] ?? null,
                ':sort_order'     => (int)($body['sort_order'] ?? 0),
                ':is_public'      => (int)($body['is_public'] ?? 1),
                ':password'       => $body['password'] ?? '',
            ]
        );

        $this->response['message'] = '集合更新成功';
    }

    /**
     * 删除集合
     */
    private function deleteCollection(array $params): void
    {
        $id = (int)$params['id'];
        Database::execute("DELETE FROM collections WHERE id = ?", [$id]);
        $this->response['message'] = '已删除';
    }

    /**
     * 向集合添加媒体
     */
    private function addCollectionItem(array $params): void
    {
        $collectionId = (int)$params['id'];
        $body = $params['body'];

        if (empty($body['media_id'])) {
            $this->error(400, '缺少 media_id');
            return;
        }

        $mediaIds = is_array($body['media_id']) ? $body['media_id'] : [$body['media_id']];
        $count = 0;

        foreach ($mediaIds as $mediaId) {
            try {
                Database::execute(
                    "INSERT OR IGNORE INTO collection_items (collection_id, media_id, sort_order, note)
                     VALUES (?, ?, ?, ?)",
                    [
                        $collectionId,
                        (int)$mediaId,
                        (int)($body['sort_order'] ?? 0),
                        $body['note'] ?? '',
                    ]
                );
                $count++;
            } catch (Exception $e) {
                // 跳过重复项
            }
        }

        $this->response['message'] = "成功添加 {$count} 个媒体";
    }

    /**
     * 从集合移除媒体
     */
    private function removeCollectionItem(array $params): void
    {
        $collectionId = (int)$params['id'];
        $mediaId = (int)$params['mediaId'];

        Database::execute(
            "DELETE FROM collection_items WHERE collection_id = ? AND media_id = ?",
            [$collectionId, $mediaId]
        );

        $this->response['message'] = '已移除';
    }

    // ============================================================
    // 标签相关
    // ============================================================

    /**
     * 标签列表
     */
    private function listTags(array $params): void
    {
        $query = $params['query'];
        $type = $query['type'] ?? '';

        $sql = "SELECT t.*, COUNT(mt.media_id) as media_count
                FROM tags t
                LEFT JOIN media_tags mt ON mt.tag_id = t.id";
        $whereParams = [];

        if ($type) {
            $sql .= " WHERE t.type = :type";
            $whereParams[':type'] = $type;
        }

        $sql .= " GROUP BY t.id ORDER BY media_count DESC, t.sort_order ASC";

        $this->response['data'] = Database::query($sql, $whereParams);
    }

    /**
     * 创建标签
     */
    private function createTag(array $params): void
    {
        $body = $params['body'];

        if (empty($body['name'])) {
            $this->error(400, '标签名称不能为空');
            return;
        }

        $slug = $body['slug'] ?? $this->slugify($body['name']);

        // 检查是否已存在
        $existing = Database::queryOne("SELECT id FROM tags WHERE name = ? OR slug = ?", [$body['name'], $slug]);
        if ($existing) {
            $this->error(409, '标签已存在');
            return;
        }

        $id = Database::insert(
            "INSERT INTO tags (name, slug, description, type, color, sort_order)
             VALUES (:name, :slug, :description, :type, :color, :sort_order)",
            [
                ':name'        => $body['name'],
                ':slug'        => $slug,
                ':description' => $body['description'] ?? '',
                ':type'        => $body['type'] ?? 'general',
                ':color'       => $body['color'] ?? '#6366f1',
                ':sort_order'  => (int)($body['sort_order'] ?? 0),
            ]
        );

        $this->response['code'] = 201;
        $this->response['message'] = '标签创建成功';
        $this->response['data'] = ['id' => $id];
    }

    /**
     * 删除标签
     */
    private function deleteTag(array $params): void
    {
        $id = (int)$params['id'];
        Database::execute("DELETE FROM tags WHERE id = ?", [$id]);
        $this->response['message'] = '已删除';
    }

    /**
     * 给媒体添加标签
     */
    private function addMediaTag(array $params): void
    {
        $mediaId = (int)$params['id'];
        $body = $params['body'];

        $tagId = (int)($body['tag_id'] ?? 0);
        $tagName = $body['tag_name'] ?? '';

        if (!$tagId && !$tagName) {
            $this->error(400, '请提供 tag_id 或 tag_name');
            return;
        }

        // 如果提供了 tag_name 但没提供 id，自动创建或查找
        if (!$tagId && $tagName) {
            $existing = Database::queryOne("SELECT id FROM tags WHERE name = ?", [$tagName]);
            if ($existing) {
                $tagId = $existing['id'];
            } else {
                $tagId = Database::insert(
                    "INSERT INTO tags (name, slug) VALUES (?, ?)",
                    [$tagName, $this->slugify($tagName)]
                );
            }
        }

        Database::execute(
            "INSERT OR IGNORE INTO media_tags (media_id, tag_id) VALUES (?, ?)",
            [$mediaId, $tagId]
        );

        $this->response['message'] = '标签添加成功';
        $this->response['data'] = ['tag_id' => $tagId];
    }

    /**
     * 移除媒体标签
     */
    private function removeMediaTag(array $params): void
    {
        $mediaId = (int)$params['id'];
        $tagId = (int)$params['tagId'];

        Database::execute(
            "DELETE FROM media_tags WHERE media_id = ? AND tag_id = ?",
            [$mediaId, $tagId]
        );

        $this->response['message'] = '标签已移除';
    }

    // ============================================================
    // 搜索 & 统计
    // ============================================================

    /**
     * 全文搜索（返回分页 + 扁平列表格式）
     */
    private function search(array $params): void
    {
        $query = $params['query'];
        $keyword = trim($query['q'] ?? $query['keyword'] ?? '');

        if (empty($keyword)) {
            $this->error(400, '搜索关键词不能为空');
            return;
        }

        $likeKeyword = '%' . $keyword . '%';
        $page = max(1, (int)($query['page'] ?? 1));
        $pageSize = min(max(1, (int)($query['page_size'] ?? PAGE_SIZE_DEFAULT)), PAGE_SIZE_MAX);
        $offset = ($page - 1) * $pageSize;

        // 搜索媒体（搜索结果集扁平化，匹配 Android SearchResult 模型）
        $total = (int) Database::queryOne(
            "SELECT COUNT(*) as c FROM media_items
             WHERE title LIKE ? OR description LIKE ? OR author LIKE ? OR source LIKE ?",
            [$likeKeyword, $likeKeyword, $likeKeyword, $likeKeyword]
        )['c'];

        $items = Database::query(
            "SELECT id, title, description, type, url, thumbnail_url, file_size, mime_type,
                    width, height, duration, source, author, is_favorite, rating,
                    created_at, updated_at
             FROM media_items
             WHERE title LIKE ? OR description LIKE ? OR author LIKE ? OR source LIKE ?
             ORDER BY created_at DESC
             LIMIT ? OFFSET ?",
            [$likeKeyword, $likeKeyword, $likeKeyword, $likeKeyword, $pageSize, $offset]
        );

        $totalPages = (int) ceil($total / $pageSize);

        $this->response['data'] = [
            'results' => $items,
            'pagination' => [
                'page'       => $page,
                'page_size'  => $pageSize,
                'total'      => $total,
                'total_pages' => $totalPages,
            ],
        ];
    }

    /**
     * 系统统计
     */
    private function stats(array $params): void
    {
        $this->response['data'] = [
            'media' => [
                'total'      => (int)Database::queryOne("SELECT COUNT(*) as c FROM media_items")['c'],
                'photos'     => (int)Database::queryOne("SELECT COUNT(*) as c FROM media_items WHERE type = 'photo'")['c'],
                'videos'     => (int)Database::queryOne("SELECT COUNT(*) as c FROM media_items WHERE type = 'video'")['c'],
                'active'     => (int)Database::queryOne("SELECT COUNT(*) as c FROM media_items WHERE status = 'active'")['c'],
                'trashed'    => (int)Database::queryOne("SELECT COUNT(*) as c FROM media_items WHERE status = 'trashed'")['c'],
                'favorites'  => (int)Database::queryOne("SELECT COUNT(*) as c FROM media_items WHERE is_favorite = 1")['c'],
                'total_size' => (int)Database::queryOne("SELECT COALESCE(SUM(file_size), 0) as s FROM media_items")['s'],
            ],
            'collections' => [
                'total' => (int)Database::queryOne("SELECT COUNT(*) as c FROM collections")['c'],
                'albums' => (int)Database::queryOne("SELECT COUNT(*) as c FROM collections WHERE type = 'album'")['c'],
                'folders'=> (int)Database::queryOne("SELECT COUNT(*) as c FROM collections WHERE type = 'folder'")['c'],
            ],
            'tags' => [
                'total' => (int)Database::queryOne("SELECT COUNT(*) as c FROM tags")['c'],
            ],
        ];
    }

    // ============================================================
    // 辅助方法
    // ============================================================

    /**
     * 路由路径转正则
     */
    private function pathToRegex(string $path): string
    {
        // 把 {id} 替换为命名捕获组
        $regex = preg_replace('/\{(\w+)\}/', '(?P<$1>[^/]+)', $path);
        return '#^' . $regex . '$#';
    }

    /**
     * 获取请求体
     *
     * 优先使用 route.php 缓存的请求体（绕过 php://input 只能读一次的限制），
     * 再依次尝试 php://input、$_POST、parse_str。
     */
    private function getRequestBody(): array
    {
        $method = $_SERVER['REQUEST_METHOD'];

        if ($method === 'GET' || $method === 'DELETE') {
            return [];
        }

        // 优先使用 route.php 缓存的请求体（兼容 FastCGI 下 php://input 不可用的情况）
        $rawBody = $_SERVER['CACHED_REQUEST_BODY'] ?? '';

        // 备用：直接读取 php://input
        if ($rawBody === '') {
            $rawBody = file_get_contents('php://input') ?: '';
        }

        // 兼容多种 Content-Type 来源
        $contentType = $_SERVER['CONTENT_TYPE'] ?? $_SERVER['HTTP_CONTENT_TYPE'] ?? '';

        // 如果是 JSON 格式，尝试解析
        if ($rawBody !== '' && ($rawBody[0] === '{' || $rawBody[0] === '[')) {
            $data = json_decode($rawBody, true);
            if (is_array($data)) {
                return $data;
            }
        }

        // Content-Type 检测（某些 PHP-FPM 配置下 CONTENT_TYPE 可能缺省）
        if ($rawBody !== '' && str_contains($contentType, 'application/json')) {
            $data = json_decode($rawBody, true);
            if (is_array($data)) {
                return $data;
            }
        }

        // 表单数据
        if ($method === 'POST' && !empty($_POST)) {
            return $_POST;
        }

        // URL 编码格式
        if ($rawBody !== '') {
            parse_str($rawBody, $parsed);
            if (!empty($parsed)) {
                return $parsed;
            }
        }

        return [];
    }

    /**
     * 字符串转 slug
     */
    private function slugify(string $text): string
    {
        $text = preg_replace('/[^\pL\pN]+/u', '-', $text);
        $text = trim($text, '-');
        $text = mb_strtolower($text);
        return $text ?: 'tag';
    }

    /**
     * 构建树形结构
     */
    private function buildTree(array $items, ?int $parentId = null): array
    {
        $branch = [];

        foreach ($items as $item) {
            if ((int)$item['parent_id'] === $parentId) {
                $children = $this->buildTree($items, (int)$item['id']);
                if ($children) {
                    $item['children'] = $children;
                }
                $branch[] = $item;
            }
        }

        return $branch;
    }

    /**
     * 设置错误响应
     */
    private function error(int $code, string $message): void
    {
        $this->response['success'] = false;
        $this->response['code'] = $code;
        $this->response['message'] = $message;
        $this->response['data'] = null;
    }

    /**
     * 发送 JSON 响应
     */
    private function sendResponse(): void
    {
        http_response_code($this->response['code']);
        header('Content-Type: application/json; charset=utf-8');

        echo json_encode($this->response, JSON_UNESCAPED_UNICODE | JSON_PRETTY_PRINT | JSON_UNESCAPED_SLASHES);
    }

    // ============================================================
    // 认证辅助
    // ============================================================

    /**
     * 验证 Token 并获取当前用户
     * 验证失败则自动设置 401 错误响应并返回 null
     */
    private function requireAuth(): ?array
    {
        $user = AuthMiddleware::authenticate();
        if (!$user) {
            $this->error(401, '未登录或登录已过期，请重新登录');
        }
        return $user;
    }

    // ============================================================
    // 认证相关
    // ============================================================

    /**
     * 用户注册
     * POST /api/v1/auth/register
     */
    private function register(array $params): void
    {
        $body = $params['body'];

        // 校验必填字段
        if (empty($body['username']) || empty($body['password'])) {
            $this->error(400, '用户名和密码不能为空');
            return;
        }

        $username = trim($body['username']);
        $password = $body['password'];

        // 用户名格式校验
        if (!preg_match('/^[a-zA-Z0-9_\x{4e00}-\x{9fa5}]{2,30}$/u', $username)) {
            $this->error(400, '用户名格式不正确（2-30个字符，支持中文、字母、数字、下划线）');
            return;
        }

        // 密码长度校验
        if (strlen($password) < 6) {
            $this->error(400, '密码长度不能少于6位');
            return;
        }

        // 检查用户名是否已存在
        $existing = Database::queryOne(
            "SELECT id FROM users WHERE username = :username",
            [':username' => $username]
        );
        if ($existing) {
            $this->error(409, '用户名已存在');
            return;
        }

        // 创建用户
        $passwordHash = password_hash($password, PASSWORD_BCRYPT);
        $displayName = trim($body['display_name'] ?? '') ?: $username;
        $email = trim($body['email'] ?? '');

        $userId = Database::insert(
            "INSERT INTO users (username, password_hash, display_name, email)
             VALUES (:username, :password_hash, :display_name, :email)",
            [
                ':username'      => $username,
                ':password_hash' => $passwordHash,
                ':display_name'  => $displayName,
                ':email'         => $email,
            ]
        );

        // 生成 Token
        $token = AuthMiddleware::generateToken($userId, 'android');
        $expiresAt = AuthMiddleware::getTokenExpiry($token);

        $this->response['code'] = 201;
        $this->response['message'] = '注册成功';
        $this->response['data'] = [
            'user_id'    => $userId,
            'username'   => $username,
            'token'      => $token,
            'expires_at' => $expiresAt,
        ];
    }

    /**
     * 用户登录
     * POST /api/v1/auth/login
     */
    private function login(array $params): void
    {
        $body = $params['body'];

        if (empty($body['username']) || empty($body['password'])) {
            $this->error(400, '用户名和密码不能为空');
            return;
        }

        // 查询用户（兼容 body 和 query 传递参数）
        $username = trim($body['username']);
        $password = $body['password'];

        // 查找用户
        $user = Database::queryOne(
            "SELECT id, username, display_name, password_hash, status
             FROM users WHERE username = :username",
            [':username' => $username]
        );

        if (!$user) {
            $this->error(401, '用户名或密码错误');
            return;
        }

        if ($user['status'] !== 'active') {
            $this->error(403, '账号已被禁用');
            return;
        }

        // 验证密码
        if (!password_verify($password, $user['password_hash'])) {
            $this->error(401, '用户名或密码错误');
            return;
        }

        // 更新最后登录时间
        Database::execute(
            "UPDATE users SET last_login_at = datetime('now', 'localtime') WHERE id = :id",
            [':id' => $user['id']]
        );

        // 生成新 Token
        $token = AuthMiddleware::generateToken($user['id'], 'android');
        $expiresAt = AuthMiddleware::getTokenExpiry($token);

        // 获取收藏数和历史数
        $favCount = Database::queryOne(
            "SELECT COUNT(*) as c FROM user_favorites WHERE user_id = :uid",
            [':uid' => $user['id']]
        )['c'];
        $histCount = Database::queryOne(
            "SELECT COUNT(*) as c FROM user_history WHERE user_id = :uid",
            [':uid' => $user['id']]
        )['c'];

        $this->response['message'] = '登录成功';
        $this->response['data'] = [
            'user_id'        => (int)$user['id'],
            'username'       => $user['username'],
            'display_name'   => $user['display_name'],
            'token'          => $token,
            'expires_at'     => $expiresAt,
            'favorite_count' => (int)$favCount,
            'history_count'  => (int)$histCount,
        ];
    }

    /**
     * 退出登录
     * POST /api/v1/auth/logout
     */
    private function logout(array $params): void
    {
        $user = $this->requireAuth();
        if (!$user) return;

        // 从请求头获取 token 并撤销
        $header = $_SERVER['HTTP_AUTHORIZATION'] ?? '';
        if (preg_match('/^Bearer\s+(.+)$/i', $header, $matches)) {
            AuthMiddleware::revokeToken(trim($matches[1]));
        }

        $this->response['message'] = '已退出登录';
    }

    /**
     * 获取当前用户信息
     * GET /api/v1/auth/me
     */
    private function me(array $params): void
    {
        $user = $this->requireAuth();
        if (!$user) return;

        $userId = $user['id'];

        // 获取完整用户信息
        $userInfo = Database::queryOne(
            "SELECT id, username, display_name, avatar_url, email, created_at, last_login_at
             FROM users WHERE id = :id",
            [':id' => $userId]
        );

        // 收藏数和历史数
        $favCount = Database::queryOne(
            "SELECT COUNT(*) as c FROM user_favorites WHERE user_id = :uid",
            [':uid' => $userId]
        )['c'];
        $histCount = Database::queryOne(
            "SELECT COUNT(*) as c FROM user_history WHERE user_id = :uid",
            [':uid' => $userId]
        )['c'];

        $userInfo['favorite_count'] = (int)$favCount;
        $userInfo['history_count'] = (int)$histCount;

        $this->response['data'] = $userInfo;
    }

    // ============================================================
    // 用户收藏
    // ============================================================

    /**
     * 获取当前用户的收藏列表
     * GET /api/v1/user/favorites
     */
    private function listFavorites(array $params): void
    {
        $user = $this->requireAuth();
        if (!$user) return;

        $query = $params['query'];
        $page = max(1, (int)($query['page'] ?? 1));
        $pageSize = min(max(1, (int)($query['page_size'] ?? PAGE_SIZE_DEFAULT)), PAGE_SIZE_MAX);
        $pagination = Database::buildPagination($page, $pageSize);
        $userId = $user['id'];

        // 总数
        $total = Database::queryOne(
            "SELECT COUNT(*) as c FROM user_favorites WHERE user_id = :uid",
            [':uid' => $userId]
        )['c'];

        // 收藏列表（含完整媒体信息）
        $items = Database::query(
            "SELECT m.id, m.title, m.description, m.url, m.local_path, m.type,
                    m.mime_type, m.file_size, m.width, m.height, m.duration,
                    m.thumbnail_url, m.source, m.author, m.rating,
                    m.created_at, m.updated_at,
                    uf.created_at as favorited_at
             FROM user_favorites uf
             JOIN media_items m ON m.id = uf.media_id
             WHERE uf.user_id = :uid
             ORDER BY uf.created_at DESC
             {$pagination}",
            [':uid' => $userId]
        );

        $this->response['data'] = [
            'items'      => $items,
            'pagination' => [
                'page'        => $page,
                'page_size'   => $pageSize,
                'total'       => (int)$total,
                'total_pages' => (int)ceil($total / max($pageSize, 1)),
            ],
        ];
    }

    /**
     * 批量检查媒体是否被当前用户收藏
     * GET /api/v1/user/favorites/check?media_ids=1,2,3
     */
    private function checkFavorites(array $params): void
    {
        $user = $this->requireAuth();
        if (!$user) return;

        $mediaIdsParam = $params['query']['media_ids'] ?? '';
        if (empty($mediaIdsParam)) {
            $this->error(400, '请提供 media_ids 参数，多个用逗号分隔');
            return;
        }

        $mediaIds = array_map('intval', explode(',', $mediaIdsParam));
        if (empty($mediaIds)) {
            $this->error(400, 'media_ids 格式不正确');
            return;
        }

        // 查询已收藏的 media_id
        $placeholders = implode(',', array_fill(0, count($mediaIds), '?'));
        $favorited = Database::query(
            "SELECT media_id FROM user_favorites
             WHERE user_id = ? AND media_id IN ({$placeholders})",
            array_merge([$user['id']], $mediaIds)
        );

        $favoritedIds = array_column($favorited, 'media_id');
        $result = [];
        foreach ($mediaIds as $mid) {
            $result[(string)$mid] = in_array($mid, $favoritedIds);
        }

        $this->response['data'] = $result;
    }

    /**
     * 添加收藏
     * POST /api/v1/user/favorites
     */
    private function addFavorite(array $params): void
    {
        $user = $this->requireAuth();
        if (!$user) return;

        $body = $params['body'];
        $mediaId = (int)($body['media_id'] ?? 0);

        if ($mediaId <= 0) {
            $this->error(400, 'media_id 无效');
            return;
        }

        // 检查媒体是否存在
        $media = Database::queryOne("SELECT id FROM media_items WHERE id = ?", [$mediaId]);
        if (!$media) {
            $this->error(404, '媒体不存在');
            return;
        }

        Database::execute(
            "INSERT OR IGNORE INTO user_favorites (user_id, media_id) VALUES (?, ?)",
            [$user['id'], $mediaId]
        );

        $this->response['code'] = 201;
        $this->response['message'] = '已收藏';
    }

    /**
     * 取消收藏
     * DELETE /api/v1/user/favorites/{mediaId}
     */
    private function removeFavorite(array $params): void
    {
        $user = $this->requireAuth();
        if (!$user) return;

        $mediaId = (int)$params['mediaId'];

        Database::execute(
            "DELETE FROM user_favorites WHERE user_id = ? AND media_id = ?",
            [$user['id'], $mediaId]
        );

        $this->response['message'] = '已取消收藏';
    }

    // ============================================================
    // 用户浏览历史
    // ============================================================

    /**
     * 获取当前用户的浏览历史
     * GET /api/v1/user/history
     */
    private function listHistory(array $params): void
    {
        $user = $this->requireAuth();
        if (!$user) return;

        $query = $params['query'];
        $page = max(1, (int)($query['page'] ?? 1));
        $pageSize = min(max(1, (int)($query['page_size'] ?? PAGE_SIZE_DEFAULT)), PAGE_SIZE_MAX);
        $pagination = Database::buildPagination($page, $pageSize);
        $userId = $user['id'];

        // 总数
        $total = Database::queryOne(
            "SELECT COUNT(*) as c FROM user_history WHERE user_id = :uid",
            [':uid' => $userId]
        )['c'];

        // 历史列表（含完整媒体信息）
        $items = Database::query(
            "SELECT uh.media_id, uh.media_type, uh.watch_position, uh.watched_at,
                    m.id, m.title, m.description, m.url, m.local_path, m.type,
                    m.mime_type, m.file_size, m.width, m.height, m.duration,
                    m.thumbnail_url, m.source, m.author, m.rating,
                    m.created_at as media_created_at
             FROM user_history uh
             JOIN media_items m ON m.id = uh.media_id
             WHERE uh.user_id = :uid
             ORDER BY uh.watched_at DESC
             {$pagination}",
            [':uid' => $userId]
        );

        // 重塑为嵌套结构：media 对象内嵌到每条历史记录中
        $historyItems = [];
        foreach ($items as $item) {
            $historyItems[] = [
                'media_id'      => (int)$item['media_id'],
                'media_type'    => $item['media_type'],
                'watch_position'=> (int)$item['watch_position'],
                'watched_at'    => $item['watched_at'],
                'media'         => [
                    'id'           => (int)$item['id'],
                    'title'        => $item['title'],
                    'description'  => $item['description'],
                    'url'          => $item['url'],
                    'type'         => $item['type'],
                    'thumbnail_url'=> $item['thumbnail_url'],
                    'source'       => $item['source'],
                    'author'       => $item['author'],
                ],
            ];
        }

        $this->response['data'] = [
            'items'      => $historyItems,
            'pagination' => [
                'page'        => $page,
                'page_size'   => $pageSize,
                'total'       => (int)$total,
                'total_pages' => (int)ceil($total / max($pageSize, 1)),
            ],
        ];
    }

    /**
     * 记录浏览历史
     * POST /api/v1/user/history
     */
    private function addHistory(array $params): void
    {
        $user = $this->requireAuth();
        if (!$user) return;

        $body = $params['body'];
        $mediaId = (int)($body['media_id'] ?? 0);

        if ($mediaId <= 0) {
            $this->error(400, 'media_id 无效');
            return;
        }

        // 检查媒体是否存在
        $media = Database::queryOne(
            "SELECT id, type FROM media_items WHERE id = ?",
            [$mediaId]
        );
        if (!$media) {
            $this->error(404, '媒体不存在');
            return;
        }

        $mediaType = $body['media_type'] ?? $media['type'];
        $watchPosition = (int)($body['watch_position'] ?? 0);

        Database::execute(
            "INSERT OR REPLACE INTO user_history (user_id, media_id, media_type, watch_position, watched_at)
             VALUES (:user_id, :media_id, :media_type, :watch_position, datetime('now', 'localtime'))",
            [
                ':user_id'        => $user['id'],
                ':media_id'       => $mediaId,
                ':media_type'     => $mediaType,
                ':watch_position' => $watchPosition,
            ]
        );

        $this->response['message'] = '已记录';
    }

    /**
     * 删除单条历史
     * DELETE /api/v1/user/history/{mediaId}
     */
    private function removeHistory(array $params): void
    {
        $user = $this->requireAuth();
        if (!$user) return;

        $mediaId = (int)$params['mediaId'];

        Database::execute(
            "DELETE FROM user_history WHERE user_id = ? AND media_id = ?",
            [$user['id'], $mediaId]
        );

        $this->response['message'] = '已删除';
    }

    /**
     * 清空所有浏览历史
     * DELETE /api/v1/user/history
     */
    private function clearHistory(array $params): void
    {
        $user = $this->requireAuth();
        if (!$user) return;

        Database::execute(
            "DELETE FROM user_history WHERE user_id = ?",
            [$user['id']]
        );

        $this->response['message'] = '已清空所有浏览记录';
    }

    // ============================================================
    // 系统检测
    // ============================================================

    /**
     * API 兼容性检测
     * GET /api/v1/system/check
     *
     * 供 Android 客户端调用来检测 API 服务器是否兼容
     */
    private function systemCheck(array $params): void
    {
        // 检测各端点是否可用（通过快速请求测试）
        $baseUrl = $this->getBaseUrl();
        $checks = [];

        // 统计端点
        $statsOk = false;
        try {
            $stats = Database::queryOne("SELECT COUNT(*) as c FROM media_items");
            $statsOk = true;
        } catch (Exception $e) {
            $statsOk = false;
        }

        // 认证端点（仅检测表是否存在）
        $authOk = false;
        try {
            $userTable = Database::queryOne("SELECT COUNT(*) as c FROM users");
            $authOk = true;
        } catch (Exception $e) {
            $authOk = false;
        }

        // 收藏/历史端点
        $userFavOk = false;
        try {
            $favTable = Database::queryOne("SELECT COUNT(*) as c FROM user_favorites");
            $userFavOk = true;
        } catch (Exception $e) {
            $userFavOk = false;
        }

        // 媒体总数
        $mediaCount = 0;
        try {
            $mediaCount = (int)Database::queryOne("SELECT COUNT(*) as c FROM media_items")['c'];
        } catch (Exception $e) {}

        $this->response['data'] = [
            'server_name'  => 'MediaCollectorAPI',
            'api_version'  => '1.0',
            'media_count'  => $mediaCount,
            'checks'       => [
                'general' => [
                    'status'    => $statsOk,
                    'endpoints' => ['/api/v1/stats'],
                ],
                'media' => [
                    'status'    => $statsOk,
                    'endpoints' => ['/api/v1/media', '/api/v1/media/{id}'],
                ],
                'collections' => [
                    'status'    => true,
                    'endpoints' => ['/api/v1/collections', '/api/v1/collections/tree'],
                ],
                'tags' => [
                    'status'    => true,
                    'endpoints' => ['/api/v1/tags'],
                ],
                'search' => [
                    'status'    => true,
                    'endpoints' => ['/api/v1/search'],
                ],
                'auth' => [
                    'status'    => $authOk,
                    'endpoints' => ['/api/v1/auth/register', '/api/v1/auth/login'],
                ],
                'user' => [
                    'status'    => $userFavOk,
                    'endpoints' => ['/api/v1/user/favorites', '/api/v1/user/history'],
                ],
            ],
            'features' => [
                'multi_user'       => $authOk,
                'favorites'        => $userFavOk,
                'history'          => $userFavOk,
                'collections_tree' => true,
                'tags_type'        => true,
            ],
        ];
    }

    /**
     * 获取当前请求的 Base URL
     */
    private function getBaseUrl(): string
    {
        $scheme = (!empty($_SERVER['HTTPS']) && $_SERVER['HTTPS'] !== 'off') ? 'https' : 'http';
        $host = $_SERVER['HTTP_HOST'] ?? 'localhost';
        return "{$scheme}://{$host}";
    }

    // ============================================================
    // 聊天室
    // ============================================================

    /**
     * 获取聊天室列表
     * GET /api/v1/chat/rooms
     */
    private function chatRooms(array $params): void
    {
        $user = $this->requireAuth();
        if (!$user) return;

        // 默认房间（可扩展到从数据库读取）
        $defaultRooms = [
            ['id' => 'lobby',   'name' => '大厅',   'description' => '默认聊天室，所有人都在这里'],
            ['id' => 'general', 'name' => '闲聊',   'description' => '随便聊聊'],
        ];

        // 从数据库读取自定义房间（如果表中有数据）
        $dbRooms = Database::query("SELECT * FROM chat_rooms ORDER BY sort_order ASC");
        $rooms = !empty($dbRooms) ? $dbRooms : $defaultRooms;

        $this->response['data'] = $rooms;
    }

    /**
     * 获取聊天历史消息
     * GET /api/v1/chat/messages?room=lobby&before=1690000000&limit=30
     */
    private function chatMessages(array $params): void
    {
        $user = $this->requireAuth();
        if (!$user) return;

        $query = $params['query'];
        $roomId = $query['room'] ?? 'lobby';
        $before = (int)($query['before'] ?? time());
        $limit = min(max(1, (int)($query['limit'] ?? 50)), 100);

        $messages = Database::query(
            "SELECT message_id, room_id, sender_user, sender_name, type, content, timestamp
             FROM chat_messages
             WHERE room_id = :room AND timestamp < :before_ts
             ORDER BY timestamp DESC
             LIMIT :limit",
            [
                ':room'      => $roomId,
                ':before_ts' => $before,
                ':limit'     => $limit,
            ]
        );

        // 按时间正序返回（最新的消息在末尾）
        $messages = array_reverse($messages);

        $this->response['data'] = [
            'items' => $messages,
            'has_more' => count($messages) >= $limit,
        ];
    }

    /**
     * 同步单条消息到服务端
     * POST /api/v1/chat/messages
     */
    private function chatSyncMessage(array $params): void
    {
        $user = $this->requireAuth();
        if (!$user) return;

        $body = $params['body'];

        $messageId = trim($body['message_id'] ?? '');
        $roomId = trim($body['room_id'] ?? 'lobby');
        $type = trim($body['type'] ?? 'text');
        $content = $body['content'] ?? '';
        $timestamp = (int)($body['timestamp'] ?? time());

        if (empty($messageId) || empty($content)) {
            $this->error(400, 'message_id 和 content 不能为空');
            return;
        }

        // 防止重复同步
        $existing = Database::queryOne(
            "SELECT message_id FROM chat_messages WHERE message_id = :mid",
            [':mid' => $messageId]
        );

        if ($existing) {
            $this->response['message'] = '已存在，跳过';
            return;
        }

        Database::execute(
            "INSERT INTO chat_messages (message_id, room_id, sender_user, sender_name, type, content, timestamp)
             VALUES (:mid, :room, :user, :name, :type, :content, :ts)",
            [
                ':mid'     => $messageId,
                ':room'    => $roomId,
                ':user'    => $user['username'],
                ':name'    => $user['display_name'] ?: $user['username'],
                ':type'    => $type,
                ':content' => is_string($content) ? $content : json_encode($content, JSON_UNESCAPED_UNICODE),
                ':ts'      => $timestamp,
            ]
        );

        $this->response['code'] = 201;
        $this->response['message'] = '同步成功';
    }

    /**
     * 清空当前用户的所有聊天消息
     * DELETE /api/v1/chat/messages
     */
    private function chatClearMessages(array $params): void
    {
        $user = $this->requireAuth();
        if (!$user) return;

        Database::execute(
            "DELETE FROM chat_messages WHERE sender_user = :user",
            [':user' => $user['username']]
        );

        $this->response['message'] = '已清除聊天记录';
    }

    /**
     * 心跳（更新在线状态）
     * POST /api/v1/chat/heartbeat
     */
    private function chatHeartbeat(array $params): void
    {
        $user = $this->requireAuth();
        if (!$user) return;

        $body = $params['body'];
        $roomId = $body['room_id'] ?? 'lobby';

        // 更新或插入心跳
        Database::execute(
            "INSERT OR REPLACE INTO chat_presence
             (user_id, username, display_name, room_id, last_heartbeat)
             VALUES (:uid, :user, :name, :room, datetime('now', 'localtime'))",
            [
                ':uid'  => $user['id'],
                ':user' => $user['username'],
                ':name' => $user['display_name'] ?: $user['username'],
                ':room' => $roomId,
            ]
        );

        // 清理超过 120 秒无心跳的离线用户
        Database::execute(
            "DELETE FROM chat_presence
             WHERE last_heartbeat < datetime('now', 'localtime', '-120 seconds')"
        );

        // 返回当前在线人数
        $onlineCount = Database::queryOne(
            "SELECT COUNT(*) as c FROM chat_presence WHERE room_id = :room",
            [':room' => $roomId]
        )['c'];

        $this->response['data'] = [
            'online_count' => (int)$onlineCount,
        ];
    }

    /**
     * 获取在线用户列表
     * GET /api/v1/chat/online?room=lobby
     */
    private function chatOnline(array $params): void
    {
        $user = $this->requireAuth();
        if (!$user) return;

        $roomId = $params['query']['room'] ?? 'lobby';

        // 先清理过期心跳
        Database::execute(
            "DELETE FROM chat_presence
             WHERE last_heartbeat < datetime('now', 'localtime', '-120 seconds')"
        );

        $users = Database::query(
            "SELECT username, display_name, last_heartbeat
             FROM chat_presence
             WHERE room_id = :room
             ORDER BY display_name ASC",
            [':room' => $roomId]
        );

        $this->response['data'] = [
            'count' => count($users),
            'users' => $users,
        ];
    }

    /**
     * 仅获取在线人数（轻量轮询用）
     * GET /api/v1/chat/online/count?room=lobby
     */
    private function chatOnlineCount(array $params): void
    {
        $user = $this->requireAuth();
        if (!$user) return;

        $roomId = $params['query']['room'] ?? 'lobby';

        // 先清理过期心跳
        Database::execute(
            "DELETE FROM chat_presence
             WHERE last_heartbeat < datetime('now', 'localtime', '-120 seconds')"
        );

        $count = Database::queryOne(
            "SELECT COUNT(*) as c FROM chat_presence WHERE room_id = :room",
            [':room' => $roomId]
        )['c'];

        $this->response['data'] = [
            'count' => (int)$count,
        ];
    }
}
