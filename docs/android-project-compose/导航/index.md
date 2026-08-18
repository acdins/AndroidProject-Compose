# 导航概览

## 介绍

导航模块基于 AndroidX Navigation 3 的 `NavKey` 和 `NavBackStack` 管理页面栈，统一提供路由声明、页面注册、跳转、回退、登录拦截和结果回传。当前工程只有一个 Gradle `:app` 模块；`core/navigation` 与 `feature/*/navigation` 是包级分层，不是独立 Gradle 模块。

源码位置：`core/navigation/`。

开始前应已读完 [Core 概览](../框架核心/index.md)和 [ViewModel 基类](../框架核心/base.md)。Navigation 只改变页面栈，不负责加载页面数据；进入目标 Route 后，数据仍由目标 ViewModel 和 Repository 获取。

## 设计目标

- 使用带 `@Serializable` 的 `NavKey` 表示类型安全路由，避免在业务代码中散落字符串路径。
- 由 `AppNavHost` 统一创建回退栈和 `NavDisplay`，各业务域只注册自己的 Graph。
- 由 `AppNavigator` 作为导航执行的统一入口，集中处理命令、登录拦截和结果事件；业务层通过顶层函数或模块 Navigator 发起动作。
- 让页面参数沿着 `entry<Route> { key -> Route(navKey = key) }` 传入页面和 ViewModel，不通过全局变量传递。

## 模块组成

| 路径或类型 | 职责 |
| --- | --- |
| `core/navigation/*Routes.kt` | 声明实现 `NavKey` 的可序列化路由对象或数据类。 |
| `core/navigation/*Navigator.kt` | 封装业务域的语义化跳转方法，例如 `DemoNavigator.toNetworkDemo()`。 |
| `core/navigation/AppNavHost.kt` | 创建从 `MainRoutes.Main` 开始的 `NavBackStack`，配置 `NavDisplay`、动画、状态保存和 Graph 聚合。 |
| `core/navigation/AppNavigator.kt` | 接收导航请求，执行登录拦截；控制器未绑定时缓存命令。 |
| `core/navigation/NavigationService.kt` | 在宿主绑定期间提供 `navigate(...)` 等对象方法和顶层简写函数。 |
| `core/navigation/NavigationController.kt` | 定义导航控制器边界；当前实现由 `BackStackNavigationController` 操作 `NavBackStack`。 |
| `core/navigation/NavigationOptions.kt` | 描述 `popUpToRoute`、`inclusive` 和 `allowPopToEmpty`。 |
| `core/navigation/NavigationResultKey.kt` | 以泛型 Key 绑定结果类型，并提供序列化和反序列化扩展点。 |
| `feature/*/navigation/*Graph.kt` | 把路由映射到对应的 `Route` Composable。 |

当前已注册的 Graph 为 `mainGraph()`、`demoGraph()`、`authGraph()` 和 `userGraph()`；注册入口位于 `AppNavHost.kt` 的 `appEntryProvider`。

初学者可以先记住四个名词：

| 名词 | 回答的问题 |
| --- | --- |
| Route（`NavKey`） | 要去哪个页面，携带哪些参数 |
| Graph | Route 对应的 Composable 入口 |
| Navigator / 顶层函数 | 由谁发起跳转或返回 |
| BackStack | 当前页面历史按什么顺序保存 |

## 运行链路

下图展示从业务事件到 Route Composable 的完整调用链。

```mermaid
flowchart LR
    Caller["ViewModel 或业务事件"] --> Service["NavigationService / 顶层函数"]
    Service --> Navigator["AppNavigator"]
    Navigator --> Guard["RouteInterceptor"]
    Guard --> Controller["NavigationController"]
    Controller --> Stack["NavBackStack<NavKey>"]
    Stack --> Display["NavDisplay"]
    Display --> Graph["mainGraph / demoGraph / authGraph / userGraph"]
    Graph --> Route["Route Composable"]
```

`AppNavHost` 首次创建 `rememberNavBackStack(MainRoutes.Main)`，然后在 `DisposableEffect` 中绑定 `AppNavigator` 和 `NavigationService`。宿主销毁时会解绑二者，避免命令继续发送到失效控制器。

## 能力边界

Navigation 只负责路由对象、页面注册、回退栈变化、拦截和一次性结果事件。它不加载目标页面数据，也不保存需要跨进程恢复的业务状态。

| 读者任务 | 对应章节 |
| --- | --- |
| 定义 `NavKey`、注册 Graph、传入路由参数 | [路由配置](./router.md) |
| 理解跳转、返回和栈清理行为 | [导航流程](./flow.md) |
| 为路由增加登录要求 | [登录与路由拦截](./guard.md) |
| 传递页面参数、发送返回结果或刷新信号 | [参数传递与结果回传](./result.md) |

## 推荐阅读顺序

1. [路由配置](./router.md)：先学会声明 Route、注册 Graph 和封装模块 Navigator。
2. [导航流程](./flow.md)：再理解命令、栈操作和宿主生命周期。
3. [登录与路由拦截](./guard.md)：为需要认证的 Route 增加统一入口检查。
4. [参数传递与结果回传](./result.md)：理解正向参数、返回结果和刷新通知。

## 相关链接

- [路由配置](./router.md)：定义 `NavKey`、模块 Navigator 和 Feature Graph。
- [导航流程](./flow.md)：查看宿主、命令、控制器和回退栈的运行顺序。
- [登录与路由拦截](./guard.md)：为页面配置登录要求。
- [参数传递与结果回传](./result.md)：在页面之间传递类型安全参数和一次性结果。
- [Navigation 3 官方指南](https://developer.android.com/guide/navigation/navigation-3)：了解 `NavKey`、`NavBackStack` 和 `NavDisplay` 的平台 API。
- [AndroidX Navigation 3 版本说明](https://developer.android.com/jetpack/androidx/releases/navigation3)：核对当前工程 `navigation3` 版本（当前版本目录值为 `1.1.6`）。
