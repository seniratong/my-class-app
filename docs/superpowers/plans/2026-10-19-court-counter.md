# Court Counter Implementation Plan

> Use superpowers:executing-plans to execute in the current workspace.

Goal: 实现运行时只有一个页面的双队计分程序。
Architecture: MainActivity 保存两队状态，activity_main.xml 定义双栏与底部 RESET。
Tech stack: Java, XML, AppCompat, AndroidX, Espresso。

- [x] 先编写 ScoreboardTest，确认旧 BMI 页面缺少计分组件导致编译失败。
- [x] 替换 MainActivity、activity_main.xml 与文字样式，实现两队独立加分和统一重置。
- [x] 实现两队分数的 Bundle 保存恢复和系统栏边距。
- [x] 构建 debug / androidTest APK，JVM 测试与 lint 检查通过。
- [x] 模拟器运行初始值、独立加分、重置、重建恢复四项流程测试，加上既有包名测试共 5 项通过。
- [x] 导出真实 Android Studio 布局预览、Component Tree、Java/XML 与 199 像素宽的 9:16 运行截图，保存在 app/build/submission。
集成方式：用户已明确授权提交并推送当前项目原 master；执行结果以 Git 提交与远程校验为准。

独立代码审查未发现问题。所有截图与安装包仅位于忽略的 app/build，未纳入提交范围。
