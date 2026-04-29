# 安卓日记 App 分步开发

## 目标

按 `docs/04-development-plan.md` 分阶段实现 Android 日记 App。当前执行阶段 0：工程初始化。

## 交付边界

- 创建 Android Gradle 工程骨架。
- 配置 Kotlin、Compose、Material 3、Hilt、Room、DataStore、WorkManager、Biometric 等依赖。
- 实现可启动的单 Activity、主题、底部导航和五个空页面。
- 保持错误和环境阻塞显式可见。

## 当前环境发现

- Java 21 可用。
- 系统 `gradle` 启动失败：`Failed to load native library 'native-platform.dll'`。
- 未配置 `ANDROID_HOME` 和 `ANDROID_SDK_ROOT`。
- 本地 Gradle 缓存中存在 Android Gradle Plugin `8.13.2`。

## 不做

- 不引入 mock 成功路径。
- 不伪造构建通过。
- 不跳过环境错误。
