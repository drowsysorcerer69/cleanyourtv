#!/usr/bin/env sh
# CleanYourTV control script (macOS / Linux / Git Bash / WSL).
# Usage: ./tvctl.sh <tv-ip[:port]> <install <apk> | safe | optional | stock-launchers | restore-all>
set -e

DEV="$1"; MODE="$2"; APK="$3"
[ -z "$DEV" ] || [ -z "$MODE" ] && { sed -n '2,4p' "$0"; exit 1; }
case "$DEV" in *:*) ;; *) DEV="$DEV:5555" ;; esac
DIR="$(cd "$(dirname "$0")" && pwd)"
HOME_ACT="com.gathin.cleanyourtv/.MainActivity"
LAUNCHER_PKG="com.gathin.cleanyourtv"

adb connect "$DEV" >/dev/null
sh_tv() { adb -s "$DEV" shell "$@" </dev/null; }

each_pkg() { # file callback
  sed 's/#.*//' "$DIR/debloat/$1" | while read -r pkg; do
    [ -z "$pkg" ] && continue
    printf '%s ... ' "$pkg"
    "$2" "$pkg"
  done
}
do_remove()  { sh_tv pm uninstall -k --user 0 "$1"; }
do_restore() { sh_tv cmd package install-existing --user 0 "$1"; }

case "$MODE" in
  install)
    [ -f "$APK" ] || { echo "APK not found: $APK"; exit 1; }
    adb -s "$DEV" install -r "$APK"
    sh_tv cmd package set-home-activity "$HOME_ACT" || echo "Could not set default Home; press Home on the remote and choose CleanYourTV > Always."
    ;;
  safe)     each_pkg safe.txt do_remove ;;
  optional) each_pkg optional.txt do_remove ;;
  stock-launchers)
    sh_tv pm list packages "$LAUNCHER_PKG" | grep -q "$LAUNCHER_PKG" || { echo "Install the CleanYourTV launcher first."; exit 1; }
    sh_tv cmd package set-home-activity "$HOME_ACT" || true
    each_pkg stock-launchers.txt do_remove
    ;;
  restore-all)
    for f in safe.txt optional.txt stock-launchers.txt; do each_pkg "$f" do_restore; done
    ;;
  *) echo "Unknown mode: $MODE"; exit 1 ;;
esac
