# Anwenderbericht: OPPO Find X9

Am 31. Oktober 2026 bestätigt Snadretto sinngemäß, dass die Alarmierung nun
komplett mit einem simulierten Pikettalarm aus einem Produktivsystem funktioniert.
Das ist ein extern ausgelöster Testalarm aus dem verwendeten Pikett-System,
kein ausschließlich lokaler App-Selbsttest.

Gerät laut vorangehendem Bericht: OPPO Find X9. Android-/ColorOS-Version,
App-Versionsnummer, SIM, DND-Profil, Wiederholungszahl, gemessene Latenz und
Dauer wurden nicht ausdrücklich angegeben. Zuletzt bereitgestellte APK war
0.1.1; dies ersetzt keine bestätigte Versionsangabe des Testgeräts.

Zuvor blockierte eine eingeschränkte Android-Einstellung die SMS-Freigabe.
Der anschließende erfolgreiche Anwenderbericht bestätigt die grundsätzliche
Nutzbarkeit dieses Ablaufs auf dem genannten Telefon. Er gibt keine universelle
Geräte-/DND-/OEM-Freigabe und schließt die übrige Gerätematrix nicht ab.

Das neu beauftragte Update 0.2.0 ergänzt vorhandene Smartphone-Klingeltöne und
eine hervorgehobene Anleitung für diese Freigabe. Die neuen Funktionen müssen
zum damaligen Berichtsstand auf dem OPPO nach dem Update noch praktisch geprüft werden.

## Ergänzende Anwenderbestätigung vom 1. November 2026

Snadretto bestätigt im anschließenden Gerätetest folgende Ergebnisse auf dem
bereits genannten OPPO Find X9. Die Angaben sind manuelle Anwenderberichte;
exakte Android-/ColorOS-/App-Version, Wiederholungszahlen, Laufzeiten, Latenzen
und vollständige Systemeinstellungen wurden weiterhin nicht mitgeteilt.
Im ergänzenden Bericht nennt der Anwender die Updatefolge 0.1.0 → 0.1.1 → 0.2.0.
Ein APK-Hash beziehungsweise vollständiger Buildcode wurde nicht ausgelesen.

| Prüfung | Bestätigtes Ergebnis |
| --- | --- |
| SMS und Regeln | SMS werden erkannt; die getesteten Regelfilter funktionieren. |
| SIM2-Filter | Bei Auswahl von SIM2 alarmieren nur SMS auf SIM2. |
| Smartphone-Klingeltöne | Zugriff auf die auf dem Smartphone verfügbaren Töne funktioniert. |
| Gerätemodi | Alarmierung funktioniert bei Laut, Vibration, Lautlos und dem getesteten DND-Profil. |
| System-Alarmlautstärke | Vorübergehendes Anheben und anschließendes Absenken funktionieren. |
| Hintergrund und Energiesparmodus | Alarmierung funktioniert im Hintergrund und im getesteten Energiesparmodus. |
| Zwei offene Alarme | Beide Ereignisse müssen quittiert werden. |
| App-Pause | Pausieren funktioniert laut Anwender sauber. |
| Alarm bis zur Quittierung | Die im bisherigen Gebrauch beobachtete Dauer und Hörbarkeit werden als ausreichend akzeptiert. |

### Weitere bestätigte Ergebnisse vom selben Tag

| Prüfung | Bestätigtes Ergebnis |
| --- | --- |
| Offline-Betrieb | Funktioniert laut Anwender im zuvor beschriebenen Test ohne WLAN und mobile Daten bei verfügbarem SMS-Mobilfunk. |
| Sperrbildschirm und Verlauf | Die beschriebenen Sperrbildschirm-/Datenschutzprüfungen und das Leeren des Verlaufs funktionieren. Ein aktiver Alarm während des Leerens wurde nicht gesondert beschrieben. |
| Updates der Regeln | In 0.1.0 erstellte, in 0.1.1 und 0.2.0 geänderte Regeln wurden bei den Updates korrekt übernommen. |
| SMS-Recht entziehen | Die App erkennt den Entzug direkt; SMS lösen dann keine Alarme mehr aus. |
| SMS-Recht wieder erteilen | Nach erneuter Freigabe funktionieren die Alarme wieder. |
| Mehrere passende Regeln | Eine SMS, auf die zwei Regeln passen, erzeugt genau ein Alarmereignis. |
| Identische neue SMS | Zwei separat empfangene identische SMS erzeugen zwei Alarmereignisse. |
| Einfacher Textfilter | Der Textbestandteil `(TEST)` als Regelkriterium erkennt die entsprechende SMS und alarmiert. |
| Quittierung auf Sperrbildschirm | Funktioniert. |
| Pause während Alarm | Funktioniert. |
| Reaktivierung nach pausiertem Empfang | Während Pause empfangene passende SMS lösen nach Aktivierung keine nachträgliche Benachrichtigung aus. |
| Neustart | Die App kehrt laut Anwender in den vorherigen Zustand zurück. Eine getrennte Messreihe für alle Zustände/offenen Ereignisse wurde nicht angegeben. |

### Geänderter Abnahmeumfang

Der Projektverantwortliche verzichtet ausdrücklich auf den separaten physischen
30-Minuten-Daueralarmtest. Dieses bisherige Pflichtkriterium entfällt für die
aktuelle Abnahme. Status: **ENTFALLEN AUF ANWENDERWUNSCH**, nicht bestanden oder
gemessen. Der Alarmcode und sein Verhalten ohne eingebauten Timeout bleiben
unverändert. Frühere Emulator-Dauerläufe bleiben historische Nachweise.

Zusätzlich legt Snadretto den Fokus auf einfache Textkriterien wie `(TEST)`.
Lange beziehungsweise mehrteilige SMS sowie besondere Unicode-/Emoji-Fälle sind
für die aktuelle Abnahme kein Pflichtanwendungsfall. Sie werden als
**ZURÜCKGESTELLT AUF ANWENDERWUNSCH** geführt, nicht als physisch getestet.
Vorhandene Text-/Mehrteilerverarbeitung wird dafür weder entfernt noch geändert;
ein späterer gezielter Ausbau kann als eigener Featureauftrag erfolgen.

### Verbleibende Nachweise

Nicht gesondert bestätigt sind insbesondere Telefonate, Bluetooth/Kopfhörer,
Lautstärkeänderungen durch den Nutzer während eines Alarms, Quittierung über die
Benachrichtigungsaktion, SIM1/eSIM und Entzug der Benachrichtigungsberechtigung.
Als technische Restnachweise verbleiben Kaltprozess/Doze, SMS-/Pause-Rennen,
Transportduplikate, detaillierte Wiederanläufe mit offenen/quittierten Ereignissen,
Klingelton-/Pausenerhalt beim Update, Speicherbegrenzung, Backup/Transfer und
gemessene Latenzen. Die allgemeine Neustartbestätigung wird hierfür nicht in
eine vollständige Zustandsmatrix umgedeutet. Die hervorgehobene Berechtigungshilfe
wurde nicht ausdrücklich beurteilt. Pixel und Samsung bleiben ungeprüft.

Die bestätigten Teilprüfungen werden in #1, #2, #7, #8 und #10–#14 berücksichtigt.
Sie stellen keine vollständige Geräte- oder Pilotabnahme dar.
