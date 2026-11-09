## 个人信息

- 姓名：禹启硕
- 学号：42411124
- 班级：24级计科三班
- 专业：计算机科学与技术

## 项目介绍

本项目是基于 Android Studio 开发的移动应用项目，项目内容来自《移动技术开发与创业》课程。

## 已实现功能

- 双队计分器：加分、重置、修改比分、页面结果返回、持久化及横竖屏恢复。
- 汇率换算：人民币换算美元、欧元和韩元，支持手动修改汇率。
- 网络汇率列表：Jsoup 解析我查网，Thread/Runnable 获取，Handler 更新 ListActivity。
- 每日缓存：成功更新后保存日期，当天重开直接读取本地数据；失败保留旧值。
- MySQL 持久化：本机 HTTP 接口保存完整每日汇率快照，支持数据库实时查询，数据库保存失败独立提示。

## 开发验证

```powershell
.\gradlew.bat :app:testDebugUnitTest :app:assembleDebug :app:lintDebug
.\gradlew.bat :app:connectedDebugAndroidTest
```

联网及数据库用例需单独启用，并保证网络和本机接口可用。启动 MySQL 接口、配置设备端口转发以及查询数据库的方法见 [server/README.md](server/README.md)。数据库密码通过本机 MySQL 登录配置提供。
