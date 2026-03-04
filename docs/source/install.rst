Downloads
=========

Release page: `Open release page on GitHub <release_page_>`_

Windows
-------

* Installer (recommended, x64, ``.exe``):
  `Download installer <windows_installer_>`_
* Portable archive (x64, ``.zip``):
  `Download portable archive <windows_portable_>`_
* Standalone JAR (x64):
  `Download standalone JAR <windows_standalone_>`_

macOS
-----

* Installer for Apple Silicon (arm64, ``.dmg``):
  `Download installer <macos_installer_arm64_>`_
* Installer for Intel (x64, ``.dmg``):
  `Download installer <macos_installer_x64_>`_
* Standalone JAR for Apple Silicon (arm64):
  `Download standalone JAR <macos_standalone_arm64_>`_
* Standalone JAR for Intel (x64):
  `Download standalone JAR <macos_standalone_x64_>`_

Linux
-----

* Flatpak (recommended):
  `Open Flathub page <flathub_page_>`_
* App archive (x64, ``.tar.gz``):
  `Download archive <linux_archive_x64_>`_
* App archive (arm64, ``.tar.gz``):
  `Download archive <linux_archive_arm64_>`_
* Standalone JAR (x64):
  `Download standalone JAR <linux_standalone_x64_>`_
* Standalone JAR (arm64):
  `Download standalone JAR <linux_standalone_arm64_>`_

.. note::

   Standalone JAR files require Java 25 to be installed manually.

Runtime Files
-------------

Corese GUI stores logs and preferences in user-writable OS locations.

Windows
~~~~~~~

* Logs: ``%LOCALAPPDATA%\\Corese GUI\\logs\\``
* Preferences file: ``%APPDATA%\\Corese GUI\\preferences\\preferences.properties``

macOS
~~~~~

* Logs: ``~/Library/Logs/Corese GUI/``
* Preferences file: ``~/Library/Application Support/Corese GUI/preferences/preferences.properties``

Linux
~~~~~

* Logs: ``$XDG_STATE_HOME/corese-gui/logs/`` (fallback: ``~/.local/state/corese-gui/logs/``)
* Preferences file: ``$XDG_CONFIG_HOME/corese-gui/preferences/preferences.properties``
  (fallback: ``~/.config/corese-gui/preferences/preferences.properties``)
* ``$XDG_STATE_HOME`` / ``$XDG_CONFIG_HOME`` are environment variables, not literal folder names.
* On Linux, Corese GUI does not use ``~/.corese-gui``.
