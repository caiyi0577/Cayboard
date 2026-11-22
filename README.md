# Cayboard

Cayboard 是基于 CyIME 改造的 Android 中文输入法，面向后续 iOS 风格键盘体验的开发。

当前版本重点：

- 中文九键、中文 26 键和英文键盘
- iOS 风格深色键盘皮肤
- 统一的长按字符选择盘
- 离线词库、九键候选模型、手写、语音和翻译模型支持
- 模型管理：显示模型名称、版本和容量；支持本机导入新版本并保留旧版本
- 全量用户数据备份与恢复：通过坚果云 WebDAV 保存设置、词库、模型、布局、剪贴板和应用数据
- 备份账号使用坚果云“第三方应用密码”，不会使用网页登录密码

## 构建

仓库需要 Android SDK、NDK、CMake、JDK 21 和网络可用的 Gradle 依赖缓存。

```powershell
powershell -NoProfile -ExecutionPolicy Bypass -File .\work\build-cyime.ps1
```

构建脚本会生成 arm64-v8a APK。构建前请确保磁盘有足够空间，源码中的模型和词库资源体积较大。

## 版本说明

Cayboard 当前使用两个方向：

- 普通调试/联网版本：用于坚果云 WebDAV 全量备份与恢复。
- 离线模型版本：内置模型和词库，适合不需要云备份的设备环境。

坚果云配置：在坚果云【第三方应用管理】中新建应用密码，然后在 Cayboard 的【设置 → 备份与恢复】填写：

- WebDAV 目录：`https://dav.jianguoyun.com/dav/`
- 坚果云注册邮箱
- 第三方应用密码

大备份会自动分片上传，避免超过坚果云的单文件限制。账号和应用密码使用 Android Keystore 加密保存。

## 开源与许可

本项目基于 CyIME 继续开发。请同时遵守上游项目、Rime、模型、词库、字体和第三方依赖各自的许可证与署名要求。模型和词库的许可证文件位于 `app/src/main/assets/licenses/` 及相关资源目录。
