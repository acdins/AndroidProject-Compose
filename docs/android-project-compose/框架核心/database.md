# Room 数据库

## 介绍

项目的 Room 模块当前提供一张 `demo_items` 示例表，用于演示 `Entity → DAO → DataSource → Repository → ViewModel → Compose` 的完整链路。数据库实例由 Hilt 单例提供，当前 `AppDatabase` 版本为 `2`，未配置 `fallbackToDestructiveMigration()`；修改 schema 时必须同步设计迁移。

本页建立在[数据层](./data.md)之上：Room 只负责持久化，页面仍然通过 `DemoRepository` 读取 `Flow` 或调用挂起 CRUD。开始前无需直接了解 SQL，但需要知道 ViewModel 不应持有 DAO。

## 模块结构

以下目录展示 Room Demo 从实体到 Hilt 模块的真实位置。

```text
core/database/
├── AppDatabase.kt
├── entity/DemoEntity.kt
├── dao/DemoDao.kt
├── datasource/demo/DemoDataSource.kt
└── di/DatabaseModule.kt
```

| 类型 | 真实职责 |
| --- | --- |
| `DemoEntity` | 映射 `demo_items` 表 |
| `DemoDao` | 声明插入、更新、删除与查询 SQL |
| `DemoDataSource` | 封装 DAO，更新时间字段由此层维护 |
| `DemoRepository` | 向 Feature 暴露 Flow 与挂起 CRUD |
| `DatabaseModule` | 提供 `AppDatabase` 和 `DemoDao` 单例 |

一次新增记录后的实际变化是：

```text
DatabaseViewModel.addItem()
→ DemoRepository.createItem()
→ DemoDataSource.createItem()
→ DemoDao.insertItem()
→ Room 写入 demo_items
→ DemoDao.getAllItems() 的 Flow 发出新列表
→ items StateFlow 更新
→ DatabaseScreen 重新组合
```

## 当前数据库定义

文件位置：`core/database/AppDatabase.kt`。

```kotlin
package com.joker.kit.core.database

import androidx.room.Database
import androidx.room.RoomDatabase
import com.joker.kit.core.database.dao.DemoDao
import com.joker.kit.core.database.entity.DemoEntity

/**
 * 应用数据库
 */
@Database(
    entities = [DemoEntity::class],
    version = 2,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {

    /**
     * 获取 Demo DAO
     *
     * @return Demo 表 DAO
     */
    abstract fun demoDao(): DemoDao

    companion object {
        const val DATABASE_NAME = "app-database"
    }
}
```

`DatabaseModule.provideDatabase()` 使用 `Room.databaseBuilder(context, AppDatabase::class.java, AppDatabase.DATABASE_NAME).build()` 创建实例，未添加 destructive migration 或自定义 `Migration`。

## DAO 与 DataSource

文件位置：`core/database/dao/DemoDao.kt` 与 `.../datasource/demo/DemoDataSource.kt`。

```kotlin
package com.joker.kit.core.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.joker.kit.core.database.entity.DemoEntity
import kotlinx.coroutines.flow.Flow

/**
 * Demo 表 DAO
 */
@Dao
interface DemoDao {

    /**
     * 插入一条记录
     *
     * @param item Demo 实体
     * @return 插入后的行 ID
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertItem(item: DemoEntity): Long

    /**
     * 观察全部记录
     *
     * @return 按更新时间倒序的记录流
     */
    @Query("SELECT * FROM demo_items ORDER BY updatedAt DESC")
    fun getAllItems(): Flow<List<DemoEntity>>
}
```

`DemoDataSource` 位于 DAO 的下一层。它负责把标题和描述组装成实体，并在更新时统一刷新 `updatedAt`；DAO 不知道这些业务字段如何生成。

```kotlin
package com.joker.kit.core.database.datasource.demo

import com.joker.kit.core.database.dao.DemoDao
import com.joker.kit.core.database.entity.DemoEntity
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Demo 数据源
 *
 * @param demoDao Demo DAO
 */
@Singleton
class DemoDataSource @Inject constructor(
    private val demoDao: DemoDao
) {

    /**
     * 新增记录
     *
     * @param title 标题
     * @param description 描述
     * @return 插入后的行 ID
     */
    suspend fun createItem(title: String, description: String = ""): Long {
        return demoDao.insertItem(
            DemoEntity(title = title, description = description)
        )
    }

    /**
     * 观察记录
     *
     * @return Demo 列表流
     */
    fun observeItems(): Flow<List<DemoEntity>> = demoDao.getAllItems()
}
```

当前 `DemoDataSource.updateItem()` 会复制实体并写入 `System.currentTimeMillis()`，`deleteItem()`、`clearAll()` 和 `getItem()` 分别代理 DAO 对应方法。

## 在 ViewModel 中观察 Room

`DemoRepository` 负责把 `DemoDataSource.observeItems()` 切换到 `Dispatchers.IO`，并向 Feature 暴露 `Flow<List<DemoEntity>>` 与挂起 CRUD。Repository 的完整写法只在[数据层](./data.md#数据库仓库示例)维护；本节聚焦 Room Flow 到页面状态的连接方式。

文件位置：`feature/demo/viewmodel/DatabaseViewModel.kt`。

```kotlin
package com.joker.kit.feature.demo.viewmodel

import androidx.lifecycle.viewModelScope
import com.joker.kit.core.base.viewmodel.BaseViewModel
import com.joker.kit.core.data.repository.DemoRepository
import com.joker.kit.core.database.entity.DemoEntity
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * 数据库示例页 ViewModel
 *
 * @param demoRepository Demo 仓库
 */
@HiltViewModel
class DatabaseViewModel @Inject constructor(
    private val demoRepository: DemoRepository
) : BaseViewModel() {

    /** Demo 列表状态 */
    val items: StateFlow<List<DemoEntity>> = demoRepository
        .observeItems()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = emptyList()
        )

    /**
     * 删除记录
     *
     * @param id 记录 ID
     */
    fun deleteItem(id: Long) {
        viewModelScope.launch {
            // 数据库写入在 ViewModel 协程中调用挂起仓库方法
            demoRepository.deleteItem(id)
        }
    }
}
```

View 通过 `collectAsState()` 订阅 `items`；项目内完整增删页面为 `DatabaseScreen.kt`。

`stateIn(..., SharingStarted.WhileSubscribed(5_000), emptyList())` 的含义是：页面有订阅者时收集 Room Flow；所有订阅者离开后继续保留 5 秒，再停止上游收集；重新进入页面时从数据库重新获得最新列表。首次还没有记录时，UI 先看到 `emptyList()`，这不是数据库错误，而是明确的空数据状态。

## 新增表的步骤

1. 在 `core/database/entity` 新增 `@Entity`，明确主键和列类型。
2. 在 `core/database/dao` 新增 `@Dao`，为查询方法补充返回值和挂起标记。
3. 在 `AppDatabase.entities` 追加实体，并提升 `version`。
4. 在 `DatabaseModule` 提供新的 DAO；需要复杂业务时再封装 DataSource 与 Repository。
5. 为旧版本编写 `Migration` 并在 `Room.databaseBuilder` 中注册，使用内存数据库测试迁移结果。

::: warning 迁移边界
当前源码没有提供任何自定义 `Migration`。开发阶段直接提升版本会在已有数据库上触发 Room 的迁移错误；不能把 `fallbackToDestructiveMigration()` 当作生产方案。
:::

## 下一步

继续阅读[本地存储](./datastore.md)，对比结构化表数据与小型 JSON/KV 数据的使用边界；需要跨页面共享用户资料时，再阅读[全局状态](./state.md)。

## 官方文档

- [Room 持久化库概览](https://developer.android.com/training/data-storage/room)
- [Room 迁移](https://developer.android.com/training/data-storage/room/migrating-db-versions)
- [Room 与 Kotlin Flow](https://developer.android.com/training/data-storage/room/async-queries)
