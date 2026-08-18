# 数据模型

## 介绍

`core/model` 使用 Kotlin `data class` 描述请求参数、服务端响应和业务实体，是 Service、DataSource、Repository 与 ViewModel 共同使用的数据契约。模型只表达字段和与字段直接相关的计算，不发起请求、不访问数据库，也不保存 Compose 页面状态。

在完整网络链路中，模型所处的位置如下：

```text
ViewModel 创建请求模型
→ Repository 转交给 NetworkDataSource
→ Service 把请求模型序列化为 JSON
→ 服务端响应反序列化为 NetworkResponse<T>
→ ResultHandler 解包 T 并交给 ViewModel
```

## 模块结构

以下目录列出当前网络、请求和实体模型的位置。

```text
core/model/
├── common/
│   ├── Id.kt
│   └── Ids.kt
├── entity/
│   ├── Auth.kt
│   ├── Goods.kt
│   └── User.kt
├── network/
│   ├── NetworkResponse.kt
│   ├── NetworkPageData.kt
│   └── NetworkPageMeta.kt
└── request/
    └── GoodsSearchRequest.kt
```

## 网络响应模型

### `NetworkResponse<T>`

下表记录当前源码的真实响应模型，不是所有后端都必须遵守的固定格式。

| 字段 | 类型 | 默认值 | 说明 |
| --- | --- | --- | --- |
| `data` | `T?` | `null` | 真实业务数据 |
| `code` | `Int` | `1000` | `1000` 表示成功 |
| `message` | `String?` | `null` | 业务错误提示 |
| `isSucceeded` | `Boolean` | 计算属性 | `code == 1000` |

`ResultHandler` 使用 `isSucceeded` 判断业务成功；页面不直接比较 `1000`、`0` 或其他固定值。接入不同后端时，应按真实 JSON 契约调整字段名称、字段类型、可空性和 `isSucceeded`，并同步验证 `ResultHandler`，不能只修改页面判断。

### 分页模型

`NetworkPageData<T>` 包含可空的 `list: List<T>?` 与 `pagination: NetworkPageMeta?`。`NetworkPageMeta` 的字段如下：

| 字段 | 类型 | 说明 |
| --- | --- | --- |
| `total` | `Int?` | 总条数 |
| `size` | `Int?` | 当前页大小 |
| `page` | `Int?` | 当前页码 |

分页基类在 `pagination` 为空时认为没有更多数据；有值时按 `size * page < total` 计算下一页，详见[分页列表](./pagination.md)。

## 请求模型

文件位置：`core/model/request/GoodsSearchRequest.kt`

当前 `GoodsSearchRequest` 把分页参数集中为一个可序列化对象，避免 ViewModel 和 Repository 之间传递含义不清的 `Map<String, Any>`：

```kotlin
package com.joker.kit.core.model.request

import kotlinx.serialization.Serializable

/**
 * 商品分页请求
 *
 * @param page 页码，默认从第 1 页开始
 * @param size 每页大小，普通调用默认 20
 */
@Serializable
data class GoodsSearchRequest(
    val page: Int = 1,
    val size: Int = 20
)
```

创建 `GoodsSearchRequest()` 时会得到第 1 页、每页 20 条；传入 `GoodsSearchRequest(page = 2, size = 15)` 才会覆盖默认值。分页 Demo 的 ViewModel 使用 `pageSize = 15` 构造请求，因此不要把请求模型默认值和页面实际分页大小混为一谈。

## 实体模型

### `Goods`

`Goods` 是当前网络示例实体，包含 `id`、`typeId`、`title`、可空副标题与图片列表、价格、销量、推荐/精选标记、状态、排序和时间字段。`price`、`sold`、`status`、`sortNum` 均为 `Int`；`pics` 与 `contentPics` 为可空 `List<String>`。

### `User`

`User` 记录用户资料与服务端状态：`id`、`unionid`、`avatarUrl`、`nickName`、手机号、性别、状态、登录方式和时间字段。`avatarUrl`、`nickName` 等个人资料可能为空，UI 应使用 `orEmpty()` 或明确的空态。

### `Auth`

`Auth` 保存 `token`、`refreshToken`、过期时长和创建时间，并提供：

| 方法 | 行为 |
| --- | --- |
| `isExpired()` | 当前时间达到 `createdAt + expire * 1000` |
| `isRefreshTokenExpired()` | 当前时间达到刷新令牌的过期时间 |
| `shouldRefresh()` | 访问令牌过期前 15 分钟进入刷新窗口，且刷新令牌未过期 |

## 通用 ID 模型

文件位置：`core/model/common/Id.kt`。

```kotlin
import kotlinx.serialization.Serializable

/**
 * 单个 ID 响应
 *
 * @param id ID 值
 */
@Serializable
data class Id(
    val id: Long = 0
)
```

只返回一个 ID 的接口可复用 `Id`；批量 ID 使用现有 `Ids(ids: List<Long>)`。

## 新增业务模型

假设新接口返回分类信息，新增模型时按以下顺序处理：

1. 根据服务端契约确认 JSON 字段名、类型、可空性和默认值。
2. 在 `core/model/entity/` 新建 `@Serializable` 数据类。
3. 在 Service 与 DataSource 的返回类型中使用该模型。
4. 在 Repository 中决定直接返回、转换或组合数据。
5. 用成功、缺少可选字段和字段类型错误三类响应验证解析行为。

::: warning 不要用默认值掩盖契约错误
默认值只适合后端允许缺省的字段。后端必传字段如果被随意声明为可空或赋默认值，解析虽然可能成功，错误数据却会继续进入 UI。
:::

## 失败时如何定位

| 现象 | 优先检查 |
| --- | --- |
| 反序列化异常 | JSON 字段类型、可空性、泛型层级是否与响应一致 |
| 请求参数未生效 | Service 注解和请求模型字段名是否与接口约定一致 |
| 业务成功但页面没有数据 | 响应是否满足项目成功规则，页面必需数据字段是否为空或结构不匹配 |
| 分页不再加载下一页 | `pagination.total`、`size`、`page` 是否真实返回 |

`NetworkModule` 中的 `Json` 开启了 `ignoreUnknownKeys`，服务端新增无关字段不会直接导致解析失败；已有字段类型变化仍会失败。可在网络日志中核对原始响应，但认证令牌和个人信息必须脱敏。

## 注意事项

- `NetworkResponse.data`、`NetworkPageData.list` 和 `pagination` 都可为空，解析成功不等于业务数据一定存在。
- `@Serializable` 模型字段变化会影响 JSON 解析；当前全局 `Json` 开启 `ignoreUnknownKeys`，但仍需保持字段类型兼容。
- 不把 `Goods` 直接当作 Room Entity；需要本地持久化时应定义数据库实体并明确转换关系。
- 认证 Token 属于敏感数据，不要在日志或文档示例中写入真实值。

## 下一步

继续阅读[请求结果处理](./result.md)，了解 `NetworkResponse<T>` 怎样区分请求异常、业务失败和有效数据；随后在[网络请求](./network.md)中把模型接入 Service 与 DataSource。

## 官方文档

- [Kotlin Serialization](https://github.com/Kotlin/kotlinx.serialization)
- [Kotlin 数据类](https://kotlinlang.org/docs/data-classes.html)
- [Retrofit Kotlin Serialization 转换器](https://github.com/JakeWharton/retrofit2-kotlinx-serialization-converter)
