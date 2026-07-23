# 媒体采集系统 - API 接口使用文档

> 面向 **客户端开发者** 和 **AI 编程助手**。
>
> 本文档详细描述每个 API 接口的请求/响应格式，提供完整的 TypeScript 类型定义和 Python/JS 代码示例，方便快速生成客户端代码。
>
> 采集端开发者请参阅：[采集接入文档](./采集接入文档.md)

---

## 目录

1. [接口规范](#1-接口规范)
2. [TypeScript 类型定义](#2-typescript-类型定义)
3. [媒体接口](#3-媒体接口)
4. [EXIF 接口](#4-exif-接口)
5. [集合接口](#5-集合接口)
6. [标签接口](#6-标签接口)
7. [搜索接口](#7-搜索接口)
8. [统计接口](#8-统计接口)
9. [Python 客户端示例](#9-python-客户端示例)
10. [JavaScript/TypeScript 客户端示例](#10-javascripttypescript-客户端示例)

---

## 1. 接口规范

### 1.1 基本信息

```
Base URL: http://localhost:8000/api/v1
格式:     JSON (application/json)
编码:     UTF-8
方法:     GET / POST / PUT / PATCH / DELETE
```

### 1.2 统一响应结构

```typescript
interface ApiResponse<T = any> {
    success: boolean;    // 是否成功
    code: number;        // HTTP 状态码
    message: string;     // 操作提示
    data: T | null;      // 返回数据
}
```

### 1.3 分页响应结构

```typescript
interface PaginatedData<T> {
    items: T[];
    pagination: {
        page: number;
        page_size: number;
        total: number;
        total_pages: number;
    };
}
```

### 1.4 状态码速查

| 状态码 | 含义 | 常见场景 |
|--------|------|----------|
| 200 | 成功 | GET / PUT / PATCH / DELETE |
| 201 | 创建成功 | POST 新建资源 |
| 400 | 参数错误 | 缺少必填字段、格式错误 |
| 404 | 资源不存在 | ID 无效 |
| 409 | 资源冲突 | 标签/媒体重复 |
| 500 | 服务端错误 | 数据库异常 |

---

## 2. TypeScript 类型定义

以下是完整的类型定义，可以直接复制到 TypeScript 项目中使用。

```typescript
// ============================================================
// 媒体类型
// ============================================================

type MediaType = 'photo' | 'video' | 'audio' | 'document' | 'other';
type MediaStatus = 'active' | 'archived' | 'trashed' | 'failed';

interface MediaItem {
    id: number;
    title: string;
    description: string;
    url: string;
    local_path: string;
    type: MediaType;
    mime_type: string;
    file_size: number;
    width: number;
    height: number;
    duration: number;
    thumbnail_url: string;
    source: string;
    source_id: string;
    source_url: string;
    author: string;
    status: MediaStatus;
    is_favorite: number;       // 0 或 1
    rating: number;            // 0-5
    sort_order: number;
    md5_hash: string;
    created_at: string;        // ISO 8601
    updated_at: string;        // ISO 8601
}

/** 媒体详情（含关联数据） */
interface MediaDetail extends MediaItem {
    tags: Tag[];
    collections: MediaCollection[];
    metadata: MetadataItem[];
    exif?: ExifData | null;
}

/** 创建媒体请求体 */
interface CreateMediaRequest {
    title?: string;
    description?: string;
    url: string;                     // 必填
    local_path?: string;
    type: MediaType;                 // 必填
    mime_type?: string;
    file_size?: number;
    width?: number;
    height?: number;
    duration?: number;
    thumbnail_url?: string;
    source?: string;
    source_id?: string;
    source_url?: string;
    author?: string;
    status?: MediaStatus;
    is_favorite?: number;
    rating?: number;
    sort_order?: number;
    md5_hash?: string;
    tags?: number[];                 // 标签 ID 数组
    metadata?: Record<string, string>;  // 扩展元数据
}

// ============================================================
// 集合类型
// ============================================================

type CollectionType = 'album' | 'folder' | 'playlist' | 'project';

interface MediaCollection {
    id: number;
    name: string;
    description: string;
    cover_url: string;
    cover_media_id: number | null;
    type: CollectionType;
    parent_id: number | null;
    sort_order: number;
    is_public: number;
    password: string;
    item_count: number;
    created_at: string;
    updated_at: string;
    children?: MediaCollection[];    // 树形结构时出现
}

/** 集合详情（含媒体列表） */
interface CollectionDetail extends MediaCollection {
    items: MediaItem[];
    pagination: {
        page: number;
        page_size: number;
        total: number;
        total_pages: number;
    };
}

// ============================================================
// 标签类型
// ============================================================

type TagType = 'general' | 'location' | 'person' | 'event' | 'subject';

interface Tag {
    id: number;
    name: string;
    slug: string;
    description: string;
    type: TagType;
    color: string;
    sort_order: number;
    media_count?: number;            // 关联的媒体数量
}

// ============================================================
// EXIF 类型
// ============================================================

interface ExifData {
    id: number;
    media_id: number;
    camera_make: string;
    camera_model: string;
    lens_model: string;
    focal_length: string;
    aperture: string;
    shutter_speed: string;
    iso: number;
    flash: number;
    gps_lat: number | null;
    gps_lng: number | null;
    gps_altitude: number | null;
    gps_accuracy: number | null;
    location_name: string;
    taken_at: string | null;
    color_space: string;
    orientation: number;
}

// ============================================================
// 扩展元数据类型
// ============================================================

interface MetadataItem {
    target_type: 'media' | 'collection' | 'tag';
    target_id: number;
    meta_key: string;
    meta_value: string;
    value_type: 'text' | 'number' | 'boolean' | 'json' | 'date';
}

// ============================================================
// 搜索类型
// ============================================================

interface SearchResults {
    keyword: string;
    type: string;
    results: {
        media?: MediaItem[];
        collections?: MediaCollection[];
        tags?: Tag[];
    };
}

// ============================================================
// 统计类型
// ============================================================

interface Stats {
    media: {
        total: number;
        photos: number;
        videos: number;
        active: number;
        trashed: number;
        favorites: number;
        total_size: number;     // 总字节数
    };
    collections: {
        total: number;
        albums: number;
        folders: number;
    };
    tags: {
        total: number;
    };
}

// ============================================================
// API 响应包装
// ============================================================

interface PaginatedResponse<T> {
    items: T[];
    pagination: {
        page: number;
        page_size: number;
        total: number;
        total_pages: number;
    };
    filters: {
        type: Array<{ type: string; count: number }>;
        source: Array<{ source: string; count: number }>;
        status: Array<{ status: string; count: number }>;
    };
}
```

---

## 3. 媒体接口

### 3.1 获取媒体列表

```
GET /api/v1/media
```

**查询参数：**

| 参数 | 类型 | 默认值 | 说明 |
|------|------|--------|------|
| `page` | int | 1 | 页码 |
| `page_size` | int | 20 | 每页条数（最大 100） |
| `type` | string | - | 筛选类型：`photo` / `video` |
| `status` | string | - | 筛选状态：`active` / `archived` / `trashed` |
| `source` | string | - | 筛选来源 |
| `is_favorite` | int | - | 筛选收藏：`0` / `1` |
| `keyword` | string | - | 关键词搜索（匹配标题和描述） |
| `author` | string | - | 作者模糊搜索 |
| `date_from` | string | - | 创建时间起始（如 `2025-01-01`） |
| `date_to` | string | - | 创建时间结束 |
| `sort` | string | `created_at` | 排序字段：`created_at` / `updated_at` / `title` / `file_size` / `sort_order` / `rating` |
| `order` | string | `DESC` | 排序方向：`ASC` / `DESC` |

**请求示例：**
```bash
curl "http://localhost:8000/api/v1/media?type=photo&status=active&is_favorite=1&page=1&page_size=10&sort=created_at&order=DESC"
```

**响应示例：**
```json
{
    "success": true,
    "code": 200,
    "message": "",
    "data": {
        "items": [
            {
                "id": 1,
                "title": "黄山日出",
                "description": "黄山顶上拍摄的壮丽日出",
                "url": "https://images.unsplash.com/photo-1464822759023-fed622ff2c3b",
                "type": "photo",
                "mime_type": "image/jpeg",
                "file_size": 3840000,
                "width": 1920,
                "height": 1280,
                "thumbnail_url": "",
                "source": "unsplash",
                "author": "Johannes Andersson",
                "status": "active",
                "is_favorite": 1,
                "rating": 5,
                "created_at": "2025-07-23 12:00:00",
                "updated_at": "2025-07-23 12:00:00"
            }
        ],
        "pagination": {
            "page": 1,
            "page_size": 10,
            "total": 42,
            "total_pages": 5
        },
        "filters": {
            "type": [{"type": "photo", "count": 30}, {"type": "video", "count": 12}],
            "source": [{"source": "unsplash", "count": 20}],
            "status": [{"status": "active", "count": 40}, {"status": "trashed", "count": 2}]
        }
    }
}
```

**TypeScript 调用：**
```typescript
interface MediaListParams {
    page?: number;
    page_size?: number;
    type?: 'photo' | 'video';
    status?: 'active' | 'archived' | 'trashed';
    source?: string;
    is_favorite?: 0 | 1;
    keyword?: string;
    author?: string;
    date_from?: string;
    date_to?: string;
    sort?: string;
    order?: 'ASC' | 'DESC';
}

async function getMediaList(params: MediaListParams = {}): Promise<ApiResponse<PaginatedResponse<MediaItem>>> {
    const query = new URLSearchParams();
    Object.entries(params).forEach(([k, v]) => {
        if (v !== undefined && v !== '') query.set(k, String(v));
    });
    const resp = await fetch(`/api/v1/media?${query}`);
    return resp.json();
}
```

### 3.2 获取媒体详情

```
GET /api/v1/media/{id}
```

**响应示例：**
```json
{
    "success": true,
    "code": 200,
    "data": {
        "id": 1,
        "title": "黄山日出",
        "url": "https://images.unsplash.com/photo-1464822759023-fed622ff2c3b",
        "type": "photo",
        "status": "active",
        "tags": [
            {"id": 1, "name": "风景", "slug": "landscape", "type": "subject", "color": "#22c55e"}
        ],
        "collections": [
            {"id": 1, "name": "国内旅行", "type": "folder"}
        ],
        "metadata": [
            {"meta_key": "color_palette", "meta_value": "[\"#FF6B35\"]", "value_type": "json"}
        ],
        "exif": {
            "camera_make": "Canon",
            "camera_model": "EOS R5",
            "focal_length": "24mm",
            "iso": 100,
            "taken_at": "2025-06-15 05:30:00"
        }
    }
}
```

### 3.3 创建媒体

```
POST /api/v1/media
```

**请求体：**
```json
{
    "title": "新照片",
    "url": "https://example.com/photo.jpg",
    "type": "photo",
    "mime_type": "image/jpeg",
    "source": "unsplash",
    "source_id": "photo-new-001",
    "author": "摄影师",
    "tags": [1, 2],
    "metadata": {
        "ai_description": "金色的日落"
    }
}
```

**响应：**
```json
{
    "success": true,
    "code": 201,
    "message": "创建成功",
    "data": {"id": 100}
}
```

### 3.4 更新媒体

```
PUT /api/v1/media/{id}       // 全量更新（需传所有字段）
PATCH /api/v1/media/{id}     // 部分更新（只传需要改的字段）
```

**PATCH 示例（推荐用 PATCH）：**
```bash
curl -X PATCH http://localhost:8000/api/v1/media/1 \
  -H "Content-Type: application/json" \
  -d '{"title": "新标题", "rating": 5}'
```

**PATCH 支持的更新字段：**
```
title, description, url, local_path, type, mime_type,
file_size, width, height, duration, thumbnail_url,
source, source_id, source_url, author,
status, is_favorite, rating, sort_order, md5_hash
```

### 3.5 删除媒体（软删除）

```
DELETE /api/v1/media/{id}
```

将媒体状态设为 `trashed`，数据不物理删除。可用于回收站功能。

```bash
curl -X DELETE http://localhost:8000/api/v1/media/1
```

**响应：**
```json
{"success": true, "code": 200, "message": "已移入回收站", "data": null}
```

---

## 4. EXIF 接口

### 4.1 获取 EXIF

```
GET /api/v1/media/{id}/exif
```

```bash
curl http://localhost:8000/api/v1/media/1/exif
```

**响应：**
```json
{
    "success": true,
    "code": 200,
    "data": {
        "camera_make": "Canon",
        "camera_model": "EOS R5",
        "iso": 100,
        "focal_length": "24mm",
        "aperture": "f/8",
        "gps_lat": 30.1337,
        "gps_lng": 118.1689,
        "location_name": "黄山",
        "taken_at": "2025-06-15 05:30:00"
    }
}
```

### 4.2 创建/更新 EXIF

```
PUT /api/v1/media/{id}/exif
```

不存在则创建，存在则更新。

```bash
curl -X PUT http://localhost:8000/api/v1/media/1/exif \
  -H "Content-Type: application/json" \
  -d '{
    "camera_make": "Canon",
    "camera_model": "EOS R5",
    "focal_length": "24mm",
    "aperture": "f/8",
    "iso": 100,
    "gps_lat": 30.1337,
    "gps_lng": 118.1689,
    "location_name": "黄山",
    "taken_at": "2025-06-15 05:30:00"
}'
```

**支持的全部字段：**
```
camera_make, camera_model, lens_model, focal_length,
aperture, shutter_speed, iso, flash,
gps_lat, gps_lng, gps_altitude, gps_accuracy, location_name,
taken_at, color_space, orientation
```

---

## 5. 集合接口

### 5.1 获取集合列表

```
GET /api/v1/collections?type=album
```

**响应示例：**
```json
{
    "success": true,
    "code": 200,
    "data": [
        {
            "id": 1,
            "name": "2025 摄影作品合集",
            "description": "2025年拍摄的所有精选作品",
            "type": "album",
            "parent_id": null,
            "item_count": 5,
            "children": [
                {
                    "id": 2,
                    "name": "国内旅行",
                    "parent_id": 1,
                    "item_count": 3,
                    "type": "folder"
                },
                {
                    "id": 3,
                    "name": "国外旅行",
                    "parent_id": 1,
                    "item_count": 2,
                    "type": "folder"
                }
            ]
        }
    ]
}
```

### 5.2 获取集合树形结构

```
GET /api/v1/collections/tree
```

返回完整的树形嵌套结构，根节点为 `parent_id = null` 的集合，子节点挂在 `children` 数组中。

### 5.3 获取集合详情

```
GET /api/v1/collections/{id}
```

**响应包含：** 集合信息 + 子集合 + 媒体列表（分页）

**额外查询参数：**
| 参数 | 默认值 | 说明 |
|------|--------|------|
| `page` | 1 | 媒体页码 |
| `page_size` | 20 | 每页媒体数 |

### 5.4 创建集合

```
POST /api/v1/collections
```

```bash
curl -X POST http://localhost:8000/api/v1/collections \
  -H "Content-Type: application/json" \
  -d '{
    "name": "2025 精选",
    "description": "年度最佳作品",
    "type": "album",
    "parent_id": null,
    "cover_url": "https://example.com/cover.jpg",
    "is_public": 1
}'
```

### 5.5 更新集合

```
PUT /api/v1/collections/{id}
```

支持所有集合字段全量更新。

### 5.6 删除集合

```
DELETE /api/v1/collections/{id}
```

物理删除集合及关联关系（不会删除媒体）。

### 5.7 向集合添加媒体

```
POST /api/v1/collections/{id}/items
```

支持单个或批量添加：

```bash
# 单个
curl -X POST http://localhost:8000/api/v1/collections/1/items \
  -H "Content-Type: application/json" \
  -d '{"media_id": 42, "note": "精选"}'

# 批量
curl -X POST http://localhost:8000/api/v1/collections/1/items \
  -H "Content-Type: application/json" \
  -d '{"media_id": [42, 43, 44]}'
```

### 5.8 从集合移除媒体

```
DELETE /api/v1/collections/{id}/items/{mediaId}
```

```bash
curl -X DELETE http://localhost:8000/api/v1/collections/1/items/42
```

---

## 6. 标签接口

### 6.1 获取标签列表

```
GET /api/v1/tags?type=subject
```

**响应示例：**
```json
{
    "success": true,
    "data": [
        {
            "id": 1,
            "name": "风景",
            "slug": "landscape",
            "type": "subject",
            "color": "#22c55e",
            "media_count": 15
        },
        {
            "id": 2,
            "name": "人像",
            "slug": "portrait",
            "type": "subject",
            "color": "#f43f5e",
            "media_count": 8
        }
    ]
}
```

### 6.2 创建标签

```
POST /api/v1/tags
```

```bash
curl -X POST http://localhost:8000/api/v1/tags \
  -H "Content-Type: application/json" \
  -d '{"name": "日落", "slug": "sunset", "type": "subject", "color": "#f97316"}'
```

### 6.3 删除标签

```
DELETE /api/v1/tags/{id}
```

### 6.4 给媒体添加标签

```
POST /api/v1/media/{id}/tags
```

**方式 A：通过 tag_id**
```json
{"tag_id": 1}
```

**方式 B：通过 tag_name（标签不存在则自动创建）**
```json
{"tag_name": "日落"}
```

### 6.5 移除媒体标签

```
DELETE /api/v1/media/{id}/tags/{tagId}
```

---

## 7. 搜索接口

### 7.1 全局搜索

```
GET /api/v1/search?q=关键词
```

**参数：**

| 参数 | 默认值 | 说明 |
|------|--------|------|
| `q` 或 `keyword` | 必填 | 搜索关键词 |
| `in` | `all` | 搜索范围：`all` / `media` / `collections` / `tags` |
| `page` | 1 | 页码 |
| `page_size` | 20 | 每页条数 |

**请求示例：**
```bash
curl "http://localhost:8000/api/v1/search?q=上海&in=media&page=1"
```

**响应示例（`in=all`）：**
```json
{
    "success": true,
    "code": 200,
    "data": {
        "keyword": "上海",
        "type": "all",
        "results": {
            "media": [
                {
                    "id": 3,
                    "title": "上海外滩夜景",
                    "type": "photo",
                    "url": "...",
                    "thumbnail_url": "..."
                }
            ],
            "collections": [],
            "tags": []
        }
    }
}
```

---

## 8. 统计接口

### 8.1 系统统计

```
GET /api/v1/stats
```

**响应示例：**
```json
{
    "success": true,
    "code": 200,
    "data": {
        "media": {
            "total": 100,
            "photos": 75,
            "videos": 25,
            "active": 95,
            "trashed": 5,
            "favorites": 20,
            "total_size": 5242880000
        },
        "collections": {
            "total": 8,
            "albums": 3,
            "folders": 4
        },
        "tags": {
            "total": 15
        }
    }
}
```

---

## 9. Python 客户端示例

完整的 Python API 客户端：

```python
"""
MediaCollector API Client
Python 3.8+

用法：
    client = MediaCollectorClient("http://localhost:8000")
    media = client.list_media(type="photo", page=1, page_size=20)
    result = client.create_media(url="...", type="photo")
"""

import requests
from typing import Optional, Any


class MediaCollectorClient:
    """媒体采集系统 API 客户端"""

    def __init__(self, base_url: str = "http://localhost:8000"):
        self.base = base_url.rstrip("/")
        self.api = f"{self.base}/api/v1"
        self.session = requests.Session()
        self.session.headers.update({
            "Content-Type": "application/json",
            "Accept": "application/json",
        })

    def _request(self, method: str, path: str, **kwargs) -> dict:
        url = f"{self.api}{path}"
        resp = self.session.request(method, url, **kwargs)
        resp.raise_for_status()
        return resp.json()

    # ---- 媒体 ----

    def list_media(self, **params) -> dict:
        """获取媒体列表
        
        支持参数: page, page_size, type, status, source, 
                  is_favorite, keyword, author, sort, order
        """
        return self._request("GET", "/media", params={k: v for k, v in params.items() if v is not None})

    def get_media(self, media_id: int) -> dict:
        """获取媒体详情"""
        return self._request("GET", f"/media/{media_id}")

    def create_media(self, **data) -> int:
        """创建媒体，返回 media_id"""
        result = self._request("POST", "/media", json=data)
        return result["data"]["id"]

    def update_media(self, media_id: int, **data) -> None:
        """更新媒体（全量覆盖）"""
        self._request("PUT", f"/media/{media_id}", json=data)

    def patch_media(self, media_id: int, **data) -> None:
        """部分更新媒体"""
        self._request("PATCH", f"/media/{media_id}", json=data)

    def delete_media(self, media_id: int) -> None:
        """删除媒体（软删除）"""
        self._request("DELETE", f"/media/{media_id}")

    def get_exif(self, media_id: int) -> dict:
        """获取 EXIF"""
        return self._request("GET", f"/media/{media_id}/exif")

    def set_exif(self, media_id: int, **data) -> None:
        """设置 EXIF"""
        self._request("PUT", f"/media/{media_id}/exif", json=data)

    # ---- 集合 ----

    def list_collections(self, type: str = None) -> list:
        """获取集合列表"""
        params = {"type": type} if type else {}
        return self._request("GET", "/collections", params=params)["data"]

    def get_collection_tree(self) -> list:
        """获取树形集合结构"""
        return self._request("GET", "/collections/tree")["data"]

    def get_collection(self, collection_id: int, page: int = 1, page_size: int = 20) -> dict:
        """获取集合详情（含媒体）"""
        params = {"page": page, "page_size": page_size}
        return self._request("GET", f"/collections/{collection_id}", params=params)["data"]

    def create_collection(self, **data) -> int:
        """创建集合"""
        result = self._request("POST", "/collections", json=data)
        return result["data"]["id"]

    def add_to_collection(self, collection_id: int, media_ids: list, note: str = "") -> None:
        """添加媒体到集合"""
        data = {"media_id": media_ids if len(media_ids) > 1 else media_ids[0]}
        if note:
            data["note"] = note
        self._request("POST", f"/collections/{collection_id}/items", json=data)

    def remove_from_collection(self, collection_id: int, media_id: int) -> None:
        """从集合移除媒体"""
        self._request("DELETE", f"/collections/{collection_id}/items/{media_id}")

    # ---- 标签 ----

    def list_tags(self, type: str = None) -> list:
        """获取标签列表"""
        params = {"type": type} if type else {}
        return self._request("GET", "/tags", params=params)["data"]

    def create_tag(self, name: str, type: str = "general", color: str = "#6366f1") -> int:
        """创建标签"""
        result = self._request("POST", "/tags", json={
            "name": name,
            "type": type,
            "color": color,
        })
        return result["data"]["id"]

    def add_tag_to_media(self, media_id: int, tag_id: int = None, tag_name: str = None) -> None:
        """给媒体添加标签（tag_id 或 tag_name 二选一）"""
        data = {}
        if tag_id:
            data["tag_id"] = tag_id
        elif tag_name:
            data["tag_name"] = tag_name
        else:
            raise ValueError("请提供 tag_id 或 tag_name")
        self._request("POST", f"/media/{media_id}/tags", json=data)

    def remove_tag_from_media(self, media_id: int, tag_id: int) -> None:
        """移除媒体标签"""
        self._request("DELETE", f"/media/{media_id}/tags/{tag_id}")

    # ---- 搜索与统计 ----

    def search(self, keyword: str, search_in: str = "all") -> dict:
        """搜索"""
        return self._request("GET", "/search", params={"q": keyword, "in": search_in})

    def stats(self) -> dict:
        """系统统计"""
        return self._request("GET", "/stats")["data"]


# ===== 使用示例 =====
if __name__ == "__main__":
    client = MediaCollectorClient("http://localhost:8000")

    # 创建标签
    tag_id = client.create_tag("夜景", type="subject", color="#1e293b")
    print(f"标签 ID: {tag_id}")

    # 创建媒体
    media_id = client.create_media(
        title="上海外滩",
        url="https://images.example.com/bund.jpg",
        type="photo",
        source="manual",
        tags=[tag_id],
    )
    print(f"媒体 ID: {media_id}")

    # 设置 EXIF
    client.set_exif(media_id, camera_make="Sony", iso=1600)

    # 获取统计
    stats = client.stats()
    print(f"共有 {stats['media']['total']} 条媒体")

    # 搜索
    results = client.search("上海")
    print(f"搜索到 {len(results['data']['results'].get('media', []))} 条")
```

---

## 10. JavaScript/TypeScript 客户端示例

```typescript
/**
 * MediaCollector API Client
 * TypeScript 实现，支持浏览器和 Node.js
 */

interface ApiConfig {
    baseURL: string;
    headers?: Record<string, string>;
}

class MediaCollectorClient {
    private baseURL: string;
    private headers: Record<string, string>;

    constructor(config: ApiConfig) {
        this.baseURL = `${config.baseURL.replace(/\/+$/, '')}/api/v1`;
        this.headers = {
            'Content-Type': 'application/json',
            'Accept': 'application/json',
            ...config.headers,
        };
    }

    private async request<T>(
        method: string,
        path: string,
        options?: { params?: Record<string, any>; body?: any }
    ): Promise<ApiResponse<T>> {
        const url = new URL(`${this.baseURL}${path}`);

        if (options?.params) {
            Object.entries(options.params).forEach(([k, v]) => {
                if (v !== undefined && v !== null && v !== '') {
                    url.searchParams.set(k, String(v));
                }
            });
        }

        const resp = await fetch(url.toString(), {
            method,
            headers: this.headers,
            body: options?.body ? JSON.stringify(options.body) : undefined,
        });

        return resp.json();
    }

    // ========== 媒体接口 ==========

    /** 获取媒体列表 */
    async listMedia(params?: MediaListParams) {
        return this.request<PaginatedResponse<MediaItem>>('GET', '/media', { params });
    }

    /** 获取媒体详情 */
    async getMedia(id: number) {
        return this.request<MediaDetail>('GET', `/media/${id}`);
    }

    /** 创建媒体 */
    async createMedia(data: CreateMediaRequest) {
        return this.request<{ id: number }>('POST', '/media', { body: data });
    }

    /** 更新媒体 */
    async updateMedia(id: number, data: Partial<CreateMediaRequest>) {
        return this.request<{ id: number }>('PATCH', `/media/${id}`, { body: data });
    }

    /** 删除媒体 */
    async deleteMedia(id: number) {
        return this.request<null>('DELETE', `/media/${id}`);
    }

    /** 获取 EXIF */
    async getExif(mediaId: number) {
        return this.request<ExifData>('GET', `/media/${mediaId}/exif`);
    }

    /** 设置 EXIF */
    async setExif(mediaId: number, data: Partial<ExifData>) {
        return this.request<null>('PUT', `/media/${mediaId}/exif`, { body: data });
    }

    /** 给媒体添加标签 */
    async addTag(mediaId: number, tagId: number) {
        return this.request<{ tag_id: number }>('POST', `/media/${mediaId}/tags`, {
            body: { tag_id: tagId },
        });
    }

    /** 移除媒体标签 */
    async removeTag(mediaId: number, tagId: number) {
        return this.request<null>('DELETE', `/media/${mediaId}/tags/${tagId}`);
    }

    // ========== 集合接口 ==========

    /** 获取集合列表 */
    async listCollections(type?: string) {
        return this.request<MediaCollection[]>('GET', '/collections', {
            params: { type },
        });
    }

    /** 获取集合树 */
    async getCollectionTree() {
        return this.request<MediaCollection[]>('GET', '/collections/tree');
    }

    /** 获取集合详情 */
    async getCollection(id: number, page = 1, pageSize = 20) {
        return this.request<CollectionDetail>('GET', `/collections/${id}`, {
            params: { page, page_size: pageSize },
        });
    }

    /** 创建集合 */
    async createCollection(data: { name: string; type?: string; parent_id?: number }) {
        return this.request<{ id: number }>('POST', '/collections', { body: data });
    }

    /** 添加媒体到集合 */
    async addToCollection(collectionId: number, mediaIds: number[]) {
        return this.request<null>('POST', `/collections/${collectionId}/items`, {
            body: { media_id: mediaIds },
        });
    }

    /** 从集合移除媒体 */
    async removeFromCollection(collectionId: number, mediaId: number) {
        return this.request<null>('DELETE', `/collections/${collectionId}/items/${mediaId}`);
    }

    // ========== 标签接口 ==========

    /** 获取标签列表 */
    async listTags(type?: string) {
        return this.request<Tag[]>('GET', '/tags', { params: { type } });
    }

    /** 创建标签 */
    async createTag(data: { name: string; type?: string; color?: string }) {
        return this.request<{ id: number }>('POST', '/tags', { body: data });
    }

    // ========== 搜索与统计 ==========

    /** 搜索 */
    async search(keyword: string, searchIn: string = 'all') {
        return this.request<SearchResults>('GET', '/search', {
            params: { q: keyword, in: searchIn },
        });
    }

    /** 系统统计 */
    async getStats() {
        return this.request<Stats>('GET', '/stats');
    }
}


// ===== 使用示例 =====
// const client = new MediaCollectorClient({ baseURL: 'http://localhost:8000' });
//
// async function main() {
//     // 获取统计
//     const stats = await client.getStats();
//     console.log(`共有 ${stats.data?.media.total} 条媒体`);
//
//     // 获取媒体列表
//     const media = await client.listMedia({ type: 'photo', page: 1 });
//     console.log(media.data?.items);
//
//     // 创建媒体
//     const result = await client.createMedia({
//         url: 'https://example.com/photo.jpg',
//         type: 'photo',
//         title: '示例照片',
//         tags: [1, 2],
//     });
//     console.log(`创建成功，ID: ${result.data?.id}`);
// }
// main();
```

---

## 附录：HTTP 状态码与错误处理

### 常见状态码处理逻辑

```typescript
async function safeRequest<T>(
    promise: Promise<ApiResponse<T>>
): Promise<{ data: T | null; error: string | null }> {
    try {
        const resp = await promise;
        if (resp.success) {
            return { data: resp.data, error: null };
        }
        return { data: null, error: resp.message };
    } catch (e) {
        return { data: null, error: '网络请求失败' };
    }
}
```

### 404 处理场景

| 场景 | 原因 | 应对 |
|------|------|------|
| `GET /media/999` | ID 不存在 | 提示"媒体不存在" |
| `GET /collections/999/items` | 集合不存在 | 提示"集合不存在" |
| `DELETE /media/{id}/tags/{tagId}` | 标签不存在 | 忽略（已删除等同成功） |

### 409 冲突处理

```typescript
// 标签已存在时的处理
async function ensureTag(name: string): Promise<number> {
    const client = new MediaCollectorClient({ baseURL: 'http://localhost:8000' });

    // 先尝试查找
    const tags = await client.listTags();
    const existing = tags.data?.find(t => t.name === name);
    if (existing) return existing.id;

    // 不存在则创建
    const result = await client.createTag({ name });
    return result.data!.id;
}
```
