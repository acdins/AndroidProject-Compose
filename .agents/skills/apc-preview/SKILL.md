---
name: apc-preview
description: 为 AndroidProject-Compose 页面和组件新增、修复或审查 Compose Preview，覆盖页面与组件注解、深浅色、多设备、PreviewParameterProvider、静态预览数据和状态构造。用户提到预览、Compose Preview、页面预览、组件预览或批量补充预览时使用。
---

# APC Preview

文中的 `core/...` 与 `feature/...` 均为不绑定包名的逻辑路径；在当前 Android 源码集下定位对应文件，不把现有 package 前缀写成固定目录。

## 必读资料

读取 `AGENTS.md` 获取仓库级限制，但不得用它代替本 Skill 的 Preview 规则。读取 `docs/android-project-compose/框架核心/annotation.md`、`docs/android-project-compose/框架核心/data.md#预览数据`、`docs/android-project-compose/业务功能/view.md#预览规范` 和 `docs/android-project-compose/框架核心/screen-adaptation.md`。

## 必读源码

- `core/annotation/ScreenPreview.kt`、`ComponentPreview.kt`：确认现有手机、深色、平板、折叠屏和组合注解。
- `core/data/preview/` 与目标 Feature `data/`：确认可复用的 `PreviewParameterProvider` 和静态数据。
- 当前页面的 Screen 参数与状态类型：构造真实可渲染输入，不从 Route 或 ViewModel 猜测。
- `feature/demo/view/NetworkListDemoScreen.kt`：参考 `@PreviewParameter`、成功态和深浅色组合。
- 目标页面使用的图片、主题和公共 UI 组件：确认设计时不会触发外部依赖。

## 固定规则

- 完整页面使用 `@ScreenPreview*`，局部组件使用 `@ComponentPreview*`；已有组合注解能满足时不重复声明 `@Preview`。
- Preview 直接调用 Screen 或 Content，并由 `AppTheme` 包裹；不调用 Route、不创建 Hilt ViewModel、不依赖导航宿主。
- Preview 只使用静态状态和数据，不发起网络、数据库、存储、协程初始化或真实导航。
- 复杂输入使用 `@PreviewParameter`；跨 Feature 数据进入 `core/data/preview`，Feature 私有数据保留在业务域。
- 每个普通页面至少提供浅色和深色成功布局；Loading、Empty、Error 与多设备预览按页面状态和适配风险补充。

## 工作流

1. 区分完整页面和独立组件。页面使用 `@ScreenPreview*`，组件使用 `@ComponentPreview*`。
2. 普通手机页面至少提供浅色和深色预览；需要验证手机、折叠屏和平板时选择现有多设备组合注解，不重复声明相同 `@Preview` 集合。
3. 将 Preview 函数放在对应 Screen 或组件文件末尾，用 `AppTheme` 包裹可渲染内容。
4. 直接预览 Screen 或 Content，不创建 Hilt ViewModel，不依赖 Navigation 宿主，不调用 Route。
5. 为网络、分页、数据库或存储页面构造稳定的静态成功数据和明确状态；禁止发起真实网络、数据库、协程初始化或导航操作。
6. 多组或复杂输入使用 `@PreviewParameter` 与 `PreviewParameterProvider`。跨 Feature 复用的数据放入 `core/data/preview`，Feature 私有数据放入业务域 `data/`。
7. 图片组件优先使用本地资源、占位图或可预测的 Preview 分支，不依赖远程图片在设计时可访问。
8. 为 Preview 函数、参数、Provider、公开属性和重写属性添加中文注释，确保注释独立、客观并与最终职责一致。

## 验证

确认预览可独立编译、浅色与深色可读、手机与任务涉及的大屏无溢出、空列表与成功数据符合页面状态约束。运行 `./gradlew :app:assembleDebug`、相关 Compose UI 测试与 `git diff --check`，并单独报告未完成的 IDE 渲染或真机窗口验证。
