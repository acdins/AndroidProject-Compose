# 设计系统

## 介绍

`core/designsystem` 是页面视觉和基础布局的统一入口。它把颜色、字体、圆角、间距、图标和常用 Compose 布局封装起来，让 Feature 页面可以表达“使用主色和大间距”，而不是每个页面都重新写十六进制颜色、`dp` 和对齐参数。

它解决的是视觉和布局的一致性，不负责业务状态、网络请求、页面导航或完整业务控件。需要处理 Loading、空数据、网络错误、列表项或 AppBar 语义时，应使用 `core/ui`。

## 模块内容

| 路径 | 作用 | 典型入口 |
| --- | --- | --- |
| `theme/Color.kt` | 品牌色、状态色、文本色、背景色、边框、遮罩和按压色 | `MaterialTheme.colorScheme`、`Primary` |
| `theme/Type.kt` | Material 3 `Typography` 的字号、字重、行高和字间距 | `MaterialTheme.typography.bodyLarge` |
| `theme/Shape.kt` | 4/8/12/16/24dp 圆角与 `AppShapes` | `ShapeSmall`、`ShapeExtraLarge` |
| `theme/Size.kt` | 4/8/12/16/24/32dp 间距、内边距、分割线和指示器 | `SpacePaddingMedium`、`SpaceVerticalLarge` |
| `theme/Icon.kt` | 项目图标资源和可复用图标函数 | `ArrowRightIcon` |
| `component/Box.kt` | 对齐、尺寸、圆角、边框和内边距组合 | `CenterBox`、`RoundedBox` |
| `component/Column.kt` | Column 对齐、列表、卡片内容和内边距组合 | `AppColumn`、`CardContentList` |
| `component/Row.kt` | Row 对齐、横向列表和内边距组合 | `AppRow`、`SpaceBetweenRow` |
| `component/LazyList.kt` | LazyColumn、LazyRow 和列表内边距 | `AppLazyColumn`、`AppLazyRow` |
| `component/Scroll.kt` | 垂直/水平滚动及带间距的滚动容器 | `VerticalScroll` |
| `component/Spacer.kt` | 4dp 到 32dp 的水平/垂直间距组件 | `SpaceVerticalSmall` |

## 组件怎么选

### 需要一个容器

- 要占满屏幕并居中内容：`FullScreenBox` 或 `CenterBox`。
- 要控制固定宽高：`FixedSizeBox`。
- 要统一圆角或边框：`RoundedBox` 或 `BorderBox`。
- 要让内容沿垂直方向排列：`AppColumn`；要在一行中排列：`AppRow`。

### 需要一个列表

- 普通纵向列表：`AppLazyColumn`。
- 普通横向列表：`AppLazyRow`。
- 需要统一列表内边距：`SmallPaddingLazyColumn`、`MediumPaddingLazyColumn` 或 `LargePaddingLazyColumn`。
- 只有少量静态内容：`VerticalList` 或 `HorizontalList`，不必为了几项文字引入 Lazy 容器。

### 需要间距

先根据设计规范选择语义尺寸，再调用对应组件：

| 语义 | 尺寸 | 组件示例 |
| --- | ---: | --- |
| 超小间距 | 4dp | `SpaceVerticalXSmall()` |
| 小间距 | 8dp | `SpaceVerticalSmall()` |
| 中间距 | 12dp | `SpaceVerticalMedium()` |
| 大间距 | 16dp | `SpaceVerticalLarge()` |
| 特大间距 | 24dp | `SpaceVerticalXLarge()` |
| 超大间距 | 32dp | `SpaceVerticalXXLarge()` |

需要给 `padding`、`Arrangement.spacedBy` 或 `Divider` 传值时，使用 `Size.kt` 中的 `SpacePadding*`、`SpaceHorizontal*` 和 `SpaceDivider`，不要把同一数值重新声明在页面中。

## 最小组合示例

文件位置：任意 Feature 的 `view/*Screen.kt`。下面的片段展示一个只依赖主题和布局封装的内容区；它没有 ViewModel，也没有业务导航。

```kotlin
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import com.joker.kit.core.designsystem.component.AppColumn
import com.joker.kit.core.designsystem.component.SpaceVerticalSmall

/** 展示设计系统间距和主题颜色的内容片段。 */
@Composable
private fun ProfileSummary() {
    AppColumn {
        Text(
            text = "个人资料",
            color = MaterialTheme.colorScheme.onBackground,
            style = MaterialTheme.typography.titleLarge,
        )
        // 通过语义化间距保持标题和正文的排版节奏。
        SpaceVerticalSmall()
        Text(
            text = "资料同步完成",
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            style = MaterialTheme.typography.bodyMedium,
        )
    }
}
```

预期结果是标题、正文颜色随 `MaterialTheme` 变化，标题与正文间距固定为 8dp。示例只组合设计令牌与基础布局，不引入页面容器、网络状态或导航能力。

## 与 `core/ui` 的边界

| 问题 | 应放置的位置 | 原因 |
| --- | --- | --- |
| `Box` 居中、`Row` 两端对齐、列表内边距 | `core/designsystem/component` | 只改变布局默认值，不理解业务状态 |
| 统一 AppBar、返回按钮、Snackbar | `core/ui/component/appbar` 或 `scaffold` | 具有页面语义和交互回调 |
| Loading、Error、Empty、分页刷新 | `core/ui/component/network` 或 `refresh` | 需要处理页面状态和异步交互 |
| 具体 Feature 的卡片、商品项、业务弹窗 | 对应 `feature/<domain>/component` | 只在业务域内复用，避免 Core 反向依赖业务 |

## 扩展设计系统

1. 先在规范图或设计评审中确定“主色、警告色、页面背景”等语义，不以页面名称命名颜色。
2. 颜色写入 `Color.kt`，字号写入 `Type.kt`，圆角写入 `Shape.kt`，间距写入 `Size.kt`。
3. 如果需要让 Material 3 组件读取颜色或形状，在 `Theme.kt` 的浅色和深色 `ColorScheme` / `Shapes` 中接入。
4. 只有同一布局组合在多个页面重复出现时，才新增 `component` 封装，并补充 KDoc 和 Preview。
5. 在浅色、深色和不同窗口宽度下验证，不把业务样式数字直接写回页面。

## 相关链接

- [主题系统](./theme.md)：查看四张规范图与代码映射。
- [UI 组件](./ui.md)：查看带页面状态和交互语义的公共组件。
- [Material 3 设计系统](https://m3.material.io/)
- [Compose Material 3 主题](https://developer.android.com/develop/ui/compose/designsystems/material3)
