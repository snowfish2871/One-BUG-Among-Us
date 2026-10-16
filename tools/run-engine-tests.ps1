# 跑引擎自测和运行时验证台。
#
# 这两套测试都在 engine/tools 下，只用纯 JDK 加日志库，
# 编译时对着某个版本目录的 NeoForge 类路径走。
#
# 用法：pwsh tools/run-engine-tests.ps1
param(
    [string]$VersionDir = "1.21.1-neoforge"
)

$ErrorActionPreference = 'Stop'
$root = Split-Path $PSScriptRoot -Parent
$verDir = Join-Path $root "versions/$VersionDir"
if (-not (Test-Path $verDir)) { throw "找不到版本目录: $verDir" }

$javaHome = "C:\Program Files\Zulu\zulu-21"
$javac = Join-Path $javaHome "bin\javac.exe"
$java = Join-Path $javaHome "bin\java.exe"
$out = Join-Path $root "build-v/engine-tests"
New-Item -ItemType Directory -Force -Path $out | Out-Null

# ---- 类路径：让版本目录自己打印一份 ----
$cpFile = Join-Path $out "CP.txt"
$prevJavaHome = $env:JAVA_HOME
$env:JAVA_HOME = $javaHome
$env:GRADLE_OPTS = "-Dorg.gradle.java.home=`"$javaHome`""
$cpInit = Join-Path $root "engine/tools/cp-init.gradle"
if (Test-Path $cpFile) { Remove-Item $cpFile -Force }
$gradleOut = & "$verDir/gradlew.bat" -p $verDir -I $cpInit printClasspaths --no-daemon --console=plain 2>&1
$cpLine = $gradleOut | Where-Object { $_ -match '^COMPILE_CP=' } | Select-Object -First 1
if ($cpLine) {
    Set-Content -Path $cpFile -Value ($cpLine.Substring('COMPILE_CP='.Length)) -Encoding UTF8
} else {
    Write-Host "gradle 没有打印类路径，末尾输出："
    $gradleOut | Select-Object -Last 12 | ForEach-Object { Write-Host "  $_" }
}
$env:JAVA_HOME = $prevJavaHome

if (-not (Test-Path $cpFile)) { throw "拿不到类路径，先确认 $VersionDir 能构建" }
$cp = (Get-Content $cpFile -Raw).Trim()

# ---- 编译 ----
# 版本目录里的源码是完整的（引擎 + 加载器实现），所以编译它一份就够。
# 测试文件再单独加上
$srcs = @(Get-ChildItem (Join-Path $verDir "src/main/java") -Recurse -Filter *.java |
    Select-Object -ExpandProperty FullName)
$srcs += Get-ChildItem (Join-Path $root "engine/tools") -Recurse -Filter *.java |
    Select-Object -ExpandProperty FullName

& $javac -encoding UTF-8 -proc:none -cp $cp -d $out @srcs 2>&1 | Select-Object -First 20
if ($LASTEXITCODE -ne 0) { throw "编译失败" }

# ---- 跑 ----
Write-Host ""
Write-Host "════════ 引擎自测 ════════"
& $java "-Dstdout.encoding=UTF-8" -cp "$out;$cp" dev.oba.tools.SelfTest
$self1 = $LASTEXITCODE

Write-Host ""
Write-Host "════════ 运行时验证台 ════════"
& $java "-Dstdout.encoding=UTF-8" "-Dfile.encoding=UTF-8" -cp "$out;$cp" dev.oba.tools.RuntimeHarness
$self2 = $LASTEXITCODE

Write-Host ""
if ($self1 -eq 0 -and $self2 -eq 0) {
    Write-Host "两套都通过"
} else {
    Write-Host "有失败：自测退出码 $self1，验证台退出码 $self2"
    exit 1
}
