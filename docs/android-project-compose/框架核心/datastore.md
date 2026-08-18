# 本地存储

## 介绍

`core/datastore` 用于组织键值配置、认证信息、用户资料等本地存储能力。Feature 不直接依赖具体存储库，而是通过 Store Repository 和 Store DataSource 读写业务数据；底层可以根据项目需要使用 MMKV、Jetpack DataStore、SharedPreferences 或其他实现。

当前 AndroidProject-Compose 使用 `MMKVUtils + kotlinx.serialization`，所以本页后续 API、初始化和失败行为都以当前 MMKV 实现为准。这是脚手架的默认技术选择，不是 `core/datastore` 的永久限制。替换存储库时，应保留上层业务接口，并重新核对数据迁移、并发、加密和异常恢复策略。

## 当前实现

当前项目使用 MMKV 保存认证信息和用户资料，Repository 对上层隐藏 MMKV key、JSON 序列化和容错细节。如果业务需要 `Flow` 形式的键值观察、事务或不同的跨进程一致性，应先评估存储方案，不能把当前实现当成 Jetpack DataStore 使用。

## 模块结构

以下目录展示当前本地存储领域数据源和 MMKV 底层工具类的位置。

```text
core/datastore/
├── datasource/auth/
│   ├── AuthStoreDataSource.kt
│   └── AuthStoreDataSourceImpl.kt
├── datasource/userinfo/
│   ├── UserInfoStoreDataSource.kt
│   └── UserInfoStoreDataSourceImpl.kt
└── di/DataStoreModule.kt
core/util/storage/MMKVUtils.kt
```

| 层 | 真实职责 |
| --- | --- |
| `MMKVUtils` | 初始化 MMKV，提供基础键值和序列化对象读写 |
| Store DataSource | 管理领域 key、JSON 编解码和默认值 |
| `DataStoreModule` | 通过 Hilt `@Binds` 绑定接口实现 |
| Store Repository | 向 ViewModel 提供语义化挂起方法 |

## 初始化顺序

`MMKVUtils` 必须在任何 DataSource 读写前初始化。当前 `Application.onCreate()` 的顺序是：

```kotlin
override fun onCreate() {
    super.onCreate()
    initMMKV()
    // UserState 依赖认证和用户信息存储
    userState.initialize()
}

private fun initMMKV() {
    MMKVUtils.init(this)
}
```

如果跳过 `MMKVUtils.init(application)`，首次访问默认实例会抛出 `IllegalStateException`。

当前真实入口是 `Application.kt`：`initMMKV()` 完成初始化后，才调用 `userState.initialize()`。Hilt 会先构造依赖对象，但数据源真正读写发生在初始化之后的协程中。

## 认证数据源

`AuthStoreDataSourceImpl` 使用 key `auth_info` 保存 `Auth` 的 JSON 字符串：

| 方法 | 行为 |
| --- | --- |
| `saveAuth(auth)` | `Json.encodeToString(auth)` 后写入 MMKV |
| `getAuth()` | 读取并解析，空值或解析异常返回 `null` |
| `getToken()` | 返回 `getAuth()?.token` |
| `clearAuth()` | 删除 `auth_info` |
| `isLoggedIn()` | Token 非空且 `Auth.isExpired()` 为 `false` |

`AuthStoreRepository.shouldRefreshToken()` 进一步调用 `Auth.shouldRefresh()`，用于判断访问令牌是否进入提前 15 分钟的刷新窗口。

## 用户信息数据源

`UserInfoStoreDataSourceImpl` 使用 key `user_info` 保存 `User` JSON：

| 方法 | 行为 |
| --- | --- |
| `saveUserInfo(user)` | 序列化完整用户信息 |
| `getUserInfo()` | 解析完整用户信息，异常返回 `null` |
| `updateUserInfo(updates)` | 解析 JSON 后仅更新 String、Number、Boolean 字段；`null` 值删除字段 |
| `clearUserInfo()` | 删除 `user_info` |
| `getUserId()` / `getNickName()` / `getAvatarUrl()` | 从当前用户对象读取便捷字段 |

局部更新失败时实现会保留原始数据，不向上层抛出解析异常；需要知道写入是否成功时，应调用完整的 `saveUserInfo` 并在业务层记录结果。

## ViewModel 使用示例

文件位置：`feature/demo/viewmodel/LocalStorageViewModel.kt`。

```kotlin
package com.joker.kit.feature.demo.viewmodel

import androidx.lifecycle.viewModelScope
import com.joker.kit.core.base.viewmodel.BaseViewModel
import com.joker.kit.core.data.repository.UserInfoStoreRepository
import com.joker.kit.core.model.entity.User
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * 本地存储示例 ViewModel
 *
 * @param userInfoStoreRepository 用户资料仓库
 */
@HiltViewModel
class LocalStorageViewModel @Inject constructor(
    private val userInfoStoreRepository: UserInfoStoreRepository
) : BaseViewModel() {

    /** 本地用户信息可变状态源 */
    private val _user = MutableStateFlow<User?>(null)

    /** 对外暴露的本地用户信息状态 */
    val user: StateFlow<User?> = _user.asStateFlow()

    init {
        loadUser()
    }

    /**
     * 从仓库读取用户资料
     */
    fun loadUser() {
        viewModelScope.launch {
            // ViewModel 不感知 MMKV key 和 JSON 格式
            _user.value = userInfoStoreRepository.getUserInfo()
        }
    }

    /**
     * 清除本地用户资料
     */
    fun clearUser() {
        viewModelScope.launch {
            userInfoStoreRepository.clearUserInfo()
            _user.value = null
        }
    }
}
```

当前 Demo 还维护用户 ID、昵称和头像输入流，并在 `saveUser()` 中构造 `User` 后调用 `saveUserInfo`；完整交互见 `LocalStorageScreen.kt`。

## 使用当前实现扩展本地领域

1. 在 `core/datastore/datasource/<domain>` 定义接口，只暴露业务字段和挂起方法。
2. 在实现类中为领域选择独立 key，使用 `Json` 编解码 `@Serializable` 模型。
3. 在 `DataStoreModule` 添加 `@Binds @Singleton` 绑定。
4. 新增对应 Repository，将 DataSource 细节转换为业务语义。
5. ViewModel 注入 Repository，并使用 `StateFlow` 暴露给 Compose。

不要把密码、完整 Token 或其他敏感值写入日志；MMKV 默认实例为单进程模式，需要多进程时显式调用 `getMultiProcessInstance`，并评估并发一致性。

## 替换存储实现

更换本地存储库时，优先保持 ViewModel 和 Repository 的调用语义不变：

1. 保留 `AuthStoreDataSource`、`UserInfoStoreDataSource` 等业务接口，避免 Feature 感知具体存储库。
2. 使用新存储库实现 DataSource，并在 Hilt 模块中替换接口绑定。
3. 根据新存储库调整初始化时机、线程模型、序列化方式和错误处理。
4. 已发布应用需要设计旧数据读取与迁移路径，不能直接更换 key 或删除历史数据。
5. 重新验证首次安装、版本升级、损坏数据、清除数据和多进程访问等场景。

如果新方案的能力模型与现有挂起接口差异较大，例如需要持续观察 `Flow`，应先调整 DataSource 接口，再由 Repository 向 ViewModel 暴露稳定的业务 API，不要在页面中同时保留两套存储调用方式。

## MMKVUtils 常用 API

| 方法 | 用途 |
| --- | --- |
| `init(application)` | 应用启动时初始化，返回 MMKV 根目录 |
| `getInstance(name, mode, cryptKey, rootDir)` | 获取命名实例，可选加密和多进程 |
| `putString` / `getString` | 默认实例字符串读写 |
| `putObject<T>` / `getObject<T>` | 使用 Kotlin Serialization 存取 `@Serializable` 对象 |
| `remove(key)` / `clearAll()` | 删除键或清空默认实例 |

对于认证和用户资料，优先调用对应 Store Repository，而不是在 Feature 中直接使用这些底层方法。直接使用 `MMKVUtils` 会绕过 key 命名、序列化和错误处理约定。

## 失败与恢复

| 场景 | 当前行为 | 业务处理 |
| --- | --- | --- |
| 应用启动前访问 MMKV | `MMKVUtils` 抛出 `IllegalStateException` | 确保 `Application.onCreate()` 先调用 `init` |
| key 不存在 | `getString` 返回默认值，领域读取方法通常返回 `null` | 在 ViewModel 显示首次使用或空态 |
| JSON 解析失败 | `getAuth()` / `getUserInfo()` 返回 `null` | 清除损坏数据并要求重新登录或重新保存 |
| `updateUserInfo` 写入异常 | 实现保留原始 JSON，不向上层抛出异常 | 需要强校验时使用完整 `saveUserInfo` 并自行记录结果 |

认证信息和用户信息使用独立 key（`auth_info`、`user_info`），清除其中一个不会自动清除另一个；登出流程由 `UserState.logout()` 同时清理两类仓库。

## 官方文档

- [MMKV 官方仓库](https://github.com/Tencent/MMKV)
- [Jetpack DataStore](https://developer.android.com/topic/libraries/architecture/datastore)
- [Kotlin Serialization](https://github.com/Kotlin/kotlinx.serialization)
- [Hilt 绑定接口实现](https://developer.android.com/training/dependency-injection/hilt-android#provide)
