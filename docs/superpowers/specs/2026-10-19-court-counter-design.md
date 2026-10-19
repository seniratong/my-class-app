# 双队计分器

当前应用只运行一个双队计分页面。按用户要求使用简洁双栏外观。

- Team A、Team B 分别从 0 开始。
- 两队各有 +3 POINTS、+2 POINTS、FREE THROW（加 1 分）。
- RESET 同时将两队清零；清零后可继续加分。
- Bundle 保存两队分数，页面重建后恢复。
- 系统栏边距位于滚动容器外，短屏可滚动；按钮至少 48dp。
- 保留 Java/XML 技术栈，无额外页面和功能入口。

提交材料：布局文件截图、真实 Android Studio Component Tree 截图、Java 功能代码、宽度小于 200 像素的运行截图。导出材料放在 app/build/submission，不加入 Git。

验证：初始值、两队独立加分、所有加分按钮、RESET 和页面重建的设备测试；debug APK、单元测试、lint 和独立审查。
