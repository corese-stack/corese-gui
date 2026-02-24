#!/bin/sh
# Prefer portals for open/save dialogs when available.
export GTK_USE_PORTAL=1
export GDK_BACKEND=x11

exec /app/jre/bin/java \
  --enable-native-access=ALL-UNNAMED \
  -jar /app/bin/corese-gui-standalone.jar \
  "$@"
