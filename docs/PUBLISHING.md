# Veröffentlichung ohne Play Store

SMS-Alert wird zunächst als direkt installierbare APK über GitHub Releases
verteilt. Für 0.2.0 wird die bereits auf dem OPPO getestete Datei unverändert
verwendet. Es gibt keinen eigenen App-Server und keinen automatischen Updater.

## Vorbereiteter Release 0.2.0

- Version: `0.2.0+4`, Paket: `ch.snadretto.smsalert`, Android 12 oder neuer.
- Öffentlicher Dateiname: `sms-alert-0.2.0.apk`.
- Identisch mit `dist/sms-alert-0.2.0-test.apk`; lediglich der Downloadname ändert sich.
- APK-SHA-256: `ee5e148af3121242a50d172dc4499e9cbab35bf98d3bf8b3bd6d3cf3f23949eb`.
- Anwendungsquellstand: `560fc19b628862ffd1b486d891b53f3946097554`.
- Signaturzertifikat SHA-256: `201ddc780d7c63987847c2127b08971021b4303f2ba587b9b2eabc1e656847c8`.

Die APK ist im Release-Modus gebaut, nicht debuggable und besitzt keine
INTERNET-Berechtigung. Sie benutzt denselben privaten lokalen Signierschlüssel
wie die bisherigen Testversionen. Der Zertifikatsname enthält weiterhin „Test“;
ein Umbenennen der APK ändert weder Signatur noch Androids Vertrauensentscheidung.

Die Veröffentlichung ist als öffentliche Vorschau vorbereitet. Snadretto stellt
die verbleibenden Gerätetests vorerst zurück. Sie werden nicht als bestanden
markiert und blockieren diese Vorschau nicht. Bestätigte OPPO-Ergebnisse und
zurückgestellte Fälle stehen im [Anwenderbericht](testing/OPPO-Find-X9-user-2026-10-31.md).
Der bestehende PR #15 wird durch einen Release nicht automatisch gemergt.

## Entwurf veröffentlichen

1. Mit dem Besitzerkonto die [Releases](https://github.com/Snadretto/SMS-Alert/releases)
   öffnen und den Entwurf für `v0.2.0` bearbeiten.
2. Titel, [Versionshinweise](releases/0.2.0.md) und die vier Dateien prüfen:
   `sms-alert-0.2.0.apk`, `SHA256SUMS`, `source.json`, `INSTALLATION.md`.
3. Vor der Veröffentlichung den privaten Ordner `.tools/signing/` einschließlich
   Keystore und Properties-Datei separat geschützt sichern. Diese Dateien niemals
   als Release-Asset oder im Repository hochladen. Ein Schlüsselwechsel würde
   die normalen Updates bestehender Installationen verhindern.
4. **Publish release** wählen. Erst danach können andere den Release herunterladen.
5. Den Link `https://github.com/Snadretto/SMS-Alert/releases/tag/v0.2.0` teilen.
   Nutzer laden unter **Assets** die APK herunter; die automatisch angebotenen
   Quellcodearchive sind keine installierbare App.

Ein öffentliches Repository erlaubt Downloads ohne GitHub-Konto. Ein Entwurf ist
hingegen nur für berechtigte Repository-Mitwirkende sichtbar.

## Installation und Updates

Die beiliegende `INSTALLATION.md` ist eine Kopie der ausführlichen
[Bedienungsanleitung](USER-GUIDE.md). Nutzer erlauben die Installation aus ihrer
Downloadquelle, öffnen die APK und richten anschließend SMS-Zugriff,
Benachrichtigungen, Alarmton und eine Regel ein. Neuinstallationen starten pausiert.
Bei eingeschränkten Einstellungen zeigt die App den Weg über App-Info → ⋮ →
Eingeschränkte Einstellungen zulassen. Rechte werden durch den Nutzer erteilt.

Updates werden manuell als neue APK installiert, ohne die App vorher zu
deinstallieren. Paketname und Signierschlüssel bleiben erhalten; der Buildcode
muss steigen. Die erneute Installation derselben 0.2.0 ist kein Funktionsupdate.
Keine CI-APK mit dem kurzlebigen Debug-Testzertifikat als Update veröffentlichen.

## Android-Entwicklerverifizierung

Die Entwickler-/Paketregistrierung ist von der Veröffentlichung bei GitHub und
von der SMS-Berechtigung getrennt. Google führt die Verifizierung regional ein
und plant die weltweite Ausweitung ab 2027. Für Verteilung außerhalb Google Play
ist die Android Developer Console vorgesehen. Snadretto muss den Kontotyp und
die Identitätsprüfung selbst erledigen; registriert werden Paketname und
öffentlicher Zertifikatsfingerabdruck. Ein Besitznachweis kann eine gesondert
signierte APK mit einer von Google bereitgestellten Challenge verlangen.
Der private Signierschlüssel wird dafür nicht herausgegeben.

Die Registrierung ersetzt keine SMS-Freigabe und garantiert nicht, dass ein
Hersteller den Zusatzschritt für eingeschränkte Einstellungen entfallen lässt.

Quellen, geprüft am 01.11.2026:
[GitHub-Releases verwalten](https://docs.github.com/en/repositories/releasing-projects-on-github/managing-releases-in-a-repository),
[Android-Entwicklerverifizierung und Zeitplan](https://developer.android.com/developer-verification),
[Android Developer Console](https://developer.android.com/developer-verification/guides/android-developer-console),
[Eingeschränkte Einstellungen](https://support.google.com/android/answer/12623953?hl=de).
