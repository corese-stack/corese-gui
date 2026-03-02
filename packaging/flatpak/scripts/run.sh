#!/bin/sh
# Prefer portals for open/save dialogs when available.
export GTK_USE_PORTAL=1
export GDK_BACKEND=x11

exec /app/jre/bin/java \
  --enable-native-access=ALL-UNNAMED \
  -splash:/app/share/corese-gui/startup-splash-primer-dark.png \
  -jar /app/bin/corese-gui-standalone.jar \
  "$@"
