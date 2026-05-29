---
tags:
  - 架构
  - lot
  - marker
  - 地图数据
  - L4
date: 2026-04-27
project: lot
status: done
scope: lot
generalized: false
retrieval_triggers: [marker点位, 地图数据, listAll, bag版本, YAML解析, MinIO, L4网关, 参数补充]
confidence: 已验证（代码级分析）
---

# L4 网关适配器架构 + Marker 点位数据流分析

**结论**：L4 Gateway 是纯透传层，参数补充（resourceId 解析、HDOS 签名注入、虚拟资源档案创建）全部在 Adapter 侧完成。Marker 点位数据从 MinIO YAML 文件解析，Bag 版本机制保证 listAll 接口始终返回最新版本。

## 背景

L4 层分为网关（l4-gateway）和适配器（l4-res-adapter），需要搞清楚请求链路中参数补充的职责归属。同时分析 lot/open-api 的 `/openapi/v2/place/marker/listAll` 接口数据获取逻辑。

## 决策记录

| 方案 | 结论 | 原因 |
|------|------|------|
| 方案 A：Gateway 补充参数后转发 Adapter（方向 A） | ❌ 否决 | 历史设计考虑过，但已过时 |
| 方案 B：Adapter 自行补充参数（方向 B） | ✅ 采纳 | 代码已实现：Adapter 自动从 atomization 解析 resourceId，注入 HDOS 签名 |

## L4 Gateway/Adapter 调用链

```
外部调用 → Gateway (POST /coordinate/resources/:resourceId/execute)
              → Gateway 调 Adapter (POST /api/v1/skills/execute)
                  → Adapter SkillController.execute()
                      → 从 skillId 解析 atomization → 取 ownerResourceId
                      → HDOS 签名注入
                      → 创建虚拟资源档案（如需要）
                      → ExecuteActionUseCase.run() 执行任务
```

**Gateway 职责**：纯透传，不做任何参数加工。
**Adapter 职责**：参数补充、签名、资源档案、幂等、异步等待。

## Marker 点位数据流

### 请求链路

```
GET /openapi/v2/place/marker/listAll?placeId=xxx[&floor=1][&containsPose=true]
  → OpenMapApi.getPlaceAllMarkers()
    → mapService.listMaps(placeId) → 找 isUsing == 1 的地图
    → mapService.getRecentMapMarkers(placeId, mapName)  ← Hessian RPC
      → MapServiceImpl → mapInfoService.getMapMarkers(placeId, mapName, null)
        → MapInfoServiceImpl.getMapMarkers()
          → bagMapper.selectBagDetail(placeId, mapName, null) ← version=null 取最新
          → 遍历 files，筛选 type == "MARKER"
          → 从 MinIO 下载 YAML (cosUrl + "/" + file.key)
          → SnakeYAML 解析 → Marker 对象
    → 按 floor + name 去重
    → 可选楼层过滤 / 去除位姿
```

### YAML 源文件结构 → Marker 字段映射

| YAML 字段 | Marker 属性 | 类型 | 必填 | 含义 |
|-----------|------------|------|------|------|
| `marker_name` | `name` | String | ✅ | 点位名称，同楼层唯一 |
| `key` | `type` | String | ✅ | 点位类型标识 |
| `floor` | `floor` | int | ✅ | 楼层编号 |
| `num` | `num` | int | ❌ | 点位序号 |
| `avatar` | `avatar` | String | ❌ | 图标标识 |
| `properties` | `properties` | String | ❌ | 扩展属性 JSON |
| `pose.position` | `position` | Position | ❌ | `{x, y, z}` 三维坐标（米） |
| `pose.orientation` | `orientation` | Orientation | ❌ | `{x, y, z, w}` 四元数 |
| *(API 层填充)* | `placeId` | String | - | 门店 ID |
| *(API 层填充)* | `mapName` | String | - | 地图名称 |
| *(API 层填充)* | `version` | String | - | Bag 版本号 |

### 版本机制

同一张地图可有多个 Bag 版本（每次文件变动触发打包生成新版本）。`selectBagDetail(placeId, mapName, null)` 中 version 参数为 null 时查询最新（HEAD）版本。listAll 接口始终返回当前最新版本的点位数据。

**数据重复问题**：如果某个 HDOS 相关接口（非 listAll）返回了多个版本的 marker 数据混在一起，表现为同名点位出现两次但 version 值不同。根因是查询未按单一版本过滤。

### 存储架构

```
MinIO: {bucket}/{placeId}/{mapName}/{version}/{-1}.yaml (MARKER 文件)
MySQL (rw_map): bag 表（版本信息）、map_info 表（地图元数据、isUsing）、map_file 表（文件 type/key/floor）
```

### 关键文件索引

| 文件 | 位置 | 作用 |
|------|------|------|
| OpenMapApi | lot/open-api/.../map/OpenMapApi.java | listAll/list 端点 |
| Marker | common/webservice/.../dto/Marker.java | 点位实体 |
| MapService | common/webservice/.../MapService.java | Hessian 接口 |
| MapServiceImpl | iot-min/common-base/.../MapServiceImpl.java | 服务实现（委托 mapInfoService）|
| MapInfoServiceImpl | iot-min/common-base/.../MapInfoServiceImpl.java | 核心：MinIO YAML 解析 |

## 相关链接

- [[arch-iot-platform]]
