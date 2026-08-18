---
name: apc-build
description: 管理 AndroidProject-Compose 的 Gradle、版本目录、SDK、BuildConfig、构建类型、ABI、签名、混淆、应用资源和 APK/AAB 发布流程。用户要求升级依赖、修改构建配置、替换图标或启动页、配置签名、排查构建失败、缩减包体或生成发布包时使用。
---

# APC Build

## 必读资料

读取 `AGENTS.md` 获取仓库级限制，但不得用它代替本 Skill 的构建规则。读取 `docs/android-project-compose/简介/environment.md`、`docs/android-project-compose/简介/quick-start.md` 和根目录 `README.md`。

依赖和插件行为以当前版本的官方文档为准，不凭记忆填写 AGP、Kotlin、Compose、KSP 或 Gradle 配置。

## 必读源码与配置

- `settings.gradle.kts`：插件仓库、依赖仓库、根项目名和模块列表。
- 根目录 `build.gradle.kts`：插件版本入口与全局构建约定。
- `app/build.gradle.kts`：SDK、applicationId、版本、ABI、签名、BuildConfig、构建类型、混淆和依赖。
- `gradle/libs.versions.toml` 与 Wrapper 配置：当前插件、库和 Gradle 版本关系。
- `app/src/main/AndroidManifest.xml`、`res/values*/`、`mipmap-*`、`drawable*`：应用入口、名称、主题、图标和启动资源。
- 任务涉及的 CI、ProGuard、测试和实际调用 `BuildConfig` 的源码。

修改任何配置前先读取当前值和所有引用方，不根据其他平台、旧模板或其他项目字段猜测。

## 固定规则

- 版本统一通过版本目录管理，除非插件或工具明确不能使用 alias；不在多个 Gradle 文件重复声明同一版本。
- SDK、JDK、Gradle、AGP、Kotlin、Compose 与 KSP 必须按兼容矩阵整体核对，不进行孤立升级。
- Debug 与 Release 的 applicationId、BuildConfig、签名、混淆、资源压缩和产物差异必须明确。
- 签名文件和凭据不进入仓库；未取得真实配置时不得创造密钥或假值。
- 运行时资源进入 Android `res/`，文档图片进入 `docs/images/`，两者不得混放。
- 不编辑构建产物、缓存或生成目录，不用构建成功替代安装、签名和目标设备验证。

## 配置边界

- 依赖和插件版本统一维护在 `gradle/libs.versions.toml`，业务模块通过 alias 引用。
- `namespace`、`applicationId`、版本号、SDK、BuildConfig 和构建类型在 `app/build.gradle.kts` 管理。
- 应用名称、图标、启动主题和 Android 资源写入 `app/src/main/res/` 与 Manifest，不把运行时资源放进文档图片目录。
- 签名密码、密钥和服务端密钥不得提交到仓库；使用本地属性、环境变量或 CI Secret，并为缺失配置提供清晰失败信息。
- 不编辑 `build/`、`.gradle/`、生成的 APK/AAB 或缓存文件；不在无明确需求时修改 Gradle Wrapper。

## 工作流

1. 记录工作区状态，确认目标是本地开发、CI、Debug、Release、APK、AAB 还是依赖升级。
2. 核对 JDK、Gradle、AGP、Kotlin、Compose BOM 和 KSP 的兼容关系，再修改最小配置范围。
3. 修改 BuildConfig、ABI、混淆或资源压缩时，同时检查调用方、产物数量和 Release 行为。
4. 修改图标或启动页时更新源资源及所有密度、Adaptive Icon 和 Android 12 主题入口，检查透明边距与前景安全区。
5. 修改签名时先确认 keystore、alias 和凭据来源，不创造占位凭据，不把真实凭据写入版本控制。
6. 审查 Gradle 差异，确认没有意外升级、仓库变更、依赖重复或生成产物。

## 验证

- Debug：运行 `./gradlew :app:assembleDebug` 与 `./gradlew :app:testDebugUnitTest`。
- 静态检查：按任务运行 `./gradlew :app:lint`。
- Release：配置有效签名后运行 `./gradlew :app:assembleRelease` 或 `./gradlew :app:bundleRelease`。
- 签名：需要证书摘要时运行 `./gradlew :app:signingReport`，不要在交付信息中泄露敏感凭据。
- 最后运行 `git diff --check`，报告未能执行的平台、签名或真机验证。
