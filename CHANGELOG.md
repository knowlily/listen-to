# 更新日志

所有版本都遵循 [语义化版本](https://semver.org/lang/zh-CN/)。

GitHub Releases: https://github.com/knowlily/listen-to/releases

## 当前版本
- **最新版本**: 1.1 (versionCode 2)
- **最新标签**: v1.5
- **构建日期**: 2026-03-20

## [1.1.0] - 2026-03-20

### 新增
- 添加页面加载进度条显示
- 添加WebChromeClient支持，显示网页标题
- 添加网络连接状态检查
- 添加加载中旋转动画图标
- 改进错误处理，支持新旧API版本
- 添加完整的历史记录功能 (HistoryActivity)
- 添加明暗主题切换功能 (浅色/深色/跟随系统)
- 添加设置页面，包含缓存清除功能
- 添加Material Design动态取色功能 (Android 12+)
- 添加APK签名配置和密钥库生成

### 改进
- 优化showLoading函数，加载时显示旋转图标
- 增强网络错误处理，显示具体错误信息
- 改进页面加载状态管理
- 优化进度条显示逻辑
- 改进URL处理，支持更多网址格式 (http://, https://, www., IP地址, localhost)
- 增强WebView配置，添加缓存和用户代理设置
- 改进WebView错误回调日志
- 添加Gradle wrapper脚本和配置
- 更新Android Gradle插件至8.3.2
- 更新Material依赖至1.13.0

### 修复
- 修复页面加载时按钮状态更新问题
- 修复错误发生时进度条不隐藏的问题
- 改进URL处理逻辑
- 修复标题栏重复显示问题
- 修复Drawable资源加载错误 (动画图标问题)
- 修复网页无法打开问题 (WebView配置问题)
- 修复设置页面大小异常
- 修复闪退错误和无法跳转网页问题
- 修复启动器图标引用问题
- 添加网络安全配置，允许明文流量

## [1.0.0] - 2026-03-20

### 初始版本
- Material Design 3 (M3Expressive) 界面设计
- 动态取色 (Dynamic Color) 支持
- Android System WebView浏览器核心
- 中文界面本地化
- 前进/后退/刷新导航
- 底部导航栏
- 夜间模式支持
- 手势返回支持