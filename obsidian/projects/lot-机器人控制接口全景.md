---
tags: [架构, lot, 接口全景]
date: 2026-04-22
project: lot
status: done
---

# LOT 机器人控制接口全景

> 更新日期：2026-04-22
> 项目根路径：`~/appstore/project/lot/`

## 子项目一览

| 子项目 | 角色 | 核心功能 |
|---|---|---|
| `open-robot-call-api` | 对外开放层 | 第三方/H5 调用机器人任务（召唤、取消、回充等），详见 [[open-robot-call-api-接口文档]] |
| `open-api` | 统一开放平台 | 机器人直控 + 底盘任务流 + 调度查询，内部系统对接 |
| `api` | 核心调度 API | 召唤、送物、取物、巡游等主逻辑，含同步/异步两种调用 |
| `robot-api` | 机器人基础服务 | 门禁控制、SIM 卡、设备密码、版本查询、告警 |
| `robot-call-api` | 通知经纪人 | 机器人任务完成后通知用户（语音电话 / 第三方通知） |
| `robot-lift-api` | 电梯控制 | 机器人乘梯全流程（召梯、开关门、激活、释放） |
| `open-yuncang-api` | 云仓开放 API | 云仓可用性检查、统一调度、操作上报 |

---

## 一、open-robot-call-api（对外开放机器人召唤）

详细字段说明见 [[open-robot-call-api-接口文档]]。

### V5 核心控制（`RobotV5ControlApi`）

| 接口路径 | 方法 | 功能 |
|---|---|---|
| `/openapi/v5/robot/call` | POST | 召唤机器人（支持 delivery/call/cruise 等 taskType） |
| `/openapi/v5/robot/door/open` | POST | 开舱门 |
| `/openapi/v5/robot/door/close` | POST | 关舱门 |
| `/openapi/v5/robot/back` | POST | 机器人回充电桩 |
| `/openapi/v5/robot/task/cancel` | POST | 取消当前任务 |
| `/openapi/v5/robot/redeliver` | POST | 重新派送（取件失败后再次触发） |
| `/openapi/v5/robot/schedule` | POST | 按场地自动分配机器人调度 |
| `/openapi/v5/robot/task/status` | GET | 查询机器人当前任务状态 |
| `/openapi/v5/robot/app/notify` | POST | 向机器人 App 推送屏幕通知 |

### V5 Flow 接口（`UpRobotV5ControlApi`，底盘+上仓一体机）

| 接口路径 | 方法 | 功能 |
|---|---|---|
| `/openapi/v5/robot/flow/submit` | POST | 提交完整任务流（executors JSON 数组） |
| `/openapi/v5/robot/flow/continue` | POST | 在现有任务流上追加步骤 |
| `/openapi/v5/robot/flow/relet` | POST | 续租任务流超时时间 |
| `/openapi/v5/robot/flow/cancel` | POST | 取消指定任务流 |
| `/openapi/v5/robot/chassis/control` | POST | 底盘直控（升降等机械动作） |

### V1 辅助（`RobotV1ControlApi`）

| 接口路径 | 方法 | 功能 |
|---|---|---|
| `/openapi/v1/robot/appview/customize` | POST | 控制机器人屏幕是否显示自定义 H5 页面 |
| `/openapi/v1/chassis/task/flow` | POST | 直接创建底盘任务流（底层接口） |

### 特殊场景

| 接口路径 | 方法 | 功能 |
|---|---|---|
| `/openapi/v5/agv/schedule/task/submit` | POST | 工厂 AGV 调度 |
| `/openapi/v5/jinjiang/ipass/goods` | POST | 锦江集团专用配送接口 |

### V4 已废弃接口（`RobotV4ControlApi`）

路径前缀 `/openapi/v4/`，功能与 V5 相同但走旧命令链，不建议新业务使用。

---

## 二、open-api（统一开放平台）

> 路径：`~/appstore/project/lot/open-api`
> Controller 路径：`src/main/java/ai/yunji/rw/openapi/web/api/`

### 机器人直控（`OpenRobotControlApi`）

| 接口路径 | 方法 | 功能 |
|---|---|---|
| `/openapi/v1/robot/task/state` | GET | 查询机器人任务状态 |
| `/openapi/v1/robot/appview/customize` | POST | 自定义应用视图 |
| `/openapi/v1/robot/app_items` | POST | 设置应用菜单项 |
| `/openapi/v2/uv-lamps/control` | POST | 紫外灯控制（消毒机器人） |
| `/openapi/v1/robot/back` | POST | 机器人返回充电桩 |
| `/openapi/v1/robot/door/open` | POST | 开舱门 |
| `/openapi/v1/robot/door/close` | POST | 关舱门 |
| `/openapi/v1/robot/box/open` | POST | 打开储物箱 |
| `/openapi/v1/robot/box/lock` | POST | 储物箱上锁 |
| `/openapi/v1/robot/box/rule` | POST | 设置储物箱规则 |
| `/openapi/v2/robot/settings` | POST | 设置机器人参数 |

### 底盘任务流（`OpenChassisControlApi`）

| 接口路径 | 方法 | 功能 |
|---|---|---|
| `/openapi/v3/chassis/status` | GET | 查询底盘实时状态 |
| `/openapi/v3/chassis/shutdown` | POST | 底盘关机 |
| `/openapi/v5/chassis/task/flow` | POST | 创建底盘任务流 |
| `/openapi/v5/chassis/cancel/task/flow` | POST | 取消底盘任务流 |
| `/openapi/v5/chassis/update/task/flow` | POST | 更新底盘任务流 |
| `/openapi/v5/chassis/task` | GET | 获取底盘当前任务 |
| `/openapi/v5/chassis/executor/command` | POST | 底盘执行器命令 |
| `/openapi/v5/chassis/sweep/path/current` | GET | 获取当前扫地路径 |
| `/openapi/v5/chassis/create/update/task` | POST | 创建或更新底盘任务 |
| `/openapi/v5/chassis/relet/task` | POST | 重新分配底盘任务 |
| `/openapi/v5/chassis/file/play` | POST | 语音文件播放（UP 系列） |
| `/openapi/v5/chassis/task/status` | GET | 查询 UP 任务状态 |
| `/openapi/v5/chassis/interface/forwarding` | POST | UP 接口转发（透传） |
| `/openapi/v5/chassis/estop` | POST | 紧急停止（E-Stop） |
| `/openapi/v2/robot/chassis/available` | GET | 查询可用底盘列表 |
| `/openapi/v2/robot/up/marker` | GET | 查询舱标记 |
| `/openapi/v2/robot/up/marker/reset` | POST | 重置舱标记 |

### 上舱控制（`OpenScControlApi`）

| 接口路径 | 方法 | 功能 |
|---|---|---|
| `/openapi/v5/deck/executor/command` | POST | 上舱执行器命令 |
| `/openapi/v5/deck/locker/task` | POST | 创建上舱储物柜任务 |

### 调度查询（`OpenScheduleApi`）

| 接口路径 | 方法 | 功能 |
|---|---|---|
| `/openapi/v2/schedule/storeId/query` | GET | 查询门店 StoreId |
| `/openapi/v2/schedule/device/query` | GET | 查询调度设备列表 |
| `/openapi/v2/schedule/marker/query` | GET | 查询可调度标记点 |
| `/openapi/v2/schedule/task/create` | POST | 创建调度任务 |

---

## 三、api（核心调度 API）

> 路径：`~/appstore/project/lot/api`
> Controller 路径：`src/main/java/ai/yunji/rw/api/web/rest/open/v1/robot/OpenRobotCallApi.java`

| 接口路径 | 方法 | 功能 |
|---|---|---|
| `/openapi/v1/robot/callable` | GET | 检查机器人是否可召唤 |
| `/openapi/v1/robot/call/sync` | POST | 同步召唤（等待机器人到达后返回） |
| `/openapi/v1/robot/call` | POST | 异步召唤（完整版，支持多目标） |
| `/openapi/v1/robot/cancel` | POST | 取消当前任务 |
| `/openapi/v1/robot/call/transport` | POST | 机器人送物（多段运输） |
| `/openapi/v1/robot/cancel/transport` | POST | 取消送物任务 |
| `/openapi/v1/robot/call/getitem` | POST | 机器人取物（到取货点等人取件） |
| `/openapi/v1/robot/cancel/getitem` | POST | 取消取物任务 |
| `/openapi/v1/robot/cruise` | POST | 机器人巡游模式 |

---

## 四、robot-api（机器人基础服务）

> 路径：`~/appstore/project/lot/robot-api`
> Controller 路径：`src/main/java/ai/yunji/rw/robotapi/web/api/`

| 接口路径 | 方法 | 控制器 | 功能 |
|---|---|---|---|
| `/api/v2/door/open` | POST | `DoorApi` | 门禁开门 |
| `/api/v2/door/close` | POST | `DoorApi` | 门禁关门 |
| `/robotapi/v1/simcard/detail` | GET | `SimCardApi` | SIM 卡详情查询 |
| `/robotapi/v1/reset/password/check` | POST | `DevicePasswordApi` | 设备密码重置校验 |
| `/robotapi/v1/reset/password/finish` | POST | `DevicePasswordApi` | 密码重置完成确认 |
| `/robotapi/v1/reset/password/update` | POST | `DevicePasswordApi` | 密码更新 |
| `/robotapi/v1/robot/version/query` | GET | `UpdateApi` | 查询机器人版本信息 |
| `/api/v2/robot/param` | POST | `RobotConfigApi` | 设置机器人运行参数 |
| `/api/v2/robot/alert` | POST | `AlertApi` | 机器人告警上报 |
| `/robotapi/v1/robot/detail` | GET | `RobotInfoApi` | 查询机器人详情 |

---

## 五、robot-call-api（通知经纪人）

> 路径：`~/appstore/project/lot/robot-call-api`
> 项目名：rw-broker
> Controller 路径：`src/main/java/ai/yunji/rw/broker/web/api/`

机器人完成任务后，通过电话或第三方推送通知酒店客人（"您的物品已送达"）。

| 接口路径 | 方法 | 控制器 | 功能 |
|---|---|---|---|
| `/api/v2/notify` | POST | `NotifyApi` | 触发通知（语音/短信/第三方渠道） |
| `/api/v2/notify/event` | POST | `NotifyApi` | 通知事件上报（已废弃） |
| `/api/v2/phone/call` | POST | `PhoneApi` | 发起电话呼叫 |
| `/api/v2/phone/callback` | POST | `PhoneApi` | 电话呼叫结果回调 |
| `/api/v2/phone/call/check` | GET | `PhoneApi` | 检查电话可用性 |

---

## 六、robot-lift-api（电梯控制）

> 路径：`~/appstore/project/lot/robot-lift-api`
> Controller 路径：`src/main/java/ai/yunji/rw/liftapi/web/api/lift/LiftV5Api.java`

统一封装各品牌电梯（三菱、奥的斯、日立等）的控制协议，机器人通过本服务完成乘梯全流程。

| 接口路径 | 方法 | 功能 | 乘梯阶段 |
|---|---|---|---|
| `/api/v5/lift/call` | POST | 内呼电梯（机器人在梯内选层） | 乘梯中 |
| `/api/v5/lift/outer_call` | POST | 外呼电梯（机器人在梯外等候） | 等梯 |
| `/api/v5/lift/open` | POST | 电梯开门 | 进出梯 |
| `/api/v5/lift/close` | POST | 电梯关门 | 进出梯 |
| `/api/v5/lift/release` | POST | 释放电梯占用 | 完成后 |
| `/api/v5/lift/active` | POST | 激活电梯（初始化连接） | 启动阶段 |
| `/api/v5/lift/status` | GET | 查询电梯当前状态 | 任意 |
| `/api/v5/lift/robot_status` | GET | 查询机器人与电梯的联动状态 | 任意 |

---

## 七、open-yuncang-api（云仓开放 API）

> 路径：`~/appstore/project/lot/open-yuncang-api`
> Controller 路径：`src/main/java/ai/yunji/rw/yuncang/openapi/web/api/OpenYuncangApi.java`

| 接口路径 | 方法 | 功能 |
|---|---|---|
| `/api/v3/yuncang/isAvailable` | GET | 检查云仓设备是否可用 |
| `/api/v2/yuncang/order/unifiedDispatching` | POST | 云仓统一调度（下发取/放货任务） |
| `/api/v2/manager/yuncang/task/putActionInfo` | POST | 云仓操作动作信息上报 |

---

## 整体架构关系

```
外部调用方（第三方/H5/内部系统）
    │
    ├─ open-robot-call-api   → 对外统一入口，多品牌机器人任务调度
    ├─ open-api              → 内部系统直控接口 + 底盘任务流
    └─ api                   → 核心业务逻辑（送物/取物/巡游）
         │
         ├─ robot-api        → 基础设备服务（门禁/SIM/密码/版本）
         ├─ robot-call-api   → 任务完成后通知用户（电话/推送）
         ├─ robot-lift-api   → 电梯联动控制
         └─ open-yuncang-api → 云仓设备调度
```

## 设计决策

| 决策 | 结论 | 原因 |
|------|------|------|
| 为什么拆出 open-robot-call-api 而非全放 api | open-robot-call-api 面向第三方/H5，有独立鉴权（appname+appSecret）和版本管理需求；api 是内部调度核心，不对外暴露 |
| V4 接口为何仍保留 | 存量合作方接入，不能强制升级；V4 走旧命令链，不建议新业务用 |
| productId 前缀识别品牌而非配置表 | 运行时零查库，减少一次 DB/RPC，且前缀由硬件出厂 SN 规则决定，稳定 |
| robot-call-api 单独服务 | 通知渠道（电话/短信/推送）迭代频繁，独立部署可热更新不影响调度核心 |
| robot-lift-api 单独服务 | 各品牌电梯协议差异大，独立服务隔离故障域；乘梯失败不影响机器人其他任务 |

## 机器人品牌 / productId 识别规则

| productId 前缀 | 机器人类型 | 控制器 |
|---|---|---|
| HOTGG / GEGE / GG / QL | 歌歌 | GEGE |
| HOT | 女娲 | NVWA |
| KF | 启雷 | KF |
| DELI / DL | 得力 | DELI |
| WATER / WT / OCEAN | UP 底盘 | CHASSIS |
| SC | 医疗上舱 | SC |
