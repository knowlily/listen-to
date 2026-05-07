# 更新日志

所有版本都遵循 [语义化版本](https://semver.org/lang/zh-CN/)。

GitHub Releases: https://github.com/knowlily/listen-to/releases

## 当前版本
- **最新版本**: 1.6 (versionCode 7)
- **构建日期**: 2026-05-07

## [1.6.0] - 2026-05-07

### 新增
- 添加书签管理功能（BookmarksActivity），支持添加、查看、清除书签
- 添加用户代理（UA）切换功能，支持电脑模式和手机模式
- 添加滚动时自动隐藏/显示导航栏，提供更大阅读空间
- 添加弹窗窗口支持（window.open），在覆盖层中显示弹窗内容
- 添加历史记录清除按钮

### 修复
- 修复地址栏按回车键时重复加载 URL 的问题（移除冗余 KeyListener）
- 修复地址栏失去焦点时意外触发页面跳转的问题
- 修复 WebView 混合内容安全漏洞（MIXED_CONTENT_ALWAYS_ALLOW → NEVER_ALLOW）
- 修复历史记录/书签列表 RecyclerView 在 NestedScrollView 中滚动性能问题
- 修复设置页面版本号（1.4）与 build.gradle（1.6）不一致的问题
- 修复 CI 工作流中 gradlew 无执行权限导致 exit code 126 的问题
- 禁止 WebView 访问本地文件（file:// 协议），增强安全性

### 改进
- 优化地址栏 URL 加载后光标定位到末尾
- 优化历史记录时间戳解析，增加异常容错
- 优化用户代理字符串，PC 模式使用完整桌面端 Chrome UA

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