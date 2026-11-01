# Entscheidungen für die Android-Testversion

Stand: 30. Oktober 2026. Die Testversion setzt die Android-Funktionen vor der
manuellen Geräteabnahme um, wie vom Projektinitiator beauftragt. Ein vorhandener
Emulatornachweis ersetzt keine Freigabe für den realen Pikettdienst.

## Umfang und Vertrieb (#3)

Für diese Iteration wird eine direkt installierbare, signierte Test-APK gebaut.
Die App ersetzt nicht die Standard-SMS-App. Es werden weder SMS versendet noch
das bestehende SMS-Postfach oder Kontakte ausgelesen. Eine Google-Play-Zulassung
wird nicht vorausgesetzt. Vor einer Store-Veröffentlichung muss die Eignung für
die eingeschränkten SMS-Rechte separat bestätigt werden; der reine Empfang von
Pikett-SMS ist keine automatisch genehmigte Ausnahme.

Grundlage: [Google-Play-Regeln für SMS-Berechtigungen](https://support.google.com/googleplay/android-developer/answer/10208820?hl=en-GB).

## Produktverhalten

- Mehrere Regeln: exakter normalisierter Absender und/oder wörtlicher Text;
  ein Kriterium genügt. Zwei Kriterien können ausdrücklich mit UND oder ODER
  verbunden werden, Standard ist UND. Zwischen aktiven Regeln gilt ODER.
- Ein optionaler SIM-Filter passt nur, wenn Android die betreffende SIM eindeutig
  meldet. Unbekannte SIM-Zuordnung wird nicht als Treffer einer bestimmten SIM ausgegeben.
- Nationale Nummern werden mit dem gewählten Länderkontext verglichen;
  alphanumerische Absender und Kurzwahlen bleiben eigene Identitäten.
- Jede neue passende SMS wird ein eigenes offenes Alarmereignis. Mehrere passende
  Regeln erzeugen keine mehrfachen Ereignisse für dieselbe Zustellung.
- Exakt gleiche Transportdaten werden kurzfristig anhand eines gesalzenen
  Fingerabdrucks dedupliziert. Es gibt keine pauschale Sperre gleicher Nachrichtentexte.
- Quittierung gilt für das angezeigte Ereignis. Andere offene Ereignisse bleiben
  erhalten. „Alle quittieren“ ist eine gesonderte bewusste Aktion.
- App-Pause beendet laufende Signale, verwirft offene Alarme als bewusst pausiert
  und bleibt bis zur manuellen Reaktivierung bestehen. Während Pause eintreffende
  SMS werden weder ausgewertet noch protokolliert und später nicht nachgeholt.
- Lokale Testalarme sind gekennzeichnet und benutzen dieselbe native Alarmsteuerung.
  Sie bestätigen keine Funkzustellung und keine allgemeine Gerätezuverlässigkeit.

## Audio und Systemeinstellungen

Abnahmeumfang 01.11.2026: Einfache wörtliche SMS-Textkriterien wie `(TEST)`
sind der aktuelle Schwerpunkt. Lange/mehrteilige SMS und besondere Unicode-/
Emoji-Fälle werden auf Anwenderwunsch als spätere Erweiterungs-/Abnahmefälle
zurückgestellt. Die vorhandene Implementierung wird nicht eingeschränkt;
die physischen Spezialfalltests blockieren die aktuelle Abnahme nicht.

Abnahmeänderung 01.11.2026: Snadretto akzeptiert die beobachtete Alarmierung bis
zur Quittierung und verzichtet ausdrücklich auf den separaten physischen
30-Minuten-Daueralarmtest. Dieses Pflichtkriterium entfällt für die aktuelle
Abnahme, ohne als bestanden zu gelten. Der Alarmcode bleibt unverändert.
Bestätigte OPPO-Teilprüfungen und noch offene Nachweise stehen im
[Anwenderbericht](testing/OPPO-Find-X9-user-2026-10-31.md).

Ton und Vibration laufen im nativen Dienst, bis bewusst quittiert oder pausiert
wird. Die Oberfläche unterscheidet Aktivierung, technische Einrichtung und
noch ausstehenden Gerätetest. DND, Rechteentzug, Force-Stop und externe
Geräterichtlinien werden nicht heimlich umgangen.

Eine optionale vorübergehende Anhebung der System-Alarmlautstärke erfolgt nur
nach ausdrücklicher Einstellung in der App. Der vorherige Wert wird nur dann
zurückgesetzt, wenn der Nutzer ihn zwischenzeitlich nicht verändert hat.
Android-17-Alarm-Audio-Zugriff wird gesondert erklärt; Vollbild ist optional.

Abnahmeeingrenzung 01.11.2026: Manuelle Lautstärkeänderung während eines Alarms
ist laut Snadretto keine Pflichtfunktion. Der entsprechende physische Test
entfällt; Alarmierung und anschließende Quittierung bleiben der vorgesehene
Ablauf. Die vorhandene Rücknahme berücksichtigt weiterhin Nutzeränderungen.
Automatische Lautstärkeanhebung und -rücknahme sind bereits durch den Anwender
bestätigt und werden durch diese Eingrenzung nicht aufgegeben.

Grundlage: [Android 17: Hintergrund-Audio](https://developer.android.com/about/versions/17/changes/bg-audio).

## Neustart und Direct Boot (#11)

Die erste Testversion unterstützt SMS-Alarmierung nach der ersten Entsperrung.
Regeln und Ereignisse verbleiben im passwortgebundenen privaten App-Speicher.
Ein zusätzlicher Speicher mit sensiblen Regeln vor der ersten Entsperrung wird
bewusst nicht eingerichtet. Das ist eine ausdrückliche Kompatibilitätsgrenze,
keine versteckte noch zu implementierende Zusage.

Nach Boot wird kein unzulässiger Media-Foreground-Service gestartet. Bereits
offene Ereignisse bleiben sichtbar; ihre Fortsetzung erfolgt über eine bewusste
Aktion oder einen später zulässigen SMS-Alarmstart. Pausierte Apps bleiben pausiert.
Nach manuellem Force-Stop muss die App einmal geöffnet werden. Währenddessen vom
System nicht zugestellte SMS können ohne Postfachzugriff nicht nachgeholt werden.

Grundlagen: [Direct Boot](https://developer.android.com/privacy-and-security/direct-boot),
[Hintergrundstart von Diensten](https://developer.android.com/develop/background-work/services/fgs/restrictions-bg-start).

## Daten und Laufzeit-Netzwerkfreiheit (#12)

Die native Android-Datenbank ist die einzige verbindliche Datenquelle.
Flutter liest und ändert sie über eine versionierte Plattformanbindung.
SMS-Texte und rohe Transportdaten werden nicht gespeichert; der lokale Verlauf
enthält nur nötige Metadaten, Regelbezeichnungen und Quittierungen.
Aufbewahrung und Anzahl abgeschlossener Ereignisse sind begrenzt und einstellbar;
offene Alarme werden durch die Verlaufspflege nicht gelöscht.

Keine Konten, Netzwerk-SDKs, Telemetrie, Online-Schriften, Remote-Konfiguration,
Push- oder Backend-Dienste. Die Test-Release-APK hat keine INTERNET-Berechtigung.
OS-Backup und Datenübertragung werden für sensible App-Daten ausgeschlossen.
Build-Werkzeuge und CI benötigen Downloads; das ist von der installierten App
zu unterscheiden.

## iOS-Entscheidung (#4)

Für die unveränderten Vorgaben einschließlich Schweiz wird derzeit keine
gleichwertige iOS-SMS-Alarm-App zugesichert. Apples TelephonyMessagingKit setzt
eine gewählte Standard-Carrier-Messaging-App voraus und beschränkt die produktive
Nutzung laut Dokumentation auf EU-Accounts mit Gerät in der EU. Ein solcher
Nachrichten-App-Ausbau wäre ein eigener Produktumfang. AlarmKit und Critical
Alerts stellen keinen allgemeinen SMS-Empfänger bereit.

TelephonyMessagingKit ist laut Apples Framework-Metadaten ab iOS 26.0 verfügbar.
Es verlangt das Entitlement `com.apple.developer.carrier-messaging-app` und die
gewählte Rolle als Standard-App für Carrier-Nachrichten. Ob ein SMS-Dienst auf
einer bestimmten Mobilfunkleitung verfügbar ist, wird zur Laufzeit über die
Framework-Session geprüft; es gibt keinen belegten pauschalen Carrier-Support.
Entwicklungsgeräte können mit einem Apple-Provisioning-Profil auch außerhalb
der EU testen. Das hebt die produktive EU-Bedingung nicht auf. Ohne zulässigen
Zugang und reale Apple-Testgeräte bleibt der praktische Nachweis offen.
IdentityLookup filtert bestimmte Nachrichten und ist kein allgemeiner Listener.

Die Flutter-Struktur bleibt für iOS vorbereitet; eine nicht unterstützte native
Funktion wird ausdrücklich als nicht unterstützt gemeldet. Es gibt keinen
erfolgreichen Dummy-Empfang oder eine versteckte Push-Alternative.
Ein praktischer iOS-Nachweis wurde nicht erbracht.

Grundlagen: [Carrier-Messaging-App](https://developer.apple.com/documentation/telephonymessagingkit/creating-a-carrier-messaging-app),
[TelephonyMessagingKit](https://developer.apple.com/documentation/telephonymessagingkit),
[AlarmKit](https://developer.apple.com/documentation/alarmkit),
[Critical Alerts](https://developer.apple.com/documentation/bundleresources/entitlements/com.apple.developer.usernotifications.critical-alerts).
