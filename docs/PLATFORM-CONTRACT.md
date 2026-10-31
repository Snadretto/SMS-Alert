# Native Plattformanbindung v1

Android ist die verbindliche Datenquelle. Es gibt keinen zweiten Dart-Konfigurations-
oder Ereignisspeicher. Der SMS-/Alarmpfad startet keinen Flutter-Engine-Prozess.

- MethodChannel: `ch.snadretto.smsalert/methods_v1`
- EventChannel: `ch.snadretto.smsalert/events_v1`
- Standard-Codec: Strings, bool, Ganzzahlen, nullable Werte, Listen und String-Maps.
- `getSnapshot` liefert `schemaVersion: 1`, `supported`, `platform`, `settings`,
  `rules`, `pending`, `pendingCount`, `history`, `diagnostics`.
  Unbekannte Schema-Versionen werden sichtbar abgelehnt.
- Der Ereigniskanal sendet denselben Snapshot nach nativen Änderungen; bei Rückkehr
  in die App wird zusätzlich neu gelesen. Native Transaktionen serialisieren
  konkurrierende Receiver-/UI-Aktionen. Snapshots dürfen nachfolgende Zustände zeigen.

| Methode | Argumente / Ergebnis |
| --- | --- |
| `pickDeviceTone` | Öffnet ausschließlich den System-Klingeltonpicker; liefert `{id,title,uri}` für eine validierte private Kopie, bei Abbruch null. Kein Speichern der aktiven Einstellung bis `updateSettings`. |
| `getSnapshot` | Keine Argumente; vollständiger lokaler Zustand |
| `saveRule` | Regel-Map; leere ID wird nativ durch UUID ersetzt |
| `deleteRule` | `id`; bereits offene Ereignisse bleiben bestehen |
| `setEnabled` | `enabled`; Aktivierung braucht mindestens eine aktive Regel, Pause beendet alle offenen Ereignisse |
| `updateSettings` | Lautstärke, Vibration, Ton, Länder-/Aufbewahrungseinstellungen; ändert `enabled` ausdrücklich nicht |
| `previewRule` | `rule`, `sender`, `body`, nullable `simSlot`; bool, keine Speicherung/Alarmierung |
| `startTestAlarm` | Keine Argumente; verlangt aktivierte App und legt markiertes TEST-Ereignis an |
| `acknowledge` | Erwartete `id`; bereits abgeschlossene ID bleibt ohne Wirkung, andere Alarme bleiben offen |
| `acknowledgeAll` | Bewusste gesonderte Aktion für alle offenen Ereignisse |
| `resumeAlarm` | Versucht offene Alarme im zulässigen Vordergrundzustand fortzusetzen; erfindet kein Ereignis |
| `clearHistory` | Löscht nur abgeschlossene Ereignisse |
| `requestCorePermissions` | Nur fehlende SMS-/Benachrichtigungsrechte anfragen; tatsächliche Freigaben prüfen und Ablehnung als verständlichen Fehler zurückgeben |
| `requestSmsPermission` | Nur fehlenden SMS-Zugriff anfragen; keine gekoppelte Benachrichtigungsanfrage |
| `openSettings` | Nur definierte `kind`-Werte; keine frei übergebbaren URLs oder Intents |

Eine Regel hat `id`, `name`, `sender`, `text`, `logic` (AND/OR), `enabled`,
`caseSensitive`, `simSlot` (null, 0, 1). Namen und Kriterien sind begrenzt.
Sendernormalisierung: NFKC, numerische Telefonnummern mit explizitem Land nach
E.164, Kurz-/Textkennungen separat und exakt. Suchtext: literal, NFC-normalisiert,
optional Locale.ROOT-Kleinschreibung. Ein vorhandenes Kriterium genügt; bei zwei
gilt die gewählte Logik. Ein unbekannter SIM-Slot passt nie zu einem konkreten Filter.
Referenzfälle liegen in `ModelsTest.kt`.

Ereignisse enthalten UUID, Quelle SMS/TEST, Empfangs-/Sendezeiten, Regel-IDs/-Namen,
maskierten Absender, SIM, Status PENDING/ACKNOWLEDGED/PAUSED und bei Bedarf
Wiedergabestart, letzten Ausgang und Latenz. SMS-Text und Roh-PDUs verlassen die
native Verarbeitung nicht. Monotone Latenzwerte sind nur innerhalb desselben
bekannten Boot-Zyklus sinnvoll; sie beweisen keine Hörbarkeit oder Mobilfunklatenz.

Native Fehler werden mit stabilem Code und verständlicher deutscher Meldung
zurückgegeben (`invalid_rule`, `invalid_settings`, `no_rules`, `paused`, `storage`,
`busy`, `settings_unavailable`, …). Keine Roh-SMS oder nativen Stacktraces im
Fehlerpayload. Unbekannte Methoden werden als nicht implementiert gemeldet.

SQLite-Schema v2 migriert v1 ohne Löschen von Regeln, Pause oder Ereignissen.
Kurzfristige Transport-Fingerabdrücke sind mit einem privaten zufälligen HMAC-
Schlüssel geschützt. UI-/Service-Zugriffe laufen über dieselbe transaktionale
Datenquelle; Quittierung und Pause prüfen den aktuellen Ereignisstatus.

Auf iOS und anderen Plattformen liefert der Flutter-Adapter ausdrücklich
`supported=false`. Geräteaktionen werfen einen Unsupported-Fehler. Eine künftige
Swift-Anbindung braucht zunächst einen positiven fachlichen Plattformentscheid.

Ab 0.2.0 unterstützt `settings.tone` außerdem `device` und das optionale Objekt
`deviceTone` mit SHA-256-Asset-ID, Anzeigename und ursprünglicher lokaler URI
(für die Vorauswahl im Systempicker). Fehlende Felder aus 0.1.x bleiben null;
SQLite v2 und Channelschema v1 sind additiv kompatibel. Ungültige IDs, HTTP-/Datei-
URIs oder eine nicht vorhandene Kopie bei neuer Speicherung werden abgelehnt.
Beim späteren Alarmstart kann ein fehlendes Asset auf den gebündelten Signalton
zurückfallen. Der ursprüngliche URI wird im Hintergrund nicht erneut geöffnet.
Audioimport nutzt einen eigenen I/O-Executor, damit der SMS-Worker frei bleibt.
