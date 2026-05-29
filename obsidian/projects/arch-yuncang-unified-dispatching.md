---
tags: [架构, lot, yuncang, 下货, UBOX, 数据依赖]
date: 2026-04-29
project: lot
status: done
scope: lot
generalized: false
retrieval_triggers: [unifiedDispatching, simpleDispatch, 云仓下货, UBOX下货, 货柜派单, lot数据依赖, v3下货, ES去依赖]
---

# 云仓统一调度接口 unifiedDispatching 架构梳理

商城统一下货入口 `POST /api/v2/yuncang/order/unifiedDispatching`，根据货柜编号前缀分流到两条完全不同的处理链路：UBOX 货柜走外部 HTTP API 下货，传统云仓走 MQTT 指令 + MySQL/ES 持久化。

## 接口定位

| 项目 | 值 |
|------|-----|
| 模块 | `lot/open-yuncang-api` |
| Controller | `OpenYuncangApi.java` L147-196 |
| 入参 | `Goods`（JSON Body） |
| 出参 | `DispatchingResult`（errorCode / errorMsg / requestId） |

## 核心路由：containerSN 前缀分流

```
入口：unifiedDispatching(Goods)
  │
  ├─ containerSN 以 "UBOX" 开头 → UBOX 货柜路径（调外部平台 API）
  │
  └─ 其他前缀 → 传统云仓路径（MQTT 指令 + 本地持久化）
```

分流常量定义在 `UboxConsts.UBOX_VMID_PREFIX = "UBOX"`。

---

## 路径 A：UBOX 货柜（containerSN 以 "UBOX" 开头）

### 调用链

```
1. uboxService.saveUboxOrder(goods)
   → MySQL: INSERT 订单（orderId → Goods JSON）

2. uboxService.resolveStoreId(containerSn)
   → MySQL: 查货柜绑定的门店 ID

3. uboxService.queryContainerMode(containerSn)
   → MySQL: 查货柜模式 mode

4. 按 mode 分支：
   ├─ mode == 1（老模式，按商品下单）
   │   ├─ uboxService.queryNeedCheckStock()
   │   │   → MySQL: 查是否需要库存校验
   │   ├─ uboxVmService.checkGoodsStock()
   │   │   → MySQL: 查 goodsId → uboxId 映射
   │   │   → HTTP: UBOX API /opentrade/getNewProductForBsj 查实时库存
   │   └─ uboxVmService.sendGoodsMsg()
   │       → MySQL: 查 goodsId → uboxId 映射
   │       → HTTP: UBOX API /opentrade/notifyOrderAndShip 下货
   │
   ├─ mode == 2 / 3 / 4（新模式，按货道下单）
   │   └─ uboxVmService.sendGoodsMsgNew()
   │       → HTTP: UBOX API /generalapi/typeOutProduct 按货道下货
   │
   └─ 其他 → 返回 500 "unknown mode"
```

### 数据依赖一览

| 依赖 | 存储类型 | 说明 |
|------|---------|------|
| `UboxMapper.insertOrder()` | MySQL | 保存订单（orderId + Goods JSON） |
| `UboxMapper.selectStoreIdByContainerSn()` | MySQL | 货柜 → 门店映射 |
| `UboxMapper.selectContainerMode()` | MySQL | 货柜模式（1=按商品 / 2,3,4=按货道） |
| `UboxMapper.selectCheckStock()` | MySQL | 是否需要库存校验 |
| `UboxMapper.selectGoodsByStoreId()` | MySQL | 本地 goodsId → UBOX uboxId 映射 |
| `/opentrade/getNewProductForBsj` | 外部 HTTP | 查 UBOX 实时商品库存 |
| `/opentrade/notifyOrderAndShip` | 外部 HTTP | 按商品 ID 下货（mode=1） |
| `/generalapi/typeOutProduct` | 外部 HTTP | 按货道类型下货（mode=2/3/4） |
| `ubox.apiurl / appid / appkey` | 配置文件 | UBOX 平台认证 |

### UBOX API 签名机制

`SignGenerator.getSign(params, appKey)` — 将参数按 key 排序拼接 `key=value`，末尾追加 `_` + appKey，SHA-1 摘要。

### out_type 判断

`robotSN` 为空或以 `"fake"` 开头 → `out_type=101`（无人配合），否则 `out_type=102`（机器人配合出货）。

---

## 路径 B：传统云仓货柜（非 UBOX 前缀）

### 调用链

```
1. 参数校验：containerSN、taskId、orderItem 不为空

2. 生成 requestId (UUID)

3. processSendingGoods(goods, requestId)  ← @Transactional
   │
   ├─ saveGoodsInfo()
   │   → MySQL: INSERT upload_goods（requestId, taskId, items JSON, ...）
   │
   ├─ saveYuncangTask()
   │   ├─ 遍历 orderItem → 构建 YuncangTaskSell（row+column → channelId）
   │   └─ yuncangTaskService.saveYuncangTask(task)
   │       ├─ yuncangTaskMapper.selectTaskList(sellTaskId)  → 查已有任务
   │       ├─ ES: device.info.yuncang  → 查货柜信息（storeId, storeName, brand, status）
   │       ├─ yuncangInfoMapper.selectYuncangInfo()  → DB 兜底
   │       ├─ ES: device.info.yuncang  → 补客户信息（broker/brand/group）
   │       ├─ yuncangTaskMapper.insertTask()  → 任务主表
   │       ├─ yuncangTaskMapper.insertTaskSell()  → 货道明细
   │       └─ yuncangTaskMapper.insertTaskAct()  → 活动日志
   │
   └─ sendGoodsMsg()
       → MQTT: publish("robot/yuncang/{containerSN}/topic/request")
       payload: { command: "/api/send/goods", params: { taskId, items, ... } }
```

### 数据依赖一览

| 依赖 | 存储类型 | 说明 |
|------|---------|------|
| `UploadGoodsMapper.insert()` | MySQL | 货物信息持久化 |
| `YuncangTaskMapper.selectTaskList()` | MySQL | 根据 sellTaskId 查是否已有任务（幂等） |
| ES `device.info.yuncang` | ElasticSearch | 货柜信息（storeId, storeName, brand, status, isClose） |
| `YuncangInfoMapper.selectYuncangInfo()` | MySQL | ES 不可用时的兜底查询 |
| ES `device.info.yuncang` | ElasticSearch | 补充客户维度（broker/brand/group 的 Id+Name） |
| `YuncangTaskMapper.insertTask()` | MySQL | 任务主表 |
| `YuncangTaskMapper.insertTaskSell()` | MySQL | 货道出货明细 |
| `YuncangTaskMapper.insertTaskAct()` | MySQL | 任务活动日志 |
| `MsgPublisher.publish()` | MQTT | topic: `robot/yuncang/{containerSN}/topic/request` |

### 任务保存逻辑细节（YuncangTaskServiceImpl）

- **幂等设计**：先查 sellTaskId 是否已有任务，有则 update，无则 insert
- **货柜状态检查**：isClose="Y" 或 status="2" 的货柜直接跳过不记录
- **客户信息补充**：从 ES 查 brokerId/Name, brandId/Name, groupId/Name 写入任务
- **默认状态**：新建任务状态为 `STATUS_EXEC="1"`（进行中），货道默认状态 `"6"`（出货失败），等回调更新

---

## 入参数据结构（Goods）

| 字段 | 类型 | 说明 | 必填场景 |
|------|------|------|---------|
| `containerSN` | String | 货柜编号，UBOX前缀走UBOX路径 | 传统路径必填 |
| `taskId` | String | 销售任务编号 | 传统路径必填 |
| `orderItem` | List\<OrderItem\> | 商品列表 | 传统路径必填 |
| `orderId` | String | 订单编号 | UBOX路径用做 appTranId |
| `taskType` | int | 任务类型，默认 0 | 否 |
| `robotSN` | String | 机器人编号 | 影响 UBOX out_type |
| `callbackUrl` | String | 回调地址 | 通过 MQTT 传给货柜 |
| `storeId` | String | 门店 ID | v2: UBOX 路径自动从 DB 查询；v3: 调用方传入 |

## 出参数据结构（DispatchingResult）

| 字段 | 说明 |
|------|------|
| `errorCode` | 0=成功, 404=参数缺失, 500=业务异常 |
| `errorMsg` | 错误描述 |
| `requestId` | 成功时返回（UBOX=orderId, 传统=UUID） |

---

## 整体数据流向

```
                    ┌─────────────┐
                    │   商城调用方  │
                    └──────┬──────┘
                           │ POST Goods
                    ┌──────▼──────┐
                    │ unifiedDispatching │
                    └──────┬──────┘
                    ┌──────▼──────────────────┐
                    │ containerSN 前缀判断      │
                    └──┬───────────────────┬───┘
              "UBOX"前缀│                   │其他
               ┌────────▼──────┐    ┌───────▼────────┐
               │  UBOX 路径     │    │  传统云仓路径    │
               └────┬──────────┘    └────┬───────────┘
                    │                     │
          ┌─────────┼─────────┐    ┌──────┼──────┐
          ▼         ▼         ▼    ▼      ▼      ▼
       MySQL    MySQL     HTTP   MySQL  ES    MQTT
      (订单)  (配置映射)  (UBOX API) (任务) (货柜) (指令)
```

## 关键设计决策

| 决策 | 结论 | 原因 |
|------|------|------|
| UBOX 和传统云仓共用一个接口而非拆分 | ✅ 统一入口 | 对商城方屏蔽货柜类型差异，降低接入成本 |
| UBOX 路径不做本地任务持久化 | 只存订单 JSON | UBOX 平台自己管理任务状态，回调走 UBOX 通道 |
| 传统路径默认货道状态为"出货失败" | ✅ 悲观默认 | 等货柜回调确认成功才更新，避免状态不一致 |
| ES 不可用时 MySQL 兜底 | ✅ 双读 | ES 偶尔不稳定，DB 查询保证可用性 |
| SocketTimeoutException 视为成功 | UBOX 路径特有 | 超时不代表失败，UBOX 侧可能已收到指令 |

---

## v3 简化版接口 `/api/v3/yuncang/order/dispatch`

> 2026-04-29：在 v2 基础上新增 v3 版本，核心改动是 UBOX 路径的 storeId 改为上游传入，Legacy 路径暂时保持 v2 逻辑不变（委托 YuncangTaskServiceImpl）。

### 与 v2 的差异

| 维度 | v2 unifiedDispatching | v3 simpleDispatch |
|------|----------------------|-------------------|
| Controller | `OpenYuncangApi.java` L147 | `OpenYuncangApi.java` L128 |
| Service | Controller 内直接调用 | `SimpleDispatchServiceImpl.dispatch()` |
| UBOX storeId | DB 查 `t_place_yuncang + t_place` | 调用方传入 `goods.storeId` ✅ |
| Legacy 任务保存 | `YuncangTaskServiceImpl.saveYuncangTask()` | 同 v2（委托同一 Service） |
| ES 依赖 | 3 次 ES 查询 | 同 v2（标记 TODO 待后续去除） |
| Legacy 空值校验 | ContainerSN/taskId/orderItem | 仅 Controller 层校验（Service 层委托 v2） |

### v2 中 ES 查出的数据哪些是"非 ES 不可"的

> 分析结论：ES 查出的所有字段中，真正没有 DB 替代源的只有 customer hierarchy 6 个字段（broker/brand/group 层级信息），且它们不参与任何业务逻辑判断，仅用于报表展示。

| ES 查询 | 字段 | 用途 | 是否有 DB 替代 | 参与业务逻辑？ |
|---------|------|------|:--:|-------------|
| ① 全文档 | productId, isClose, status, brand, storeId, storeName | 效验阻断 + 填充任务表 | ✅ `t_yuncang_info` 全覆盖 | 🔴 是 |
| ② 指定字段 | brokerId/Name, brandId/Name, groupId/Name | 填充任务表（客户层级） | ❌ 无 DB 替代 | ⬜ 否（catch 吞异常） |
| ③ 全文档 | placeId, placeName | MQTT 异常告警消息体 | ✅ `t_place_yuncang` | 🟡 仅异常分支 |

### 后续改造 TODO

| 序号 | 内容 | 优先级 | 方案 |
|------|------|--------|------|
| 1 | ES 三次查询合并 | 🟡 | 合并为一次全文档查询，字段从内存取 |
| 2 | ES → DB 迁移 | 🟡 | `device.info.yuncang` → `t_yuncang_info`，customer hierarchy 砍掉（报表字段后置填充） |
| 3 | MQTT 异常告警 placeId/Name | 🟡 | 砍掉或改用 `t_place_yuncang` 查询 |

### 关键决策记录

| 决策 | 结论 | 原因 |
|------|------|------|
| v3 先保留 ES，不激进去除 | ✅ 标记 TODO | ES 驱动的 isClose/status 拦截在业务上有实际效果，DB 兜底链虽存在但未经充分验证 |
| v3 Legacy 路径委托 YuncangTaskServiceImpl 而非自行实现 | ✅ | 避免重复实现复杂的状态机逻辑（双凯品牌特殊处理、sell upsert、task_act 记录），只改依赖来源不改逻辑 |
| SimpleDispatchServiceImpl 去掉 YuncangInfoMapper / YuncangTaskMapper 直接依赖 | ✅ | Legacy 路径完全委托给 YuncangTaskService，SimpleDispatchService 不再直接操作 ES/DB |

---

## 相关链接

- [[arch-lot-robot-api-overview]] — LOT 子项目接口总览
- [[pitfalls-open-yuncang-api-sign]] — 签名验证踩坑记录
