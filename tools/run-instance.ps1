# 用实例自带的 test.json 拼出启动命令，直接跑客户端。
# 参数含义：-GameDir 实例目录，-ExtraMods 额外要塞进 mods 的 jar，-TimeoutSec 到点收工。
param(
    [string]$GameDir = "D:\dshgowork\modtest",
    [string]$JavaExe = "C:\Program Files\Zulu\zulu-21\bin\java.exe",
    [int]$TimeoutSec = 240
)

$ErrorActionPreference = 'Stop'
$mcHome = "D:\hmcl\.minecraft"
$json = Get-Content (Join-Path $GameDir 'test.json') -Raw | ConvertFrom-Json

# ---- 按操作系统规则筛库 ----
function Test-Allowed($lib) {
    if (-not $lib.rules) { return $true }
    $allow = $false
    foreach ($rule in $lib.rules) {
        $osName = if ($rule.os) { $rule.os.name } else { $null }
        $matches = $true
        if ($osName -and $osName -ne 'windows') { $matches = $false }
        if ($matches) { $allow = ($rule.action -eq 'allow') }
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
$clientJar = Join-Path $GameDir "$($json.id).jar"
if (Test-Path $clientJar) { $cpParts.Add($clientJar) }
else {
    $alt = Join-Path $mcHome "versions\1.21.1-NeoForge\1.21.1-NeoForge.jar"
    if (Test-Path $alt) { $cpParts.Add($alt) }
}
$classpath = ($cpParts | Select-Object -Unique) -join ';'

# ---- 占位符 ----
$natives = Join-Path $GameDir 'natives-run'
New-Item -ItemType Directory -Force -Path $natives | Out-Null
$repl = @{
    '${natives_directory}'      = $natives
    '${launcher_name}'          = 'ObaTest'
    '${launcher_version}'       = '1.0'
    '${classpath}'              = $classpath
    '${classpath_separator}'    = ';'
    '${library_directory}'      = (Join-Path $mcHome 'libraries')
    '${version_name}'           = $json.id
    '${game_directory}'         = $GameDir
    '${assets_root}'            = (Join-Path $mcHome 'assets')
    '${assets_index_name}'      = $json.assetIndex.id
    '${auth_player_name}'       = 'Tester'
    '${auth_uuid}'              = '00000000000000000000000000000000'
    '${auth_access_token}'      = '0'
    '${clientid}'               = '0'
    '${auth_xuid}'              = '0'
    '${user_type}'              = 'msa'
}

function Expand-Args($list) {
    $out = New-Object System.Collections.Generic.List[string]
    foreach ($a in $list) {
        if ($a -is [string]) {
            $s = $a
            foreach ($k in $repl.Keys) { $s = $s.Replace($k, $repl[$k]) }
            $out.Add($s)
        }
    }
    # 逗号包一层，免得 PowerShell 把 List 拆成数组返回
    return ,$out
}

$jvmArgs = [System.Collections.Generic.List[string]]::new()
$jvmArgs.AddRange([string[]](Expand-Args $json.arguments.jvm))
$gameArgs = Expand-Args $json.arguments.game

# 加一点诊断用的 JVM 参数
$jvmArgs.Insert(0, '-Doba.lang=zh')
$jvmArgs.Insert(0, '-Dstdout.encoding=UTF-8')
$jvmArgs.Insert(0, '-Xmx2G')

Write-Host "主类: $($json.mainClass)"
Write-Host "类路径条目: $($cpParts.Count)"
Write-Host "natives: $natives"

$logOut = Join-Path $GameDir 'oba-test-stdout.log'
$logErr = Join-Path $GameDir 'oba-test-stderr.log'
Remove-Item $logOut, $logErr -Force -ErrorAction SilentlyContinue

$all = @($jvmArgs) + @($json.mainClass) + @($gameArgs)
$proc = Start-Process -FilePath $JavaExe -ArgumentList $all -WorkingDirectory $GameDir `
    -RedirectStandardOutput $logOut -RedirectStandardError $logErr -PassThru -NoNewWindow
Write-Host "已启动 pid=$($proc.Id)，最多等 $TimeoutSec 秒"

if (-not $proc.WaitForExit($TimeoutSec * 1000)) {
    Write-Host "超时，结束进程"
    try { $proc.Kill() } catch {}
    Start-Sleep -Seconds 3
}
Write-Host "退出码: $($proc.ExitCode)"
