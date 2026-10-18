# 把各版本的源码整理成独立文件夹。
#
# 结构：
#   engine/                    引擎的唯一权威副本（所有版本共用这一份源码）
#     main/java                dev.oba.core / dev.oba.loader / dev.oba.mod 的共享部分
#     main/resources           描述文件与图标
#     tools                    自测与运行时验证台
#   src/loader/neoforge/       加载器实现（NeoForge）
#   src/loader/forge/          加载器实现（Forge）
#   versions/<版本号>-<加载器>/ 每个版本一个独立目录，里面是能直接构建的完整源码
#
# 本脚本把 engine/ 和对应的加载器实现同步进各版本目录。
# 引擎只改 engine/ 那一份，然后重跑本脚本，避免四份代码各自漂移。
#
# 用法：pwsh tools/sync-versions.ps1
param(
    [switch]$CheckOnly
)

$ErrorActionPreference = 'Stop'
$root = Split-Path $PSScriptRoot -Parent
$engineJava = Join-Path $root 'engine/main/java'
$engineRes = Join-Path $root 'engine/main/resources'
$loaderRoot = Join-Path $root 'src/loader'
$versionsDir = Join-Path $root 'versions'

# 每条轨道：目录名 / 用哪套加载器实现 / 本版本要哪份描述文件 / 排掉哪几份
$tracks = @(
    @{ name = '1.21.1-neoforge';        loader = 'neoforge'; keep = 'neoforge.mods.toml' },
    @{ name = '1.20.2-1.20.4-neoforge'; loader = 'neoforge'; keep = 'mods.toml' },
    @{ name = '1.20.5-1.20.6-neoforge'; loader = 'neoforge'; keep = 'neoforge.mods.toml' },
    @{ name = '1.20.1-forge';           loader = 'forge';    keep = 'mods-forge.toml' },
    @{ name = '1.20.2-1.20.4-forge';    loader = 'forge';    keep = 'mods-forge.toml' },
    @{ name = '1.20.5-1.20.6-forge';    loader = 'forge';    keep = 'mods-forge.toml' },
    @{ name = '1.21.1-forge';           loader = 'forge';    keep = 'mods-forge.toml' }
)

if (-not (Test-Path $engineJava)) { throw "找不到引擎源码目录: $engineJava" }

$report = @()
foreach ($t in $tracks) {
    $dst = Join-Path $versionsDir $t.name
    $dstJava = Join-Path $dst 'src/main/java'
    $dstRes = Join-Path $dst 'src/main/resources'

    if (-not $CheckOnly) {
        # 每次重建源码目录，避免删过的文件留下残余
        Remove-Item $dstJava -Recurse -Force -ErrorAction SilentlyContinue
        Remove-Item $dstRes -Recurse -Force -ErrorAction SilentlyContinue
        New-Item -ItemType Directory -Force -Path $dstJava, $dstRes | Out-Null

        # 引擎（唯一权威副本在 engine/）
        Copy-Item "$engineJava\*" $dstJava -Recurse -Force
        # 这条轨道的加载器实现
        Copy-Item "$loaderRoot\$($t.loader)\*" $dstJava -Recurse -Force

        # 资源：图标都带上，META-INF 里只留本轨道那一份描述文件
        Get-ChildItem $engineRes -Recurse -File | Where-Object {
            if ($_.Directory.Name -ne 'META-INF') { return $true }
            return $_.Name -eq $t.keep
        } | ForEach-Object {
            $rel = $_.FullName.Substring($engineRes.Length + 1)
            $target = Join-Path $dstRes $rel
            New-Item -ItemType Directory -Force -Path (Split-Path $target) | Out-Null
            Copy-Item $_.FullName $target -Force
        }
    }

    $javaCount = if (Test-Path $dstJava) { (Get-ChildItem $dstJava -Recurse -File).Count } else { 0 }
    $resCount = if (Test-Path $dstRes) { (Get-ChildItem $dstRes -Recurse -File).Count } else { 0 }
    $report += [pscustomobject]@{
        轨道 = $t.name
        加载器 = $t.loader
        源码 = $javaCount
        资源 = $resCount
        描述文件 = $t.keep
    }
}

$report | Format-Table -AutoSize
Write-Host "引擎权威副本: engine/main/java  ($((Get-ChildItem $engineJava -Recurse -File).Count) 个文件)"
Write-Host "改引擎请改那一份，然后重跑本脚本同步到各版本"
