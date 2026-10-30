# Android-MVP: Testkandidat 0.1.0

Stand: 30. Oktober 2026. Die Implementierung ist für erste Gerätetests vorbereitet.
Es wurde keine echte Mobilfunk-SMS auf einem physischen Telefon und keine physische
Hörbarkeit/Vibration nachgewiesen. Daher keine Freigabe für produktiven Pikettdienst.

## Prüfstand

- Flutter-Analyse ohne Befunde; acht Dart-/Widgettests bestanden.
- Acht native JVM-Tests für Kriterien, Länder-/Sendernormalisierung, Unicode,
  unbekannte SIM und Transport-Fingerabdrücke bestanden.
- Neun SQLite-Instrumentationstests auf Android 12 bestanden: Transaktionen,
  Queue/Einzelquittierung, Pause, Wiederöffnung, v1→v2-Migration (aktiv/pausiert),
  Aufbewahrung/Löschen und Ereignisdiagnose. Zuvor sieben Tests auf Android 17 bestanden.
- Debug- und Release-Modus-APK mit allen drei Flutter-Android-Architekturen baubar.
  Lokaler Testschlüssel bleibt außerhalb der Versionsverwaltung.
- Gepacktes Release-Manifest: elf begründete Systemrechte plus ein privates
  AndroidX-Signaturrecht; kein INTERNET, READ_SMS, SEND_SMS, Kontakte- oder
  Notification-Listener-Zugriff. Backup deaktiviert, Datenextraktionsausschlüsse
  gebündelt; SMS-Receiver durch BROADCAST_SMS geschützt; andere App-Komponenten
  nicht exportiert. AndroidX ProfileInstallReceiver benötigt das Systemrecht DUMP.
- Release-Signatur erfolgreich mit apksigner geprüft (APK Signature Scheme v2).
- Android-Lint: keine Fehler. 15 Warnungen: gepinnte Werkzeugversion, unnötige Ressourcen-API-Qualifier,
  fehlende monochrome Icon-Variante, bewusst erforderliche Telefonie sowie
  KTX-/Textressourcen-Stilhinweise in der deutschen nativen Sperransicht.
  Keine dieser Meldungen belegt fehlgeschlagene Alarmfunktion. CI wird separat geprüft.

## Emulatornachweise

Die maschinenlesbaren Berichte enthalten die Prüfsumme des tatsächlich geprüften
Artefakts. Sie dürfen nicht einem später neu gebauten APK untergeschoben werden.
Der erste Android-17-Dauerlauf verwendet den Kandidaten vor den Ergänzungen für
Ereignisdiagnose, lokalen Lizenzhinweisen und App-Icon. Der abschließende Smoke-Test
wird auf dem finalen Kandidaten gesondert dokumentiert.

- [Android 17: SMS und Dauerlauf](MVP-emulator-2026-10-30.json)
- [Android 12: finaler Kandidat](MVP-android12-2026-10-30.json)

Laufende Prüfungen sind noch nicht als bestandene Abnahme markiert.
Der Emulator verwendet synthetische SMS, keine Providerzustellung. WLAN und mobile
Daten sind ausgeschaltet; SMS-Telefonie bleibt verfügbar. Host-Audio ist deaktiviert.
Ein gestarteter Player/Foreground-Service beweist keine hörbare Ausgabe.

## Noch durchzuführen

Die [Gerätematrix](MVP-device-matrix.csv) und Issue #13 sammeln echte Android-
Tests. Ausstehend sind mindestens Pixel, Samsung und ein restriktives OEM-Gerät,
reale SMS und Mehrteiler, SIM/eSIM-Kombinationen, hörbarer Ton/physische Vibration,
DND-Profile, Laut/Vibration/lautlos, System-Alarmlautstärke null und optionale
Rücksetzung, Doze, Energiesparen, Anruf, Bluetooth/Kopfhörer, gesicherter Sperrbildschirm,
Neustart/Entsperrung, Rechteentzug, Update und 30 Minuten unter realen Bedingungen.

Je Profil Zahl der Test-SMS, Verluste, Startlatenz ab vollständiger Android-
Übergabe, Signale, Quittierungswege und Energiebedarf erfassen. 5 Sekunden sind
vorläufiges Messziel, keine Mobilfunklatenzgarantie. Ein Fehler blockiert das
betroffene Profil. Die direkte Test-APK ist kein Store-Release.
