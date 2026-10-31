# 用实例自带的版本 json 拼出启动命令，直接跑客户端。
# 从 HMCL 的 versions/<名字>/ 目录启动，无需复制实例。
#
# 参数：
#   -VersionDir  实例目录（含 <名字>.json 和 <名字>.jar）
#   -JavaExe     用哪个 java
#   -TimeoutSec  到点收工（游戏不会自己退，靠这个收尾）
param(
    [Parameter(Mandatory = $true)][string]$VersionDir,
    [string]$JavaExe = "C:\Program Files\Zulu\zulu-21\bin\java.exe",
    [int]$TimeoutSec = 240
)

$ErrorActionPreference = 'Stop'
$mcHome = "D:\hmcl\.minecraft"
$VersionDir = (Resolve-Path $VersionDir).Path

# 版本 json 就在实例目录下，文件名和目录名一致
$name = Split-Path $VersionDir -Leaf
$jsonPath = Join-Path $VersionDir "$name.json"
if (-not (Test-Path $jsonPath)) {
    $jsonPath = (Get-ChildItem $VersionDir -Filter '*.json' |
        Where-Object { $_.BaseName -notmatch '^(emi|patchouli|usercache|usernamecache)' } |
        Select-Object -First 1).FullName
}
if (-not $jsonPath) { throw "在 $VersionDir 里找不到版本 json" }
$json = Get-Content $jsonPath -Raw | ConvertFrom-Json
Write-Host "版本: $($json.id)  主类: $($json.mainClass)"

# ---- 按操作系统规则筛库 ----
function Test-Allowed($lib) {
    if (-not $lib.rules) { return $true }
    $allow = $false
    foreach ($rule in $lib.rules) {
        $osName = if ($rule.os) { $rule.os.name } else { $null }
        $ok = $true
        if ($osName -and $osName -ne 'windows') { $ok = $false }
        if ($ok) { $allow = ($rule.action -eq 'allow') }
    }
    return $allow
}

$cpParts = New-Object System.Collections.Generic.List[string]
foreach ($lib in $json.libraries) {
    if (-not (Test-Allowed $lib)) { continue }
    if (-not $lib.downloads -or -not $lib.downloads.artifact) { continue }
    $p = Join-Path $mcHome ("libraries\" + ($lib.downloads.artifact.path -replace '/', '\'))
    if (Test-Path $p) { $cpParts.Add($p) }
}
# 客户端本体
$clientJar = Join-Path $VersionDir "$($json.id).jar"
if (Test-Path $clientJar) {
    $cpParts.Add($clientJar)
} else {
    $alt = Join-Path $mcHome "versions\$($json.id)\$($json.id).jar"
    if (Test-Path $alt) { $cpParts.Add($alt) }
}
$classpath = ($cpParts | Select-Object -Unique) -join ';'
if ($cpParts.Count -eq 0) { throw "类路径是空的，检查 $jsonPath 里的 libraries" }

# ---- 占位符 ----
$natives = Join-Path $VersionDir 'natives-run'
New-Item -ItemType Directory -Force -Path $natives | Out-Null
$assetIndex = if ($json.assetIndex) { $json.assetIndex.id } else { 'legacy' }
$repl = @{
    '${natives_directory}'   = $natives
    '${launcher_name}'       = 'ObaTest'
    '${launcher_version}'    = '1.0'
    '${classpath}'           = $classpath
    '${classpath_separator}' = ';'
    '${library_directory}'   = (Join-Path $mcHome 'libraries')
    '${version_name}'        = $json.id
    '${game_directory}'      = $VersionDir
    '${assets_root}'         = (Join-Path $mcHome 'assets')
    '${assets_index_name}'   = $assetIndex
    '${auth_player_name}'    = 'Tester'
    '${auth_uuid}'           = '00000000000000000000000000000000'
    '${auth_access_token}'   = '0'
    '${clientid}'            = '0'
    '${auth_xuid}'           = '0'
    '${user_type}'           = 'msa'
}

$jvmArgs = New-Object System.Collections.Generic.List[string]
foreach ($a in $json.arguments.jvm) {
    if ($a -isnot [string]) { continue }
    $s = $a
    foreach ($k in $repl.Keys) { $s = $s.Replace($k, $repl[$k]) }
    $jvmArgs.Add($s)
}
$jvmArgs.Insert(0, '-Xmx2G')
$jvmArgs.Insert(0, '-Dstdout.encoding=UTF-8')
$jvmArgs.Insert(0, '-Doba.lang=zh')

$gameArgs = New-Object System.Collections.Generic.List[string]
foreach ($a in $json.arguments.game) {
    if ($a -isnot [string]) { continue }
    $s = $a
    foreach ($k in $repl.Keys) { $s = $s.Replace($k, $repl[$k]) }
    $gameArgs.Add($s)
}

$all = @($jvmArgs) + @($json.mainClass) + @($gameArgs)
$logOut = Join-Path $VersionDir 'oba-test-stdout.log'
$logErr = Join-Path $VersionDir 'oba-test-stderr.log'
Remove-Item $logOut, $logErr -Force -ErrorAction SilentlyContinue

Write-Host "类路径条目: $($cpParts.Count)"
Write-Host "assets index: $assetIndex"
$proc = Start-Process -FilePath $JavaExe -ArgumentList $all -WorkingDirectory $VersionDir `
    -RedirectStandardOutput $logOut -RedirectStandardError $logErr -PassThru -NoNewWindow
Write-Host "已启动 pid=$($proc.Id)，最多等 $TimeoutSec 秒"

if (-not $proc.WaitForExit($TimeoutSec * 1000)) {
    # 先请窗口自己关。这一步很关键：Windows 上 Process.Kill() 是
    # TerminateProcess，等于立刻处死，JVM 的关闭钩子根本不会跑 ——
    # 而 OBA 正是靠关闭钩子把报告落盘的（崩在加载阶段时日志里没有痕迹，
    # 只有钩子能补上）。所以先发关闭请求，给它时间走完收尾
    Write-Host "超时，先请求窗口正常关闭"
    try { $proc.CloseMainWindow() | Out-Null } catch {}
    if (-not $proc.WaitForExit(45000)) {
        Write-Host "窗口没响应，强制结束"
        try { $proc.Kill() } catch {}
        Start-Sleep -Seconds 3
    } else {
        Write-Host "已正常退出，关闭钩子有机会执行"
    }
}
Write-Host "退出码: $($proc.ExitCode)"
