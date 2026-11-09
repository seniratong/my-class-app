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
OUT = ROOT / "assignment-output" / "rate-parameters"
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


render_code("06-code-pass-parameters.png", "关键代码 1：传入三种汇率", [
    span("RateActivity.java", "private float dollarRate", 3),
    block("RateActivity.java", "private void openRateSettings()"),
    block("RateSettingsActivity.java", "if (savedInstanceState == null)"),
])
render_code("07-code-return-result.png", "关键代码 2：保存返回并接收结果", [
    block("RateSettingsActivity.java", "private void saveAndReturn()"),
    block("RateActivity.java", "private final ActivityResultLauncher<Intent>"),
])
render_code("08-code-save-and-calculate.png", "关键代码 3：本地保存与按新汇率计算", [
    span("RateActivity.java", "preferences = getSharedPreferences", 2),
    block("RateActivity.java", "private void saveRates()"),
    block("RateMath.java", "static String convert("),
    block("RateActivity.java", "private void showResults("),
])

summary = """处理页面参数传递——过程小结

本次作业在当前 Android 项目中独立完成汇率换算页面和汇率修改页面，与计数器没有页面跳转关系。换算页使用 dollarRate、euroRate、wonRate 三个 float 变量保存美元、欧元和韩元汇率，约定汇率表示 1 元人民币可兑换的外币金额。

换算页输入人民币金额后，点击“开始换算”，分别使用人民币金额乘以三种汇率，显示保留两位小数的结果。点击“修改汇率”按钮或右上角菜单时，通过 Intent 和 Bundle 的 putFloat() 将当前三种汇率传入修改页，修改页通过 getExtras() 和 getFloat() 读取并回填输入框。

修改页点击“保存并返回”时，先检查三个输入是否为有效的正数，再通过 Intent 携带新汇率，调用 setResult(RESULT_OK, result) 和 finish() 返回。换算页使用 ActivityResultLauncher 接收结果，检查返回码和数据是否完整，更新三个汇率变量，再使用新汇率计算当前金额。“取消”和返回键均不修改原来的汇率。

结合前面的课件，使用 MODE_PRIVATE 模式的 SharedPreferences 保存已确认的三种汇率，并通过 Editor.putFloat() 和 apply() 写入。再次打开汇率页时读取本地保存的汇率。页面重建时通过 onSaveInstanceState 保存金额和汇率，修改页通过有独立 ID 的 EditText 自动恢复未提交内容。

运行验证中，输入 100 元，初始汇率为美元 0.14、欧元 0.13、韩元 190，结果分别为 14.00、13.00 和 19000.00。将汇率修改为 0.2、0.15 和 200 后保存返回，结果更新为 20.00、15.00 和 20000.00，说明三种汇率成功传入、修改并返回，且新值参与了计算。

实现时重点保证传参与取参的键名、float 类型一致，区分“保存”和“取消”，并处理空值、0、负数及无效输入。已在 Android 模拟器验证页面传参、保存返回、取消、页面重建和重新打开后的汇率恢复，同时完成计算单元测试与原计数器回归测试。
"""
(OUT / "过程小结.txt").write_text(summary, encoding="utf-8-sig")

items = [
    ("01-layout-conversion.png", "换算页面布局"),
    ("02-layout-editor.png", "汇率修改页面布局（已传入当前汇率）"),
    ("06-code-pass-parameters.png", "关键代码：Intent / Bundle 传入三种汇率"),
    ("07-code-return-result.png", "关键代码：setResult 返回与 ActivityResultLauncher 接收"),
    ("08-code-save-and-calculate.png", "关键代码：SharedPreferences 保存与计算"),
    ("03-runtime-before.png", "运行：100 元按原汇率换算"),
    ("04-runtime-edit.png", "运行：输入三种新汇率"),
    ("05-runtime-after.png", "运行：保存返回，按新汇率重新计算"),
]
missing = [name for name, _ in items if not (OUT / name).is_file()]
if missing:
    raise SystemExit("Missing device screenshots: " + ", ".join(missing))

readme = """# 处理页面参数传递——提交材料

本目录按题目四项要求整理，截图均可单独上传。运行图来自真实 Android 模拟器；关键代码图由项目实际源码按行号排版导出。

1. 两个页面布局：01、02。
2. 参数传递关键代码：06、07；08 补充本地保存和计算。
3. 运行截图：03（修改前）、04（修改中）、05（保存返回后）。
4. 过程小结：打开“过程小结.txt”，可直接复制到作业文本框。

页面位于当前 app 模块，桌面选择“汇率换算”入口；计数器仍使用原入口，两者之间没有跳转。Android Studio 可指定启动 com.example.myapplication1.RateActivity。命令行也可执行：

    adb shell am start -n com.example.myapplication1/.RateActivity

本项目现有最低系统要求为 Android API 37；本次未修改 SDK 或依赖版本。汇率是手动设置的课堂示例值。

“作业报告.html”可在浏览器中整体查看。截图 PNG 保留原始清晰度，未裁剪或拼接。压缩包仅含提交材料，不含个人信息、凭据或本机配置。尚未提交到课程平台。
"""
(OUT / "提交说明.md").write_text(readme, encoding="utf-8")

sections = []
for name, label in items:
    with Image.open(OUT / name) as screenshot:
        width, height = screenshot.size
    sections.append(
        f'<section><h2>{html.escape(label)}</h2>'
        f'<p>{width} × {height} px</p>'
        f'<img src="{name}" alt="{html.escape(label)}"></section>'
    )
report = """<!doctype html><html lang="zh-CN"><meta charset="utf-8">
<title>处理页面参数传递——作业报告</title><style>
body{max-width:1050px;margin:40px auto;padding:0 24px;font:17px/1.8 system-ui,'Microsoft YaHei',sans-serif;color:#182237}
h1{color:#234da7}section{border-top:1px solid #dde4f0;margin:36px 0;padding-top:20px;break-inside:avoid}
img{max-width:100%;max-height:900px;display:block;border:1px solid #e2e6ef}p{color:#5b6473}pre{white-space:pre-wrap;font:16px/1.9 inherit}
@media print{body{margin:0}section{break-before:page}img{max-height:230mm}}
</style><h1>处理页面参数传递</h1><p>汇率换算与汇率修改两个独立页面 · 提交材料预览</p>
""" + "".join(sections) + "<section><h2>过程小结</h2><pre>" + html.escape(summary) + "</pre></section></html>"
(OUT / "作业报告.html").write_text(report, encoding="utf-8")

archive = OUT.parent / "页面参数传递-提交材料.zip"
with zipfile.ZipFile(archive, "w", zipfile.ZIP_DEFLATED) as zipped:
    for path in sorted(OUT.iterdir()):
        if path.is_file():
            zipped.write(path, arcname=path.name)
print(f"Created {archive}")
for name, _ in items:
    with Image.open(OUT / name) as screenshot:
        print(f"{name}: {screenshot.width}x{screenshot.height}")
