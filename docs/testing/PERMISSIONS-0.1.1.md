# SMS-Freigabe: Test-Update 0.1.1

Anwenderbericht: „SMS empfangen“ bleibt mit Ausrufezeichen markiert; passende SMS
alarmieren nicht. Handymodell, Android-Version und genaue Systemreaktion wurden
noch nicht mitgeteilt. Eine bestimmte Installations-/OEM-Ursache ist daher nicht
bestätigt. Das Ausrufezeichen stammt aus dem tatsächlichen Android-Berechtigungs-
status, es wird nicht durch einen fingierten Haken ersetzt.

## Änderung

- Native Permission-Anfragen prüfen anschließend die effektiv gewährten Rechte.
  Ablehnung, leeres Ergebnis und automatische Verweigerung werden als verständlicher
  Fehler zurückgegeben. Bereits gewährte Rechte werden nicht erneut angefragt.
- Die SMS-Karte fragt gezielt `RECEIVE_SMS` an; der vorhandene kombinierte Button
  kann weiterhin fehlende SMS-/Benachrichtigungsrechte anfragen.
- „App-Berechtigungen öffnen“ führt direkt zur Android-App-Info. Von dort lässt
  sich eine manuell freigebbare SMS-Berechtigung ändern, auch wenn Android den
  Dialog nach mehrfacher Ablehnung nicht mehr anzeigt.
- Die automatische Aktualisierung bei Rückkehr aus dem Systemdialog löscht eine
  gerade empfangene Ablehnungsmeldung nicht mehr. Manuelle Aktualisierung und
  die nächste bewusste Aktion können die Meldung wie bisher quittieren.
- Version `0.1.1+3` nutzt dasselbe lokale Testzertifikat. Als Update installieren;
  Regeln, Pause und Verlauf bleiben erhalten. Keine Deinstallation nötig.

## Prüfungen

- Flutter-Analyse ohne Befunde und alle zehn Flutter-/Widgettests bestanden.
- Release-Modus-APK gebaut; unverändert elf Systemrechte und ein privates
  Signaturrecht, kein INTERNET/READ_SMS/SEND_SMS. apksigner bestätigt die Signatur.
- [Sechs Tests echter Android-17-Permission-Dialoge](permissions-0.1.1-api37.json),
  ohne `pm grant`: erste Ablehnung, dauerhafte Ablehnung ohne weiteren Dialog,
  Öffnen der App-Info, manuelle Freigabe unter Android und Aktualisierung des
  Hakens, Erlauben im ersten SMS-Dialog, Installer-Sperre.
- Die Installer-Sperre wurde auf einem frisch installierten Testpaket mit
  `adb install --restrict-permissions` erzeugt. Android meldete `APPLY_RESTRICTION`
  und `granted=false`; die App behauptet keine erteilte Freigabe. Anschließend
  stellt der Testtreiber eine normale, frische Emulatorinstallation wieder her.
- [Zehn Android-12-Smoke-Szenarien](smoke-0.1.1-api31.json) bestanden, einschließlich
  SMS-Kaltstart, Alarmquittierung, Pause und Update-Erhalt.

Die reproduzierbaren Dialogtests liegen in `tools/emulator_permissions.py` und
verweigern echte Telefone. Die [Artefaktzuordnung](artifact-0.1.1.json) enthält
Versionsnummer, Quellstand und die Prüfsumme der tatsächlich getesteten APK.
Die vorherigen 0.1.0-Berichte bleiben als historische Nachweise erhalten.

## Verbleibende Grenze

Android stuft `RECEIVE_SMS` als streng eingeschränkt ein: Der verantwortliche
Installer muss die Berechtigung zulassen, bevor der normale Nutzerentscheid
wirksam werden kann. Eine gesperrte Installations-/Geräterichtlinie kann die
App nicht selbst aufheben. Das Update verbessert Anfrage, Erklärung und
Wiederherstellungsweg; es erteilt diese Systemrechte nicht selbst.

Auf dem Anwendertelefon sind die SMS-Auswahl unter App-Info und gegebenenfalls
Installationsquelle/Systemtext zu prüfen. #9 bleibt bis zur Bestätigung offen.
Die bisherigen Grenzen für reale SMS, OEM-/DND-Profile und Pikettfreigabe gelten weiter.

Grundlagen: [RECEIVE_SMS](https://developer.android.com/reference/android/Manifest.permission#RECEIVE_SMS),
[Anfragen und dauerhafte Ablehnung](https://developer.android.com/training/permissions/requesting),
[Streng eingeschränkte Rechte](https://source.android.com/docs/core/permissions/runtime_perms#hard-and-soft-restrictions-in-android-10).
