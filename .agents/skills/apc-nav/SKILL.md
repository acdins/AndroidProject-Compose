---
name: apc-nav
description: 配置和维护 AndroidProject-Compose 的 Navigation 3 导航，覆盖类型安全 NavKey、Feature Graph、模块 Navigator、回退栈、普通参数、Assisted ViewModel、结果回传和登录拦截。用户要求新增路由、页面跳转、传参、返回结果、登录保护或修复导航状态时使用。
---

# APC Navigation

文中的 `core/...` 与 `feature/...` 均为不绑定包名的逻辑路径；在当前 Android 源码集下定位对应文件，不把现有 package 前缀写成固定目录。

## 必读资料

读取 `AGENTS.md` 获取仓库级限制，但不得用它代替本 Skill 的导航契约。读取以下专题：

- `docs/android-project-compose/导航/index.md`
- `docs/android-project-compose/导航/router.md`
- `docs/android-project-compose/导航/flow.md`
- `docs/android-project-compose/导航/guard.md`
- `docs/android-project-compose/导航/result.md`
- 创建页面模板或使用模板生成文件时读取 `docs/android-project-compose/业务功能/templates.md`
- 新增完整页面时读取 `docs/android-project-compose/业务功能/create-page.md`
- 需要确认页面分层或 ViewModel 接入方式时读取 `docs/android-project-compose/业务功能/view.md` 和 `docs/android-project-compose/业务功能/viewmodel.md`

## 必读源码

修改导航前直接读取当前实现：

- 宿主与运行时：`core/navigation/AppNavHost.kt`、`AppNavigator.kt`、`NavigationService.kt`、`BackStackNavigationController.kt`、`NavigationOptions.kt`。
- 路由注册：目标功能域 `*Routes.kt`、`*Navigator.kt`、`feature/<domain>/navigation/*Graph.kt` 和 `AppNavHost.kt` 中的聚合入口。
- 参数：`core/navigation/demo/DemoRoutes.kt`、`feature/demo/view/NavigationWithArgsScreen.kt`、`NavigationWithArgsViewModel.kt`。
- 结果：`NavigationResultKey.kt`、`DemoResultKey.kt`、`NavigationResultViewModel.kt` 和结果页面。
- 登录保护：`RouteInterceptor.kt`、`UserState.kt` 与受保护路由的 Navigator。
- 页面模板对应的实际实现：目标功能域的 `view/*Screen.kt`、`viewmodel/*ViewModel.kt`、`navigation/*Graph.kt`，以及对应的 `*Routes.kt` 和 `*Navigator.kt`。

不能只看路由名称猜测行为；必须核对导航方法最终生成的命令、回退栈处理、结果流 replay/buffer 和宿主绑定时机。

## 页面模板与导航接入

创建导航页面时，页面模板和导航专题分别解决不同问题，不能只读取其中一类资料：

- `业务功能/templates.md` 规定 Screen、ViewModel、Routes、Graph 和模块级 Navigator 的生成内容、变量和文件边界，是模板的唯一依据。
- `业务功能/create-page.md` 规定从定义 Route 到来源页面发起跳转的完整接入顺序，适用于新增页面的端到端实现。
- `业务功能/view.md` 和 `业务功能/viewmodel.md` 规定 Route → Screen → Content 分层、状态收集、事件回调和 ViewModel 职责；导航接入不能为了跳转而省略页面层级。

新增页面必须核对完整链路：

```text
模块级 Navigator（跳转封装） → Routes（路由契约） → Graph（入口注册）
                                              ↓
                                    Route → Screen → Content
                                      ↘ ViewModel
```

- 普通页面保留 `Route`、`Screen` 和 `Content`；`Screen` 负责页面框架及 Loading、Empty、Error 等缺省状态，`Content` 只负责成功后的业务布局。`MainScreen` 是不使用 `Scaffold` 的特殊页面，但仍保留页面分层。
- `Graph` 只把 `NavKey` 映射到 `Route`，不能承担请求、状态或页面布局。
- `Navigator` 集中构造路由、参数和结果回传方法；ViewModel 调用模块级 Navigator，View 通过回调转发用户事件。
- 普通返回由页面模板中的 `navigateBack()` 完成，不为返回动作额外增加 ViewModel 包装方法。

模板文件的生成边界必须先确认：

- 新建功能域时，按 `templates.md` 生成一次 Routes、Graph 和模块 Navigator，再为每个页面生成 Screen 和 ViewModel。
- 已有功能域增加页面时，只在现有 Routes、Graph 和 Navigator 中追加声明、`entry` 和跳转方法；不得重复生成、覆盖或创建同职责文件。
- 只修改已有导航契约时，不要套用完整页面模板；只创建页面而没有 Graph、Navigator 或来源页面接入时，也不能视为完成导航。

## 固定规则

- 路由契约与模块 Navigator 放在 `core/navigation/<domain>/`，页面映射放在 Feature Graph，Graph 再统一聚合到宿主。
- Graph 只把 `NavKey` 映射到 Route，不请求数据、不创建业务状态。
- ViewModel 发起业务导航，跨 Feature 调用目标模块 Navigator；普通返回由 Screen 直接调用 `navigateBack()`。
- 参数进入可序列化 `NavKey`，运行时参数按当前 Assisted Factory 链路进入 ViewModel；Screen 不读取回退栈。
- 结果使用类型安全 `NavigationResultKey<T>`，发送前必须建立接收；长期共享数据不使用导航结果承载。
- 登录保护集中在 `RouteInterceptor`，不在每个页面复制登录判断。

## 工作流

1. 如果任务包含创建页面，先读取 `业务功能/templates.md` 和 `业务功能/create-page.md`；如果只修改导航契约，则按任务读取对应导航专题。
2. 检查目标功能域现有 Routes、Navigator、Graph、`appEntryProvider` 和测试，保持命名与注册方式一致。
3. 在 `core/navigation/<domain>/` 声明实现 `NavKey` 的 `@Serializable` 路由和模块 Navigator；在 `feature/<domain>/navigation/` 只注册 Graph。
4. 将新 Graph 汇总到 `appEntryProvider`，确保一个路由只有一个明确页面入口。
5. 创建页面时按模板补齐 Route、Screen、Content 和 ViewModel，再让 Feature ViewModel 调用模块 Navigator；跨 Feature 调用目标功能域 Navigator，不直接 import 目标页面。
6. 将普通参数声明为 NavKey 字段。需要运行时参数创建 ViewModel 时使用当前项目的 Hilt Assisted Factory 链路，不在 Screen 读取回退栈。
7. 将返回数据建模为 `NavigationResultKey<T>`，发送方通过结果 API 回退，接收方在结果发送前订阅 `resultEvents`。
8. 将登录保护集中到 `RouteInterceptor`，从 `UserState` 读取认证状态，不在每个 ViewModel 重复判断。
9. 只返回上一页时直接调用 `navigateBack()`；需要清栈时明确区分回退到已有页面和清理后追加页面。

## 关键约束

- Graph 只完成 `NavKey` 到 Route 的映射，不发起请求或修改业务状态。
- Navigator 集中管理路由类型、参数和结果，View 不散落具体导航构造逻辑。
- 路由参数只保存页面恢复所需的小型可序列化数据；详情数据通过目标页 Repository 重新读取。
- 商品 ID、用户 ID 等必填标识不提供 `0`、空字符串或其他无效默认值，让漏传参数在编译期暴露。
- 结果流不承担长期共享或持久化职责；跨页面长期数据进入 Repository、Room、本地存储或应用级状态。

## 验证

检查路由注册唯一、参数可序列化、Assisted Factory 创建正确、结果订阅时机、登录与未登录分支及回退栈顺序。全局 Navigator 依赖 `NavigationService` 完成宿主绑定；单元测试优先验证路由构造和事件参数，真实跳转在宿主绑定后的集成或 UI 测试中验证。运行导航相关测试、受影响页面测试、`./gradlew :app:assembleDebug` 与 `git diff --check`。
