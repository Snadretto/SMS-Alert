# SMS-Alert 0.2.0: Android-Testversion

Diese Version ist für die ersten abgestimmten Gerätetests vorbereitet.
Noch keine Kombination aus echtem Telefon, Mobilfunk und DND-Profil ist für den
Pikettdienst freigegeben. Die App läuft ab Android 12; iOS zeigt die fehlende
SMS-Alarmunterstützung ausdrücklich an.

## Installation und Update

1. Die signierte Datei `dist/sms-alert-0.2.0-test.apk` auf das Testtelefon kopieren.
   Prüfsumme mit `dist/SHA256SUMS` vergleichen.
2. Die Installation aus der dafür verwendeten Quelle erlauben und die APK öffnen.
   Die App ersetzt nicht die vorhandene SMS-App. Firmenrichtlinien können die
   Installation oder SMS-Rechte verbieten; solche Geräte sind gesondert zu prüfen.
3. Bei späteren Testversionen die APK mit demselben Zertifikat und höherer
   Versionsnummer darüber installieren. Nicht vorher deinstallieren: Das würde
   Regeln und Verlauf löschen. Versionsnummer steht in `app/pubspec.yaml`.

Die App startet bei einer Neuinstallation **pausiert**. Ein Update übernimmt
Regeln, Pause und vorhandene Alarmereignisse; ein Update beendet laufende Prozesse.
Danach die App öffnen, Einrichtung kontrollieren und gegebenenfalls einen noch
offenen Alarm bewusst fortsetzen. Es wird keine SMS nachträglich importiert.

## Erste Einrichtung

1. Unter **Regeln** einen Namen und mindestens einen exakten Absender oder
   wörtlichen Textbestandteil festlegen. Bei zwei Kriterien ist standardmäßig
   **UND** gewählt; **ODER** ist eine eigene Auswahl. Mehrere aktive Regeln
   werden untereinander mit ODER behandelt.
2. Für nationale Nummern unter **Einstellungen** den Länderkontext wählen
   (Standard Schweiz). Internationale Schreibweise `+41…` ist eindeutig.
   Kurzwahlen und alphanumerische Kennungen werden separat und exakt verglichen.
   Text ignoriert standardmäßig Groß-/Kleinschreibung; die Regel kann dies ändern.
3. Falls nötig eine bestimmte SIM auswählen. Ist die SIM-Zuordnung einer SMS
   unbekannt, trifft eine auf SIM 1/2 beschränkte Regel ausdrücklich nicht.
   Die **Vorschau** prüft eine eingegebene Beispielnachricht mit derselben nativen
   Regelprüfung, ohne SMS zu senden oder Alarm auszulösen.
4. Unter **Einrichtung** SMS- und Benachrichtigungsrechte gewähren. Alarmkanal und
   System-Alarmlautstärke prüfen. Android 17 benötigt für verlässliches Hintergrund-
   Alarm-Audio die Freigabe **Wecker und Erinnerungen**; die App plant keine Termine.
5. Für DND die gewünschten **Alarme** im jeweiligen Systemprofil erlauben.
   DND-Zugriff oder ein freigegebener Benachrichtigungskanal allein beweisen keine
   Hörbarkeit. Die App schaltet globales DND nicht aus. Vollbild und eine passende
   herstellerspezifische Energieausnahme sind gesonderte Optionen.
6. Unter **Übersicht** ausdrücklich aktivieren und **Testalarm starten** wählen.
   Ton, Vibration und Quittierung selbst am Gerät prüfen. Anschließend echte
   Test-SMS mit der gewünschten Konfiguration senden lassen.

Die Alarmtonlautstärke in der App ist eine relative Wiedergabelautstärke. Ist die
System-Alarmlautstärke null, kann es trotz hoher App-Lautstärke stumm bleiben.
Optional lässt sich das vorübergehende Anheben der System-Alarmlautstärke aktivieren.
Beim Beenden stellt die App den vorherigen Wert nur wieder her, wenn er noch dem
von ihr gesetzten Wert entspricht und seitdem kein Neustart stattfand.
Zwischenzeitliche Nutzeränderungen werden respektiert. Das ist keine DND-Umgehung.

## Vorhandene Klingeltöne auswählen

Unter **Einstellungen → Klingelton vom Smartphone auswählen** öffnet sich die
Android-Systemauswahl. Dort lassen sich die auf dem Telefon angebotenen
Klingel-, Alarm- und Benachrichtigungstöne wählen. „Lautlos“ ist nicht vorgesehen.
Die verfügbaren Kategorien und Namen hängen vom Hersteller ab.

Nach der Auswahl wird der Name in der App angezeigt. Erst **Einstellungen speichern**
übernimmt den Ton für die Alarmierung. Abbrechen der Systemauswahl behält die
bisherige Auswahl; Verlassen der Einstellungen ohne Speichern ändert den aktiven
Alarmton nicht. „Signal“ und „Puls“ bleiben jederzeit auswählbar.

Der gewählte Ton wird mit höchstens 20 MB in den privaten App-Speicher kopiert.
Es sind keine allgemeinen Medien-/Speicherrechte erforderlich. So hängt die
spätere Alarmierung nicht von einer vorübergehenden Picker-Freigabe oder dem
weiteren Bestehen der ursprünglichen Datei ab. Die Kopie ist wie die übrigen
App-Daten von Backup/Transfer ausgeschlossen und wird bei Deinstallation gelöscht.
Die App hält den gespeicherten Ton und höchstens eine weitere, noch nicht
übernommene Auswahl; ungenutzte Kopien werden bei weiterer Auswahl/Start bereinigt.

Die Wiederholung, Alarmlautstärke, Vibration und Quittierung laufen über denselben
nativen Alarmdienst. Falls die gespeicherte Kopie beim Alarmstart nicht lesbar
ist, verwendet er „Signal“ und zeigt einen Audiohinweis an. Bitte dann erneut
auswählen. Ein danach bewusst gestarteter lokaler Testalarm prüft die tatsächliche
Ausgabe; die Systemvorschau kann eine andere Lautstärke verwenden.

## SMS-Freigabe fehlt weiterhin

Ein Ausrufezeichen bei **SMS empfangen** bedeutet, dass Android der App
`RECEIVE_SMS` aktuell nicht gewährt. Ab Version 0.1.1 prüft die App das wirksame
Ergebnis nach dem Dialog; abgelehnte oder automatisch verweigerte Anfragen
bleiben sichtbar und werden nicht als erfolgreiche Freigabe behandelt.

1. **Einrichtung → SMS empfangen → SMS-Zugriff anfragen** wählen und im Android-
   Dialog erlauben. Die Benachrichtigungsfreigabe ist ein getrenntes Recht.
2. Erscheint kein Dialog oder bleibt das Ausrufezeichen, **App-Berechtigungen öffnen**
   wählen. In Android unter **Berechtigungen → SMS** prüfen und, falls verfügbar,
   den Zugriff erlauben. Nach Rückkehr aktualisiert sich die Anzeige.
3. Meldet Android **„App wurde Zugriff verweigert“** oder eingeschränkte Einstellungen,
   **Hilfe bei eingeschränkten Einstellungen** öffnen. Die Anleitung hebt das
   **Dreipunkt-Menü ⋮** auf der Hauptseite der App-Info hervor. **App-Info öffnen**
   führt direkt dorthin: **⋮ → Eingeschränkte Einstellungen zulassen**, falls nötig
   bestätigen; danach **Berechtigungen → SMS → Zulassen**. Die Markierung befindet
   sich in der Anleitung; fremde Hersteller-Systemseiten kann die App nicht
   verlässlich überlagern oder markieren. Rechte ändert ausschließlich der Nutzer.
4. Ist SMS danach weiterhin gesperrt oder fehlt eine freigebbare Auswahl, Modell,
   Android-Version und den genauen Systemtext festhalten. Mehrfach abgelehnte
   Anfragen, Installationsrestriktionen und Geräteverwaltung sind verschiedene
   Fälle. Die App kann Installations-/Administrationsrechte nicht selbst erteilen.

Ein gewöhnlicher Laufzeitdialog reicht für Androids streng eingeschränktes
SMS-Recht nur aus, wenn der verantwortliche Installer die Berechtigung zulässt.
Eine APK-Installation ist daher nicht auf jedem Telefon ein gleichwertiger
Vertriebsnachweis. Ein App-Update hebt eine solche Installationssperre nicht auf.
Die vorhandene App für das Update nicht deinstallieren; dabei gingen Regeln und
Verlauf verloren. Für eine nötige Änderung des Installationswegs erst den konkreten
Gerätezustand prüfen. Es ist kein Abschalten von Play Protect vorgesehen.

Grundlagen: [RECEIVE_SMS](https://developer.android.com/reference/android/Manifest.permission#RECEIVE_SMS),
[Android-Laufzeitberechtigungen](https://developer.android.com/training/permissions/requesting),
[Installationsabhängige Einschränkungen](https://source.android.com/docs/core/permissions/runtime_perms#hard-and-soft-restrictions-in-android-10).

## Alarm und Pause

Ein Treffer startet die native Ton-/Vibrationsschleife ohne eingebauten Timeout.
Weitere neue passende SMS bleiben als eigene offene Ereignisse erhalten. Mehrere
passende Regeln für dieselbe SMS erzeugen gemeinsam ein Ereignis. Kurzfristige,
identische Transportwiederholungen werden zwei Minuten lang dedupliziert; gleicher
Text in einer neu zugestellten SMS ist keine allgemeine Textsperre.

**Diesen Alarm quittieren** beendet nur das gewählte Ereignis. Gibt es weitere,
läuft die Alarmsteuerung für diese weiter. **Alle quittieren** ist eine gesonderte
bestätigte Aktion. App, Benachrichtigung und gegebenenfalls native Sperransicht
bieten Quittierung an. Sie gilt ausschließlich auf diesem Telefon; die App sendet
keine Rückmeldung an Arbeitgeber oder Leitstelle.

**App pausieren** verlangt eine Bestätigung, beendet alle offenen Alarme als
„durch App-Pause beendet“ und unterdrückt neue SMS-Alarme bis zur manuellen
Aktivierung. Während Pause wird keine Nachricht verarbeitet/protokolliert oder
später nachgeholt. Pause bleibt bei Prozessende, Neustart und Update bestehen.
Das Löschen einer Regel quittiert keinen bereits offenen Alarm.

## Verlauf und Datenschutz

Der Verlauf enthält Metadaten wie Regelname, maskierten Absender, SIM,
Empfang, Wiedergabestart und Beendigung. Er enthält keine vollständigen SMS-Texte
oder Roh-PDUs. Testalarme sind erkennbar. Abgeschlossene Ereignisse werden nach
7/30/90 Tagen und einer einstellbaren Anzahl begrenzt; offene Alarme werden nicht
von der Verlaufspflege gelöscht. **Verlauf leeren** löscht nur abgeschlossene
Ereignisse. Gespeicherte Filter können natürlich selbst sensible Angaben enthalten.

Keine Konten, Telemetrie, Werbung, Push-Dienste, Cloud oder App-Update-Verbindungen.
Die Test-Release-APK hat keine INTERNET-Berechtigung; SMS benötigt weiterhin einen
funktionierenden SMS-Mobilfunkdienst. Android und die normale Nachrichten-App
können eigene Verbindungen aufbauen und SMS in ihrem eigenen Speicher halten.
Die App-Daten sind privat gespeichert und von OS-Backup/Datenübertragung
programmseitig ausgeschlossen; das konkrete OEM-Verhalten ist Teil der Gerätetests.

Deinstallation beziehungsweise „Speicherinhalt löschen“ entfernt die App-Daten.
Es gibt keinen Cloud-Wiederherstellungsdienst. Idee/Design: Snadretto;
Codegenerierung: GPT-6.1 Sol. Lizenzen sind lokal unter Einstellungen abrufbar.

## Grenzen bei Problemen

- Nach Neustart zuerst entsperren. **Vor der ersten Entsperrung besteht keine
  Alarmbereitschaft.** Der Boot-Empfänger startet keinen Media-Foreground-Service.
  Noch offene Ereignisse werden nicht als quittiert behandelt; zum Fortsetzen
  App öffnen. Auch eine später regulär zugestellte passende SMS kann den Dienst starten.
- Nach **Stopp erzwingen** die App einmal öffnen. Android liefert einer so
  gestoppten App keine normale SMS-Bereitschaft; es erfolgt kein Postfach-Nachholen.
- Bei Rechteentzug, blockiertem Alarmkanal oder veränderten Systemeinstellungen
  Einrichtung erneut prüfen. „Aktiviert“ ist der Aktivierungswunsch und keine
  Zusage, dass jede Systemeinstellung funktioniert.
- Total-Silence-DND, Anruf, Bluetooth/Kopfhörer, feste Lautstärke, ausgeschaltetes
  Telefon, fehlende SMS-Zustellung und Firmen-/OEM-Richtlinien können Alarme
  verhindern oder umleiten. Nur tatsächlich getestete Profile später freigeben.
- Die App verarbeitet SMS, keine RCS-Nachrichten, MMS oder App-Mitteilungen.
  Eine Absenderkennung ist kein kryptografischer Echtheitsnachweis.
