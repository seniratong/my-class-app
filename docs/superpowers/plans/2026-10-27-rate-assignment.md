# 页面参数传递作业实施计划

## 已确认范围

在现有 app 模块添加 RateActivity 和 RateSettingsActivity。汇率页有独立桌面入口，与计数器没有跳转关系。三种汇率使用 float 变量；用 Bundle / Intent 传入修改页，通过 ActivityResultLauncher 接收 setResult 返回的值。SharedPreferences 保存已确认汇率；取消不保存。示例汇率用于课堂演示。

## 验收步骤

- [x] 先添加汇率输入和金额计算的边界测试并验证实现前失败。
- [x] 完成两个独立页面、菜单和按钮事件，保存未提交输入以应对页面重建。
- [x] 验证修改前后 100 元的结果：14 / 13 / 19000 → 20 / 15 / 20000。
- [x] 验证传入当前值、保存返回、取消、输入错误、重建与重开后的汇率恢复。
- [x] 构建并运行汇率及既有计数器测试，确认原计数器没有新入口或跳转。
- [x] 从模拟器采集两页与运行过程截图；从实际源代码导出关键代码图片。
- [x] 整理过程小结和提交说明，打包到 assignment-output/rate-parameters。

## 提交材料

两张页面布局图、三张运行过程图、参数传递/保存返回/持久化关键代码图、过程小结文本。截图按正常手机比例保存，代码图保证字体可读；不自动提交课程平台，不推送 Git。

## 最终验证

- assembleDebug、testDebugUnitTest、lintDebug、connectedDebugAndroidTest 全部成功。
- 6 项单元测试、15 项设备测试通过，Lint 0 错误。
- 汇率入口使用独立 taskAffinity，已验证与计数器分别打开。
- 提交材料包含 5 张真实竖屏截图、3 张实际源码导出图、过程小结及整体 HTML 预览。
