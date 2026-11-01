# Android-Testkandidat 0.1.0: Prüfnachweise

Stand: 30. Oktober 2026. Die Android-Implementierung ist für erste Gerätetests
vorbereitet. Es gibt **keine Freigabe für realen Pikettdienst**. Weder echte
Mobilfunkzustellung noch hörbarer Ton/physische Vibration wurden hier nachgewiesen.

## Finales Artefakt

- Version: `0.1.0+2`, Android 12 / API 31 oder neuer, Ziel-API 37.
- Lokal signierte Release-Modus-APK: `dist/sms-alert-0.1.0-test.apk`.
- SHA-256: `ca4d9caa6bc1f67be619e91fa42f5f01ce8d965e73f0080c51dc672ed4c3efd0`.
- Anwendungsquellstand: `29387e12d4bee592473341f3ec436df7b21a3ab6`.
- [Versionierte Artefaktzuordnung](MVP-artifact.json). `dist/source.json` und
  `dist/SHA256SUMS` stehen zusätzlich neben der APK. Weitere Commits ergänzen
  Testtreiber und Berichte; der Anwendungsquellstand ist eindeutig zugeordnet.

Die APK enthält ARMv7, ARM64 und x86_64. Der persistente lokale Testschlüssel ist
ignoriert und nicht veröffentlicht. apksigner bestätigt eine gültige APK-Signatur
nach v2. CI verwendet ausdrücklich ein anderes, kurzlebiges Testzertifikat;
dieses ist nicht mit lokalen Testinstallationen updatekompatibel.

## Erfolgreiche Prüfungen

| Prüfung | Ergebnis |
| --- | --- |
| Flutter-Analyse | Keine Befunde |
| Flutter-/Widgettests | 8 bestanden, einschließlich Pause-Bestätigung, Einzelquittierung, Regelvalidierung, Unsupported, große Schrift und Fehlermeldungen |
| Kotlin-JVM-Tests | 8 bestanden: Kriterien, Länder-/Sendernormalisierung, Unicode, SIM und Transport-Fingerabdrücke |
| SQLite-Instrumentation auf Android 12 | 9 bestanden: Transaktionen, Queue, Pause, Wiederöffnung, Migration aktiv/pausiert, Aufbewahrung, Löschung, Ereignisdiagnose |
| SQLite-Basis auf Android 17 | 7 Tests des früheren Testumfangs bestanden |
| Release-Build / Manifest / Signatur | Erfolgreich |
| Android-Lint | Keine Fehler; Hinweise zu gepinnter Werkzeugversion, Ressourcen-Qualifiern, monochromem Icon, erforderlicher Telefonie und KTX-/Textressourcen-Stil |
| Android 12: finaler Smoke-Test | 10 Szenarien bestanden |
| Android 17: finaler Smoke-Test | 10 Szenarien bestanden |
| Android 12: erweiterter Lifecycle | 8 Szenarien bestanden |
| Fremd-App-/Netzwerkprüfung | 3 Szenarien bestanden |
| GitHub-CI auf finalem Anwendungsstand | [Erfolgreicher Lauf](https://github.com/Snadretto/SMS-Alert/actions/runs/41075756249); Push-Lauf desselben Stands ebenfalls erfolgreich |

CI prüft frischen Checkout, den gepinnten Flutter-Download, SDK-Setup, Analyse,
Flutter-/JVM-Tests, Lint, signierten Release-Build und das gepackte Manifest.
Emulator-/Geräteprüfungen laufen separat. Die Reports enthalten die Prüfsumme
des tatsächlich untersuchten Artefakts.

## Szenarien und Rohdaten

- [Android 12: finaler Kandidat](MVP-android12-2026-10-30.json)
- [Android 17: finaler Kandidat](MVP-android17-final-2026-10-30.json)
- [Notification, Reboot, Update und Rechteentzug](MVP-lifecycle-2026-10-30.json)
- [Netzwerk-/Fremd-App-Prüfung](MVP-privacy-security-2026-10-30.json),
  [strace-Rohdaten](MVP-privacy-security-2026-10-30.trace)
- [30-Minuten-Android-17-Basislauf](MVP-emulator-2026-10-30.json)

Die finalen Smoke-Tests umfassen Neuinstallation in Pause, Regelanlage über
Flutter, Nichttreffer, lokalen Test mit Quittierung, nativen SMS-Kaltstart bei
zuvor ausgeschaltetem Display, zwei offene Ereignisse mit Einzelquittierung,
Pause ohne Speicherung eingehender SMS, signiertes Update mit Regel-/Pausenerhalt,
Force-Stop als Negativfall und Prüfung auf SMS-Text in App-Speicher/Logcat.
Latenz ist ab nativer Android-Übergabe gemessen, kein Mobilfunklatenzversprechen.

Der erweiterte Android-12-Test bestätigt Notification-Quittierung, neue SMS mit
identischem Text ohne pauschale Textsperre, Gesamtquittierung im Ablauf,
Reboot mit offenem Alarm ohne BOOT→mediaPlayback-Start, Update mit offenem Alarm,
bewusstes Fortsetzen, Pause über Reboot sowie Rechteentzug/erneute Freigabe.
Der Emulator hat keine gesicherte Geräte-PIN; das ist kein PIN-/OEM-Nachweis.

Die getrennte Probe-App besitzt eine andere unprivilegierte UID. Sie erhält
beim geschützten SMS-Broadcast eine SecurityException und kann die private Pause
nicht auslösen. strace beobachtet laufende App-Threads, Rückkehr zur Oberfläche
und eine synthetische SMS: keine AF_INET-/AF_INET6-Socketversuche. Binder-/AF_UNIX-
Verkehr ist lokale Android-IPC. Dieser Beobachtungszeitraum deckt nicht jeden
möglichen Kaltstartpfad ab. Die fehlende INTERNET-Berechtigung begrenzt IP-Zugriff
unabhängig davon.

Der gepackte Release hat elf begründete Systemrechte plus ein privates AndroidX-
Signaturrecht. Kein INTERNET, READ_SMS, SEND_SMS, Kontakte- oder Notification-
Listener-Zugriff. Backup ist deaktiviert, Datenextraktionsausschlüsse sind
gebündelt, der SMS-Receiver ist durch BROADCAST_SMS geschützt. Andere App-Komponenten
sind nicht exportiert; AndroidX ProfileInstallReceiver benötigt das Systemrecht DUMP.

## Dauerlauf und Testgrenzen

Der Android-17-Basislauf erreichte alle 60 Messpunkte über mindestens 30 Minuten
mit aktivem Foreground-Alarm ohne Timeout. Er untersuchte den Kandidaten vor
Ereignisdiagnose, lokalen Lizenzergänzungen, App-Icon und den abschließenden
Korrekturen der Sperransicht/Vollbildsteuerung. Sein Hash ist im Rohbericht
bewahrt und wird ausdrücklich nicht der finalen APK zugeordnet.

Nach den bestandenen Alarm-/Pause-/Updatefällen scheiterte im ursprünglichen
Testtreiber ein ungeeigneter Sicherheits-Assert mit der privilegierten Android-
Shell. Der Rohbericht bewahrt diesen Harness-Fehler als FAIL. Die Schutzwirkung
wurde anschließend mit einer echten getrennten App-UID erfolgreich geprüft;
der korrigierte finale Smoke-Test hat diesen ungeeigneten Assert nicht mehr.

Im erweiterten Test gefundene Bedienprobleme sind korrigiert: unveränderte Texte
in der nativen Sperransicht erzeugen keine ständigen Accessibility-Updates;
Zähleraktualisierungen starten Vollbild nicht erneut, und die aktive Flutter-
Bedienung bleibt im Vordergrund. Der Testtreiber schließt vor App-Aktionen den
nach Notification-Bedienung offenbleibenden Systemvorhang und verwirft veraltete
UI-Hierarchien. Bildschirm-Idle wird im Test-Setup vermieden; der Kaltstartfall
schaltet das Display ausdrücklich aus. Das ist kein Doze-Nachweis.

Alle Emulator-SMS sind synthetisch. WLAN und mobile Daten sind ausgeschaltet;
SMS-Telefonie bleibt verfügbar. Host-Audio ist deaktiviert. Ein gestarteter
Player oder Foreground-Service beweist keine hörbare Ausgabe/physische Vibration.

## Offene reale Abnahme

Die [Gerätematrix](MVP-device-matrix.csv) und die entsprechend markierten Issues
sammeln die verbleibenden Prüfungen. Ausstehend: Pixel, Samsung, restriktives
OEM-Gerät, echte SMS/Mehrteiler, SIM/eSIM, Ton/Vibration, DND-Profile,
Laut/Vibration/lautlos, Systemlautstärke null mit optionaler vorsichtiger
Rücknahme, Doze/Energiesparen, Telefonat, Bluetooth/Kopfhörer, sicherer Sperrbildschirm,
Neustart/Entsperrung, Rechteentzug, Backup/Transfer, Update und realer Daueralarm.

Pro Profil Wiederholungszahl, Verluste, Signale, Startlatenz ab vollständiger
Android-Übergabe, Quittierungswege und Energiebedarf messen. Fünf Sekunden sind
vorläufiges Messziel; ein Fehler blockiert das betroffene Profil. Die direkte
Test-APK ist kein Store-Release. Vor erster Entsperrung nach Reboot und nach
Force-Stop gelten die dokumentierten Betriebsgrenzen; iOS hat keine gleichwertige
freigegebene SMS-Anbindung.
