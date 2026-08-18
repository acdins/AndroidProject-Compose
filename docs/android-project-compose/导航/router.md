# 路由配置

## 介绍

路由配置把一个已实现的页面接入当前 Navigation 3 运行时，包含四个位置：`core/navigation` 定义路由，业务域 Navigator 封装跳转，`feature/*/navigation` 注册页面，`AppNavHost` 聚合 Graph。本页只讲路由声明、注册和 Graph 聚合；参数值如何进入 Route、ViewModel 与 Screen，由[参数传递与结果回传](./result.md#普通参数传递)说明。完整页面创建步骤见[创建页面流程](../业务功能/create-page.md)。

## 前置条件

- AndroidX Navigation 3 runtime 和 UI 已由工程依赖提供；版本以 `gradle/libs.versions.toml` 的 `navigation3` 为准。
- 页面需要有一个 `Route` Composable，并在对应 Feature Graph 中注册。
- 需要传参时，参数字段必须定义在 `NavKey` 数据类中，并通过 Graph 的 `key` 参数传递。

## 一、定义类型安全路由

路由文件放在 `core/navigation/<domain>/`，无参数页面使用 `data object`，带参数页面使用 `data class`。两者都要标记 `@Serializable` 并实现 `NavKey`。

文件位置：`core/navigation/demo/DemoRoutes.kt`。

```kotlin
package com.joker.kit.core.navigation.demo

import androidx.navigation3.runtime.NavKey
import kotlinx.serialization.Serializable

/**
 * Demo 模块路由示例。
 */
object DemoRoutes {
    /**
     * 无参数网络页面。
     */
    @Serializable
    data object NetworkDemo : NavKey

    /**
     * 携带商品 ID 的页面。
     * @property goodsId 商品 ID
     */
    @Serializable
    data class NavigationWithArgs(
        val goodsId: Long,
    ) : NavKey
}
```

验证方式：编译时 `DemoRoutes.NetworkDemo` 和 `DemoRoutes.NavigationWithArgs(goodsId = 123)` 都应满足 `NavKey` 类型要求。

## 二、封装业务域跳转

在 `core/navigation/<domain>/` 增加 `<Domain>Navigator`，让业务代码依赖动作名称而不是具体路由对象。模块 Navigator 是 Feature ViewModel 发起页面跳转的默认入口；顶层 `navigate(...)` 主要用于 Navigator 内部实现或无法归属具体业务域的应用级导航。

文件位置：`core/navigation/demo/DemoNavigator.kt`。

```kotlin
package com.joker.kit.core.navigation.demo

import com.joker.kit.core.navigation.navigate

/**
 * Demo 模块导航封装。
 */
object DemoNavigator {
    /**
     * 打开网络示例页。
     */
    fun toNetworkDemo() {
        // 由统一导航服务将 NavKey 放入当前回退栈。
        navigate(DemoRoutes.NetworkDemo)
    }

    /**
     * 打开带参数示例页。
     *
     * @param goodsId 商品 ID
     */
    fun toNavigationWithArgs(goodsId: Long = 0) {
        // 参数随类型安全路由一起进入 Graph 和 Route。
        navigate(DemoRoutes.NavigationWithArgs(goodsId = goodsId))
    }
}
```

验证方式：调用 `DemoNavigator.toNavigationWithArgs(goodsId = 123)` 后，Graph 的 `key.goodsId` 应为 `123`。

## 三、在 Feature Graph 注册页面

Graph 文件放在 `feature/<domain>/navigation/`，使用 `EntryProviderScope<NavKey>.entry<Route>` 将路由映射到 Route Composable。带参数的 `entry` lambda 会收到对应的路由对象。

文件位置：`feature/demo/navigation/DemoGraph.kt`。

```kotlin
package com.joker.kit.feature.demo.navigation

import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import com.joker.kit.core.navigation.demo.DemoRoutes
import com.joker.kit.feature.demo.view.NavigationWithArgsRoute
import com.joker.kit.feature.demo.view.NetworkDemoRoute

/**
 * Demo 模块 Graph 注册示例。
 */
fun EntryProviderScope<NavKey>.demoGraph() {
    entry<DemoRoutes.NetworkDemo> {
        NetworkDemoRoute()
    }
    entry<DemoRoutes.NavigationWithArgs> { key ->
        // Route 负责接收 NavKey，再决定如何创建带参数 ViewModel。
        NavigationWithArgsRoute(navKey = key)
    }
}
```

`NavigationWithArgsRoute` 的真实实现使用 Hilt Assisted Factory 创建 ViewModel；UI 层只接收 ViewModel 暴露的 `goodsId`。完整参数链路只在[参数传递与结果回传](./result.md#普通参数传递)维护，ViewModel Factory 的通用规则见[ViewModel 规范](../业务功能/viewmodel.md#路由参数与-assisted-inject)。

## 四、聚合应用级 Graph

`AppNavHost.kt` 中的 `appEntryProvider` 聚合当前四个业务 Graph。新增业务域后需要在此处增加一行，页面才会被 `NavDisplay` 找到。

文件位置：`core/navigation/AppNavHost.kt`。

```kotlin
import androidx.compose.animation.SharedTransitionScope
import androidx.navigation3.runtime.entryProvider
import com.joker.kit.feature.auth.navigation.authGraph
import com.joker.kit.feature.demo.navigation.demoGraph
import com.joker.kit.feature.main.navigation.mainGraph
import com.joker.kit.feature.user.navigation.userGraph

/**
 * 聚合所有业务域的页面入口。
 *
 * @param scope 共享元素动画作用域
 * @return Navigation 3 EntryProvider
 */
private fun appEntryProvider(scope: SharedTransitionScope) = entryProvider {
    mainGraph()
    demoGraph()
    authGraph()
    userGraph()
}
```

当前签名接收 `SharedTransitionScope`，但函数体没有读取 `scope`。新增 Graph 时保持现有签名并追加调用即可；不要假定 Feature Graph 已经自动获得共享元素作用域。

## 验证新路由

完成四个位置后按以下顺序验证：

1. 编译工程，确认 `@Serializable`、Hilt assisted factory 和 `entry<Route>` 类型都能解析。
2. 从已注册页面调用模块 Navigator，确认目标 Route 出现在栈顶。
3. 带参页面传入一个可识别值，例如 `goodsId = 123`，确认 ViewModel 与 Screen 都收到 `123`。
4. 返回上一页，确认没有重复压栈或误清除 `MainRoutes.Main`。

| 现象 | 优先检查 |
| --- | --- |
| 点击后无目标页面 | Navigator 是否调用正确 Route，Graph 是否被 `appEntryProvider` 聚合 |
| 目标页面解析失败 | `entry<Route>` 泛型是否与实际 NavKey 相同 |
| 参数始终是默认值 | Navigator 是否真的构造带参 data class，Graph 是否把 `key` 传给 Route |
| ViewModel 无法创建 | assisted factory 注解、`creationCallback` 和 `Factory.create(navKey)` 是否一致 |

## 注意事项

- `entry<Route>` 注册必须与 `NavKey` 的实际类型一致；类型不匹配时，`NavDisplay` 无法解析目标页面。
- 带参页面不要把参数改写成全局状态或字符串；直接使用 `entry` 提供的 `key`，再交给 Route / ViewModel。
- 只新增 `*Routes.kt` 而不更新 Graph，不会自动生成页面入口。
- 只更新 Graph 而不让 `appEntryProvider` 调用它，模块页面仍不会被应用导航宿主发现。
- `MainRoutes.Main` 是当前回退栈首项；不要在普通业务跳转中随意清空栈底。

## 相关链接

- [导航概览](./index.md)：了解目录职责和运行链路。
- [导航流程](./flow.md)：了解 Graph 如何进入 `NavDisplay`。
- [Android Navigation 3 路由与 Entry 官方指南](https://developer.android.com/guide/navigation/navigation-3)：核对 `NavKey`、`entry` 和 `NavDisplay` API。
