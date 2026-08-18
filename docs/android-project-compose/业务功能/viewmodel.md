# ViewModel 规范

## 介绍

Feature ViewModel 负责持有页面状态、调用 Repository、处理业务副作用并向 Route 暴露只读状态流。View 把业务操作以回调形式传到 Route，Route 再绑定 ViewModel 方法；业务校验、状态更新、数据请求、页面跳转和结果回传在 ViewModel 内完成。页面顶部栏的普通返回操作由 Screen 直接调用 `navigateBack()`，不需要 ViewModel 包装。ViewModel 继承 `BaseViewModel`、`BaseNetWorkViewModel<T>` 或 `BaseNetWorkListViewModel<T>`；ViewModel 不直接绘制 UI，Content 也不直接读取 ViewModel。

把状态分成两类更容易决定放置位置：网络基类提供的 Loading/Success/Error 或分页状态属于通用请求状态；搜索关键词、弹窗开关、选中 ID 等只属于当前页面的状态，应由具体 ViewModel 持有。

## 基类选择

| 页面场景 | 基类 | 必须实现或调用 |
| --- | --- | --- |
| 纯 UI、导航或本地状态 | `BaseViewModel` | 自行管理 `StateFlow` 和副作用 |
| 单次网络请求 | `BaseNetWorkViewModel<T>` | 实现 `requestApiFlow()`，在 `init` 中调用 `executeRequest()` |
| 分页列表 | `BaseNetWorkListViewModel<T>` | 实现 `requestListData()`，在 `init` 中调用 `initLoad()` |

这些基类都继承 `BaseViewModel : ViewModel`，不会自动注入导航器或用户状态；需要时在具体 ViewModel 构造函数中显式注入。`MainActivityViewModel` 是预留类，当前 `MainActivity` 未引用它，不应作为页面状态方案使用。

## 注释规范

ViewModel 中的声明必须说明业务含义，不能只依赖变量名推断用途：

| 声明位置 | 注释要求 |
| --- | --- |
| 构造参数 | 在类 KDoc 中逐项使用 `@param` 说明注入对象或路由参数 |
| 成员变量 | 每个 `val`、`var`、`MutableStateFlow` 和只读 `StateFlow` 都添加 KDoc；可变源与只读状态分别说明 |
| 重写属性 | 在属性上方使用 KDoc 说明覆盖后的业务值和影响，例如页大小或最短加载时间 |
| 方法参数 | 在方法 KDoc 中逐项使用 `@param` 说明业务含义 |
| 方法内部变量 | 在声明上方使用中文行内注释，说明数据来源、转换结果或用途 |

注释描述代码的最终职责，例如“商品列表每页请求数量”和“去除首尾空格后的标题”。不要写“调整后的页大小”“临时变量”或其他依赖修改过程才能理解的内容。

## StateFlow 封装

公开只读 `StateFlow`，将可变源保留在 ViewModel 内部：

文件位置：任意 `feature/<domain>/viewmodel/<PageName>ViewModel.kt` 的状态声明区域。

```kotlin
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/** 搜索关键词可变状态源 */
private val _query = MutableStateFlow("")

/** 对外暴露的搜索关键词状态 */
val query: StateFlow<String> = _query.asStateFlow()

/**
 * 更新搜索关键词
 *
 * @param value 输入内容
 */
fun onQueryChange(value: String) {
    _query.value = value
}
```

命名使用完整业务语义，例如 `isRefreshing`、`hasMoreData`、`demoResult`。不要把 `MutableStateFlow` 直接暴露给 Route。私有可变源与公开只读状态是两个不同声明，必须分别添加注释。

## 用户事件入口

ViewModel 为页面业务操作提供具有业务语义的方法。View 只决定何时触发回调，不在 `onClick`、`onValueChange` 或其他 UI lambda 中直接更新 ViewModel 状态、调用 Repository 或发起业务页面跳转。顶部栏调用 `navigateBack()` 是统一页面返回行为，不需要转发给 ViewModel。

以当前 `StateManagementViewModel` 为例，页面只把递增、递减和重置操作绑定到下面的方法：

文件位置：`feature/demo/viewmodel/StateManagementViewModel.kt`

```kotlin
package com.joker.kit.feature.demo.viewmodel

import com.joker.kit.core.base.viewmodel.BaseViewModel
import com.joker.kit.core.state.DemoCounterState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.StateFlow
import javax.inject.Inject

/**
 * 状态管理示例页 ViewModel
 *
 * @param counterState 计数器状态
 */
@HiltViewModel
class StateManagementViewModel @Inject constructor(
    private val counterState: DemoCounterState,
) : BaseViewModel() {

    /** 对外暴露的计数器状态。 */
    val count: StateFlow<Int> = counterState.count

    /** 增加计数。 */
    fun increase() = counterState.increase()

    /** 减少计数。 */
    fun decrease() = counterState.decrease()

    /** 重置计数。 */
    fun reset() = counterState.reset()
}
```

Route 使用 `onIncrease = viewModel::increase`、`onDecrease = viewModel::decrease` 和 `onReset = viewModel::reset` 直接绑定。如果 ViewModel 方法需要额外参数，Route 可以做简单参数适配，但不在适配 lambda 中实现业务分支。

## 网络请求 ViewModel

`BaseNetWorkViewModel<T>` 将请求结果转换为 `BaseNetWorkUiState<T>`。具体 ViewModel 只负责注入 Repository、实现 `requestApiFlow()`，并决定何时调用 `executeRequest()`：

```kotlin
/**
 * 提供单对象页面的请求流
 *
 * @return 商品详情响应流
 */
override fun requestApiFlow(): Flow<NetworkResponse<Goods>> {
    return goodsRepository.getGoodsInfo("1")
}
```

失败重试使用继承自基类的 `retryRequest()`；不要在 Feature 层重复维护与 `BaseNetWorkUiState` 相同的状态机。完整 `NetworkDemoViewModel`、状态变化和页面重试写法统一见[非分页网络基类](../框架核心/network-base.md)。

`NetworkRequestViewModel` 是“点击按钮后才请求”的轻量 Demo，它直接把成功数据写入 `goods`，没有完整的 Loading/Error 流。真实详情页或列表页应优先使用网络基类，否则必须自行补齐请求中、业务失败、异常和重复点击状态。

## 分页列表 ViewModel

`BaseNetWorkListViewModel<T>` 维护 `listData`、`isRefreshing`、`isLoadingMore`、`hasMoreData` 和 `uiState`。具体 ViewModel 只声明分页参数和请求流；不能在 Feature 中自行递增、回退 `currentPage`：

```kotlin
/** 商品列表每页请求数量 */
override val pageSize: Int get() = 15

/**
 * 提供当前页的分页请求流
 *
 * @return 商品分页响应流
 */
override fun requestListData(): Flow<NetworkResponse<NetworkPageData<Goods>>> {
    return goodsRepository.getGoodsPage(
        GoodsSearchRequest(page = currentPage, size = pageSize),
    )
}
```

首屏只调用一次 `initLoad()`。列表页的 Route 将 `onRefresh`、`onLoadMore` 和 `retryRequest` 传给状态容器；完整 `NetworkListDemoViewModel`、页码规则与四态页面统一见[分页列表](../框架核心/pagination.md)。

## 路由参数与 Assisted Inject

无参数页面可直接使用 `hiltViewModel()`。Navigation 3 的带参页面由 Graph 把 `navKey` 传给 Route；当前 `NavigationWithArgsViewModel` 使用 Assisted Factory：

```kotlin
package com.joker.kit.feature.demo.viewmodel

import com.joker.kit.core.base.viewmodel.BaseViewModel
import com.joker.kit.core.navigation.demo.DemoRoutes
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import dagger.hilt.android.lifecycle.HiltViewModel

/**
 * 带参跳转页面 ViewModel
 *
 * @param navKey 页面路由参数
 */
@HiltViewModel(assistedFactory = NavigationWithArgsViewModel.Factory::class)
class NavigationWithArgsViewModel @AssistedInject constructor(
    @Assisted private val navKey: DemoRoutes.NavigationWithArgs,
) : BaseViewModel() {

    /** 当前页面的商品 ID。 */
    val goodsId: Long = navKey.goodsId

    /**
     * Navigation 3 页面 ViewModel 工厂
     */
    @AssistedFactory
    interface Factory {
        /**
         * 创建页面 ViewModel
         *
         * @param navKey 页面路由参数
         * @return ViewModel 实例
         */
        fun create(navKey: DemoRoutes.NavigationWithArgs): NavigationWithArgsViewModel
    }
}
```

Route 中使用 `hiltViewModel<NavigationWithArgsViewModel, NavigationWithArgsViewModel.Factory> { factory -> factory.create(navKey) }`。带参页面通过 `navKey + AssistedInject` 创建 ViewModel，不使用 `SavedStateHandle.toRoute()`。

## 本地数据与共享状态

本地数据库示例由 `DatabaseViewModel` 注入 `DemoRepository`，把 `observeItems()` 转为 `stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())`；页面输入则用私有 `MutableStateFlow`。跨页面共享状态由 `UserState` 或 `DemoCounterState` 提供，ViewModel 只暴露其只读流，例如 `val isLoggedIn: StateFlow<Boolean> = userState.isLoggedIn`。

## 导航与结果副作用

ViewModel 响应用户事件并发起业务导航副作用。打开业务页面时优先调用模块 Navigator，结果回传使用 `popBackStackWithResult(...)`；任何 ViewModel 都不直接修改 `NavBackStack`。页面顶部栏在 Screen 中直接调用 `navigateBack()`，只有登录成功关闭登录页等与业务流程绑定的返回动作才由 ViewModel 执行。接收方在 ViewModel 中收集 `resultEvents(key)`，并确保监听任务只注册一次。

本页只规定副作用的归属，不重复导航 API 和完整发送/接收代码。跳转、回退与栈清理见[导航流程](../导航/flow.md)，普通参数、结果回传与刷新信号见[参数传递与结果回传](../导航/result.md)。

## 官方文档

- [Android ViewModel](https://developer.android.com/topic/libraries/architecture/viewmodel)
- [StateFlow 与 SharedFlow](https://developer.android.com/kotlin/flow/stateflow-and-sharedflow)
- [Hilt ViewModel 注入](https://developer.android.com/training/dependency-injection/hilt-jetpack)
