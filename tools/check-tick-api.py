"""用真实的 NeoForge jar 验证 TickAdapter 的版本探测。

直接问 jar：两套 tick 事件类各自在不在，然后对照 TickAdapter 里写死的
两个类名，确认它在每个版本上会选哪一套、以及其它用到的 API 是否都在。
"""
import pathlib
import zipfile

PROBE = pathlib.Path(r"D:\dshgowork\moddoctor\build-v\neoforge-probe")

JARS = [
    ("20.2.93", PROBE / "neoforge-20.2.jar"),
    ("20.4.250", PROBE / "neoforge-20.4.250-universal.jar"),
    ("20.6.141", PROBE / "neoforge-20.6.jar"),
    ("21.1.256", pathlib.Path(r"D:\hmcl\.minecraft\libraries\net\neoforged\neoforge\21.1.256\neoforge-21.1.256-universal.jar")),
]

MODERN = "net/neoforged/neoforge/event/tick/ServerTickEvent$Post.class"
LEGACY = "net/neoforged/neoforge/event/TickEvent$ServerTickEvent.class"
CHECKS = {
    "停止": "net/neoforged/neoforge/event/server/ServerStoppingEvent.class",
    "关停": "net/neoforged/neoforge/event/GameShuttingDownEvent.class",
    "登录": "net/neoforged/neoforge/client/event/ClientPlayerNetworkEvent$LoggingIn.class",
    "命令": "net/neoforged/neoforge/event/RegisterCommandsEvent.class",
    "Mod注解": "net/neoforged/fml/common/Mod.class",
    "命令注册": "net/minecraft/commands/Commands.class",
}

rows = []
for label, path in JARS:
    if not path.exists():
        rows.append((label, "-", "-", "找不到 jar", {}))
        continue
    with zipfile.ZipFile(path) as z:
        names = set(z.namelist())
    modern = MODERN in names
    legacy = LEGACY in names
    picked = "新版 Post(一次)" if modern else ("老版 TickEvent(用 END)" if legacy else "都没有")
    rows.append((label, "有" if modern else "-", "有" if legacy else "-", picked,
                 {k: (v in names) for k, v in CHECKS.items()}))

hdr = f"{'版本':<10} {'新版tick':<8} {'老版tick':<8} {'适配器会选':<22} " + " ".join(f"{k:<6}" for k in CHECKS)
print(hdr)
print("-" * 80)
for label, modern, legacy, picked, extra in rows:
    tail = " ".join(f"{'有' if extra.get(k) else '-':<6}" for k in CHECKS) if extra else ""
    print(f"{label:<10} {modern:<8} {legacy:<8} {picked:<22} {tail}")

print()
print("结论:")
print("  20.2 / 20.4 -> TickAdapter 走老版分支，只认 Phase.END")
print("  20.6 / 21.x -> 走新版分支，Post 一次 tick 触发一回")
