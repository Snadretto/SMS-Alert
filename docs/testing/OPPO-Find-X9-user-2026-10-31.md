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
0.2.0 ist die zuletzt bereitgestellte APK, keine ausgelesene Versionsbestätigung.

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

### Geänderter Abnahmeumfang

Der Projektverantwortliche verzichtet ausdrücklich auf den separaten physischen
30-Minuten-Daueralarmtest. Dieses bisherige Pflichtkriterium entfällt für die
aktuelle Abnahme. Status: **ENTFALLEN AUF ANWENDERWUNSCH**, nicht bestanden oder
gemessen. Der Alarmcode und sein Verhalten ohne eingebauten Timeout bleiben
unverändert. Frühere Emulator-Dauerläufe bleiben historische Nachweise.

### Verbleibende Nachweise

Nicht aus diesem Bericht ableitbar sind insbesondere mehrere DND-Profile,
Lautstärkeänderungen durch den Nutzer während eines Alarms, eine bestimmte
Kaltprozess-/Doze-/PIN-Sperrkonfiguration, Quittierung über alle Bedienwege,
SMS-/Pause-Rennen, Wiederaktivierung ohne Nachholen, Pause über Neustart/Update,
SIM1-/eSIM-/Mehrteiler-/Transportduplikatfälle, Telefonate und Audiozubehör,
Offline-Betrieb mit abgeschalteten IP-Verbindungen, Rechteentzug, Backup/Transfer
und gemessene Latenzen. Auch die hervorgehobene Berechtigungshilfe wurde nicht
ausdrücklich beurteilt. Pixel und Samsung bleiben ungeprüft.

Die bestätigten Teilprüfungen werden in #1, #2, #7, #8, #10 und #13 berücksichtigt.
Sie stellen keine vollständige Geräte- oder Pilotabnahme dar.
