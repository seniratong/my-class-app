# 本机 MySQL 汇率接口

Python 3 标准库服务 + 官方 MySQL 命令行客户端，无额外 Python 依赖。数据库密码仅保存在当前 Windows 用户的 MySQL 登录配置中，仓库和 APK 均无密码。

## 首次连接

启动已安装的 MySQL 服务。用 PowerShell 执行以下命令并在本机提示中输入密码：

```powershell
& "C:\Program Files\MySQL\MySQL Server 9.2\bin\mysql_config_editor.exe" set --login-path=rate-assignment --host=127.0.0.1 --port=3306 --user=root --password
```

若客户端安装在其他位置，设置 `RATE_MYSQL_EXE` 为 mysql.exe 的完整路径。已有 MySQL 登录配置可以通过 `--login-path` 指定。

## 运行

```powershell
python server/rates_server.py
```

或运行 `server/start-server.ps1` 在后台启动。仅监听 `127.0.0.1:8787`，无需开放 MySQL 防火墙端口。默认创建 `exchange_rates` 库及两张表；同日快照在事务中替换，历史日期保留。数据库不可用时接口返回 503。

将调试设备的本机端口转发到电脑：

```powershell
adb reverse tcp:8787 tcp:8787
```

每次设备重连后需重新转发。Android 仍直接从我查网解析汇率，并在同一个后台工作流程中 POST 完整列表及日期。MySQL 保存失败时，本地换算和离线缓存继续可用，页面明确提示重试。当天缓存有效时重开页面不会再次请求网页或数据库。

- `GET /health`：实际数据库连接检查。
- `POST /api/rates`：保存完整每日快照，校验日期、币种唯一性、报价及三种必需币种。
- `GET /api/rates`：查询最后更新的数据库快照。
- `GET /database`：实时数据库查询页面，可用于截图。

## 验证

```powershell
python -m unittest discover -s server -v
$env:MYSQL_INTEGRATION='1'
python -m unittest discover -s server -v
```

集成测试创建独立的进程专用测试库，验证中文/特殊字符、每日替换、历史保留及事务失败回滚，结束后只清理本次新建的测试库。
