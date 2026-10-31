# Bauen und prüfen

Die vollständige App liegt unter `app/`; `spikes/` enthält den früheren
Machbarkeitsprototyp. Linux-Build geprüft, iOS benötigt zusätzlich macOS/Xcode
und hat in dieser Version keine SMS-Alarmanbindung.

## Festgelegte Werkzeuge

`toolchain.lock.json` und `app/pubspec.lock` versionieren die Grundlage:
Flutter 3.47.9 / Dart 3.13.7, Java 17, Gradle 9.6.0, AGP 9.4.0,
Android SDK 37, Build Tools 36.0.0, NDK 28.2.13676358 und CMake 3.22.1.
`minSdk=31` bedeutet Android 12 oder neuer. Eine baubare Version ist noch keine
Freigabe eines bestimmten Telefons. AGP 9 verwendet aktuell die vom Flutter-Plugin
benötigten Legacy-Kompatibilitätsflags; Gradle-10-Migration ist kein Teil dieses Builds.

Flutter unter Linux lässt sich mit geprüftem Archiv installieren:

```bash
python3 tools/install_flutter.py
export PATH="$PWD/.tools/flutter/bin:$PATH"
flutter config --no-analytics
```

Java 17 und Android Command-line Tools installieren; `JAVA_HOME`, `ANDROID_HOME`
und gegebenenfalls `ANDROID_SDK_ROOT` auf die jeweiligen Installationen setzen.
SDK-Lizenzen im Rahmen der Installation akzeptieren. SDK-Komponenten:

```bash
sdkmanager 'platforms;android-37.0' 'build-tools;36.0.0' 'platform-tools' 'cmake;3.22.1'
python3 tools/install_ndk.py
```

Das NDK-Hilfsprogramm prüft das offizielle Google-Archiv gegen dessen Index.
Es umgeht einen beobachteten Entpackfehler der Android CLI 1.1 bei internen
NDK-Symlinks. Bereits installierte passende NDKs bleiben unverändert.

Die hier vorbereitete lokale Umgebung liegt vollständig in `.tools/`.
Für sie vor den folgenden Befehlen zusätzlich setzen:

```bash
export JAVA_HOME="$PWD/.tools/jdk"
export ANDROID_HOME="$PWD/.tools/android-sdk"
export ANDROID_SDK_ROOT="$ANDROID_HOME"
export PUB_CACHE="$PWD/.tools/pub-cache"
export GRADLE_USER_HOME="$PWD/.tools/gradle"
export PATH="$JAVA_HOME/bin:$ANDROID_HOME/platform-tools:$PATH"
```

Build-Werkzeuge laden Abhängigkeiten herunter. Die installierte Test-Release-App
besitzt dagegen keine INTERNET-Berechtigung. Flutter-Debug/Profile-Builds haben
für Entwicklungswerkzeuge diese Berechtigung und sind nicht das auszuliefernde Artefakt.

## Prüfungen

```bash
cd app
flutter pub get --enforce-lockfile
dart format --output=none --set-exit-if-changed lib test
flutter analyze --no-pub
flutter test --no-pub
cd android
./gradlew testDebugUnitTest lintRelease
```

Datenbanktests benötigen einen explizit gewählten Emulator. Sie verwenden eine
eigene Datenbank und betreffen keine echten Pikettdaten:

```bash
ANDROID_SERIAL=emulator-5558 ./gradlew connectedDebugAndroidTest -Ptarget-platform=android-x64
```

Der Gradle-Testrunner kann die Test-App nach dem Lauf deinstallieren. Nur auf
Testgeräten verwenden. Die GitHub-Pipeline führt Analyse, Flutter-/Kotlin-Tests,
Lint und Release-Build samt Manifest-/Signaturprüfung aus. CI-Artefakte verwenden
bewusst ein kurzlebiges Testzertifikat und sind nicht mit dem lokalen Testschlüssel
updatekompatibel. Emulator-/Gerätetests werden separat durchgeführt.

## Signierter Testkandidat

Vom Repo-Verzeichnis:

```bash
python3 tools/build_test_apk.py
```

Das Programm nutzt Umgebungsvariablen oder die vorbereiteten `.tools/`-Pfade.
`FLUTTER_BIN` kann eine andere lokale Flutter-Installation auswählen.
Es erstellt einmalig einen lokalen **Testschlüssel** und geschützte
`signing.properties` unter `.tools/signing/`; weitere Builds benutzen denselben
Schlüssel. Ergebnis: `dist/sms-alert-0.2.0-test.apk` und `dist/SHA256SUMS`.
Der Build ist reproduzierbar ausführbar; binär identische APKs über verschiedene
Hosts/Zeitpunkte werden nicht zugesichert. Ohne explizite Signaturkonfiguration
wird nicht still ein Debug-Schlüssel für einen Release gewählt.

Den Ordner `.tools/signing/` separat sicher aufbewahren. Schlüssel und Passwörter
sind ignoriert und dürfen weder ins Repository noch in GitHub-Kommentare gelangen.
Bei Schlüsselverlust sind bestehende Installationen nicht mit einem neuen
Zertifikat aktualisierbar; eine Deinstallation löscht die lokalen Daten.
Produktionssignierung und Store-Veröffentlichung benötigen einen eigenen Entscheid.

`SMS_ALERT_SIGNING_PROPERTIES` darf eine externe Properties-Datei mit `storeFile`
(absolut), `storePassword`, `keyAlias`, `keyPassword` angeben.
Nur CI kann mit `SMS_ALERT_TEST_SIGNING=true` ausdrücklich das Debug-Testzertifikat
für einen Release-Modus-Build wählen.

## Emulatorprüfung

AOSP-Images für API 31 und 37, ein benannter AVD, Hardwarevirtualisierung und ADB
werden benötigt. Der Smoke-Test verweigert echte Telefone und verlangt ausdrücklich
`--reset`. Er löscht ausschließlich die SMS-Alert-Daten auf dem gewählten Emulator.
`adb root` wird nur für Zustandsauslesen/Prozessabbruch verwendet; Einstellungen
entstehen über die Oberfläche, SMS über die Emulator-Konsole.

```bash
python3 tools/emulator_smoke.py \
  --adb .tools/android-sdk/platform-tools/adb --serial emulator-5556 \
  --apk dist/sms-alert-0.2.0-test.apk --reset --soak-seconds 1800
```

Das Testgerät bleibt ohne Host-Audio. Physische Vibration, Hörbarkeit, echte
Mobilfunkzustellung und OEM-Energieverwaltung sind damit nicht bewiesen.
Eigene Tondateien lassen sich mit `python3 tools/generate_tones.py` regenerieren.

Zusätzliche Emulatorprüfungen liegen in `tools/emulator_lifecycle.py`
(Notification-Aktion, identische neue SMS, Reboot, Update, Rechteentzug) und
`tools/emulator_privacy_security.py` (fremde App-UID und strace während UI/SMS).
Die Privacy-Prüfung erwartet einen über die Oberfläche aktivierten Smoke-Test-Zustand.
Sie legt ein offenes synthetisches Ereignis an; danach kann der Lifecycle-Test starten.
Die getrennte Probe-App lässt sich ohne Flutter so bauen:

```bash
app/android/gradlew -p tests/android-security-probe assembleDebug
```

Sie fordert keine Rechte an und gehört nicht zur ausgelieferten App.
Privilegierte Shell-Broadcasts sind kein Ersatz für eine fremde App-UID.
Die Netzwerkprüfung erfasst Netzwerk- und IOCTL-Aufrufe, damit auch lokale Binder-
Aktivität als Nachweis eines tatsächlich angehängten Tracers sichtbar ist.
Sie beansprucht keine vollständige Abdeckung jedes denkbaren Ausführungspfads.

`tools/emulator_ringtones.py` prüft ab 0.2.0 die echte System-Klingeltonauswahl,
Entwurf/Speichern/Abbruch, private Tonkopie, Testalarm, SMS-Kaltstart und Ersatzton.
Der Test akzeptiert nur explizite Emulator-Seriennummern mit `--reset`. Root liest
Testzustände und entfernt ausschließlich die ausgewählte private Testdatei zur
Nachbildung eines fehlenden Assets. Die Hilfe selbst erteilt keine Berechtigung.
