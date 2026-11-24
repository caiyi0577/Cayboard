# Cayboard

Cayboard 是一个基于 CyIME 与 Rime 改造的 Android 输入法项目，主要面向中文输入，并提供英文输入及本地化的键盘工具。项目当前仅支持 Android；iOS 和桌面端移植尚未提供。

## 功能

- 中文九键、中文 26 键和英文键盘
- 基于本地词库与 Rime 引擎的离线拼音输入
- 候选选择后的九键使用习惯记忆
- 剪贴板历史、字符选择盘、表情和语音输入
- 本地语音识别及离线翻译模型
- 本机模型信息与版本管理
- 坚果云 WebDAV 用户数据备份与恢复

功能及模型组合会随构建版本变化。发布包的具体内容以对应 GitHub Release 说明为准。输入法只在用户启用相应功能时访问所需系统能力；语音识别与翻译使用本机模型。坚果云备份需要用户主动配置 WebDAV 账号和应用密码。

## 获取应用

从 [GitHub Releases](https://github.com/caiyi0577/Cayboard/releases) 下载与设备架构匹配的 APK。请以最新公开 Release 的资产和说明为准；仓库中的本地调试 APK 不作为正式发布文件。

## 构建

构建需要 JDK、Android SDK、Android NDK、CMake，以及 Gradle 下载依赖所需的网络连接。使用仓库包装器构建 Android 调试版：

```powershell
./gradlew.bat assembleDebug
```

输出位于 `app/build/outputs/apk/`。要构建签名发布版，需要配置 `app/keystore.properties` 中的发布签名。完整构建与发布流程请参阅仓库内的开发文档和 GitHub Actions 工作流。

## 上游与许可

Cayboard 保留 CyIME/Rime 的输入引擎及相关方案基础，并包含其他上游依赖、词库和模型。请遵守各组件原有许可证与署名要求；项目许可证及第三方声明见 [LICENSE](LICENSE)、[NOTICE](app/src/main/assets/NOTICE.txt) 和 `app/src/main/assets/licenses/`。

本项目与 CyIME 上游及其中列出的第三方项目保持各自名称和商标归属；Cayboard 名称不表示对上游商标的所有权。
