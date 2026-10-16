"""修掉 README 里几个已改名的引用。

TickAdapter 已经拆成两套：共享的 TickHook 接口 + 各加载器的实现。
英文 README 的 IDEA 一节标题和中文不一致，改成同一节处理。
"""
import pathlib

R = pathlib.Path(r"D:\dshgowork\moddoctor")

# 中文用 ZH_NEW 那份，英文用 EN_NEW 那份；两边标题不同，各自匹配
ZH_MARK = "### 用 IntelliJ IDEA 构建"
EN_MARK = "### Building in IntelliJ IDEA"

TEXT_FIXES = [
    # TickAdapter -> 现在的名字
    ("跨过这条线的部分由 `TickAdapter` 在**运行时**探测该用哪套，",
     "跨过这条线的部分由 `TickHook` 的实现在**运行时**探测该用哪套，"),
    ("照 `TickAdapter` 的样子加一层运行时适配即可。",
     "照 `src/loader/` 下那两套 `TickHook` 实现的样子加一层运行时适配即可。"),
    ("the `TickAdapter` resolves that at **runtime** by probing for whichever shape is present",
     "the `TickHook` implementations resolve that at **runtime** by probing for whichever shape is present"),
    ("add another runtime adapter shaped like `TickAdapter`.",
     "add another runtime adapter shaped like the `TickHook` implementations under `src/loader/`."),
    ("| `TickAdapter.java`        tick 事件的跨版本适配",
     "| `TickHook.java`          服务器 tick 的挂载点接口"),
    ("`TickAdapter`", "`TickHook`"),
    # 路径
    ("`common/tools/", "`engine/tools/"),
    ("common/tools/", "engine/tools/"),
]

for name in ("README.md", "README.en.md"):
    p = R / name
    t = p.read_text(encoding="utf-8")
    orig = t
    for a, b in TEXT_FIXES:
        t = t.replace(a, b)
    if t != orig:
        p.write_text(t, encoding="utf-8", newline="")
        print("修了", name)

# 各版本目录里的也一样
for d in (R / "versions").iterdir():
    if not d.is_dir():
        continue
    for fname in ("README.md", "README.en.md"):
        p = d / fname
        if not p.exists():
            continue
        t = p.read_text(encoding="utf-8")
        orig = t
        for a, b in TEXT_FIXES:
            t = t.replace(a, b)
        if t != orig:
            p.write_text(t, encoding="utf-8", newline="")
            print("修了", d.name, fname)
