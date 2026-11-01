# GitHub-Arbeitspakete

Stand: 29. Oktober 2026. Die folgenden Issues wurden angelegt und per API geprüft.

Zentrale Übersicht: [#1 – [Plan] SMS-Alert: Android-MVP, Betriebsgrenzen und bedingte iOS-Option](https://github.com/Snadretto/SMS-Alert/issues/1)

Arbeitsstand 30. Oktober 2026: Die Android-Testversion wird in
[PR #15](https://github.com/Snadretto/SMS-Alert/pull/15) umgesetzt.
Die Implementierung der Pakete A03–A10 sowie Test-APK und Anleitung sind vorhanden;
A02 wählt den direkten APK-Testweg, I01 dokumentiert ein No-Go für unverändertes iOS
inklusive Schweiz. Die ursprüngliche Reihenfolge „erst physischer G0, dann MVP“
wurde auf ausdrücklichen Folgeauftrag geändert. Es gibt weiterhin keine Gerätefreigabe.

[Automatisierte Nachweise](testing/MVP-REPORT.md) und
[Betriebsentscheidungen](IMPLEMENTATION-DECISIONS.md) trennen erledigte
Implementierung von offener Geräte-/Pilotabnahme. In den Issues werden nur
nachgewiesene Aufgaben abgehakt; Hardwaretests bleiben offen.

| Paket | Phase | Priorität | Issue | Abhängigkeiten |
| --- | --- | --- | --- | --- |
| A01 | M0: Machbarkeit | P0 | [#2 – Android-Machbarkeit: echte SMS startet anhaltenden Alarm im Hintergrund](https://github.com/Snadretto/SMS-Alert/issues/2) | — |
| A02 | M0: Machbarkeit | P0 | [#3 – Android-Vertrieb und SMS-Berechtigungen entscheiden](https://github.com/Snadretto/SMS-Alert/issues/3) | — |
| I01 | iOS: eigenständige Entscheidung | P1 | [#4 – Offline-SMS-Alarmierung und EU-Sonderweg auf Machbarkeit prüfen](https://github.com/Snadretto/SMS-Alert/issues/4) | — |
| A03 | M1: Android-MVP | P1 | [#5 – Flutter-Projekt, native Plattformverträge und Build-Grundlage aufsetzen](https://github.com/Snadretto/SMS-Alert/issues/5) | [#2](https://github.com/Snadretto/SMS-Alert/issues/2), [#3](https://github.com/Snadretto/SMS-Alert/issues/3) |
| A04 | M1: Android-MVP | P1 | [#6 – Lokale Regeln, native Auswertung und versionierte Persistenz implementieren](https://github.com/Snadretto/SMS-Alert/issues/6) | [#5](https://github.com/Snadretto/SMS-Alert/issues/5) |
| A05 | M1: Android-MVP | P1 | [#7 – Android-SMS-Empfang mit Mehrteilern, Dual-SIM und Deduplizierung umsetzen](https://github.com/Snadretto/SMS-Alert/issues/7) | [#2](https://github.com/Snadretto/SMS-Alert/issues/2), [#6](https://github.com/Snadretto/SMS-Alert/issues/6) |
| A06 | M1: Android-MVP | P1 | [#8 – Nativen Daueralarm mit Ton, Vibration und robuster Quittierung bauen](https://github.com/Snadretto/SMS-Alert/issues/8) | [#2](https://github.com/Snadretto/SMS-Alert/issues/2), [#6](https://github.com/Snadretto/SMS-Alert/issues/6), [#7](https://github.com/Snadretto/SMS-Alert/issues/7) |
| A07 | M1: Android-MVP | P1 | [#9 – Geführte Einrichtung, Berechtigungsdiagnose und lokalen Testalarm entwickeln](https://github.com/Snadretto/SMS-Alert/issues/9) | [#5](https://github.com/Snadretto/SMS-Alert/issues/5), [#8](https://github.com/Snadretto/SMS-Alert/issues/8) |
| A08 | M1: Android-MVP | P1 | [#10 – Flutter-Oberfläche für Regeln, Alarmanzeige und dauerhafte App-Pause erstellen](https://github.com/Snadretto/SMS-Alert/issues/10) | [#6](https://github.com/Snadretto/SMS-Alert/issues/6), [#8](https://github.com/Snadretto/SMS-Alert/issues/8), [#9](https://github.com/Snadretto/SMS-Alert/issues/9) |
| A09 | M2: Härtung | P1 | [#11 – Lebenszyklus, Geräte-Neustart und Direct-Boot-Verhalten absichern](https://github.com/Snadretto/SMS-Alert/issues/11) | [#7](https://github.com/Snadretto/SMS-Alert/issues/7), [#8](https://github.com/Snadretto/SMS-Alert/issues/8), [#10](https://github.com/Snadretto/SMS-Alert/issues/10) |
| A10 | M2: Härtung | P1 | [#12 – Datenminimierung, lokales Ereignisprotokoll und Netzwerkfreiheit nachweisen](https://github.com/Snadretto/SMS-Alert/issues/12) | [#5](https://github.com/Snadretto/SMS-Alert/issues/5), [#6](https://github.com/Snadretto/SMS-Alert/issues/6), [#7](https://github.com/Snadretto/SMS-Alert/issues/7), [#8](https://github.com/Snadretto/SMS-Alert/issues/8), [#9](https://github.com/Snadretto/SMS-Alert/issues/9), [#10](https://github.com/Snadretto/SMS-Alert/issues/10), [#11](https://github.com/Snadretto/SMS-Alert/issues/11) |
| A11 | M2: Abnahme | P1 | [#13 – Geräte-Matrix, echte SMS und Dauertests als Release-Abnahme durchführen](https://github.com/Snadretto/SMS-Alert/issues/13) | [#9](https://github.com/Snadretto/SMS-Alert/issues/9), [#10](https://github.com/Snadretto/SMS-Alert/issues/10), [#11](https://github.com/Snadretto/SMS-Alert/issues/11), [#12](https://github.com/Snadretto/SMS-Alert/issues/12) |
| A12 | M3: Pilot und Release | P2 | [#14 – Signierte Pilot-APK, Update-Weg und Betriebsanleitung bereitstellen](https://github.com/Snadretto/SMS-Alert/issues/14) | [#3](https://github.com/Snadretto/SMS-Alert/issues/3), [#12](https://github.com/Snadretto/SMS-Alert/issues/12), [#13](https://github.com/Snadretto/SMS-Alert/issues/13) |

Der vollständige lokale [Umsetzungsplan](PLAN.md) erläutert Anforderungen, Plattformgrenzen, Architektur, Phasen und Tests.

P0: frühe Machbarkeits-/Vertriebsentscheidung. P1: MVP, Härtung oder gesonderte iOS-Prüfung. P2: Pilot nach bestandener Abnahme.

## Status der Arbeitspakete

Abgeschlossen: #3 (APK-Vertriebsentscheid), #4 (begründeter iOS-No-Go),
#5 (Flutter-/Build-Grundlage), #6 (Regeln und Persistenz), #9 (Einrichtung;
OPPO-Basisalarmierung inzwischen durch Anwender bestätigt).
Die Implementierung der übrigen Pakete ist ebenfalls vorhanden. #2, #7, #8,
#10, #11, #12, #13 und #14 bleiben für die in ihren Abnahmekriterien geforderten
realen Geräte-, Mobilfunk-, Audio-/DND-/OEM-/Backup- und Pilotprüfungen offen.
Sie werden nicht allein wegen Emulatorerfolgen als freigegeben geschlossen.
#1 bleibt die zentrale Abnahmeübersicht.

Update 0.1.1: #9 ist wegen des Anwenderberichts zur fehlenden SMS-Freigabe wieder
geöffnet. Effektive Freigaben werden geprüft, Ablehnungen erklärt und
App-Berechtigungen direkt angeboten. [Dialogtests und Grenzen](testing/PERMISSIONS-0.1.1.md)
sind dokumentiert; die Bestätigung auf dem konkreten Anwendertelefon steht aus.

Update 0.2.0 (31.10.2026): Smartphone-Klingeltöne und hervorgehobene Anleitung
für eingeschränkte Einstellungen sind umgesetzt. [Nachweise](testing/RINGTONES-0.2.0.md).
Der [OPPO-Anwenderbericht](testing/OPPO-Find-X9-user-2026-10-31.md) bestätigt die
Basisalarmierung mit einem extern ausgelösten Testalarm aus dem Produktivsystem;
die übrige Gerätematrix bleibt offen.

#9 ist mit dem Anwenderbericht und der ergänzten Anleitung wieder abgeschlossen.
Update 01.11.2026: Snadretto bestätigt auf dem OPPO SMS-Erkennung, getestete
Regelfilter, SIM2-Auswahl und Zugriff auf Smartphone-Klingeltöne. Zudem sind
Alarmierung bei Laut/Vibration/Lautlos/DND, Anheben und Absenken der Lautstärke,
Hintergrund-/Energiesparbetrieb, getrennte Quittierung zweier Alarme und App-Pause
bestätigt. Details und bewusst nicht abgeleitete Nachweise stehen im
[Anwenderbericht](testing/OPPO-Find-X9-user-2026-10-31.md).

Der separate physische 30-Minuten-Daueralarmtest entfällt auf ausdrücklichen
Anwenderwunsch als Abnahmekriterium; er wird nicht als bestanden markiert.
Weitere Bestätigung vom selben Tag: Offline-Betrieb, Sperrbildschirm und
Verlaufleeren, Übernahme der Regeln über 0.1.0 → 0.1.1 → 0.2.0,
Erkennung von SMS-Rechteentzug und Wiederherstellung nach Freigabe, ein Ereignis
bei zwei passenden Regeln, zwei Ereignisse bei zwei identischen neuen SMS,
Textkriterium `(TEST)`, Sperrbildschirm-Quittierung, Pause während Alarm,
Reaktivierung ohne Nachholen pausierter SMS und Rückkehr in den vorherigen
Zustand nach Neustart sind bestätigt.

Lange/mehrteilige SMS und besondere Unicode-/Emoji-Fälle werden auf Anwenderwunsch
für die aktuelle Abnahme zurückgestellt; die vorhandene Verarbeitung bleibt.
Zusätzlich bestätigt: Klingelton und Aktivierungs-/Pausenzustand bleiben bei
Updates erhalten; Quittierung über die Benachrichtigung sowie Entzug mit
App-Hinweis und erneute Freigabe des Benachrichtigungsrechts funktionieren.
Die manuelle Lautstärkeänderung während Alarm ist auf Anwenderwunsch keine
Pflichtfunktion und kein verbleibender Pflicht-Gerätetest; der vorhandene Code bleibt.

Offen bleiben insbesondere Telefonat/Audiozubehör, SIM1/eSIM, technische
Lifecycle-/Rennen-/Backup- und Messnachweise sowie Pixel/Samsung und die
abschließende Pilotabnahme.
