-- ============================================================
-- 媒体采集系统 - SQLite3 数据库建表脚本
-- 设计目标：通用、可扩展、高性能
-- ============================================================

-- 开启外键约束
PRAGMA foreign_keys = ON;
PRAGMA journal_mode = WAL;        -- 写前日志，提升并发性能
PRAGMA synchronous = NORMAL;      -- 平衡安全性与速度

-- ============================================================
-- 1. 媒体主表
--    统一存储照片和视频，通过 type 字段区分
-- ============================================================
CREATE TABLE IF NOT EXISTS media_items (
    id              INTEGER PRIMARY KEY AUTOINCREMENT,
    title           TEXT NOT NULL DEFAULT '',                -- 标题
    description     TEXT NOT NULL DEFAULT '',                -- 描述/备注
    url             TEXT NOT NULL,                           -- 原始链接（必填）
    local_path      TEXT DEFAULT '',                         -- 本地存储路径（若已下载）
    type            TEXT NOT NULL CHECK(type IN ('photo', 'video', 'audio', 'document', 'other')),
    mime_type       TEXT DEFAULT '',                         -- 如 image/jpeg, video/mp4
    file_size       INTEGER DEFAULT 0,                       -- 文件大小（字节）
    width           INTEGER DEFAULT 0,                       -- 宽度（像素）
    height          INTEGER DEFAULT 0,                       -- 高度（像素）
    duration        INTEGER DEFAULT 0,                       -- 视频/音频时长（秒）
    thumbnail_url   TEXT DEFAULT '',                         -- 缩略图链接
    source          TEXT DEFAULT '',                         -- 来源（如：weibo, twitter, pinterest）
    source_id       TEXT DEFAULT '',                         -- 来源平台的原始 ID（去重用）
    source_url      TEXT DEFAULT '',                         -- 来源页面链接
    author          TEXT DEFAULT '',                         -- 作者/上传者
    license         TEXT DEFAULT '',                         -- 版权信息
    status          TEXT NOT NULL DEFAULT 'active' CHECK(status IN ('active', 'archived', 'trashed', 'failed')),
    is_favorite     INTEGER NOT NULL DEFAULT 0,              -- 是否收藏/标记
    rating          INTEGER DEFAULT 0 CHECK(rating >= 0 AND rating <= 5),  -- 评分 0-5
    sort_order      INTEGER DEFAULT 0,                       -- 自定义排序
    md5_hash        TEXT DEFAULT '',                         -- 文件 MD5（去重/校验）
    downloaded_at   TEXT DEFAULT NULL,                       -- 下载完成时间
    created_at      TEXT NOT NULL DEFAULT (datetime('now', 'localtime')),
    updated_at      TEXT NOT NULL DEFAULT (datetime('now', 'localtime'))
);

-- 索引
CREATE INDEX IF NOT EXISTS idx_media_type ON media_items(type);
CREATE INDEX IF NOT EXISTS idx_media_status ON media_items(status);
CREATE INDEX IF NOT EXISTS idx_media_source ON media_items(source);
CREATE INDEX IF NOT EXISTS idx_media_source_id ON media_items(source_id);
CREATE INDEX IF NOT EXISTS idx_media_favorite ON media_items(is_favorite);
CREATE INDEX IF NOT EXISTS idx_media_created ON media_items(created_at);
CREATE INDEX IF NOT EXISTS idx_media_md5 ON media_items(md5_hash);

-- ============================================================
-- 2. EXIF/拍摄信息表
--    存储照片拍摄的元数据（1:1 关系）
-- ============================================================
CREATE TABLE IF NOT EXISTS exif_data (
    id              INTEGER PRIMARY KEY AUTOINCREMENT,
    media_id        INTEGER NOT NULL UNIQUE REFERENCES media_items(id) ON DELETE CASCADE,
    camera_make     TEXT DEFAULT '',                         -- 相机品牌
    camera_model    TEXT DEFAULT '',                         -- 相机型号
    lens_model      TEXT DEFAULT '',                         -- 镜头型号
    focal_length    TEXT DEFAULT '',                         -- 焦距（如 "24mm"）
    aperture        TEXT DEFAULT '',                         -- 光圈（如 "f/2.8"）
    shutter_speed   TEXT DEFAULT '',                         -- 快门速度（如 "1/1000"）
    iso             INTEGER DEFAULT 0,                       -- ISO
    flash           INTEGER DEFAULT 0,                       -- 是否使用闪光灯
    gps_lat         REAL DEFAULT NULL,                       -- GPS 纬度
    gps_lng         REAL DEFAULT NULL,                       -- GPS 经度
    gps_altitude    REAL DEFAULT NULL,                       -- GPS 海拔（米）
    gps_accuracy    REAL DEFAULT NULL,                       -- GPS 精度
    location_name   TEXT DEFAULT '',                         -- 拍摄地点名称
    taken_at        TEXT DEFAULT NULL,                       -- 拍摄时间
    color_space     TEXT DEFAULT '',                         -- 色彩空间
    orientation     INTEGER DEFAULT 0                        -- 旋转方向（0/90/180/270）
);

-- 索引
CREATE INDEX IF NOT EXISTS idx_exif_gps ON exif_data(gps_lat, gps_lng);
CREATE INDEX IF NOT EXISTS idx_exif_taken ON exif_data(taken_at);

-- ============================================================
-- 3. 集合/专辑表
--    支持树形结构（文件夹嵌套）
-- ============================================================
CREATE TABLE IF NOT EXISTS collections (
    id              INTEGER PRIMARY KEY AUTOINCREMENT,
    name            TEXT NOT NULL,                           -- 集合名称
    description     TEXT NOT NULL DEFAULT '',                 -- 描述
    cover_url       TEXT DEFAULT '',                         -- 封面图链接
    cover_media_id  INTEGER DEFAULT NULL REFERENCES media_items(id) ON DELETE SET NULL,
    type            TEXT NOT NULL DEFAULT 'album' CHECK(type IN ('album', 'folder', 'playlist', 'project')),
    parent_id       INTEGER DEFAULT NULL REFERENCES collections(id) ON DELETE SET NULL,
    sort_order      INTEGER DEFAULT 0,
    is_public       INTEGER NOT NULL DEFAULT 1,              -- 是否公开
    password        TEXT DEFAULT '',                         -- 访问密码（空=不需要）
    item_count      INTEGER DEFAULT 0,                       -- 缓存：媒体数量（可定期更新）
    created_at      TEXT NOT NULL DEFAULT (datetime('now', 'localtime')),
    updated_at      TEXT NOT NULL DEFAULT (datetime('now', 'localtime'))
);

-- 索引
CREATE INDEX IF NOT EXISTS idx_collections_parent ON collections(parent_id);
CREATE INDEX IF NOT EXISTS idx_collections_type ON collections(type);
CREATE INDEX IF NOT EXISTS idx_collections_sort ON collections(sort_order);

-- ============================================================
-- 4. 集合-媒体关联表
--    多对多关系，媒体可属于多个集合
-- ============================================================
CREATE TABLE IF NOT EXISTS collection_items (
    id              INTEGER PRIMARY KEY AUTOINCREMENT,
    collection_id   INTEGER NOT NULL REFERENCES collections(id) ON DELETE CASCADE,
    media_id        INTEGER NOT NULL REFERENCES media_items(id) ON DELETE CASCADE,
    sort_order      INTEGER DEFAULT 0,                       -- 集合内排序
    note            TEXT DEFAULT '',                         -- 在集合中的备注
    added_at        TEXT NOT NULL DEFAULT (datetime('now', 'localtime')),
    UNIQUE(collection_id, media_id)
);

CREATE INDEX IF NOT EXISTS idx_col_items_collection ON collection_items(collection_id);
CREATE INDEX IF NOT EXISTS idx_col_items_media ON collection_items(media_id);

-- ============================================================
-- 5. 标签表
-- ============================================================
CREATE TABLE IF NOT EXISTS tags (
    id              INTEGER PRIMARY KEY AUTOINCREMENT,
    name            TEXT NOT NULL UNIQUE,                    -- 标签名称
    slug            TEXT NOT NULL UNIQUE,                    -- URL友好标识
    description     TEXT DEFAULT '',                         -- 描述
    type            TEXT DEFAULT 'general' CHECK(type IN ('general', 'location', 'person', 'event', 'subject')),
    color           TEXT DEFAULT '#6366f1',                  -- 标签颜色（前端用）
    sort_order      INTEGER DEFAULT 0,
    created_at      TEXT NOT NULL DEFAULT (datetime('now', 'localtime'))
);

CREATE INDEX IF NOT EXISTS idx_tags_type ON tags(type);
CREATE INDEX IF NOT EXISTS idx_tags_slug ON tags(slug);

-- ============================================================
-- 6. 媒体-标签关联表
-- ============================================================
CREATE TABLE IF NOT EXISTS media_tags (
    id              INTEGER PRIMARY KEY AUTOINCREMENT,
    media_id        INTEGER NOT NULL REFERENCES media_items(id) ON DELETE CASCADE,
    tag_id          INTEGER NOT NULL REFERENCES tags(id) ON DELETE CASCADE,
    UNIQUE(media_id, tag_id)
);

CREATE INDEX IF NOT EXISTS idx_media_tags_media ON media_tags(media_id);
CREATE INDEX IF NOT EXISTS idx_media_tags_tag ON media_tags(tag_id);

-- ============================================================
-- 7. 扩展元数据表（EAV 模式）
--    用于存储任意自定义字段，无需改表结构
-- ============================================================
CREATE TABLE IF NOT EXISTS metadata (
    id              INTEGER PRIMARY KEY AUTOINCREMENT,
    target_type     TEXT NOT NULL CHECK(target_type IN ('media', 'collection', 'tag')),
    target_id       INTEGER NOT NULL,                        -- 关联对象的 ID
    meta_key        TEXT NOT NULL,                           -- 元数据键名
    meta_value      TEXT NOT NULL DEFAULT '',                -- 元数据值
    value_type      TEXT NOT NULL DEFAULT 'text' CHECK(value_type IN ('text', 'number', 'boolean', 'json', 'date')),
    UNIQUE(target_type, target_id, meta_key)
);

CREATE INDEX IF NOT EXISTS idx_meta_target ON metadata(target_type, target_id);
CREATE INDEX IF NOT EXISTS idx_meta_key ON metadata(meta_key);

-- ============================================================
-- 8. 下载记录表
--    用于追踪媒体文件的下载状态
-- ============================================================
CREATE TABLE IF NOT EXISTS download_log (
    id              INTEGER PRIMARY KEY AUTOINCREMENT,
    media_id        INTEGER NOT NULL REFERENCES media_items(id) ON DELETE CASCADE,
    status          TEXT NOT NULL DEFAULT 'pending' CHECK(status IN ('pending', 'downloading', 'completed', 'failed')),
    file_path       TEXT DEFAULT '',                         -- 本地保存路径
    file_size       INTEGER DEFAULT 0,                       -- 实际下载大小
    error_message   TEXT DEFAULT '',                         -- 错误信息
    retry_count     INTEGER DEFAULT 0,                       -- 重试次数
    started_at      TEXT DEFAULT NULL,
    completed_at    TEXT DEFAULT NULL,
    created_at      TEXT NOT NULL DEFAULT (datetime('now', 'localtime'))
);

CREATE INDEX IF NOT EXISTS idx_dl_media ON download_log(media_id);
CREATE INDEX IF NOT EXISTS idx_dl_status ON download_log(status);

-- ============================================================
-- 9. 访问日志表（可选）
--    记录 API 访问/媒体浏览次数
-- ============================================================
CREATE TABLE IF NOT EXISTS access_log (
    id              INTEGER PRIMARY KEY AUTOINCREMENT,
    media_id        INTEGER DEFAULT NULL REFERENCES media_items(id) ON DELETE SET NULL,
    collection_id   INTEGER DEFAULT NULL REFERENCES collections(id) ON DELETE SET NULL,
    ip_address      TEXT DEFAULT '',
    user_agent      TEXT DEFAULT '',
    action          TEXT NOT NULL DEFAULT 'view',
    created_at      TEXT NOT NULL DEFAULT (datetime('now', 'localtime'))
);

CREATE INDEX IF NOT EXISTS idx_access_media ON access_log(media_id);
CREATE INDEX IF NOT EXISTS idx_access_time ON access_log(created_at);



-- ============================================================
-- 12. 用户表
-- ============================================================
CREATE TABLE IF NOT EXISTS users (
    id              INTEGER PRIMARY KEY AUTOINCREMENT,
    username        TEXT NOT NULL UNIQUE,               -- 用户名
    password_hash   TEXT NOT NULL,                      -- 密码哈希（bcrypt）
    display_name    TEXT DEFAULT '',                    -- 显示名称
    avatar_url      TEXT DEFAULT '',                    -- 头像链接
    email           TEXT DEFAULT '',                    -- 邮箱
    status          TEXT NOT NULL DEFAULT 'active' CHECK(status IN ('active', 'disabled')),
    last_login_at   TEXT DEFAULT NULL,
    created_at      TEXT NOT NULL DEFAULT (datetime('now', 'localtime')),
    updated_at      TEXT NOT NULL DEFAULT (datetime('now', 'localtime'))
);

CREATE INDEX IF NOT EXISTS idx_users_username ON users(username);

-- ============================================================
-- 13. 用户 Token 表
-- ============================================================
CREATE TABLE IF NOT EXISTS user_tokens (
    id              INTEGER PRIMARY KEY AUTOINCREMENT,
    user_id         INTEGER NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    token           TEXT NOT NULL UNIQUE,               -- Token 值
    device_name     TEXT DEFAULT '',                    -- 设备名
    expires_at      TEXT NOT NULL,                      -- 过期时间
    created_at      TEXT NOT NULL DEFAULT (datetime('now', 'localtime'))
);

CREATE INDEX IF NOT EXISTS idx_tokens_token ON user_tokens(token);
CREATE INDEX IF NOT EXISTS idx_tokens_user ON user_tokens(user_id);

-- ============================================================
-- 14. 用户收藏表
-- ============================================================
CREATE TABLE IF NOT EXISTS user_favorites (
    id              INTEGER PRIMARY KEY AUTOINCREMENT,
    user_id         INTEGER NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    media_id        INTEGER NOT NULL REFERENCES media_items(id) ON DELETE CASCADE,
    created_at      TEXT NOT NULL DEFAULT (datetime('now', 'localtime')),
    UNIQUE(user_id, media_id)
);

CREATE INDEX IF NOT EXISTS idx_fav_user ON user_favorites(user_id);
CREATE INDEX IF NOT EXISTS idx_fav_media ON user_favorites(media_id);

-- ============================================================
-- 15. 用户浏览历史表
-- ============================================================
CREATE TABLE IF NOT EXISTS user_history (
    id              INTEGER PRIMARY KEY AUTOINCREMENT,
    user_id         INTEGER NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    media_id        INTEGER NOT NULL REFERENCES media_items(id) ON DELETE CASCADE,
    media_type      TEXT NOT NULL DEFAULT 'photo',      -- 冗余，方便显示
    watch_position  INTEGER DEFAULT 0,                  -- 视频播放位置（秒）
    watched_at      TEXT NOT NULL DEFAULT (datetime('now', 'localtime')),
    UNIQUE(user_id, media_id)
);

CREATE INDEX IF NOT EXISTS idx_history_user ON user_history(user_id);
CREATE INDEX IF NOT EXISTS idx_history_time ON user_history(watched_at);

-- ============================================================
-- 16. 聊天室定义表
-- ============================================================
CREATE TABLE IF NOT EXISTS chat_rooms (
    id              TEXT PRIMARY KEY,                 -- room_id: "lobby"
    name            TEXT NOT NULL,                    -- 显示名称
    description     TEXT DEFAULT '',
    sort_order      INTEGER DEFAULT 0,
    created_at      TEXT NOT NULL DEFAULT (datetime('now', 'localtime'))
);

-- ============================================================
-- 17. 聊天消息同步表（按账号持久化）
-- ============================================================
CREATE TABLE IF NOT EXISTS chat_messages (
    message_id      TEXT PRIMARY KEY,                 -- UUID v4
    room_id         TEXT NOT NULL,
    sender_user     TEXT NOT NULL,                    -- 发送者用户名
    sender_name     TEXT NOT NULL,                    -- 发送者显示名
    type            TEXT NOT NULL DEFAULT 'text',     -- text / image / system
    content         TEXT NOT NULL,                    -- JSON: 加密 data 或 明文 data
    timestamp       INTEGER NOT NULL,                 -- Unix 秒
    created_at      TEXT NOT NULL DEFAULT (datetime('now', 'localtime'))
);

CREATE INDEX IF NOT EXISTS idx_chat_msg_room_time ON chat_messages(room_id, timestamp DESC);
CREATE INDEX IF NOT EXISTS idx_chat_msg_sender ON chat_messages(sender_user);

-- ============================================================
-- 18. 在线用户心跳表
-- ============================================================
CREATE TABLE IF NOT EXISTS chat_presence (
    user_id         INTEGER PRIMARY KEY REFERENCES users(id) ON DELETE CASCADE,
    username        TEXT NOT NULL,
    display_name    TEXT NOT NULL,
    room_id         TEXT DEFAULT 'lobby',
    last_heartbeat  TEXT NOT NULL DEFAULT (datetime('now', 'localtime'))
);

-- ============================================================
-- 触发器（置于所有 CREATE TABLE 之后，确保表已存在）
-- ============================================================

-- 自动更新 media_items.updated_at
CREATE TRIGGER IF NOT EXISTS trg_media_updated
    AFTER UPDATE ON media_items
    FOR EACH ROW
BEGIN
    UPDATE media_items SET updated_at = datetime('now', 'localtime') WHERE id = OLD.id;
END;

-- 自动更新 collections.updated_at
CREATE TRIGGER IF NOT EXISTS trg_collection_updated
    AFTER UPDATE ON collections
    FOR EACH ROW
BEGIN
    UPDATE collections SET updated_at = datetime('now', 'localtime') WHERE id = OLD.id;
END;

-- 插入时自动更新集合的 item_count
CREATE TRIGGER IF NOT EXISTS trg_collection_count_insert
    AFTER INSERT ON collection_items
    FOR EACH ROW
BEGIN
    UPDATE collections SET item_count = (
        SELECT COUNT(*) FROM collection_items WHERE collection_id = NEW.collection_id
    ) WHERE id = NEW.collection_id;
END;

-- 删除时自动更新集合的 item_count
CREATE TRIGGER IF NOT EXISTS trg_collection_count_delete
    AFTER DELETE ON collection_items
    FOR EACH ROW
BEGIN
    UPDATE collections SET item_count = (
        SELECT COUNT(*) FROM collection_items WHERE collection_id = OLD.collection_id
    ) WHERE id = OLD.collection_id;
END;
