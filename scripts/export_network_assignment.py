"""Export actual source excerpts and organize the course submission materials.

Device screenshots are copied unchanged from the instrumented app. This script
renders code excerpts only; it never fabricates or edits runtime screenshots.
"""
from pathlib import Path
import html
import re
import zipfile

from PIL import Image, ImageDraw, ImageFont


ROOT = Path(__file__).resolve().parents[1]
OUT = ROOT / "assignment-output" / "network-rates"
SOURCE = ROOT / "app/src/main/java/com/example/myapplication1"
OUT.mkdir(parents=True, exist_ok=True)

CODE_FONT = ImageFont.truetype("C:/Windows/Fonts/consola.ttf", 21)
TITLE_FONT = ImageFont.truetype("C:/Windows/Fonts/msyh.ttc", 29)
LABEL_FONT = ImageFont.truetype("C:/Windows/Fonts/msyh.ttc", 19)
TOKEN = re.compile(
    r'//.*$|"(?:\\.|[^"\\])*"|@\w+|\b(?:private|public|protected|static|final|void|float|return|new|if|try|catch|super|null|true|false)\b'
)


def source_lines(filename):
    return (SOURCE / filename).read_text(encoding="utf-8").splitlines()


def block(filename, signature):
    lines = source_lines(filename)
    start = next(i for i, line in enumerate(lines) if signature in line)
    depth, opened = 0, False
    for end in range(start, len(lines)):
        depth += lines[end].count("{") - lines[end].count("}")
        opened |= "{" in lines[end]
        if opened and depth == 0:
            break
    return filename, start + 1, lines[start:end + 1]


def span(filename, signature, count):
    lines = source_lines(filename)
    start = next(i for i, line in enumerate(lines) if signature in line)
    return filename, start + 1, lines[start:start + count]


def render_code(filename, title, sections):
    probe = ImageDraw.Draw(Image.new("RGB", (1, 1)))
    width = max(1400, int(max(
        (probe.textlength(line, font=CODE_FONT) for _, _, lines in sections for line in lines),
        default=1000,
    )) + 160)
    height = 110 + sum(58 + 31 * len(lines) + 24 for _, _, lines in sections) + 20
    canvas = Image.new("RGB", (width, height), "#ffffff")
    draw = ImageDraw.Draw(canvas)
    draw.rectangle((0, 0, width, 82), fill="#edf2ff")
    draw.text((28, 20), title, font=TITLE_FONT, fill="#183a87")
    y = 102
    for source_name, first_number, lines in sections:
        draw.rounded_rectangle((20, y, width - 20, y + 42), radius=7, fill="#f2f4f7")
        draw.text((32, y + 6), source_name, font=LABEL_FONT, fill="#46536b")
        y += 58
        for number, line in enumerate(lines, first_number):
            draw.text((24, y), f"{number:>3}", font=CODE_FONT, fill="#8a95a5")
            x, cursor = 98, 0
            for match in TOKEN.finditer(line):
                prefix = line[cursor:match.start()]
                draw.text((x, y), prefix, font=CODE_FONT, fill="#202a39")
                x += draw.textlength(prefix, font=CODE_FONT)
                token = match.group()
                color = "#66758c" if token.startswith("//") else (
                    "#127a47" if token.startswith('"') else (
                        "#a66313" if token.startswith("@") else "#2459b8"
                    )
                )
                draw.text((x, y), token, font=CODE_FONT, fill=color)
                x += draw.textlength(token, font=CODE_FONT)
                cursor = match.end()
            draw.text((x, y), line[cursor:], font=CODE_FONT, fill="#202a39")
            y += 31
        y += 24
    canvas.save(OUT / filename)



render_code("05-code-thread-handler.png", "关键代码：子线程联网与 Handler 更新主线程", [
    block("RateActivity.java", "private void fetchRates()"),
])
render_code("06-code-html-parser.png", "关键代码：按表头和币种解析 HTML", [
    block("WebRates.java", "static WebRates parse("),
])
def project_line(relative_path, needle):
    lines = (ROOT / relative_path).read_text(encoding="utf-8").splitlines()
    index = next(i for i, line in enumerate(lines) if needle in line)
    return relative_path, index + 1, [lines[index]]

render_code("07-code-source-permission.png", "关键代码：HTTPS 数据源、网络权限与 Jsoup", [
    span("WebRates.java", "static final String URL", 2),
    block("WebRates.java", "static WebRates fetch()"),
    project_line("app/src/main/AndroidManifest.xml", "uses-permission"),
    project_line("app/build.gradle.kts", "implementation(libs.jsoup)"),
])
summary = """实验名称：使用子线程获取网页汇率

一、实验目的
在 Android 应用中使用 Thread / Runnable 访问网络，利用 Jsoup 解析 HTML，借助 Handler 在主线程更新界面。

二、实验步骤
1. 在 AndroidManifest.xml 声明 INTERNET 权限，在 Gradle 中添加 org.jsoup:jsoup:1.18.3。
2. 选择用户指定的我查网今日汇率网页 https://chl.cn/huilv/?jinri 作为数据源，使用 HTTPS 请求。
3. 点击“获取网络汇率”启动 exchange-rate-network 子线程，使用 Jsoup.connect().timeout(15000).get() 取得 HTML。网络访问和解析均在子线程完成。
4. 使用 table、tr、th、td 选择器查找表格，按“货币名称”和“现汇卖出价”表头定位列，再提取美元、欧元、韩国元对应报价。避免课件中固定 table 下标和固定间隔导致的解析错误。
5. 网页报价表示 100 外币对应的人民币金额，而应用保存的是 1 元人民币对应外币，因此使用 100 / 报价转换。三种币种齐全、报价有效才接受结果。
6. 通过 Handler(Looper.getMainLooper()).post() 返回主线程，更新三种汇率、换算结果、来源和本次获取时间，并用 SharedPreferences 保存。界面显示的是获取时间，不是银行报价发布时间。
7. 超时、断网、缺少币种或无效报价时显示失败提示，不覆盖原有汇率。请求中禁用重复获取和修改按钮；销毁页面时中断工作线程并清除排队回调。

三、运行操作
打开独立“汇率换算”入口，输入 100 元人民币，点击“获取网络汇率”，观察来源与获取时间、汇率及换算结果。重建页面后核对保存的数据。关闭测试模拟器网络后重试，检查失败提示及原汇率保留。

四、提交材料
01-before.png 获取前；02-network-success.png 实际联网成功；03-restored.png 页面重建；04-offline-preserved.png 断网保留原值；05 至 07 为实际关键代码导出图。
截图来自真实模拟器，汇率仅用于课堂实验，随网页数据变化。

五、代码状态
未推送远程；未自动向课程平台提交。此前未提交的计数器及汇率页面改动予以保留。
"""
(OUT / "实验步骤小结.txt").write_text(summary, encoding="utf-8-sig")
images = sorted(OUT.glob("*.png"))
page = "<!doctype html><meta charset='utf-8'><title>网页汇率获取作业</title><style>body{max-width:1100px;margin:40px auto;font:18px sans-serif;line-height:1.7;background:#f5f7fb}img{max-width:100%;background:white}figure{padding:20px;background:white}pre{white-space:pre-wrap}</style><h1>网页汇率获取作业</h1><pre>" + html.escape(summary) + "</pre>"
for img in images:
    page += "<figure><figcaption>" + html.escape(img.name) + "</figcaption><img src='" + img.name + "'></figure>"
(OUT / "作业报告.html").write_text(page, encoding="utf-8")
archive = ROOT / "assignment-output/本周汇率网络获取-提交材料.zip"
with zipfile.ZipFile(archive, "w", zipfile.ZIP_DEFLATED) as z:
    for file in sorted(OUT.iterdir()):
        if file.is_file(): z.write(file, "网页汇率获取/" + file.name)
print(archive)
