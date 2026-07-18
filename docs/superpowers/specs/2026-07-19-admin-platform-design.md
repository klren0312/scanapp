# ScanApp 管理后台设计文档

- 日期：2026-07-19
- 状态：已获用户批准
- 范围：新增独立的管理后台系统（Node.js + MySQL + Vue3），配合 App 端实时上报，实现设备点位地图展示、轨迹回放、跨区域遇见计算与重点提示

## 1. 背景与目标

现有 scanapp（Kotlin Multiplatform + Kuikly）每 5 秒扫描一轮 WiFi/蓝牙设备，本地 SQLite 按设备去重（仅保留最新位置与累计次数），无完整轨迹历史。

本设计新增管理后台，核心目标：

1. App 内直接 API 上传扫描数据到平台
2. 平台在高德地图上展示设备点位，点击查看详情
3. 按设备筛选，查看出现次数与轨迹
4. 计算同一设备在不同地点被遇见的次数（距离聚类）
5. **核心**：同一设备出现在 ≥2 个不同地点簇即标记为重点设备，全端红色高亮提示

### 关键决策（已与用户确认）

| 决策点 | 结论 |
|---|---|
| 数据入口 | App 内直接 API 上传（非文件导入） |
| "区域"界定 | 距离聚类（默认 500 米阈值，纯后端计算，不依赖外部 API） |
| 重点提示规则 | 同一设备 cluster_count ≥ 2 即触发 |
| 后台鉴权 | 单管理员账号 + JWT |
| 技术栈 | 后端 Express + mysql2（原生 SQL）；前端 Vue3 + Vite + Element Plus + 高德 JS API |

## 2. 总体架构

```
┌──────────────┐  每轮扫描后 POST /api/upload   ┌───────────────────┐
│ scanapp      │  (X-Upload-Token 鉴权, 可重试) │ admin/server      │
│ (Kuikly App) │ ─────────────────────────────►│ Express + mysql2  │
└──────────────┘                               └────────┬──────────┘
                                                        │ 写入 + 增量聚类
                                                        ▼
                                                ┌──────────────┐
                                                │    MySQL     │
                                                └──────▲───────┘
┌──────────────┐   JWT 登录 / REST API                  │
│ admin/web    │ ──────────────────────────────────────┘
│ Vue3+EP+高德 │
└──────────────┘
```

### 目录结构（仓库根新增 `admin/`，与 `shared/`、`androidApp/` 平级）

```
admin/
├── server/                 # Node.js 后端
│   ├── package.json
│   ├── .env.example
│   ├── sql/schema.sql      # 建表语句（启动时幂等执行）
│   ├── src/
│   │   ├── index.js        # 入口：加载 env、初始化 DB、挂载路由
│   │   ├── config.js       # 环境变量读取
│   │   ├── db.js           # mysql2 连接池 + schema 初始化
│   │   ├── auth.js         # 登录 + JWT 中间件
│   │   ├── geo.js          # haversine + WGS-84→GCJ-02 转换
│   │   ├── cluster.js      # 增量聚类 + 全量重建
│   │   ├── routes/
│   │   │   ├── upload.js   # App 上报
│   │   │   ├── devices.js  # 设备列表/详情/轨迹/目击
│   │   │   ├── map.js      # 地图点位
│   │   │   ├── stats.js    # 总览统计
│   │   │   └── admin.js    # 聚类全量重建
│   │   └── seed.js         # 创建管理员账号脚本
│   └── test/
│       ├── cluster.test.js
│       └── geo.test.js
└── web/                    # Vue3 前端
    ├── package.json
    ├── .env.example        # VITE_AMAP_KEY / VITE_AMAP_SECURITY_CODE / VITE_API_BASE
    ├── index.html
    ├── vite.config.js
    └── src/
        ├── main.js
        ├── App.vue
        ├── router/index.js
        ├── store/auth.js   # pinia：token 与登录态
        ├── api/index.js    # axios 封装（JWT 拦截器）
        ├── utils/amap.js   # 高德 JS API loader
        └── views/
            ├── LoginView.vue
            ├── DashboardView.vue   # 总览 + 重点设备高亮列表
            ├── MapView.vue         # 高德地图撒点 + 筛选 + 详情抽屉
            ├── DevicesView.vue     # 设备筛选表格
            └── DeviceDetailView.vue # 轨迹地图 + 簇表格
```

## 3. 数据流

1. App 每轮扫描（5 秒）保存本地 SQLite 后，将本轮目击事件（设备标识 + 信号 + 经纬度 + 时间戳）异步 POST 到服务端
2. 上传失败的批次存入 App 本地 `PendingUpload` 表，下一轮扫描后重试；服务端靠唯一索引幂等去重，重试安全
3. 服务端接收：校验 token → upsert `devices` → `INSERT IGNORE` 写入 `sightings` → 对涉及设备做增量距离聚类 → 更新 `devices.cluster_count` 与 `is_key`
4. 前端只读：登录后拉取点位/轨迹/统计数据渲染

## 4. 数据库设计（MySQL）

```sql
CREATE TABLE IF NOT EXISTS admins (
  id INT AUTO_INCREMENT PRIMARY KEY,
  username VARCHAR(64) NOT NULL UNIQUE,
  password_hash VARCHAR(255) NOT NULL,           -- bcrypt
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS devices (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  device_type ENUM('wifi','bluetooth') NOT NULL,
  device_key VARCHAR(64) NOT NULL,               -- bssid 或蓝牙 mac，统一大写归一化
  name VARCHAR(255) NOT NULL DEFAULT '',         -- ssid 或蓝牙名（取最新）
  extra JSON NULL,                               -- wifi: {frequency}; 蓝牙: {deviceType}
  first_seen DATETIME NOT NULL,
  last_seen DATETIME NOT NULL,
  total_count INT NOT NULL DEFAULT 0,            -- 目击总次数
  cluster_count INT NOT NULL DEFAULT 0,          -- 不同地点数
  is_key TINYINT(1) NOT NULL DEFAULT 0,          -- cluster_count >= 2
  last_lat DOUBLE NOT NULL,
  last_lng DOUBLE NOT NULL,
  UNIQUE KEY uk_device (device_type, device_key)
);

CREATE TABLE IF NOT EXISTS sightings (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  device_id BIGINT NOT NULL,
  uploader_id VARCHAR(64) NOT NULL,              -- 采集端标识（哪台手机）
  lat DOUBLE NOT NULL,
  lng DOUBLE NOT NULL,
  signal INT NOT NULL,                           -- wifi signalStrength 或蓝牙 rssi
  seen_at DATETIME NOT NULL,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  UNIQUE KEY uk_dedup (device_id, uploader_id, seen_at),  -- 幂等去重
  KEY idx_device_time (device_id, seen_at),
  KEY idx_seen_at (seen_at),
  FOREIGN KEY (device_id) REFERENCES devices(id)
);

CREATE TABLE IF NOT EXISTS device_clusters (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  device_id BIGINT NOT NULL,
  center_lat DOUBLE NOT NULL,
  center_lng DOUBLE NOT NULL,
  point_count INT NOT NULL DEFAULT 1,
  first_seen DATETIME NOT NULL,
  last_seen DATETIME NOT NULL,
  KEY idx_device (device_id),
  FOREIGN KEY (device_id) REFERENCES devices(id)
);
```

说明：

- `sightings` 是事实表，支撑轨迹与聚类重建；`devices` 是聚合表，支撑列表与地图快速查询
- `uk_dedup` 保证 App 重试/重复上报不产生重复数据
- 服务端启动时幂等执行 `schema.sql`；`seed.js` 从环境变量读初始管理员账号密码创建（已存在则跳过）

## 5. 核心算法：增量距离聚类

- 阈值 `CLUSTER_RADIUS_M`（默认 500，服务端 env 可配）
- 新目击点到达（按 seen_at 升序处理）：
  - 在该设备现有簇中找 haversine 最近的簇中心
  - 距离 ≤ R：并入该簇，`center` 取滑动平均（`center = (center*n + p) / (n+1)`），`point_count+1`，更新 `last_seen`
  - 否则：以该点为中心创建新簇
- 每批处理完更新：`cluster_count = COUNT(device_clusters)`，`is_key = (cluster_count >= 2)`
- `POST /api/admin/recompute`：修改阈值后清空 `device_clusters`，按设备对 `sightings`（时间升序）重跑同一贪心算法全量重建

### 遇见次数口径

- `total_count`：设备被遇见总次数（sightings 行数）
- `cluster_count`：在不同地点被遇见的地点数（≥2 即重点提示）
- 设备详情页按簇展示：每个地点的遇见次数、首次/末次遇见时间

### 坐标系

App 端记录 WGS-84 GPS 坐标（Android LocationManager 输出），高德地图使用 GCJ-02。处理策略：服务端原样存储 WGS-84；所有出图接口（`/map/points`、`/devices/:id/trajectory`）在服务端用标准偏移算法转换（仅中国境内范围转换，境外原样返回），前端零处理。

## 6. 服务端 API（前缀 `/api`）

| 方法 | 路径 | 鉴权 | 说明 |
|---|---|---|---|
| POST | `/auth/login` | 无 | 管理员登录，bcrypt 校验，返回 JWT（12h 过期） |
| POST | `/upload` | X-Upload-Token | App 上报；体见下；返回 `{inserted, duplicates}` |
| GET | `/stats/overview` | JWT | 设备总数、目击总数、重点设备数、今日新增目击数 |
| GET | `/map/points?type=&keyOnly=` | JWT | 所有设备最新点位（GCJ-02），供地图撒点 |
| GET | `/devices?type=&keyword=&keyOnly=&page=&pageSize=` | JWT | 设备分页列表（含 cluster_count、is_key），支持按名称/标识搜索、按 cluster_count 排序 |
| GET | `/devices/:id` | JWT | 设备详情 + 簇列表（每簇：中心点、遇见次数、首末次时间） |
| GET | `/devices/:id/trajectory` | JWT | 时间序轨迹点（GCJ-02，上限 1000 点，取最新） |
| GET | `/devices/:id/sightings?page=&pageSize=` | JWT | 目击明细分页（时间倒序） |
| POST | `/admin/recompute` | JWT | 全量重建聚类 |

`/upload` 请求体：

```json
{
  "uploaderId": "phone-001",
  "wifi": [
    {"bssid": "AA:BB:CC:DD:EE:FF", "ssid": "CoffeeShop", "signal": -52, "frequency": 2412, "lat": 39.908, "lng": 116.397, "timestamp": 1784...}
  ],
  "bluetooth": [
    {"address": "11:22:33:44:55:66", "name": "手环", "rssi": -61, "deviceType": "BLE", "lat": 39.908, "lng": 116.397, "timestamp": 1784...}
  ]
}
```

- `device_key` 归一化：去空格、统一大写
- `timestamp` 为毫秒时间戳，服务端转 `DATETIME`
- 经纬度为 0 的（App 端定位失败占位值）记录丢弃，不计入统计

## 7. 前端设计（Vue3 + Vite + Element Plus + vue-router + pinia）

1. **登录页**：账号密码表单 → JWT 存 localStorage；axios 请求拦截器附带 token，401 跳回登录；路由守卫拦截未登录访问
2. **总览页**：el-statistic 统计卡片（设备总数/目击总数/重点设备数/今日新增）+ 重点设备红色高亮列表（el-table，is_key 行红色 tag 标识）
3. **地图页**：
   - 高德地图撒点：WiFi 蓝色、蓝牙绿色、**重点设备红色加大标记**
   - 顶部筛选栏：设备关键字搜索（名称/bssid/mac）、类型下拉、"仅看重点"开关（el-switch）
   - 点击标记 → el-drawer 显示设备详情：基本信息、出现总次数、地点数、轨迹折线（AMap.Polyline）、各地点遇见次数表
4. **设备页**：el-table 筛选分页表格，支持按跨区域数排序，重点设备整行标红；点击行进入详情页
5. **设备详情页**：轨迹地图 + 簇表格（地点序号、中心坐标、遇见次数、首次/末次遇见时间），重点设备顶部 el-alert 红色提示

高德 Key：前端 `.env` 配置 `VITE_AMAP_KEY` 与 `VITE_AMAP_SECURITY_CODE`（需在高德开放平台申请"JS API"类型 key），`.env.example` 提供模板，不提交真实 key。

## 8. App 端改动（shared 模块，遵循现有架构模式）

- `commonMain` 新增：
  - `UploadTransport` 接口（参照 `PlatformExportController` 的平台边界模式）：`suspend fun postJson(url: String, token: String, body: String): Boolean`
  - `UploadService`：组装上报 JSON、管理 `PendingUpload` 队列（入队/每轮重试/成功删除，上限 500 批丢最旧）
- `androidMain` 新增 `HttpUrlConnectionUploadTransport`：用 `HttpURLConnection` 实现，零新依赖；iOS 端暂以空实现占位（iosApp 当前为占位工程）
- `Database.sq` 新增 `PendingUpload` 表：`id INTEGER PRIMARY KEY AUTOINCREMENT, payload TEXT NOT NULL, createdAt INTEGER NOT NULL`，并重新生成 SQLDelight 接口
- `ScannerServiceImpl.saveWifiDevices/saveBluetoothDevices` 保存本地后调用 `UploadService.enqueue(...)`
- `SettingsPage` 新增"平台上报"区块：服务器地址、Upload Token、采集端 ID、启用开关（Kuikly SP 持久化，key 前缀 `upload_`）
- 经纬度为 0（定位失败）的记录不入待传队列

## 9. 错误处理

| 场景 | 处理 |
|---|---|
| 上传 token 错误 | 401，App 端记录失败原因不无限重试（区分鉴权失败与网络失败：401 暂停上报直至配置变更，网络错误继续重试） |
| 上传参数非法 | 400 + 具体字段错误信息 |
| JWT 过期/无效 | 401，前端跳登录页 |
| 高德 key 缺失 | 前端地图区域显示配置提示而非白屏 |
| DB 连接失败 | 服务端启动即失败退出并打印原因（fast fail） |
| 重复上报 | 唯一索引 `uk_dedup` 幂等去重，返回 duplicates 计数 |

## 10. 测试与验证

- 服务端：`node:test` 单测覆盖
  - `geo.test.js`：haversine 距离、WGS-84→GCJ-02 转换（境内转换/境外不转换）
  - `cluster.test.js`：同点归一簇、超距开新簇、滑动平均、跨簇判定、全量重建与增量结果一致
- App 端：`shared/src/commonTest` 新增 `UploadService` 队列逻辑单测（入队、重试、成功删除、超限丢最旧）
- 前端：手动验证（登录、撒点、筛选、轨迹、重点高亮）
- 验证命令：服务端 `npm test`；App `.\gradlew.bat :shared:testDebugUnitTest`

## 11. 环境依赖

- 服务端：Node.js ≥ 18；MySQL ≥ 5.7（8.x 推荐），连接信息走 `.env`（`DB_HOST/DB_PORT/DB_USER/DB_PASSWORD/DB_NAME`、`JWT_SECRET`、`UPLOAD_TOKEN`、`CLUSTER_RADIUS_M`、`ADMIN_USERNAME`、`ADMIN_PASSWORD`、`PORT`）
- 前端：Node.js ≥ 18；高德 JS API key + 安全密钥
- 遵循仓库规范：所有改动更新 `CHANGELOG.md`；App 侧改动跑对应 Gradle 验证后提交
