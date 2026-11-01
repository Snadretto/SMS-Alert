# A01: Android-Machbarkeitsprototyp

Stand: 30. Oktober 2026. Zugehörig zu
[Issue #2](https://github.com/Snadretto/SMS-Alert/issues/2);
[Issue #1](https://github.com/Snadretto/SMS-Alert/issues/1) ist die Übersicht.

**Status: in Arbeit. Kein abschließender Go/No-Go und keine Gerätefreigabe.**
Der Prototyp ist als Build-/Testbasis implementiert. Die geforderten echten
Pixel-/Samsung-Tests, Hörbarkeit, physische Vibration und 30 Minuten Daueralarm
sind noch nicht nachgewiesen. Im verfügbaren ADB-Zugriff war kein reales Telefon
angeschlossen.

## Implementierter Untersuchungsaufbau

Quellcode und Build-Anleitung: [Android-Prototyp](../spikes/android-sms-alarm/README.md).

- Eigenständiges Kotlin-Projekt ohne Flutter-Engine und ohne Produktions-SDKs
  von Drittanbietern. Es untersucht den späteren nativen Plattformadapter;
  die Flutter-Produktgrundlage aus Issue #5 wird damit nicht vorweggenommen.
- Android 12+ als Prototypbasis, compileSdk/targetSdk 37, AGP 9.4.0,
  Gradle 9.6.0, Java 17. Diese Versionen sind im Build festgelegt, nicht automatisch
  der endgültige Produkt-Kompatibilitätsumfang.
- Ein manifestbasierter `SMS_RECEIVED`-Receiver, geschützt durch `BROADCAST_SMS`,
  prüft den gespeicherten Aktivierungszustand vor dem Lesen der SMS-Teile.
  Alle gelieferten Teile werden für die wörtliche Regelprüfung zusammengefügt.
- Ein nativer Service spielt einen gebündelten Ton wiederholt mit `USAGE_ALARM`
  und fordert Audiofokus erst nach `startForeground()` an. Vibration wird mit
  Alarm-Audioattributen angefordert. Systemseitige Unterdrückung bleibt möglich.
- Service-Typ `mediaPlayback`: Zweck ist die tatsächliche andauernde Audioausgabe,
  kein Hintergrund-Polling oder Datentransfer. Er läuft nur für eine offene
  Alarm-Session. Es wird kein Start aus `BOOT_COMPLETED` vorgenommen.
- Startverbote, fehlende Benachrichtigungsrechte, Fokusverlust und Player-Fehler
  werden als inhaltsfreie Diagnosecodes erfasst. Ein erfolgreicher Player-Start
  wird bewusst nicht als Beweis für hörbaren Ton bezeichnet.
- Benachrichtigung und separate Alarm-Activity bieten Quittierung mit einer
  Session-ID. Eine veraltete Quittieraktion kann eine neue Session nicht beenden.
  Die eigene Pause stoppt den Service und bleibt lokal gespeichert.
- Keine globalen DND-/Lautstärkeänderungen, keine Umgehung von Firmenrichtlinien,
  gesperrten Gerätecoden oder Force-Stop. Vollbild ist optional und systemkontrolliert.
- Empfangene SMS-Texte werden nicht dauerhaft gespeichert. Das Diagnoseprotokoll
  enthält nur den letzten Ergebniscode, Startlatenz und Quittierzeitpunkt.
  Regelfelder sind dagegen bewusst gespeicherte Nutzereinstellungen.

Die grundsätzliche Zulässigkeit eines Foreground-Service-Typs belegt noch nicht,
dass ein Start aus jedem Hintergrundzustand erlaubt ist. Das SMS-Broadcast-
Privileg wird nicht mit einer universellen Startausnahme gleichgesetzt. Siehe
[Service-Typen](https://developer.android.com/develop/background-work/services/fgs/service-types),
[Hintergrundstart](https://developer.android.com/develop/background-work/services/fgs/restrictions-bg-start)
und [Audiofokus](https://developer.android.com/media/optimize/audio-focus).

### Im Emulator gefundene Android-17-Zusatzbedingung

Der erste Kaltstartversuch zeigte: Der SMS-Broadcast durfte den Service starten,
aber Android 17 verweigerte Audiofokus für den aus dem Hintergrund gestarteten
Service. Der Prototyp hat deshalb die erklärungsbedürftige Systemfreigabe
`SCHEDULE_EXACT_ALARM` ergänzt. Für `USAGE_ALARM` beschreibt Android damit eine
Ausnahme von der zusätzlichen While-in-use-Bedingung. Es werden keine
zeitgesteuerten Alarme angelegt und keine Schutzmechanismen abgeschaltet.
Siehe [Android 17: Background audio hardening](https://developer.android.com/about/versions/17/changes/bg-audio).

Fehlende Freigabe wird als `alarm_audio_access_required` angezeigt. Ein lokaler
Test aus sichtbarer Activity genügt daher ausdrücklich nicht für den
Hintergrundnachweis. Nach veränderter Freigabe einen offenen Test quittieren und
einen neuen Test ausführen. Die Store-/Berechtigungsbewertung in Issue #3 muss
dieses zusätzliche Recht berücksichtigen.

Beim Wiederholen war ohne diese Freigabe auch ein Player-Start möglich. Android
berücksichtigt zusätzliche Lebenszyklus-Ausnahmen; ein entzogener Einzelzugriff
ist deshalb kein universeller Beweis für eine Audiosperre. Der automatisierte
Vergleich erfasst diesen Fall als `OBSERVED`, statt eine sichere Verweigerung oder
verlässliche Freigabe zu behaupten. Die Prüfung mit gewährter Alarmfreigabe und
verweigertem Vollbild bleibt ein eigener positiver Funktionstest.

## Lokale technische Prüfung

Die reproduzierbaren Befehle stehen in der Build-Anleitung. Automatisierte
Prüfungen behandeln:

- Regelgrenzen: leere Regeln, exakter Absender, Text als Literal, Unicode,
  Groß-/Kleinschreibung und UND-Verknüpfung.
- Zustände: pausierter Empfang einschließlich Simulation, Pause bei aktivem
  Alarm, idempotente/veraltete Quittierung und wiederholte echte Alarmereignisse.
- Eine echte SMS während eines Testalarms darf nicht weiter als reine Simulation
  bezeichnet werden. Im Prototyp werden weitere Treffer in einer Session gezählt;
  dies ersetzt noch keine spätere Ereigniswarteschlange.
- Debug-/Release-Build und Android-Lint.
- Prüfung beider gepackter APK-Manifeste: exakt die neun benötigten Rechte,
  kein `INTERNET`, `READ_SMS` oder `SEND_SMS`; Backup deaktiviert; interne
  Komponenten nicht exportiert; SMS-Receiver gesondert geschützt.

Lint darf über neuere Werkzeugversionen informieren. Die Telefonie-Hardware wird
für diesen SMS-Prototyp bewusst als erforderlich deklariert. Solche Hinweise
sind von Fehlern oder bestandenen Gerätetests zu unterscheiden.

### Ausgeführte Prüfungen am 30. Oktober 2026

Der versionierte [automatisierte Testbericht](testing/A01-automated-2026-10-30.json)
enthält APK-Prüfsummen und die einzelnen Ergebnisse.

| Prüfung | Ergebnis |
| --- | --- |
| Debug-APK und unsigned Release-APK | Build erfolgreich |
| Kotlin/JUnit: Regeln und Alarmzustand | 10 Tests, keine Fehler oder übersprungenen Tests |
| Android-Lint | Keine Fehler; 2 dokumentierte Hinweise zu Werkzeugversion und bewusst erforderlicher Telefonie |
| Gepackte Debug-/Release-Manifeste | Beide geprüft; 9 erwartete Rechte, kein Internet-/Postfachlese-/SMS-Senderecht |
| Android-17-AOSP-Emulator | 7 feste Szenarien bestanden, 1 zusätzlicher Audio-Zustandsvergleich als Beobachtung |
| Synthetische SMS, kalter Prozess, Display vorher aus | Foreground-Service und Player-Start; gemessene Startlatenz in diesem Lauf 70 ms |
| Quittierung und Pause | Service beendet; pausierte SMS erzeugte weder Alarm noch Diagnoseänderung |
| Force-Stop | Keine automatische Alarmierung; als Negativfall bestätigt |

Der positive Hintergrundlauf hatte gewährten Alarm-Audio-Zugriff, verweigerten
Vollbildzugriff sowie deaktivierte mobile Daten und WLAN. Es war ein einzelner
Emulatorlauf mit synthetischer SMS, ohne Audioausgabe am Host. 70 ms sind damit
kein Leistungsversprechen. Reale Funkzustellung, hörbarer Ton, physische Vibration,
DND-/Herstellerkombinationen und der 30-Minuten-Dauertest bleiben offen.

## Noch erforderliche echte Gerätetests

Die [CSV-Vorlage](testing/A01-device-matrix.csv) wird pro Gerät kopiert und mit
Modell, Android-Version, Build-/Ziel-API, Rechten und konkreten Systemeinstellungen
gefüllt. Mindestens ein Pixel und ein Samsung sind gemäß Issue #2 erforderlich.
Alle initialen Ergebnisfelder sind `NOT_RUN`.

Für jede positive Kombination:

1. Konfiguration und gewählte Regel dokumentieren; Daten und WLAN ausschalten,
   Mobilfunk/SMS-Empfang aber eingeschaltet lassen.
2. App aktivieren, auf den Startbildschirm wechseln und den App-Prozess ohne
   Force-Stop beenden. Display sperren und eine echte passende SMS senden.
3. Startlatenz ab vollständiger OS-Übergabe, tatsächlich hörbaren Ton und
   tatsächliche Vibration getrennt erfassen. Vorläufiges Latenzziel: höchstens
   5 Sekunden; keine Aussage zur Mobilfunk-Zustellzeit.
4. Ton/Vibration mindestens 30 Minuten aufrechterhalten und anschließend bewusst
   quittieren. Service, Audiofokus und Ressourcen müssen freigegeben werden.
5. Wiederholen für laut, Vibration, lautlos, konkret benannte DND-Regeln, Doze,
   Energiesparen, Telefonat sowie Bluetooth/Kopfhörer. Ausgabegerät protokollieren.
6. Pause während eines aktiven Alarms sowie passende SMS während Pause prüfen.
   Keine Nachholung dieser SMS nach Reaktivierung.

Negativfälle werden getrennt dokumentiert: Force-Stop, verweigerte/entzogene
Rechte, Alarmkanal blockiert, Alarmlautstärke null, unterdrückende DND-Regeln,
kein Mobilfunkempfang und Neustart vor erster Entsperrung. Ein erwarteter
Negativfall ist kein erfolgreicher Alarm-Nachweis. Direkter Bootbetrieb und
weitergehende Wiederanlaufstrategie sind in Issue #11 zu entscheiden.

## Vorläufige Entscheidung

Die Code-/Build-Basis erlaubt praktische Versuche. Der Gesamtentscheid bleibt
offen, bis echte Geräte den gewünschten Betriebsumfang nachweisen. Die absolute
Zusage „in jedem Zustand“ bleibt technisch nicht allgemein erfüllbar; siehe
[Anforderungskonflikt im Plan](PLAN.md#android).

Store-Eignung und Vertriebsweg gehören weiterhin zu
[Issue #3](https://github.com/Snadretto/SMS-Alert/issues/3). Der Prototyp ist kein
Nachweis einer Google-Play-Zulassung und wird nicht als fertige Pikett-Lösung freigegeben.
