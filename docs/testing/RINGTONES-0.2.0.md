# Smartphone-Klingeltöne und Berechtigungshilfe: 0.2.0

Stand: 31. Oktober 2026. APK: `dist/sms-alert-0.2.0-test.apk`, Version `0.2.0+4`.
[Quellstand und SHA-256](artifact-0.2.0.json) ordnen das tatsächlich geprüfte
Artefakt zu. apksigner bestätigt denselben lokalen Test-Signierer wie bei 0.1.1;
eine Update-Installation ist vorgesehen. Die bisherigen Geräte-/Pilotgrenzen bleiben bestehen.

## Verhalten

- „Klingelton vom Smartphone auswählen“ öffnet einen Android-Systemhandler für
  `ACTION_RINGTONE_PICKER`, mit vorhandener Auswahl, TYPE_ALL und ohne Lautlos-/
  dynamischen Standard-Eintrag. Ein fehlender Systempicker wird verständlich gemeldet.
- Der Ton wird auf einem separaten I/O-Executor begrenzt eingelesen, als Audio
  vorbereitet und mit SHA-256-ID privat kopiert. Kein allgemeiner Medien-/Speicherzugriff
  und keine dauerhafte Providerfreigabe erforderlich. Maximum 20 MB; Aufbewahrung
  des gespeicherten Tons plus höchstens einer noch nicht gespeicherten Auswahl.
- Die Auswahl ist zunächst ein Entwurf. Erst „Einstellungen speichern“ übernimmt
  sie; Picker-Abbruch und Verlassen des Editors verändern den gespeicherten Ton nicht.
- Die native Alarmsteuerung verwendet weiterhin USAGE_ALARM, Audiofokus,
  Schleifenwiedergabe, Vibration und bisherige Quittier-/Pausenpfade. Beim Start mit
  fehlender/unlesbarer privater Datei fällt sie auf „Signal“ zurück und zeigt einen
  Audiohinweis. Im Hintergrund wird der ursprüngliche URI nicht erneut geöffnet.
- Die JSON-Einstellungen in SQLite v2 erhalten optional `deviceTone`; bestehende
  Einstellungen aus 0.1.x bleiben gültig. Die Datenextraktionsausschlüsse gelten
  auch für die privaten Tonkopien. Importierte Töne werden nicht im Repo/APK verteilt.
- Die Einrichtung hebt bei fehlendem SMS-Zugriff die Hilfe für eingeschränkte
  Einstellungen hervor. Die Anleitung zeigt ein markiertes Dreipunkt-Menü und
  führt zur App-Info. Der Nutzer bestätigt in Android selbst „Eingeschränkte
  Einstellungen zulassen“ und danach die SMS-Freigabe.
- Die Hervorhebung ist Teil der App-Anleitung, keine Überlagerung einer fremden
  Systemseite. Keine Overlay-, Accessibility-, WRITE_SETTINGS- oder anderen
  zusätzlichen Berechtigungen. Der Hilfebutton erteilt keine Rechte.

## Prüfung

- Flutter-Analyse ohne Befunde, alle 12 Flutter-/Widgettests bestanden.
- 9 JVM-Tests bestanden, einschließlich sicherer Asset-IDs und Ablehnung nichtlokaler URIs.
- 10 SQLite-Instrumentationstests auf Android 17 bestanden, einschließlich privater
  Audiodatei, Wiederöffnung, unveränderter Pause und atomarer Ablehnung fehlender Assets.
- Android-Lint ohne Fehler; bekannte Stil-/Werkzeughinweise bleiben sichtbar.
- Release-Modus-APK für ARMv7, ARM64 und x86_64 gebaut; Manifest und Signatur geprüft.
  Weiterhin exakt elf Systemrechte plus ein privates AndroidX-Signaturrecht,
  insbesondere kein INTERNET und kein READ_MEDIA_AUDIO/READ_EXTERNAL_STORAGE.
- Je neun tatsächliche UI-/Alarmfälle auf [Android 12](ringtones-0.2.0-api31.json)
  und [Android 17](ringtones-0.2.0-api37.json) bestanden: Hilfe öffnet App-Info ohne
  Rechte zu erteilen; Systempicker; Entwurf; Abbruch; private Kopie; nicht gespeicherte
  Änderung; lokale Wiedergabe; SMS-Kaltstart; fehlende Datei mit Ersatzton; Pause.
  Einige Prüfungen sind jeweils in einem gemeinsamen Szenario zusammengefasst.
- Der finale Hilfebereich bleibt innerhalb der sicheren Bildschirmfläche; ein
  Widgettest prüft den Abstand zur System-Navigationsleiste. So liegt der
  App-Info-Button nicht unter den Android-Navigationstasten.

Root dient im Emulator der Zustandsprüfung, dem Prozessende und dem gezielten
Entfernen genau der ausgewählten Testdatei. Es gibt keinen entsprechenden
Produktions-Testzugang. Permission-Grants im Alarm-Test sind ausdrücklich
Test-Setup; die Hilfe selbst erteilt keine. Host-Audio ist deaktiviert: Playerstart
ist kein physischer Hörbarkeits-/Vibrationsnachweis. Die Klingeltonauswahl auf OPPO/
ColorOS muss nach dem Update noch praktisch geprüft werden.

Der [OPPO-Anwenderbericht](OPPO-Find-X9-user-2026-10-31.md) bestätigt separat die
funktionierende Basisalarmierung mit einem Testalarm aus einem Produktivsystem.
Er ersetzt keine vollständige DND-/OEM-/SIM-/Dauertestmatrix.

## Quellen

[Android-Klingeltonpicker](https://developer.android.com/reference/android/media/RingtoneManager#ACTION_RINGTONE_PICKER),
[App-Info-Intent](https://developer.android.com/reference/android/provider/Settings#ACTION_APPLICATION_DETAILS_SETTINGS),
[Googles Anleitung für eingeschränkte Einstellungen](https://support.google.com/android/answer/12623953?hl=de).
