# Cayboard

[简体中文](README.md) · [English](README.en.md) · [繁體中文](README.zh-TW.md)

Cayboard 是一款开源 Android 输入法，主打中文九键，也支持全拼、双拼、英文输入和物理键盘。

## 功能

- 中文九键、中文 26 键和英文键盘
- 基于本地词库的离线拼音输入
- 同一输入方式和编码下，上一次成功选中的词排第一，第二位起按长期词典排序
- 剪贴板历史、字符选择盘、表情和语音输入
- X-ASR 中英文离线语音识别与 Hy-MT2 多语种离线翻译
- 本机模型信息与版本管理
- 坚果云 WebDAV 用户数据备份与恢复

输入、手写、X-ASR 语音识别和 Hy-MT2 翻译在设备上运行；完整版 APK 内置这些模型。坚果云备份需要主动配置 WebDAV 账号和应用密码。数据使用和权限见应用内「关于 → 隐私政策」。

## 获取应用

从 Release 页面获取 APK，查看对应的安装说明及内置模型清单。 [GitHub Releases](https://github.com/caiyi0577/Cayboard/releases)

## 构建

构建需要 JDK 21、Android SDK、Android NDK、CMake 和 Git LFS。克隆后初始化子模块并拉取大文件。完整版还需要准备离线模型文件；模型来源和许可证见 [NOTICE](app/src/main/assets/NOTICE.txt)。

```powershell
git submodule update --init --recursive
git lfs pull
./gradlew.bat assembleDebug
```

输出位于 `app/build/outputs/apk/`。本地签名发布版使用未提交到仓库的 `app/keystore.properties` 配置。

## 上游与许可

Cayboard 使用内置离线解码引擎，并包含来自多个项目的方案、词库、模型和其他依赖。请遵守各组件原有许可证与署名要求；项目许可证及来源声明见 [LICENSE](LICENSE)、[NOTICE](app/src/main/assets/NOTICE.txt) 和 `app/src/main/assets/licenses/`。

Cayboard 基于 CyIME，CyIME 源自 Xime。各项目名称、版权和商标归各自权利人所有。
