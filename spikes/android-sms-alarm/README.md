# Android-SMS-Alarm: Machbarkeitsprototyp

Arbeitsstand zu [Issue #2](https://github.com/Snadretto/SMS-Alert/issues/2).
Dies ist ein isolierter nativer Kotlin-Prototyp, noch keine freigegebene Pikett-App.
Die spätere Produktoberfläche bleibt wie geplant Flutter; siehe
[Umsetzungsplan](../../docs/PLAN.md).

## Voraussetzungen und Build

- JDK 17 oder kompatibles neueres JDK.
- Android SDK Platform 37.0, Build Tools 36.0.0 und Platform Tools.
- `ANDROID_HOME` auf das SDK-Verzeichnis setzen, alternativ `local.properties`
  mit `sdk.dir=...` anlegen. Diese lokale Datei ist nicht versioniert.
- Android Gradle Plugin 9.4.0 und Gradle 9.6.0 sind festgelegt. Der Wrapper prüft
  den Gradle-Download anhand der hinterlegten SHA-256-Prüfsumme.
- Der Prototyp benötigt Android 12 / API 31 oder neuer und hat Ziel-API 37.
  Das ist noch keine Entscheidung über die Mindestversion des späteren Produkts.

```bash
./gradlew testDebugUnitTest assembleDebug assembleRelease lint
```

Beim ersten Build werden Werkzeuge und Abhängigkeiten heruntergeladen. Die
installierte App selbst hat keine Internetberechtigung oder Netzwerk-SDKs.

Installierbare Test-APK: `app/build/outputs/apk/debug/app-debug.apk`.
Der Release-Build `app/build/outputs/apk/release/app-release-unsigned.apk`
ist für die Artefaktprüfung bestimmt und noch nicht für eine Verteilung signiert.
Keine Signierschlüssel ins Repository aufnehmen.

```bash
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

## Bedienung auf einem Testgerät

1. App öffnen, SMS- und Benachrichtigungsrechte erteilen.
2. Exakten Absender und/oder Textbestandteil eingeben. Sind beide Felder gefüllt,
   müssen beide passen. Der Textvergleich ignoriert Groß-/Kleinschreibung;
   Absender werden im Prototyp nicht in andere Nummernformate umgerechnet.
3. Regel speichern und ausdrücklich aktivieren. Die App ist zunächst pausiert.
4. Systemeinstellungen für Alarmlautstärke, DND/zulässige Alarme, Alarmkanal und
   Vollbildzugriff prüfen. Der Zugriff allein garantiert keine hörbare Ausgabe.
   Unter Android 17 zusätzlich „Wecker und Erinnerungen“ über die erklärte
   Alarm-Audio-Schaltfläche freigeben; diese Freigabe erlaubt die dokumentierte
   Ausnahme für Alarm-Audio im Hintergrund, auch ohne zeitgesteuerten Alarm.
5. Lokalen Testalarm bewusst auslösen und quittieren. Danach echte Test-SMS
   entsprechend dem [Gerätetestplan](../../docs/A01-ANDROID-SPIKE.md) senden.
6. „App pausieren“ beendet einen laufenden Alarm und unterdrückt neue Treffer,
   bis die App ausdrücklich reaktiviert wird.

Ton und Vibration werden vom nativen Service gesteuert. Es gibt keinen geplanten
Alarm-Timeout. Die Anzeige von `playback_started_not_audibility_verified` belegt
einen erfolgreichen Player-Start, nicht die tatsächlich hörbare Ausgabe.

## Umfang und Grenzen

- Eine lokale Regel, eine Alarm-Session mit Trefferzähler und eine Quittierung
  dieser Session. Warteschlange und Einzelquittierung mehrerer unabhängiger
  Ereignisse gehören zur späteren Produktumsetzung.
- Keine Standard-SMS-App, kein historischer Postfachzugriff, keine ausgehenden
  SMS. Mehrere Teile derselben gelieferten SMS werden zusammengefügt.
- Vollbild nur mit Systemfreigabe; die Quittieraktion bleibt in der
  Benachrichtigung verfügbar. Die Alarmansicht zeigt keine SMS-Inhalte oder Regeln.
- Kein Umschalten des globalen DND-Zustands und keine automatische Änderung der
  Systemlautstärke. Lautstärke null und blockierende Systemregeln sind Testfälle,
  keine bereits gelösten Ausnahmen.
- Keine Unterstützung vor der ersten Entsperrung nach Geräte-Neustart.
  Force-Stop ist ein eigener Negativfall, kein normaler kalter Prozess.
- Regeln und Aktivierungszustand liegen im privaten App-Speicher. SMS-Texte werden
  nur während der Auswertung gehalten und nicht ins lokale Diagnoseprotokoll geschrieben.
  Cloud-/Geräte-Backup ist ausgeschlossen; OEM-Verhalten ist noch zu prüfen.
- Der Alarmton ist ein im Projekt erzeugtes WAV-Asset unter der Projektlizenz;
  reproduzierbar mit `python3 tools/generate_tone.py`.

## Zusätzliche Prüfungen

```bash
python3 tools/check_apk.py app/build/outputs/apk/debug/app-debug.apk \
  --aapt2 "$ANDROID_HOME/build-tools/36.0.0/aapt2"
python3 tools/check_apk.py app/build/outputs/apk/release/app-release-unsigned.apk \
  --aapt2 "$ANDROID_HOME/build-tools/36.0.0/aapt2"
```

Der Smoke-Test setzt ausschließlich die App-Daten eines ausdrücklich benannten
Emulators zurück. Er verändert keine realen Telefone und benutzt synthetische SMS
über die Emulator-Konsole:

```bash
python3 tools/emulator_smoke.py --adb "$ANDROID_HOME/platform-tools/adb" \
  --serial emulator-5556 --apk app/build/outputs/apk/debug/app-debug.apk --reset
```

Emulator-Ergebnisse ersetzen keine Mobilfunk-, Hörbarkeits-, Vibrations-,
Samsung-/Pixel- oder DND-Gerätetests. Details und offene Nachweise stehen im
[A01-Bericht](../../docs/A01-ANDROID-SPIKE.md).
