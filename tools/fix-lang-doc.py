"""多语言一节补上两层读法的说明，并修掉过时的路径。"""
import pathlib

R = pathlib.Path(r"D:\dshgowork\moddoctor")
TARGETS = [R / "README.md", R / "README.en.md"] + [
    p for d in (R / "versions").iterdir() if d.is_dir()
    for p in (d / "README.md", d / "README.en.md") if p.exists()
]

ZH_OLD = """也就是说，**中文客户端自动出中文报告，其他语言的客户端自动出英文报告**，不用手动配。"""
ZH_NEW = """也就是说，**中文客户端自动出中文报告，其他语言的客户端自动出英文报告**，不用手动配。

第 3 条在客户端上分两层读，这是实机验证时发现必须要的：

| 时机 | 怎么读 | 为什么 |
|---|---|---|
| 模组构造函数 | 直接读 `options.txt` 里的 `lang:` | 模组如果在构造阶段就崩，整套加载流程当场结束，`FMLCommonSetupEvent` 根本不会触发。挂在那边的话，这种崩溃的报告语言就退回 JVM 区域了，和玩家选的不是一回事 |
| 客户端就绪 / 进世界 | `LanguageManager.getSelected()` | 这才是权威值。玩家中途改了语言也能跟上 |

`options.txt` 是 UTF-8 纯文本，很小，构造函数阶段读它没有任何负担。
"""

EN_OLD = """In other words, **a Chinese client gets a Chinese report and any other language gets an
English report**, with no configuration needed."""
EN_NEW = """In other words, **a Chinese client gets a Chinese report and any other language gets an
English report**, with no configuration needed.

On the client, step 3 reads the language at two different times, which real-client testing
proved necessary:

| When | How | Why |
|---|---|---|
| Mod constructor | read `lang:` straight out of `options.txt` | if a mod throws during construction the whole load aborts and `FMLCommonSetupEvent` never fires; hanging the read off that event meant such crashes produced a report in the JVM locale instead of the player's choice |
| Client ready / joining a world | `LanguageManager.getSelected()` | the authoritative value, and it follows a mid-session language change |

`options.txt` is small UTF-8 text, so reading it during construction costs nothing.
"""

for p in TARGETS:
    t = p.read_text(encoding="utf-8")
    orig = t
    if ZH_OLD in t:
        t = t.replace(ZH_OLD, ZH_NEW, 1)
    if EN_OLD in t:
        t = t.replace(EN_OLD, EN_NEW, 1)
    # 路径过时了
    t = t.replace("`common/.../core/Lang.java`", "`engine/main/java/dev/oba/core/Lang.java`")
    if t != orig:
        p.write_text(t, encoding="utf-8", newline="")
        print("更新", p.relative_to(R))
