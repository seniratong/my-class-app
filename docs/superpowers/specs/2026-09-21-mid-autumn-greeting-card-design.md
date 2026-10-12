# 中秋节日贺卡设计说明

## 目标

将现有计算器页面替换为适合学生作业展示的中秋节日贺卡，保留老师要求的 `ConstraintLayout`、`ImageView` 和 `TextView` 实践内容，同时使用原创卡通插画形成自己的视觉风格。

## 视觉方案

- 主题：月夜玉兔
- 背景：深蓝紫色夜空，营造安静、清爽的节日氛围
- 主视觉：圆月、玉兔、桂花和少量祥云
- 强调色：暖黄色月光与橙色桂花
- 文字：中秋快乐；月满中秋，愿你平安喜乐；来自：我的祝福
- 版式：顶部标题，中部插画卡片，底部祝福文字

## 技术方案

- 使用现有 XML 布局工程，不迁移到 Compose
- 根布局继续使用 `ConstraintLayout`
- 使用本地 `drawable/mid_autumn_rabbit.png` 作为 `ImageView` 图片资源
- 使用 `strings.xml` 管理所有可见文字，使用 `colors.xml` 管理主题颜色
- 移除计算器按钮和运算逻辑，`MainActivity` 只负责加载贺卡布局
- 保留 Material 依赖，不新增外部库

## 验收标准

- 页面能显示标题、卡通图片和三段祝福文案
- 图片使用 `centerCrop`，在不同屏幕上不会变形
- 所有控件具备完整 ConstraintLayout 约束
- `assembleDebug` 和 `testDebugUnitTest` 通过
