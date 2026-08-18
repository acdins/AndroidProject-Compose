---
name: apc-data
description: 实现和维护 AndroidProject-Compose 数据访问链路，覆盖模型、Retrofit Service、NetworkDataSource、Repository、结果处理、Room、本地存储、全局状态、非分页请求和分页列表。用户要求接入接口、创建模型、持久化数据、处理请求结果或实现网络列表时使用。
---

# APC Data

文中的 `core/...` 与 `feature/...` 均为不绑定包名的逻辑路径；在当前 Android 源码集下定位对应文件，不把现有 package 前缀写成固定目录。

## 必读资料

读取 `AGENTS.md` 获取仓库级限制，但不得用它代替本 Skill 的数据边界。根据数据来源读取：

- 总体边界：`docs/android-project-compose/框架核心/data.md`
- 模型与结果：`docs/android-project-compose/框架核心/model.md`、`docs/android-project-compose/框架核心/result.md`
- 网络：`docs/android-project-compose/框架核心/network.md`、`docs/android-project-compose/框架核心/network-base.md`、`docs/android-project-compose/框架核心/pagination.md`
- Room：`docs/android-project-compose/框架核心/database.md`
- 键值存储：`docs/android-project-compose/框架核心/datastore.md`
- 应用级状态：`docs/android-project-compose/框架核心/state.md`
- 页面状态：`docs/android-project-compose/业务功能/viewmodel.md`

## 必读源码

先读取目标领域现有 Model、DataSource、Repository、ViewModel 和测试，再按链路继续读取：

- 网络通用链路：`core/model/network/NetworkResponse.kt`、`core/network/base/BaseNetworkDataSource.kt`、`core/result/ResultHandler.kt`。
- 非分页页面：`core/base/viewmodel/BaseNetWorkViewModel.kt`、`core/base/state/BaseNetWorkUiState.kt`、`core/ui/component/network/BaseNetWorkView.kt`。
- 分页页面：`core/base/viewmodel/BaseNetWorkListViewModel.kt`、`core/base/state/BaseNetWorkListUiState.kt`、`core/ui/component/network/BaseNetWorkListView.kt`、`core/ui/component/refresh/RefreshLayout.kt`。
- 网络领域示例：目标 `*Service.kt`、`*NetworkDataSource.kt`、`*Repository.kt`，以及对应 Demo ViewModel。
- Room：`core/database/AppDatabase.kt`、目标 Entity、DAO、DataSource、Repository 与 `DatabaseModule.kt`。
- 本地存储：目标 Store DataSource 接口与实现、Store Repository、`DataStoreModule.kt` 和底层存储工具。
- 应用级状态：`core/state/UserState.kt` 或同类状态持有者及其 Hilt Module。

不得只根据类名推断成功码、空值、线程切换、分页或异常行为；必须阅读准备复用的函数体和调用方。

## 固定规则

- Feature 只能通过 Repository 或应用级状态访问数据，不直接创建 Retrofit Service、DAO、具体 Store 或 DataSource。
- Model 表达真实契约；Service 只声明 HTTP；DataSource 封装具体数据源；Repository 提供业务语义；ViewModel 管理页面状态。
- ViewModel 对外暴露只读 `StateFlow`，可变状态源保持私有；Screen 和 Content 不直接发起请求或持久化。
- 网络、Room 和本地存储链路各自保持单一入口，不在 Feature 内复制第二套 Repository/DataSource。
- 业务成功条件、可空性、分页字段和错误结构来自目标项目真实契约，不继承演示接口的固定值。
- 首屏、重试、刷新和加载更多必须检查并发与互斥；复用基类前先确认它的实际状态转移和请求生命周期。

## 选择数据链路

- 网络：Request/Entity → Retrofit Service → Network DataSource → Repository → ViewModel → `StateFlow`。
- Room：Entity → DAO → Database DataSource → Repository → ViewModel。
- 键值存储：领域 Store DataSource → Store Repository → ViewModel 或应用级状态。
- 非分页请求：Repository → `BaseNetWorkViewModel` → `BaseNetWorkUiState`。
- 分页列表：Repository → `BaseNetWorkListViewModel` → 列表、刷新、加载更多和分页状态。
- 长期共享状态：Repository 恢复或写入持久化数据 → `core/state` 持有跨页面只读状态。

## 工作流

1. 从真实接口、数据库或存储契约确认字段、可空性、业务成功条件、错误结构和分页语义，不硬编码其他项目的成功码。
2. 复用分页基类前确认接口是页码、游标还是 `hasNext` 模型，并核对起始页、页大小、总数和下一页判断。协议与基类不一致时先建立明确适配，不把游标或零基页码强行套入现有页码状态。
3. 明确“业务成功但 `data == null`”应进入空态、错误态还是允许的无内容成功态，并检查基类能够结束 Loading；不让未约定的空值永久停留在加载状态。
4. 复用现有模型、结果封装、数据源接口、Hilt 模块和 Repository 命名方式，保持每层只做一次转换。
5. 使用完整领域名声明 Repository 字段，不使用无法区分职责的 `repository`、`dataSource` 等名称。
6. 将 Loading、Success、Empty、Error、刷新和加载更多交给现有基类与状态容器；不要在 Screen 重复实现请求状态机。
7. 检查首屏、重试、刷新和加载更多是否可能并发触发；根据业务要求禁用重复入口、复用请求或管理 Job，不假设基类已经覆盖全部互斥场景。
8. 将页面展示状态留在 Feature ViewModel；持久化状态经 Repository/DataSource；只有跨页面且具有应用生命周期的状态才进入 `core/state`。
9. 将跨 Feature 复用的 Preview 数据放入 `core/data/preview`；只服务单个 Feature 的 Preview 数据留在该业务域 `data/`。
10. 修改 Hilt、Room、Serialization 或 Retrofit 声明后检查 KSP 生成结果，但不手工编辑生成目录。

## 验证

为 DataSource、Repository、结果处理或 ViewModel 补充成功、业务失败、异常、空值和分页边界测试。运行相关测试、`./gradlew :app:assembleDebug`、必要的 `./gradlew :app:lint` 与 `git diff --check`，确认 Feature 没有绕过 Repository。
