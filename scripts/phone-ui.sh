#!/usr/bin/env bash
# Drives Bercail on a phone plugged in over USB, for manual checks after a wave.
# Every touch targets an element found on screen by its exact label, and nothing is touched unless Bercail is in
# front: a blind tap at fixed coordinates can land on another app's row, or on a notification that just arrived.
#
#   source scripts/phone-ui.sh
#   tap_label "Chercher" && tap_label "Réglages de Bercail" && has "Fond d'écran"
#
# ADB may point to adb when it is not on the PATH.
ADB="${ADB:-adb}"
export MSYS_NO_PATHCONV=1

# The screen as Android describes it, one element per line.
dump() {
  "$ADB" shell "uiautomator dump /sdcard/bercail_ui.xml >/dev/null 2>&1; cat /sdcard/bercail_ui.xml; rm /sdcard/bercail_ui.xml" |
    sed 's/<node/\n<node/g'
}

# The activity in front, as package/class.
foreground() {
  "$ADB" shell "dumpsys activity activities" 2>/dev/null | grep -m1 topResumedActivity | sed 's/.*u0 //; s/ .*//'
}

# Every text and description on screen.
labels() {
  dump | grep -oE '(text|content-desc)="[^"]+"' | sed -E 's/^(text|content-desc)=//' | tr '\n' ' '
  echo
}

# "yes" if the text is on screen, "NO" otherwise.
has() {
  if labels | grep -q "$1"; then echo yes; else echo NO; fi
}

back() {
  "$ADB" shell "input keyevent KEYCODE_BACK"
  sleep 2
}

# Touches the middle of the element whose text or description is exactly the label. Refuses to touch anything
# when Bercail is not in front, when the label is absent, or when it resolves to the whole screen.
tap_label() {
  local label="$1" front line bounds
  front=$(foreground)
  case "$front" in
    io.github.bahuauddimitri.bercail*) ;;
    *)
      echo "ABORT: Bercail is not in front ($front)"
      return 1
      ;;
  esac
  line=$(dump | grep -E "(text|content-desc)=\"$label\"" | head -1)
  bounds=$(echo "$line" | grep -oE 'bounds="\[[0-9]+,[0-9]+\]\[[0-9]+,[0-9]+\]"' | head -1 | grep -oE '[0-9]+' | tr '\n' ' ')
  if [ -z "$bounds" ]; then
    echo "ABORT: '$label' not on screen"
    return 1
  fi
  # shellcheck disable=SC2086
  set -- $bounds
  if [ $(($3 - $1)) -gt 1000 ] && [ $(($4 - $2)) -gt 2000 ]; then
    echo "ABORT: '$label' resolved to the whole screen"
    return 1
  fi
  "$ADB" shell "input tap $((($1 + $3) / 2)) $((($2 + $4) / 2))"
  echo "tapped $label at $((($1 + $3) / 2)),$((($2 + $4) / 2))"
  sleep 2
}
