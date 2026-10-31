# 启动客户端、进世界、在聊天栏敲一条命令，然后把窗口截下来。
# 用来肉眼确认游戏内界面到底长什么样，平时不用跑。
#
# 参数：
#   -VersionDir  实例目录
#   -World       存档目录名，给了就用 --quickPlaySingleplayer 直接进
#   -Command     要在聊天栏敲的命令
#   -Out         截图存哪
param(
    [Parameter(Mandatory = $true)][string]$VersionDir,
    [string]$World = "",
    [string]$Command = "/oba help",
    [string]$JavaExe = "C:\Program Files\Zulu\zulu-21\bin\java.exe",
    [int]$ReadyTimeoutSec = 420,
    [int]$TailSec = 20,
    [string]$Out = "D:\dshgowork\moddoctor\build-v\ui-shot.png",
    # 这台机器上外来的按键一律进不了游戏窗口（见文件末尾的说明），
    # 想让界面自己出来就用存档里的数据包定时发命令，这时把敲键整段跳掉
    [switch]$NoKeys
)

$ErrorActionPreference = 'Stop'
Add-Type -AssemblyName System.Windows.Forms
Add-Type -AssemblyName System.Drawing

# 游戏自己截的图落在实例的 screenshots 下，先记下已有的，跑完好挑出新的
$shotDir = Join-Path $VersionDir 'screenshots'
$shotsBefore = @()
if (Test-Path $shotDir) { $shotsBefore = @(Get-ChildItem $shotDir -Filter *.png | ForEach-Object { $_.Name }) }

Add-Type @"
using System;
using System.Text;
using System.Collections.Generic;
using System.Runtime.InteropServices;
public class Win {
    public delegate bool EnumProc(IntPtr h, IntPtr l);
    [DllImport("user32.dll")] public static extern bool SetForegroundWindow(IntPtr h);
    [DllImport("user32.dll")] public static extern bool ShowWindow(IntPtr h, int c);
    [DllImport("user32.dll")] public static extern bool SetWindowPos(IntPtr h, IntPtr after, int x, int y, int cx, int cy, uint flags);
    [DllImport("user32.dll")] public static extern bool GetWindowRect(IntPtr h, out RECT r);
    [DllImport("user32.dll")] public static extern IntPtr GetForegroundWindow();
    [DllImport("user32.dll")] public static extern bool EnumWindows(EnumProc cb, IntPtr l);
    [DllImport("user32.dll")] public static extern uint GetWindowThreadProcessId(IntPtr h, out uint pid);
    [DllImport("user32.dll", CharSet = CharSet.Unicode)] public static extern int GetWindowText(IntPtr h, StringBuilder s, int n);
    [DllImport("user32.dll")] public static extern bool IsWindowVisible(IntPtr h);
    [DllImport("user32.dll")] public static extern bool SetCursorPos(int x, int y);
    [DllImport("user32.dll")] public static extern void mouse_event(uint flags, int dx, int dy, uint data, IntPtr extra);
    [DllImport("user32.dll")] public static extern bool PostMessage(IntPtr h, uint msg, IntPtr w, IntPtr l);
    [DllImport("kernel32.dll")] public static extern uint GetCurrentThreadId();
    [DllImport("user32.dll")] public static extern IntPtr SetFocus(IntPtr h);
    [DllImport("user32.dll")] public static extern bool AttachThreadInput(uint a, uint b, bool f);
    // 抢键盘焦点。光把窗口抬上来不够：GLFW 认的是自己收到的 WM_SETFOCUS，
    // 它没收到就当窗口是失焦的，往消息队列里塞的按键全都会丢掉。
    // SetFocus 只对本线程的窗口生效，所以先把自己的输入队列挂到前台线程上
    public static void Focus(IntPtr h) {
        IntPtr fg = GetForegroundWindow();
        uint tf = 0;
        GetWindowThreadProcessId(fg, out tf);
        uint me = GetCurrentThreadId();
        AttachThreadInput(me, tf, true);
        SetForegroundWindow(h);
        SetFocus(h);
        AttachThreadInput(me, tf, false);
        PostMessage(h, 0x0006, (IntPtr)1, IntPtr.Zero);      // WM_ACTIVATE / WA_ACTIVE
        PostMessage(h, 0x0007, h, IntPtr.Zero);              // WM_SETFOCUS
        System.Threading.Thread.Sleep(300);
    }
    [DllImport("user32.dll")] public static extern void keybd_event(byte vk, byte scan, uint flags, IntPtr extra);
    [DllImport("user32.dll")] public static extern short VkKeyScan(char c);
    // 往窗口消息队列里塞按键。GLFW 到底认不认这些消息得实测
    public static void Type(IntPtr h, string text) {
        foreach (char c in text) {
            PostMessage(h, 0x0102, (IntPtr)c, (IntPtr)1);
            System.Threading.Thread.Sleep(25);
        }
    }
    public static void Key(IntPtr h, int vk) {
        PostMessage(h, 0x0100, (IntPtr)vk, (IntPtr)0x001E0001);
        System.Threading.Thread.Sleep(40);
        PostMessage(h, 0x0101, (IntPtr)vk, unchecked((IntPtr)(long)0xC01E0001));
        System.Threading.Thread.Sleep(40);
    }
    // 真按键事件。焦点在游戏窗口上时这个才是有保障的那条路
    public static void KeyReal(int vk) {
        keybd_event((byte)vk, 0, 0, IntPtr.Zero);
        System.Threading.Thread.Sleep(40);
        keybd_event((byte)vk, 0, 2, IntPtr.Zero);
        System.Threading.Thread.Sleep(60);
    }
    public static void TypeReal(string text) {
        foreach (char c in text) {
            short packed = VkKeyScan(c);
            if (packed == -1) { continue; }
            byte vk = (byte)(packed & 0xFF);
            bool shift = (packed & 0x100) != 0;
            if (shift) { keybd_event(0x10, 0, 0, IntPtr.Zero); }
            KeyReal(vk);
            if (shift) { keybd_event(0x10, 0, 2, IntPtr.Zero); System.Threading.Thread.Sleep(20); }
        }
    }
    // 提到最上层再点，免得别的窗口盖在上面把点击吃掉
    public static void TopMost(IntPtr h, int x, int y, int w, int ht) {
        SetWindowPos(h, (IntPtr)(-1), x, y, w, ht, 0x0040);
    }
    public static void NotTopMost(IntPtr h) {
        SetWindowPos(h, (IntPtr)(-2), 0, 0, 0, 0, 0x0003);
    }
    [StructLayout(LayoutKind.Sequential)] public struct RECT { public int L, T, R, B; }
    // 用真鼠标点一下标题栏，这是唯一稳的激活手段
    public static void ClickTitle(IntPtr h) {
        RECT r; GetWindowRect(h, out r);
        ClickAt(r.L + (r.R - r.L) / 2, r.T + 12);
    }
    public static void ClickAt(int x, int y) {
        SetCursorPos(x, y);
        System.Threading.Thread.Sleep(250);
        mouse_event(0x0002, 0, 0, 0, IntPtr.Zero);
        System.Threading.Thread.Sleep(80);
        mouse_event(0x0004, 0, 0, 0, IntPtr.Zero);
    }
    // 同一个进程里有好几个窗口（GLFW 的辅助窗口、AWT 的窗口），
    // MainWindowHandle 指到的那个经常不是游戏本体的窗口，所以按标题自己找
    public static IntPtr FindGame(uint pid) {
        IntPtr best = IntPtr.Zero; long bestArea = -1;
        EnumWindows((h, l) => {
            uint p; GetWindowThreadProcessId(h, out p);
            if (p != pid) { return true; }
            var sb = new StringBuilder(512); GetWindowText(h, sb, 512);
            if (sb.ToString().StartsWith("Minecraft")) {
                RECT r; GetWindowRect(h, out r);
                long area = (long)(r.R - r.L) * (r.B - r.T);
                if (area > bestArea) { bestArea = area; best = h; }
            }
            return true;
        }, IntPtr.Zero);
        return best;
    }
}
"@

$mcHome = "D:\hmcl\.minecraft"
$VersionDir = (Resolve-Path $VersionDir).Path

# 失焦就暂停的话，暂停菜单会把键盘全吃掉，聊天栏永远是空的。
# 临时把这条关掉，跑完再还回去。
# 另外把聊天栏往大了调（-NoKeys 时菜单有二十来行，默认露不全），跑完还回去。
# 注意：可见行数跟 guiScale 无关，别去改玩家的界面缩放
$optsPath = Join-Path $VersionDir 'options.txt'
$optsBackup = $null
if (Test-Path $optsPath) {
    $optsBackup = Get-Content $optsPath -Raw
    $optsNew = if ($optsBackup -match '(?m)^pauseOnLostFocus:') {
        $optsBackup -replace '(?m)^pauseOnLostFocus:.*$', 'pauseOnLostFocus:false'
    } else {
        $optsBackup.TrimEnd() + "`npauseOnLostFocus:false`n"
    }
    foreach ($kv in @(@('chatHeightUnfocused', '1.0'))) {
        $key = $kv[0]; $val = $kv[1]
        if (-not $NoKeys) { continue }
        $optsNew = if ($optsNew -match "(?m)^$key`:") {
            $optsNew -replace "(?m)^$key`:.*$", "$key`:$val"
        } else {
            $optsNew.TrimEnd() + "`n$key`:$val`n"
        }
    }
    if ($optsNew -ne $optsBackup) {
        Set-Content -Path $optsPath -Value $optsNew -NoNewline -Encoding utf8
        Write-Host "已关掉失焦暂停、把聊天栏拉满（跑完还原）"
    }
}

$name = Split-Path $VersionDir -Leaf
$jsonPath = Join-Path $VersionDir "$name.json"
if (-not (Test-Path $jsonPath)) {
    $jsonPath = (Get-ChildItem $VersionDir -Filter '*.json' |
        Where-Object { $_.BaseName -notmatch '^(emi|patchouli|usercache|usernamecache)' } |
        Select-Object -First 1).FullName
}
$json = Get-Content $jsonPath -Raw | ConvertFrom-Json

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
$clientJar = Join-Path $VersionDir "$($json.id).jar"
if (Test-Path $clientJar) { $cpParts.Add($clientJar) }
$classpath = ($cpParts | Select-Object -Unique) -join ';'

$natives = Join-Path $VersionDir 'natives-run'
New-Item -ItemType Directory -Force -Path $natives | Out-Null
$assetIndex = if ($json.assetIndex) { $json.assetIndex.id } else { 'legacy' }
$repl = @{
    '${natives_directory}'   = $natives
    '${launcher_name}'       = 'ObaShot'
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
function Fill($s) { foreach ($k in $repl.Keys) { $s = $s.Replace($k, $repl[$k]) }; return $s }

$jvmArgs = New-Object System.Collections.Generic.List[string]
foreach ($a in $json.arguments.jvm) { if ($a -is [string]) { $jvmArgs.Add((Fill $a)) } }
$jvmArgs.Insert(0, '-Xmx3G')
$jvmArgs.Insert(0, '-Dstdout.encoding=UTF-8')
$jvmArgs.Insert(0, '-Doba.lang=zh')
# 让模组自己跑一次 /oba help：这台机器上外部按键进不了游戏窗口。
# 值是命令本身，留空就默认跑 oba help
if ($NoKeys) {
    # 想单独截某条命令时传 -Command（比如 /oba status，输出短，标题框不会被聊天栏切掉）
    $shotCmd = if ($Command) { $Command.TrimStart('/') } else { '' }
    # Start-Process 会把参数数组按空格拼起来，带空格的命令必须自己加引号，
    # 否则 java 只拿到 "-Doba.ui.shot=oba"，后面那个词被当成主类
    $shotArg = if ($shotCmd -match '\s') { '"' + "-Doba.ui.shot=$shotCmd" + '"' } else { "-Doba.ui.shot=$shotCmd" }
    $jvmArgs.Insert(0, $shotArg)
}

$gameArgs = New-Object System.Collections.Generic.List[string]
foreach ($a in $json.arguments.game) { if ($a -is [string]) { $gameArgs.Add((Fill $a)) } }
# 窗口给大一点，聊天栏能看到更多行，字也清楚
$gameArgs.Add('--width'); $gameArgs.Add('1280')
$gameArgs.Add('--height'); $gameArgs.Add('720')
if ($World) { $gameArgs.Add('--quickPlaySingleplayer'); $gameArgs.Add($World) }

$logOut = Join-Path $VersionDir 'oba-shot-stdout.log'
$logErr = Join-Path $VersionDir 'oba-shot-stderr.log'
Remove-Item $logOut, $logErr -Force -ErrorAction SilentlyContinue
Remove-Item (Join-Path $VersionDir 'logs\latest.log') -Force -ErrorAction SilentlyContinue

$all = @($jvmArgs) + @($json.mainClass) + @($gameArgs)
Write-Host "启动 $name （世界：$World）"
$proc = Start-Process -FilePath $JavaExe -ArgumentList $all -WorkingDirectory $VersionDir `
    -RedirectStandardOutput $logOut -RedirectStandardError $logErr -PassThru -NoNewWindow

# 等窗口句柄出来
$h = [IntPtr]::Zero
$deadline = (Get-Date).AddSeconds(120)
while ((Get-Date) -lt $deadline) {
    Start-Sleep -Seconds 3
    $proc.Refresh()
    if ($proc.HasExited) { throw "进程提前退出，看 $logOut" }
    if ($proc.MainWindowHandle -ne [IntPtr]::Zero) { $h = $proc.MainWindowHandle; break }
}
Write-Host "窗口句柄: $h"

# 先等游戏把窗口和资源都准备好，再抢焦点——太早抢会被 GLFW 覆盖掉
$latest = Join-Path $VersionDir 'logs\latest.log'
$deadline = (Get-Date).AddSeconds(90)
while ((Get-Date) -lt $deadline) {
    Start-Sleep -Seconds 5
    if (Test-Path $latest) {
        $logText = Get-Content $latest -Raw -ErrorAction SilentlyContinue
        if ($logText -match 'Sound engine started|Narrator library|Backend library') { break }
    }
}

# 先把游戏窗口抬到最上层。GLFW 窗口的焦点只能靠真鼠标点标题栏拿到，
# 而这一下必须落在游戏自己的窗口上——被别的窗口盖住就点到别人那儿去了。
# 顺便把「失焦暂停」关掉：暂停菜单会吃掉所有按键，这是之前截图里聊天栏一直空着的原因
$game = [Win]::FindGame([uint32]$proc.Id)
if ($game -ne [IntPtr]::Zero) { $h = $game }
[void][Win]::ShowWindow($h, 9)
[Win]::TopMost($h, 20, 20, 1300, 780)
Start-Sleep -Seconds 2
[Win]::ClickTitle($h)
Start-Sleep -Seconds 3
$fg = [Win]::GetForegroundWindow()
$rc = New-Object Win+RECT
[void][Win]::GetWindowRect($h, [ref]$rc)
Write-Host "游戏窗口: $h  前台: $fg  一致: $($fg -eq $h)  位置: $($rc.L),$($rc.T)  大小: $($rc.R - $rc.L)x$($rc.B - $rc.T)"
if ($rc.L -lt -10000 -or ($rc.R - $rc.L) -lt 200) { throw "游戏窗口没恢复出来，截图会失败" }

# 等进世界。难搞的是存档缺数据包时会停在错误界面，得点一下「安全模式」才继续
$deadline = (Get-Date).AddSeconds($ReadyTimeoutSec)
$ready = $false
$saved = $false
while ((Get-Date) -lt $deadline) {
    Start-Sleep -Seconds 5
    $logText = Get-Content $latest -Raw -ErrorAction SilentlyContinue
    if ($logText -match 'Time elapsed|Preparing spawn area: 100') { $ready = $true; break }
    if (-not $saved -and $logText -match 'Failed to load level data or datapacks') {
        $saved = $true
        Write-Host "存档缺数据包，点「安全模式」继续"
        [Win]::ClickAt($rc.L + [int](($rc.R - $rc.L) * 0.51), $rc.T + [int](($rc.B - $rc.T) * 0.85))
    }
}
Write-Host "进世界: $ready （点过安全模式: $saved）"

function Test-Sent([string]$needle) {
    $t = Get-Content $latest -Raw -ErrorAction SilentlyContinue
    return [bool]($t -match [regex]::Escape($needle))
}

# 聊天栏：T 开、敲命令、回车。窗口消息先试，收不到就换真按键
function Grab([string]$path) {
    $rc = New-Object Win+RECT
    [void][Win]::GetWindowRect($h, [ref]$rc)
    $w = $rc.R - $rc.L
    $ht = $rc.B - $rc.T
    if ($w -le 0 -or $ht -le 0) { $rc.L = 0; $rc.T = 0; $w = 1280; $ht = 720 }
    $bmp = New-Object System.Drawing.Bitmap($w, $ht)
    $g = [System.Drawing.Graphics]::FromImage($bmp)
    $g.CopyFromScreen($rc.L, $rc.T, 0, 0, (New-Object System.Drawing.Size($w, $ht)))
    $bmp.Save($path, [System.Drawing.Imaging.ImageFormat]::Png)
    $g.Dispose(); $bmp.Dispose()
    Write-Host "截图: $path  ($w x $ht @ $($rc.L),$($rc.T))"
}

if (-not $NoKeys) {
# 先探一下按键这条路通不通：按 Esc 该弹暂停菜单，弹不出来后面全白搭
[Win]::Focus($h)
[Win]::Key($h, 0x1B)
Start-Sleep -Seconds 3
Grab ([System.IO.Path]::ChangeExtension($Out, ".esc.png"))
[Win]::Key($h, 0x1B)
Start-Sleep -Seconds 2

$needle = ($Command -split '\s+' | Select-Object -Last 1)
$channel = "无"
foreach ($mode in @('post', 'real')) {
    [Win]::Focus($h)
    if ($mode -eq 'post') {
        [Win]::Key($h, 0x54)
        Start-Sleep -Seconds 2
        [Win]::Type($h, $Command)
        [Win]::Key($h, 0x0D)
    } else {
        [Win]::KeyReal(0x54)
        Start-Sleep -Seconds 2
        [Win]::TypeReal($Command)
        [Win]::KeyReal(0x0D)
    }
    Start-Sleep -Seconds 3
    if (Test-Sent $needle) { $channel = $mode; break }
    Write-Host "  $mode 通道没送到，换下一种"
    # 收不到就把可能开着的聊天栏关掉，免得下一次把字打进旧内容里
    [Win]::Key($h, 0x1B)
    Start-Sleep -Seconds 2
}
Write-Host "按键通道: $channel"

# 再把聊天栏打开，截图里能一次看到更多行
[Win]::Focus($h)
if ($channel -eq 'real') { [Win]::KeyReal(0x54) } else { [Win]::Key($h, 0x54) }
Start-Sleep -Seconds 2
}

# 不靠按键那条路：模组自己在进世界 2 秒后跑一次命令，界面自己会冒出来。
# 聊天栏一行字只留 10 秒，所以连拍几张，回头挑最清楚的那张
if ($NoKeys) {
    $rc0 = New-Object Win+RECT
    [void][Win]::GetWindowRect($h, [ref]$rc0)
    [Win]::TopMost($h, $rc0.L, $rc0.T, ($rc0.R - $rc0.L), ($rc0.B - $rc0.T))
    foreach ($suffix in @('.a.png', '.b.png', '.c.png')) {
        Start-Sleep -Seconds 3
        Grab ([System.IO.Path]::ChangeExtension($Out, $suffix))
    }
}

Grab $Out

# 游戏帧缓冲里那几张才是准的（从窗口外面抓屏，窗口被盖住时会拿到过期画面）。
# 拷到 build-v 下一份，方便直接看
if (Test-Path $shotDir) {
    $fresh = @(Get-ChildItem $shotDir -Filter *.png |
        Where-Object { $shotsBefore -notcontains $_.Name } | Sort-Object LastWriteTime)
    $n = 1
    foreach ($f in $fresh) {
        Copy-Item $f.FullName (Join-Path (Split-Path $Out -Parent) "ui-ingame-$n.png") -Force
        $n++
    }
    if ($fresh.Count -gt 0) {
        Write-Host "游戏自己截的图 $($fresh.Count) 张 → $(Join-Path (Split-Path $Out -Parent) 'ui-ingame-*.png')"
    }
}

Start-Sleep -Seconds $TailSec
try { $proc.CloseMainWindow() | Out-Null } catch {}
if (-not $proc.WaitForExit(45000)) { try { $proc.Kill() } catch {} }
[Win]::NotTopMost($h)
# 等进程真的没了再还原：游戏退出那一下自己会回写 options.txt，抢在它前面写会被覆盖
Start-Sleep -Seconds 3
if ($null -ne $optsBackup) { Set-Content -Path $optsPath -Value $optsBackup -NoNewline -Encoding utf8 }
Write-Host "收工"

# 关于按键：这台机器上外来的按键进不了游戏窗口，三条路都试过了。
#   post —— 往窗口消息队列塞 WM_KEYDOWN/WM_CHAR，窗口收下了但 GLFW 不认
#   real —— keybd_event 造真按键，得窗口真拿到键盘焦点才生效
#   Focus —— AttachThreadInput 挂到前台线程再 SetFocus，GetForegroundWindow 也回报是它，
#            可 Esc 照样弹不出暂停菜单（ui-help.esc.png 干的就是这件事，留作证据）
# 结论：别在这条路上耗，让界面自己出来 —— 加 -NoKeys 时给 JVM 塞 -Doba.ui.shot=（见 UiShot），
# 模组进世界 2 秒后自己跑一次 /oba help，脚本连拍几张挑清楚的。
