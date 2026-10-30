# SMS-Alert

Lokale SMS-Pikettalarmierung für Android: Passende Absender oder Textbestandteile
starten einen anhaltenden Alarm mit Ton und Vibration bis zur Quittierung.
Eine eigene App-Pause unterdrückt die Alarmierung bis zur manuellen Reaktivierung.

Die **Android-Testversion 0.1.0** ist in Flutter mit nativer Kotlin-Anbindung
implementiert. Sie bietet mehrere Regeln mit UND/ODER und SIM-Filter,
Regelvorschau, Alarmwarteschlange, Einzel-/Gesamtquittierung, dauerhafte Pause,
geführte Einrichtung, lokalen Selbsttest, zwei Alarmtöne und begrenzten Verlauf.
SMS-Empfang und Alarmdienst benötigen keine laufende Flutter-Oberfläche.
Keine Konten, Backend, Tracking, Push oder eigenen Laufzeit-Netzwerkverbindungen;
die signierte Test-Release-APK besitzt keine INTERNET-Berechtigung.

**Stand: Testkandidat, noch keine Freigabe für den Pikettdienst.**
Automatisierte Prüfungen und Emulatorläufe ersetzen keine echte SMS-Zustellung,
hörbaren Ton, physische Vibration oder Hersteller-/DND-Tests auf echten Telefonen.
Android 12 ist die Mindestversion. Alarmierung in jedem beliebigen Gerätezustand
ist keine haltbare Zusage. Vor erster Entsperrung nach Neustart sowie nach
Force-Stop bestehen ausdrückliche Grenzen. iOS ist für die unveränderten
Anforderungen einschließlich Schweiz nicht gleichwertig umsetzbar; der vorbereitete
Flutter-Adapter meldet dies ausdrücklich.

- [Installation, Einrichtung und Bedienung](docs/USER-GUIDE.md)
- [Build, Signierung und Prüfungen](docs/BUILD.md)
- [Automatisierte Nachweise und offene Geräteabnahme](docs/testing/MVP-REPORT.md)
- [Produkt- und Plattformentscheidungen](docs/IMPLEMENTATION-DECISIONS.md)
- [Plan](docs/PLAN.md), [Issue-Übersicht](docs/ISSUES.md), [GitHub-Issues](https://github.com/Snadretto/SMS-Alert/issues)

App-Code: `app/`. Der ältere [Machbarkeitsprototyp](spikes/android-sms-alarm/README.md)
bleibt als nachvollziehbare Untersuchung erhalten. Der lokale signierte Build
entsteht mit `python3 tools/build_test_apk.py` unter `dist/`; Schlüssel und
Build-Artefakte werden nicht ins Git-Repository aufgenommen.

## Entstehung und Lizenz

Der Programmcode dieses Projekts wird mit **GPT-6.1 Sol** generiert.
Idee, Konzeption und Design stammen von **Snadretto**.

[Apache License 2.0](LICENSE), [Urheberhinweis](NOTICE) und
[Hinweise zu Abhängigkeiten und Assets](docs/THIRD-PARTY.md).
