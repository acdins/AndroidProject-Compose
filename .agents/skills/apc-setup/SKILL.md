---
name: apc-setup
description: 将 AndroidProject-Compose 脚手架初始化为新的 Android 应用，统一修改应用显示名称、项目名、namespace、applicationId、Kotlin 包名、运行时品牌文案、仓库地址、Logo、图标和启动页。用户拉取模板后要求改名、改包名、替换品牌或完成首次项目配置时使用。
---

# APC Setup

## 必读资料

读取 `AGENTS.md` 获取仓库级限制，但不得用它代替本 Skill 的初始化规则。读取 `README.md`、`docs/android-project-compose/简介/quick-start.md`、`docs/android-project-compose/简介/environment.md` 和 `.agents/skills/apc-build/SKILL.md`。

## 必读源码与配置

- `settings.gradle.kts`、根与 `app` 的 `build.gradle.kts`、`gradle/libs.versions.toml`。
- `app/src/main/AndroidManifest.xml`、`res/values/strings.xml`、`values-en/strings.xml`、主题和图标资源。
- Kotlin 主源码、单元测试和 Instrumentation 测试的 package/import 声明。
- `Application.kt`、`MainActivity.kt`、关于页 ViewModel、README 和应用内项目链接。
- `.agents/skills/`、`AGENTS.md` 与 `docs/android-project-compose/`，用于区分框架身份和应用运行时身份。

## 固定规则

- 显示名称、Gradle 项目名、namespace、applicationId、Kotlin 包名、版本和品牌资源分别建立映射，不使用一个字符串完成全仓库替换。
- 包名迁移必须同时更新目录、`package`、`import`、Manifest、测试和反射/序列化引用，并重新构建确认。
- 应用品牌可以迁移，框架文档、`apc-*` Skill 和架构规范默认保留，除非用户明确要求重写框架身份。
- 缺少仓库地址、文档地址、公司信息、签名或品牌源图时先确认，不生成占位链接、假密钥或假版权。
- 替换图标和启动页时按 Android 资源规则更新全部受影响目录，不只替换单张图片。
- 保留工作区已有修改，先建立旧值清单和允许列表，再执行精确变更。

## 收集项目身份

写入前取得以下信息；缺失的关键标识必须向用户确认，不自行创造：

| 信息 | 格式与用途 |
| --- | --- |
| 应用显示名称 | 用户可见名称，可与工程名不同 |
| Gradle 根项目名 | `settings.gradle.kts` 的 `rootProject.name` |
| namespace | Kotlin/Android 代码命名空间，小写反向域名 |
| applicationId | 最终安装包标识，小写反向域名 |
| 包名 | Kotlin 源码声明、目录与 import |
| 版本 | `versionCode` 与 `versionName` |
| 项目描述和链接 | README、关于页、Issue 与在线文档入口 |
| 品牌资源 | Logo、Adaptive Icon 前景/背景、启动页资源 |

先建立旧值到新值的明确映射，区分显示名称、工程名、namespace、applicationId、Debug 后缀、Kotlin 包名和品牌资源，不做单一字符串的全仓库替换。

## 保留框架参考能力

应用改名不等于删除框架参考。默认保留 `docs/android-project-compose/`、`.agents/skills/apc-*` 与 `AGENTS.md` 中的框架架构和开发规范；只修改运行时身份、应用专属说明及用户明确要求迁移的文档品牌内容。

## 工作流

1. 记录工作区状态，搜索当前显示名称、包名、ID、仓库地址和资源引用，建立精确修改清单。
2. 修改 `settings.gradle.kts`、`app/build.gradle.kts`、Manifest 与字符串资源中的工程和应用身份。
3. 移动 Kotlin 包目录并同步修改 `package`、`import`、测试包和 Manifest 引用；优先使用 Android Studio 重构语义，避免路径与声明不一致。
4. 更新应用内关于页、README、仓库和文档链接；缺失的真实地址保留原值或删除，不写假地址。
5. 替换 Logo、图标和启动页资源，按 `apc-build` 检查各密度、Adaptive Icon 和 Android 12 主题。
6. 搜索旧标识残留，并使用允许列表保留框架文档和 Skill 中合法的 AndroidProject-Compose/APC 名称。
7. 更新测试期望和必要说明，不覆盖工作区中与初始化无关的修改。

## 验证

运行 `./gradlew :app:assembleDebug`、`./gradlew :app:testDebugUnitTest`、必要的 `./gradlew :app:lint` 与 `git diff --check`。安装验证应用名称、包标识、图标、启动页、关于页和 Debug/Release 区分；无法完成签名或真机验证时明确说明。
