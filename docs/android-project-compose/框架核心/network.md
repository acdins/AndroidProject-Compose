# 网络请求

## 介绍

项目网络层使用 Retrofit、OkHttp、`kotlinx.serialization` 和 Hilt，把 HTTP 协议细节隔离在 `Service` 与 `NetworkDataSource`。完整调用链固定为 `Service → NetworkDataSource → Repository → ViewModel`；Compose 页面只订阅 ViewModel 状态，不直接创建 Retrofit、调用 Service 或处理响应码。

本页只讲 HTTP 请求如何配置、声明、发送和调试，正文在 NetworkDataSource 与 Hilt 注册处结束。Repository 如何把数据源转换成业务数据流，由[数据层](./data.md)单独说明。开始前应已理解[数据模型](./model.md)和[请求结果处理](./result.md)。

## 模块结构

以下目录只展示网络层的实现位置。

```text
core/network/
├── base/BaseNetworkDataSource.kt
├── datasource/<domain>/
├── interceptor/
├── service/
└── di/
```

| 层 | 当前职责 | 示例 |
| --- | --- | --- |
| Service | 声明 Retrofit HTTP 方法和路径 | `GoodsService` |
| NetworkDataSource | 封装 Service 调用，隔离网络实现 | `GoodsNetworkDataSourceImpl` |
| Repository | 网络层的下游；把数据源组织成业务数据流 | `GoodsRepository` |
| ViewModel | Repository 的下游；把业务结果转换成页面状态 | `NetworkDemoViewModel` |

一次详情请求从点击到渲染的方向如下：

```text
Screen 点击重试
→ ViewModel.executeRequest()
→ GoodsRepository.getGoodsInfo()
→ GoodsNetworkDataSource.getGoodsInfo()
→ GoodsService.getGoodsInfo()
→ OkHttp 添加认证 Header 并发送请求
→ NetworkResponse<Goods>
→ ResultHandler 转成页面三态
→ Screen 重新渲染
```

## 全局配置

`NetworkModule` 当前提供以下单例：

| 组件 | 当前配置 |
| --- | --- |
| `Json` | `ignoreUnknownKeys = true`、`coerceInputValues = true`、`isLenient = true` |
| `OkHttpClient` | 连接、写入、读取超时均为 10 秒；启用失败重试 |
| 认证拦截器 | 从 `AuthStoreDataSource.getToken()` 读取 Token，非空时写入 `Authorization` 请求头 |
| 日志 | Debug 使用 `BODY`，非 Debug 使用 `NONE` |
| Chucker | 仅 Debug 构建添加拦截器 |
| Retrofit | `BuildConfig.BASE_URL`，JSON Content-Type 转换器 |

`BuildConfig.BASE_URL` 来自构建配置，不应在文档或业务代码中硬编码另一套地址。

## Service 示例

文件位置：`core/network/service/GoodsService.kt`

```kotlin
package com.joker.kit.core.network.service

import com.joker.kit.core.model.entity.Goods
import com.joker.kit.core.model.network.NetworkPageData
import com.joker.kit.core.model.network.NetworkResponse
import com.joker.kit.core.model.request.GoodsSearchRequest
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Query

/**
 * 商品相关接口
 */
interface GoodsService {

    /**
     * 分页查询商品
     *
     * @param params 商品搜索请求参数
     * @return 商品分页响应
     */
    @POST("goods/info/page")
    suspend fun getGoodsPage(
        @Body params: GoodsSearchRequest
    ): NetworkResponse<NetworkPageData<Goods>>

    /**
     * 获取商品信息
     *
     * @param id 商品 ID
     * @return 商品详情响应
     */
    @GET("goods/info/info")
    suspend fun getGoodsInfo(
        @Query("id") id: String
    ): NetworkResponse<Goods>
}
```

Service 只声明 HTTP 协议，不负责页面状态、Toast 或数据库写入。

## NetworkDataSource 示例

文件位置：`core/network/datasource/goods/GoodsNetworkDataSourceImpl.kt`

```kotlin
package com.joker.kit.core.network.datasource.goods

import com.joker.kit.core.model.entity.Goods
import com.joker.kit.core.model.network.NetworkPageData
import com.joker.kit.core.model.network.NetworkResponse
import com.joker.kit.core.model.request.GoodsSearchRequest
import com.joker.kit.core.network.base.BaseNetworkDataSource
import com.joker.kit.core.network.service.GoodsService
import javax.inject.Inject

/**
 * 商品网络数据源
 *
 * @param goodsService Retrofit 商品服务
 */
class GoodsNetworkDataSourceImpl @Inject constructor(
    private val goodsService: GoodsService
) : BaseNetworkDataSource(), GoodsNetworkDataSource {

    /**
     * 请求商品分页
     *
     * @param params 分页请求参数
     * @return 商品分页响应
     */
    override suspend fun getGoodsPage(
        params: GoodsSearchRequest
    ): NetworkResponse<NetworkPageData<Goods>> {
        return goodsService.getGoodsPage(params)
    }

    /**
     * 请求商品详情
     *
     * @param id 商品 ID
     * @return 商品详情响应
     */
    override suspend fun getGoodsInfo(id: String): NetworkResponse<Goods> {
        return goodsService.getGoodsInfo(id)
    }
}
```

示例实现的两个方法只转发参数和响应。这样做不是多包一层，而是让 Repository 依赖可替换的 `GoodsNetworkDataSource` 接口，而不是依赖 Retrofit 生成的实现。

对应接口位于同目录的 `GoodsNetworkDataSource.kt`。它必须与实现保持相同签名；`DataSourceModule` 再将实现类作为单例提供给接口。

## 网络层的下游边界

NetworkDataSource 返回项目统一的 `NetworkResponse<T>`，但不创建 `Flow`、切换调度器或维护页面状态。完成网络层后，下一步由 Repository 注入对应的 DataSource，并向 ViewModel 提供业务方法。

```text
本页：Service → NetworkDataSource → Hilt 注册
后续：Repository → ViewModel → View
```

`GoodsRepository` 的完整实现只在[数据层](./data.md)维护，避免网络配置与数据访问规范出现两套教程。

## Hilt 注册

新增 Service 和 DataSource 后，分别在两个模块中注册。以下代码展示真实注册形式；省略同文件内其他业务的提供方法。

```kotlin
// 以下为 ServiceModule.kt 中的方法片段，包声明和其他 imports 已省略。
// core/network/di/ServiceModule.kt
/**
 * 提供商品 Retrofit Service。
 *
 * @param retrofit Retrofit 实例
 * @return 商品 Service
 */
@Provides
@Singleton
fun provideGoodsService(retrofit: Retrofit): GoodsService {
    return retrofit.create(GoodsService::class.java)
}
```

```kotlin
// 以下为 DataSourceModule.kt 中的方法片段，包声明和其他 imports 已省略。
// core/network/di/DataSourceModule.kt
/**
 * 提供商品网络数据源。
 *
 * @param goodsService 商品 Retrofit Service
 * @return 商品网络数据源
 */
@Provides
@Singleton
fun provideGoodsNetworkDataSource(
    goodsService: GoodsService
): GoodsNetworkDataSource {
    return GoodsNetworkDataSourceImpl(goodsService)
}
```

如果漏掉 `ServiceModule`，Hilt 无法提供 Service；如果漏掉 `DataSourceModule`，Hilt 无法把接口解析成实现。两种情况都会在编译期生成明确的缺失绑定错误。

## 错误与响应

Retrofit 请求异常会由 `ResultExt.asResult()` 转换为 `Result.Error`，不满足项目业务成功规则的响应由 `ResultHandler` 转为错误回调。当前示例项目的成功规则是 `code == 1000`；其他项目应根据真实响应协议集中调整。具体状态渲染分别由[非分页网络基类](./network-base.md)和[分页列表](./pagination.md)完成。

| 失败位置 | 典型现象 | 处理位置 |
| --- | --- | --- |
| Retrofit 参数或 Base URL | 请求路径错误、接口 404 | Service 注解与 Gradle 构建配置 |
| OkHttp 连接 | 超时、DNS 失败、断网 | `ResultHandler` 异常分支与页面重试 |
| Kotlin Serialization | 字段类型不匹配 | 数据模型与后端契约 |
| 后端业务状态 | HTTP 成功但不满足项目业务成功规则 | `ResultHandler.onError` |
| Hilt 注册 | 编译期缺失绑定 | `ServiceModule` / `DataSourceModule` |

## 如何新增接口

1. 在 `core/model` 定义请求和响应模型，并按 JSON 字段添加 `@Serializable`。
2. 在 `core/network/service/` 声明 Retrofit Service；接口较多时可按稳定业务域建立子目录，并同步调整包名和 Hilt 注册。
3. 新增 `NetworkDataSource` 接口与实现，调用 Service，不在此层拼接 UI 状态。
4. 在 `DataSourceModule` 注册实现，在 `ServiceModule` 提供 Service。
5. 编译工程，确认 Retrofit Service 与 DataSource 接口都能由 Hilt 提供。
6. 前往[数据层](./data.md)，把 DataSource 接入业务数据入口；再根据页面类型阅读[非分页网络基类](./network-base.md)或[分页列表](./pagination.md)。

完成后至少验证一次成功、业务失败、断网和重试。分页接口还要验证空列表、最后一页以及加载更多失败后的页码回退。

## 调试请求

- Debug 构建的 `HttpLoggingInterceptor` 使用 `BODY` 级别，可在 Logcat 查看请求与响应。
- Debug 构建还会加入 Chucker，可在设备通知入口查看完整会话。
- Release 构建的 HTTP 日志级别为 `NONE`，同时不会加入 Chucker。

::: danger 敏感数据
BODY 日志和 Chucker 可能记录 `Authorization`、手机号和请求体。只在受控的 Debug 环境使用，分享日志或截图前必须脱敏。
:::

## 注意事项

- `GoodsService.getGoodsInfo` 的 `id` 是 `String`，分页接口使用 `GoodsSearchRequest` 请求体，不能混用参数位置。
- 认证拦截器读取 MMKV 中的 Token；应用启动必须先调用 `MMKVUtils.init(application)`。
- Debug 的 BODY 日志和 Chucker 可能包含敏感数据，只能在 Debug 构建使用；Release 日志级别为 `NONE`。
- Retrofit `baseUrl` 必须来自 `BuildConfig.BASE_URL`，并满足 Retrofit 对结尾 `/` 的要求。
- 当前 `AuthInterceptor` 使用 `runBlocking` 读取本地 Token；不要在拦截器之外重复读取并手动拼接认证 Header。

## 下一步

继续阅读[数据层](./data.md)，学习如何在单一入口中组合网络、Room 和本地存储 DataSource；随后再把 Repository 接入非分页或分页 ViewModel。

## 官方文档

- [Retrofit 官方仓库与文档](https://github.com/square/retrofit)
- [OkHttp 官方仓库与文档](https://github.com/square/okhttp)
- [Kotlin Serialization 官方文档](https://github.com/Kotlin/kotlinx.serialization)
- [Hilt 依赖注入](https://developer.android.com/training/dependency-injection/hilt-android)
- [Chucker 官方仓库](https://github.com/ChuckerTeam/chucker)
