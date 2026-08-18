# ViewModel 基类

## 介绍

`BaseViewModel` 是项目内业务 ViewModel 的统一继承入口，位于 Compose 页面与 Repository 之间。页面把用户操作交给 ViewModel，ViewModel 调用 Repository 并更新可观察状态；`BaseViewModel` 本身只继承 AndroidX `ViewModel`，不持有导航、用户状态或业务数据。

在继续阅读前，建议先了解 [Core 概览](./index.md)中的分层关系。本页只解释三种 ViewModel 基类如何选择；单对象请求和分页列表的完整状态变化会在后续章节分别展开。

## 模块结构

以下目录展示三个基类及其配套状态文件的位置。

```text
core/base/
├── state/
│   ├── BaseNetWorkUiState.kt
│   └── BaseNetWorkListUiState.kt
└── viewmodel/
    ├── BaseViewModel.kt
    ├── BaseNetWorkViewModel.kt
    └── BaseNetWorkListViewModel.kt
```

## 继承关系

| 类型 | 负责范围 | 适用页面 |
| --- | --- | --- |
| `BaseViewModel` | 统一继承 AndroidX `ViewModel` | 自定义状态或纯本地业务 |
| `BaseNetWorkViewModel<T>` | 非分页请求的加载、成功、失败状态 | 详情页、单次请求页 |
| `BaseNetWorkListViewModel<T>` | 首屏、空数据、刷新和加载更多 | 分页列表页 |

三种基类不是按 Feature 名称选择，而是按页面的数据形态选择：

```text
页面是否需要统一网络状态？
├── 否 → BaseViewModel
└── 是
    ├── 单个对象或一次性结果 → BaseNetWorkViewModel<T>
    └── 可分页的数据集合 → BaseNetWorkListViewModel<T>
```

::: warning 依赖注入边界
`BaseViewModel` 的定义是 `abstract class BaseViewModel : ViewModel()`，不接收导航器或全局状态。需要导航或全局状态时，应在具体 ViewModel 中显式注入对应依赖。
:::

## 基础定义

文件位置：`core/base/viewmodel/BaseViewModel.kt`。该文件的职责只有统一继承 AndroidX `ViewModel`，没有隐藏的构造参数或状态逻辑。

```kotlin
package com.joker.kit.core.base.viewmodel

import androidx.lifecycle.ViewModel

/**
 * 基础 ViewModel
 */
abstract class BaseViewModel : ViewModel()
```

具体页面仍需声明 `@HiltViewModel`，通过构造函数注入自己的 Repository 或共享状态，并自行暴露只读 `StateFlow`。这些规则属于 [ViewModel 规范](../业务功能/viewmodel.md)；数据库、网络和导航的调用过程不在本页重复展开。

## 选择基类

1. 页面状态完全由业务定义时，继承 `BaseViewModel`。
2. 请求返回单个对象且需要统一三态 UI 时，继承 `BaseNetWorkViewModel<T>`。
3. 请求返回 `NetworkPageData<T>` 且需要刷新、加载更多时，继承 `BaseNetWorkListViewModel<T>`。
4. 业务页面跳转优先由具体 ViewModel 调用模块 Navigator；页面顶部栏直接调用 `navigateBack()`，不要为了普通返回向 ViewModel 或 `BaseViewModel` 增加包装方法。

::: tip 初学者常见选择
登录提交、详情加载等“一次请求得到一个结果”的页面使用 `BaseNetWorkViewModel<T>`。商品瀑布流、消息列表等需要刷新和加载下一页的页面使用 `BaseNetWorkListViewModel<T>`。表单编辑、本地计数器等状态完全由业务定义的页面直接使用 `BaseViewModel`。
:::

## 如何扩展

只有大多数业务 ViewModel 都需要的能力才应下沉到基类。例如统一日志或生命周期埋点可以放入 `BaseViewModel`；商品筛选条件、登录输入框状态仍应留在对应 Feature。

新增公共能力时按以下顺序检查：

1. 确认它与具体页面和具体 Repository 无关。
2. 确认普通、非分页网络、分页三类 ViewModel 都需要它。
3. 在基类中提供最小接口，不保存 Activity、Composable 或 `NavController` 引用。
4. 至少用一个现有 Feature 验证生命周期和异常路径。

## 注意事项

- `BaseViewModel` 是抽象类，具体 ViewModel 仍需使用 `@HiltViewModel` 和 `@Inject constructor` 接入 Hilt。
- 公共能力只有在多数业务 ViewModel 都需要时才适合下沉到基类；页面专属状态应保留在 Feature 内。
- `viewModelScope` 适合页面级异步任务；需要跨页面持续执行的任务不应依赖 ViewModel 生命周期。

## 下一步

先阅读 [数据模型](./model.md)和[请求结果处理](./result.md)，理解网络响应怎样转换成页面状态；随后再进入[非分页网络基类](./network-base.md)与[分页列表](./pagination.md)查看两类完整页面。

## 官方文档

- [ViewModel 概览](https://developer.android.com/topic/libraries/architecture/viewmodel)
- [在 ViewModel 中使用 Kotlin 协程](https://developer.android.com/topic/libraries/architecture/coroutines#viewmodelscope)
- [Hilt 与 Jetpack 集成](https://developer.android.com/training/dependency-injection/hilt-jetpack)
