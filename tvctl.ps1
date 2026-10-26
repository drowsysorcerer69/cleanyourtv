# CleanYourTV control script (Windows PowerShell).
# Usage: .\tvctl.ps1 -Ip 192.168.1.34 -Mode install -Apk app-debug.apk
#        .\tvctl.ps1 -Ip 192.168.1.34 -Mode safe | optional | stock-launchers | restore-all
param(
  [Parameter(Mandatory)][string]$Ip,
  [Parameter(Mandatory)][ValidateSet('install','safe','optional','stock-launchers','restore-all')][string]$Mode,
  [string]$Apk
)

if ($Ip -notmatch ':') { $Ip = "${Ip}:5555" }
$here = Split-Path -Parent $MyInvocation.MyCommand.Path
$homeAct = 'com.gathin.cleanyourtv/.MainActivity'
$launcherPkg = 'com.gathin.cleanyourtv'

adb connect $Ip | Out-Null

function Get-Pkgs($file) {
  Get-Content (Join-Path $here "debloat\$file") |
    ForEach-Object { ($_ -replace '#.*', '').Trim() } |
    Where-Object { $_ }
}
function Remove-Pkgs($file) {
  foreach ($p in Get-Pkgs $file) { Write-Host -NoNewline "$p ... "; adb -s $Ip shell pm uninstall -k --user 0 $p }
}
function Restore-Pkgs($file) {
  foreach ($p in Get-Pkgs $file) { Write-Host -NoNewline "$p ... "; adb -s $Ip shell cmd package install-existing --user 0 $p }
}

switch ($Mode) {
  'install' {
    if (-not (Test-Path $Apk)) { throw "APK not found: $Apk" }
    adb -s $Ip install -r $Apk
    adb -s $Ip shell cmd package set-home-activity $homeAct
  }
  'safe'     { Remove-Pkgs 'safe.txt' }
  'optional' { Remove-Pkgs 'optional.txt' }
  'stock-launchers' {
    $installed = adb -s $Ip shell pm list packages $launcherPkg
    if (-not ($installed -match $launcherPkg)) { throw 'Install the CleanYourTV launcher first.' }
    adb -s $Ip shell cmd package set-home-activity $homeAct
    Remove-Pkgs 'stock-launchers.txt'
  }
  'restore-all' { 'safe.txt','optional.txt','stock-launchers.txt' | ForEach-Object { Restore-Pkgs $_ } }
}
