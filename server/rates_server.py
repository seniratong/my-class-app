"""Local classroom API. MySQL credentials stay in mysql_config_editor's login file."""
import argparse
from datetime import date
from decimal import Decimal
import html
from http.server import BaseHTTPRequestHandler, ThreadingHTTPServer
import json
import logging
import os
from pathlib import Path
import re
import shutil
import subprocess

SOURCE_URL = "https://chl.cn/huilv/?jinri"


class DatabaseError(RuntimeError):
    pass


def validate_snapshot(value):
    if not isinstance(value, dict) or value.get("source_url") != SOURCE_URL:
        raise ValueError("Invalid snapshot source")
    day = value.get("update_date", "")
    if not isinstance(day, str) or date.fromisoformat(day).isoformat() != day:
        raise ValueError("Invalid update date")
    rows = value.get("rows")
    if not isinstance(rows, list) or not 3 <= len(rows) <= 100:
        raise ValueError("Expected 3 to 100 currencies")
    clean, names = [], set()
    for row in rows:
        if not isinstance(row, dict):
            raise ValueError("Invalid currency row")
        name, quote = row.get("currency_name"), row.get("quote")
        if (not isinstance(name, str) or not name.strip() or name != name.strip()
                or len(name) > 80 or any(ord(c) < 32 for c in name) or name in names):
            raise ValueError("Invalid or duplicate currency name")
        if (not isinstance(quote, str) or not re.fullmatch(r"\d{1,12}(?:\.\d{1,8})?", quote)
                or Decimal(quote) <= 0):
            raise ValueError("Invalid positive decimal quote")
        names.add(name)
        clean.append({"currency_name": name, "quote": quote})
    if not {"美元", "欧元", "韩国元"}.issubset(names):
        raise ValueError("Missing required currencies")
    return {"update_date": day, "source_url": SOURCE_URL, "rows": clean}


def sql_text(value):
    # Hex literals avoid dependence on MySQL SQL_MODE or backslash/quote escaping.
    return "CONVERT(X'" + value.encode("utf-8").hex() + "' USING utf8mb4)"


class MySqlStore:
    def __init__(self, database="exchange_rates", login_path="rate-assignment", mysql_exe=None):
        if not re.fullmatch(r"[a-z][a-z0-9_]{0,63}", database):
            raise ValueError("Invalid database identifier")
        self.database = database
        self.login_path = login_path
        self.mysql_exe = (mysql_exe or os.environ.get("RATE_MYSQL_EXE") or shutil.which("mysql")
                          or r"C:\Program Files\MySQL\MySQL Server 9.2\bin\mysql.exe")

    def execute(self, sql, with_database=True):
        command = [self.mysql_exe, "--login-path=" + self.login_path,
                   "--default-character-set=utf8mb4", "--batch", "--raw", "--skip-column-names",
                   "--connect-timeout=3"]
        if with_database:
            command.append("--database=" + self.database)
        try:
            result = subprocess.run(command, input=sql.encode("utf-8"), capture_output=True,
                                    timeout=12, creationflags=getattr(subprocess, "CREATE_NO_WINDOW", 0))
        except (OSError, subprocess.TimeoutExpired) as error:
            raise DatabaseError("MySQL client unavailable or timed out") from error
        if result.returncode:
            logging.warning("MySQL operation failed: %s", result.stderr.decode("utf-8", errors="replace"))
            raise DatabaseError("MySQL operation failed")
        return result.stdout.decode("utf-8")

    def initialize(self):
        schema = Path(__file__).with_name("schema.sql").read_text(encoding="utf-8")
        self.execute(schema.replace("__DATABASE__", self.database), with_database=False)

    def save(self, value):
        data = validate_snapshot(value)
        # Complete daily replacement is atomic; earlier days stay intact.
        statements = ["START TRANSACTION;",
                      "INSERT INTO rate_snapshots (update_date,source_url) VALUES ("
                      + sql_text(data["update_date"]) + "," + sql_text(data["source_url"]) + ") "
                      "ON DUPLICATE KEY UPDATE id=LAST_INSERT_ID(id),source_url=VALUES(source_url),"
                      "updated_at=CURRENT_TIMESTAMP(6);",
                      "SET @snapshot_id=LAST_INSERT_ID();",
                      "DELETE FROM currency_rates WHERE snapshot_id=@snapshot_id;"]
        values = ["(@snapshot_id," + str(i) + "," + sql_text(row["currency_name"]) + ","
                  + row["quote"] + ")" for i, row in enumerate(data["rows"])]
        statements += ["INSERT INTO currency_rates (snapshot_id,position,currency_name,quote) VALUES "
                       + ",".join(values) + ";", "COMMIT;"]
        self.execute("\n".join(statements))
        return {"saved": True, "count": len(data["rows"]), "update_date": data["update_date"]}

    def read(self):
        output = self.execute("""
SELECT JSON_OBJECT('currency_name',r.currency_name,'quote',CAST(r.quote AS CHAR),
 'update_date',DATE_FORMAT(s.update_date,'%Y-%m-%d'), 'source_url',s.source_url,
 'updated_at',DATE_FORMAT(s.updated_at,'%Y-%m-%d %H:%i:%s'))
FROM currency_rates r JOIN rate_snapshots s ON s.id=r.snapshot_id
WHERE s.id=(SELECT id FROM rate_snapshots ORDER BY updated_at DESC,id DESC LIMIT 1)
ORDER BY r.position;
""")
        records = [json.loads(line) for line in output.splitlines() if line.strip()]
        if not records:
            return {"update_date": None, "updated_at": None, "source_url": SOURCE_URL, "rows": []}
        return {"update_date": records[0]["update_date"], "updated_at": records[0]["updated_at"],
                "source_url": records[0]["source_url"],
                "rows": [{"currency_name": row["currency_name"],
                          "quote": row["quote"].rstrip("0").rstrip(".")} for row in records]}


def database_page(data, database):
    rows = "".join("<tr><td>" + str(i + 1) + "</td><td>" + html.escape(row["currency_name"])
                   + "</td><td>" + html.escape(row["quote"]) + "</td><td>100</td><td>"
                   + html.escape(data["update_date"]) + "</td></tr>" for i, row in enumerate(data["rows"]))
    return """<!doctype html><html lang="zh-CN"><meta charset="utf-8">
<title>MySQL 汇率数据库 · 实际查询</title><style>
body{font:16px 'Microsoft YaHei',sans-serif;color:#172b4d;background:#f4f6fa;margin:32px}
main{max-width:1100px;margin:auto;background:white;padding:28px;border-radius:12px}
h1{margin-top:0;font-size:28px}p{line-height:1.8}table{border-collapse:collapse;width:100%}
th,td{padding:10px 16px;text-align:left;border-bottom:1px solid #dce2eb}th{background:#edf2fb}
a{color:#1758b7}code{background:#edf2fb;padding:3px 7px}small{color:#52637d}</style>
<main><h1>MySQL 汇率数据库</h1><p>数据库：<code>""" + html.escape(database) + """</code>
 · 表：<code>rate_snapshots</code> / <code>currency_rates</code><br>
查询到 <b>""" + str(len(data["rows"])) + """</b> 种货币 · 更新日期：<b>""" + html.escape(data["update_date"] or "暂无") + """</b><br>
数据库更新时间：""" + html.escape(data["updated_at"] or "暂无") + """<br>
来源：<a href="https://chl.cn/huilv/?jinri">我查网 chl.cn 今日汇率</a> · 每 100 外币对应人民币（元）</p>
<table><thead><tr><th>序号</th><th>currency_name</th><th>quote（人民币）</th><th>quote_unit</th><th>update_date</th></tr></thead>
<tbody>""" + rows + """</tbody></table><p><a href="/database">重新查询数据库</a> · <a href="/api/rates">查看 JSON</a></p>
<small>本页每次请求实时 SELECT 查询本机 MySQL，未使用静态示例数据。</small></main></html>"""


def make_handler(store):
    class Handler(BaseHTTPRequestHandler):
        def respond(self, status, body, content_type="application/json; charset=utf-8"):
            if not isinstance(body, str):
                body = json.dumps(body, ensure_ascii=False)
            raw = body.encode("utf-8")
            self.send_response(status)
            self.send_header("Content-Type", content_type)
            self.send_header("Content-Length", str(len(raw)))
            self.send_header("Cache-Control", "no-store")
            self.send_header("X-Content-Type-Options", "nosniff")
            self.end_headers()
            self.wfile.write(raw)

        def do_GET(self):
            try:
                if self.path == "/health":
                    store.execute("SELECT 1;")
                    self.respond(200, {"status": "ok", "database": store.database})
                elif self.path in ("/api/rates", "/database", "/"):
                    data = store.read()
                    if self.path == "/api/rates":
                        self.respond(200, data)
                    else:
                        self.respond(200, database_page(data, store.database), "text/html; charset=utf-8")
                else:
                    self.respond(404, {"error": "Not found"})
            except DatabaseError:
                self.respond(503, {"error": "Database unavailable"})

        def do_POST(self):
            if self.path != "/api/rates":
                self.respond(404, {"error": "Not found"})
                return
            # JSON only, with no CORS permission for unrelated browser origins.
            if self.headers.get("Origin") or self.headers.get_content_type() != "application/json":
                self.respond(403, {"error": "Native JSON requests only"})
                return
            try:
                length = int(self.headers.get("Content-Length", "0"))
                if not 0 < length <= 65536:
                    raise ValueError("Invalid body size")
                data = json.loads(self.rfile.read(length).decode("utf-8"))
                self.respond(200, store.save(data))
            except (ValueError, TypeError, UnicodeError):
                self.respond(400, {"error": "Invalid rate snapshot"})
            except DatabaseError:
                self.respond(503, {"error": "Database unavailable"})
    return Handler


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--port", type=int, default=8787)
    parser.add_argument("--database", default="exchange_rates")
    parser.add_argument("--login-path", default="rate-assignment")
    args = parser.parse_args()
    store = MySqlStore(args.database, args.login_path)
    store.initialize()
    server = ThreadingHTTPServer(("127.0.0.1", args.port), make_handler(store))
    print("MySQL rates API: http://127.0.0.1:" + str(args.port) + "/database", flush=True)
    try:
        server.serve_forever()
    except KeyboardInterrupt:
        pass
    finally:
        server.server_close()


if __name__ == "__main__":
    main()
