"""给各版本目录补齐 build.gradle / gradle.properties / settings.gradle / 包装器。

1.21.1-neoforge 那份是手写的模板，其余三个按它改：
  - NeoForge 两条改版本号和描述文件名
  - Forge 那条换插件、换描述文件名、加 reobf
"""
import pathlib
import shutil

R = pathlib.Path(r"D:\dshgowork\moddoctor")
V = R / "versions"
TPL_NEO = (V / "1.21.1-neoforge" / "build.gradle").read_text(encoding="utf-8")

SETTINGS_NEO = """pluginManagement {
    repositories {
        gradlePluginPortal()
        // maven.neoforged.net 在部分网络下连不上，官方 CDN 镜像内容一致
        maven { url = 'https://neoforged.forgecdn.net/releases' }
    }
}

rootProject.name = 'oba-@@NAME@@'
"""

SETTINGS_FORGE = """pluginManagement {
    repositories {
        gradlePluginPortal()
        // ForgeGradle 只在 Forge 自己的 maven 上
        maven { url = 'https://maven.minecraftforge.net' }
    }
}

rootProject.name = 'oba-@@NAME@@'
"""

PROPS_HEAD = """# Gradle 自身
org.gradle.jvmargs=-Xmx3G

# ===========================================================================
# {title}
# ===========================================================================

"""

PROPS_TAIL = """
# 产物文件名里带上目标版本，免得几个轨道的 jar 分不清
archive_suffix={suffix}

# ---------------------------------------------------------------------------
# 模组元数据
# ---------------------------------------------------------------------------
mod_id=oba
mod_name=One BUG Among Us
mod_abbr=OBA
mod_license=MIT
mod_version=1.0.0
mod_group_id=dev.oba
mod_authors=OBA contributors
mod_description={desc}
"""

NEO_DESC = ("One BUG Among Us (OBA) - a crash and error diagnostician for NeoForge. Watches the log, "
            "the game process and the mod list, then names the exact mod file responsible for a "
            "failure, the class, method and line where it broke, and what every mod was doing in "
            "the moments before it. Also detects MCreator-generated mods and Sodium/Iris forks. "
            "Report only: never modifies, disables or deletes anything.")
FORGE_DESC = NEO_DESC.replace("for NeoForge.", "for Forge.")

TRACKS = [
    dict(
        name="1.20.2-1.20.4-neoforge",
        title="1.20.2 ~ 1.20.4 轨道（NeoForge）",
        suffix="mc1.20.2-1.20.4",
        toml="mods.toml",
        java=17,
        props=f"""minecraft_version=1.20.2
minecraft_version_range=[1.20.2,1.20.5)
neo_version=20.2.93
neo_version_range=[20.2.0,20.5)

# 20.2 的 FancyModLoader 还是 1.x（本机实例是 1.0.16），不是 2.x，写低了会被拒载
loader_version_range=[1,)

parchment_minecraft_version=1.20.2
parchment_mappings_version=2023.12.10
""",
        desc=NEO_DESC,
    ),
    dict(
        name="1.20.5-1.20.6-neoforge",
        title="1.20.5 ~ 1.20.6 轨道（NeoForge）",
        suffix="mc1.20.5-1.20.6",
        toml="neoforge.mods.toml",
        java=21,
        props="""# 编译用 1.20.5，声明覆盖 1.20.5 ~ 1.20.6
minecraft_version=1.20.5
minecraft_version_range=[1.20.5,1.20.7)
neo_version=20.5.21-beta
neo_version_range=[20.5.0,20.7)

# 1.20.5 起 mods.toml 要求加载器版本不低于 3
loader_version_range=[3,)

parchment_minecraft_version=1.20.5
parchment_mappings_version=2024.05.01
""",
        desc=NEO_DESC,
    ),
    dict(
        name="1.20.1-forge",
        title="1.20.1 轨道（Forge）",
        suffix="mc1.20.1-forge",
        toml="mods-1.20.1-forge.toml",
        java=17,
        forge=True,
        props="""minecraft_version=1.20.1
minecraft_version_range=[1.20.1,1.20.2)
forge_version=1.20.1-47.4.26
forge_version_range=[47,)

loader_version_range=[47,)
""",
        desc=FORGE_DESC,
    ),
]

for t in TRACKS:
    d = V / t["name"]
    d.mkdir(parents=True, exist_ok=True)

    # ---- gradle.properties ----
    (d / "gradle.properties").write_text(
        PROPS_HEAD.format(title=t["title"])
        + t["props"].rstrip() + "\n"
        + PROPS_TAIL.format(suffix=t["suffix"], desc=t["desc"]),
        encoding="utf-8", newline="")

    # ---- settings.gradle ----
    tpl = SETTINGS_FORGE if t.get("forge") else SETTINGS_NEO
    (d / "settings.gradle").write_text(tpl.replace("@@NAME@@", t["name"]),
                                       encoding="utf-8", newline="")

    # ---- build.gradle ----
    if t.get("forge"):
        b = (R / "tracks/1.20.1-forge/build.gradle").read_text(encoding="utf-8")
        # 这条轨道已经自包含，源目录就是 src/main，不需要再引仓库根
        b = b.replace("""// 共享源码：引擎、命令行，以及这条轨道的加载器实现
def repoRoot = rootProject.projectDir.parentFile.parentFile

sourceSets {
    main {
        java {
            srcDir new File(repoRoot, 'common/src/main/java')
            srcDir new File(repoRoot, 'src/main/java')
            srcDir new File(repoRoot, 'src/loader/forge')
        }
        resources {
            srcDir new File(repoRoot, 'src/main/resources')
            // 描述文件按加载器分开：这里只要 Forge 那份。
            // 另外两份用的是 NeoForge 的占位符，在 Forge 上展开不了。
            // 注意别把这份的源文件也排掉，下面还要用它
            exclude 'META-INF/neoforge.mods.toml'
            exclude 'META-INF/mods.toml'
        }
    }
}""", """// 源码就在本目录的 src/main 下，这份构建是自包含的。
// 源文件由 tools/sync-versions.ps1 从 engine/ 同步过来
sourceSets {
    main {
        resources {
            // 描述文件源名叫 mods-1.20.1-forge.toml，免得和别的加载器那份撞名
            exclude 'META-INF/mods.toml'
            exclude 'META-INF/neoforge.mods.toml'
        }
    }
}""")
    else:
        b = TPL_NEO
        b = b.replace("1.21.x 轨道的构建脚本（NeoForge）。",
                      f"{t['title'].replace('（NeoForge）', '')} 的构建脚本（NeoForge）。")
        b = b.replace("JavaLanguageVersion.of(21)", f"JavaLanguageVersion.of({t['java']})")
        b = b.replace("['META-INF/neoforge.mods.toml']", f"['META-INF/{t['toml']}']")
        b = b.replace("""// 这个目录是自包含的：源码全在 src/main 下，拷走就能单独构建。
// 源文件由 tools/sync-versions.ps1 从 engine/ 同步过来，别在这里直接改引擎逻辑。""",
                      """// 这个目录是自包含的：源码全在 src/main 下，拷走就能单独构建。
// 源文件由 tools/sync-versions.ps1 从 engine/ 同步过来，别在这里直接改引擎逻辑。""")
    (d / "build.gradle").write_text(b, encoding="utf-8", newline="")

    # ---- 包装器 ----
    for f in ("gradlew", "gradlew.bat"):
        shutil.copy2(R / "gradlew", d / f) if f == "gradlew" else shutil.copy2(R / "gradlew.bat", d / f)
    wdst = d / "gradle/wrapper"
    wdst.mkdir(parents=True, exist_ok=True)
    for f in (R / "gradle/wrapper").iterdir():
        shutil.copy2(f, wdst / f.name)

    print("生成", t["name"])

print("完成")
