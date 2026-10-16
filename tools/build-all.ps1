# 构建全部四个版本的 jar，并把产物收集到 dist/。
#
# 用法：pwsh tools/build-all.ps1
$ErrorActionPreference = 'Stop'
$root = Split-Path $PSScriptRoot -Parent
$dist = Join-Path $root 'dist'
New-Item -ItemType Directory -Force -Path $dist | Out-Null

# 版本 / 用哪个 JDK / 需不需要镜像 init 脚本 / 要不要 offline
$tracks = @(
    @{ name = '1.21.1-neoforge';        java = 'zulu-21'; mirror = $false; offline = $false },
    @{ name = '1.20.2-1.20.4-neoforge'; java = 'zulu-17'; mirror = $true;  offline = $false },
    @{ name = '1.20.5-1.20.6-neoforge'; java = 'zulu-21'; mirror = $true;  offline = $false },
    @{ name = '1.20.1-forge';           java = 'zulu-17'; mirror = $false; offline = $false }
)

$results = @()
foreach ($t in $tracks) {
    $d = Join-Path $root "versions/$($t.name)"
    $javaHome = "C:\Program Files\Zulu\$($t.java)"
    $env:JAVA_HOME = $javaHome
    $env:GRADLE_OPTS = "-Dorg.gradle.java.home=`"$javaHome`""

    $args = @('-p', $d, 'jar', '--no-daemon', '--console=plain')
    if ($t.mirror) { $args += @('-I', (Join-Path $d 'mirror.init.gradle')) }
    if ($t.offline) { $args += '--offline' }

    Write-Host "════════ $($t.name) ════════"
    # Gradle 偶尔会撞上被排除的端口，重试几次
    $ok = $false
    for ($i = 1; $i -le 4; $i++) {
        $out = & (Join-Path $d 'gradlew.bat') @args 2>&1
        if ($out -match 'BindException') {
            Write-Host "  端口冲突，15 秒后重试（第 $i 次）"
            Start-Sleep -Seconds 15
            continue
        }
        if ($out -match 'BUILD SUCCESSFUL') { $ok = $true }
        else { $out | Select-String -Pattern 'error:|\.java:|What went wrong' | Select-Object -First 8 | ForEach-Object { "  $($_.Line.Trim())" } }
        break
    }

    $jar = Get-ChildItem (Join-Path $d 'build/libs/*.jar') -ErrorAction SilentlyContinue | Select-Object -First 1
    if ($jar) { Copy-Item $jar.FullName $dist -Force }
    $results += [pscustomobject]@{
        版本 = $t.name
        结果 = if ($ok) { 'OK' } else { 'FAIL' }
        产物 = if ($jar) { $jar.Name } else { '' }
        KB = if ($jar) { [int]($jar.Length / 1KB) } else { 0 }
    }
}

Write-Host ""
$results | Format-Table -AutoSize

Write-Host "dist/ 里的产物:"
Get-ChildItem $dist -Filter *.jar | ForEach-Object { "  $($_.Name)  $([int]($_.Length/1KB)) KB" }
