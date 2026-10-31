"""README 里的测试命令与断言数按新结构更新。

旧路径 common/tools/... 已经变成 engine/tools/...，
断言数也从 127 涨到 132（补回了 tick 适配那 5 条）。
"""
import pathlib

R = pathlib.Path(r"D:\dshgowork\moddoctor")
TARGETS = [R / "README.md", R / "README.en.md"] + [
    p for d in (R / "versions").iterdir() if d.is_dir()
    for p in (d / "README.md", d / "README.en.md") if p.exists()
]

REPL = [
    ("`java common/tools/SelfTest.java`", "`pwsh tools/run-engine-tests.ps1`"),
    ("`java common/tools/RuntimeHarness.java`", "`pwsh tools/run-engine-tests.ps1`"),
    ("127 / 127 断言通过", "132 / 132 断言通过"),
    ("127 / 127 assertions pass", "132 / 132 assertions pass"),
    ("common/tools/SelfTest.java", "engine/tools/SelfTest.java"),
    ("common/tools/RuntimeHarness.java", "engine/tools/RuntimeHarness.java"),
    ("common/tools/cp-init.gradle", "engine/tools/cp-init.gradle"),
    ("common/src/main/java", "engine/main/java"),
    ("├── common/                                    诊断引擎（与 Minecraft 解耦）",
     "├── engine/                                    诊断引擎（与 Minecraft 解耦）"),
    ("├── common/                                    Diagnostic engine (decoupled from Minecraft)",
     "├── engine/                                    Diagnostic engine (decoupled from Minecraft)"),
]

for p in TARGETS:
    t = p.read_text(encoding="utf-8")
    orig = t
    for a, b in REPL:
        t = t.replace(a, b)
    if t != orig:
        p.write_text(t, encoding="utf-8", newline="")
        print("更新", p.relative_to(R))
