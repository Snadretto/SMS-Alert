# SMS-Alert

SMS-Alert ist eine geplante, lokal arbeitende App für SMS-basierte
Pikett-Alarmierung. Passende Absender oder Nachrichtentexte sollen einen
anhaltenden Alarm mit Ton und Vibration auslösen, bis dieser quittiert wird.
Ein eigener Pausenmodus soll die Alarmierung bis zur manuellen Reaktivierung
abschalten.

## Projektstand

Das Projekt befindet sich in der Machbarkeitsphase. Es gibt noch keine freigegebene
Produktversion. Für [Issue #2](https://github.com/Snadretto/SMS-Alert/issues/2)
entsteht ein isolierter [nativer Android-Prototyp](spikes/android-sms-alarm/README.md).
Der [A01-Bericht](docs/A01-ANDROID-SPIKE.md) unterscheidet technische Prüfungen
von noch offenen Tests auf echten Geräten. Vorgesehen sind
Flutter für die Oberfläche und native Plattformanbindungen für SMS und Alarme.
Die App soll ohne eigene Netzwerkverbindungen, Backend, Tracking oder Cloud-Dienste
arbeiten.

Eine Alarmierung in jedem Gerätezustand ist kein technisch haltbares Versprechen.
Die Unterstützung von Lautlosmodus, Nicht stören und Hintergrundbetrieb muss pro
Systemversion, Berechtigungskonfiguration und Gerät nachgewiesen werden.
iOS ist eine gesonderte Machbarkeitsentscheidung mit regionalen Einschränkungen.

Der [Umsetzungsplan](docs/PLAN.md) beschreibt Anforderungen, Grenzen,
Arbeitspakete und Abnahmekriterien. Die Arbeit wird über
[GitHub-Issues](https://github.com/Snadretto/SMS-Alert/issues) verfolgt.

## Entstehung

Der Programmcode dieses Projekts wird mit GPT-6.1 Sol generiert.
Idee, Konzeption und Design stammen von Snadretto.

## Lizenz

Dieses Projekt steht unter der [Apache License 2.0](LICENSE).
Der Urheberhinweis befindet sich in der Datei [NOTICE](NOTICE).
