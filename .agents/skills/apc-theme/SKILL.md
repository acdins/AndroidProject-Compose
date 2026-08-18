---
name: apc-theme
description: 实现和维护 AndroidProject-Compose 的 Material 3 主题与设计令牌，覆盖浅色、深色、动态颜色、Color、Typography、Shape、Size、主题预览和规范图同步。用户要求修改主题、颜色、字体、圆角、间距、深浅色、动态配色或 AppTheme 时使用。
---

# APC Theme

文中的 `core/...` 与 `feature/...` 均为不绑定包名的逻辑路径；在当前 Android 源码集下定位对应文件，不把现有 package 前缀写成固定目录。

## 必读资料

读取 `AGENTS.md` 获取仓库级限制，但不得用它代替本 Skill 的主题规则。依次读取：

- `docs/android-project-compose/框架核心/theme.md`
- `docs/android-project-compose/框架核心/designsystem.md`
- `docs/android-project-compose/框架核心/ui.md`
- `core/designsystem/theme/` 中的真实声明

修改视觉规范时同时检查 `docs/images/theme/` 中的色彩、文字、按钮和布局规范图。

## 必读源码

修改主题、颜色、文字、圆角或间距前，必须直接读取对应源码，不能只看文档表格：

- `core/designsystem/theme/Color.kt`：全部浅色、深色、状态色、背景、文本、边框、遮罩、按压、阴影和渐变值。
- `core/designsystem/theme/Theme.kt`：`LightColorScheme`、`DarkColorScheme`、动态颜色条件和 `MaterialTheme` 映射。
- `core/designsystem/theme/Type.kt`：所有 Typography 的字号、字重、行高和使用层级。
- `core/designsystem/theme/Shape.kt`：Radius、Shape 和 `AppShapes` 映射。
- `core/designsystem/theme/Size.kt`：垂直、水平、padding、分割线和指示器尺寸。
- `core/designsystem/component/Spacer.kt` 及目标布局封装：确认令牌实际怎样被组件消费。
- `core/ui/component/text/Text.kt` 及受影响公共组件：确认主题变化在语义组件中的实际入口。

## 固定规则

- Feature 页面优先读取 `MaterialTheme.colorScheme`、`MaterialTheme.typography` 和 `MaterialTheme.shapes`，不直接绑定某个浅色或深色常量。
- 特殊状态色、间距或形状只能使用设计系统中已有语义；不存在时先在正确主题文件增加语义，再接入页面。
- 修改颜色时同时检查浅色、深色和 `ColorScheme` 映射；修改文字、圆角或间距时同时检查使用方和封装组件。
- 不在 Feature 中用 `Color(...)`、`.sp`、`.dp` 或 `RoundedCornerShape(...)` 创建第二套局部主题规范。
- 规范图、主题文档和源码表达同一套最终规则；任一方变化时同步核对另外两方。

## 工作流

1. 从设计语义确定修改属于颜色、Typography、Shape、Size 还是 Material `ColorScheme` 映射，不用页面名称命名通用令牌。
2. 在 `Color.kt`、`Type.kt`、`Shape.kt` 或 `Size.kt` 修改令牌；需要成为 Material 语义时同步接入 `Theme.kt`。
3. 同时检查浅色与深色方案，保证文本、背景、状态色、边框和禁用态具有足够对比度。
4. 保持 `AppTheme` 默认跟随系统明暗模式；动态颜色默认关闭，启用时保留 API 31 以下的自定义方案回退。
5. 页面优先读取 `MaterialTheme` 和语义尺寸，不直接引用固定浅色令牌，不为深色模式复制两套业务 Composable。
6. 多个页面重复的业务语义组件进入 `core/ui`；纯布局与视觉默认值进入 `core/designsystem`。
7. 主题事实变化时同步更新项目内主题文档、规范说明和在线文档来源，不让图片、表格与代码互相矛盾。

## 验证

检查浅色、深色、跟随系统、API 31+ 动态颜色和旧版本回退。验证公共组件、至少一个 Feature 页面、按钮正常/按压/禁用/错误状态及相关 Preview。运行受影响测试、`./gradlew :app:assembleDebug` 与 `git diff --check`。
