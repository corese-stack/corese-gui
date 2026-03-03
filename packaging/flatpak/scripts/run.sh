#!/bin/sh
# Prefer portals for open/save dialogs when available.
export GTK_USE_PORTAL=1
# Keep JavaFX on X11/XWayland in Flatpak.
export GDK_BACKEND=x11
# JavaFX dead-key handling on Linux may require synchronized IBus commit mode.
export IBUS_ENABLE_SYNC_MODE=1
# Force IBus inside Flatpak; host XIM env often breaks dead-key composition.
export GTK_IM_MODULE=ibus
export QT_IM_MODULE=ibus
export XMODIFIERS=@im=ibus
# IME env is already set in this launcher; avoid Java bootstrap relaunch.
# Remove once LinuxInputMethodBootstrap is deleted after upstream JavaFX fix.
export CORESE_IM_BOOTSTRAP_DISABLE=1

exec /app/jre/bin/java \
  --enable-native-access=ALL-UNNAMED \
  -splash:/app/share/corese-gui/startup-splash-primer-dark.png \
  -jar /app/bin/corese-gui-standalone.jar \
  "$@"
